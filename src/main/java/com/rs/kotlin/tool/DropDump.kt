package com.rs.kotlin.tool

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.text.RegexOption

/**
 * WikiDrops.kt (corrected emitter & parser)
 *
 * Replaces the emitter/parser so generated Kotlin matches your DSL style exactly.
 *
 * Usage:
 *   ./gradlew :tools:run --args="Black Dragon"
 *
 * Output:
 *   generated/drops/BlackDragonDropTable.kt
 */

object WikiDrops {
    private val client = OkHttpClient()
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    private val outDir = File("generated/drops")
    private val cacheFile = File("data/drops/drops_data.json")
    private val failedFile = File("data/drops/drops_failed.json")

    private val dropsCache: MutableMap<String, DropData> = ConcurrentHashMap()
    private val failedCache: MutableMap<String, FailureEntry> = ConcurrentHashMap()

    init {
        outDir.mkdirs()
        cacheFile.parentFile.mkdirs()
        if (cacheFile.exists()) {
            val type = object : com.google.gson.reflect.TypeToken<Map<String, DropData>>() {}.type
            @Suppress("UNCHECKED_CAST")
            val loaded = gson.fromJson<Map<String, DropData>>(cacheFile.readText(), type)

            for ((npc, data) in loaded) {
                if (data.sections != null) {
                    dropsCache[npc] = data
                } else {
                    println("⚠ Dropping legacy cache entry for $npc (null sections)")
                }
            }
        }
        if (failedFile.exists()) {
            val type = object : com.google.gson.reflect.TypeToken<Map<String, FailureEntry>>() {}.type
            failedCache.putAll(gson.fromJson(failedFile.readText(), type))
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        if (args.isEmpty()) {
            println("Usage: WikiDrops <NPC Name>")
            return
        }

        val npcName = args.joinToString(" ")
        println("🔎 Generating drops for: $npcName")

        if (dropsCache.containsKey(npcName)) {
            println("⚠ Drops already cached for $npcName; regenerating file from cache.")
            writeKotlinFile(dropsCache[npcName]!!)
            return
        }

        if (failedCache.containsKey(npcName)) {
            println("⚠ Previous attempt failed: ${failedCache[npcName]?.reason}")
        }

        when (val fetch = fetchWikitext(npcName)) {
            is FetchResult.Failure -> {
                markFailure(npcName, fetch.reason, fetch.details)
                println("❌ Failed to fetch wikitext: ${fetch.reason}")
            }
            is FetchResult.Success -> {
                val parsed = DropParser.parseAll(npcName, fetch.wikitext)

                if (parsed.sections.isEmpty()) {
                    markFailure(npcName, "No drop data parsed")
                    println("⚠ No drop templates found.")
                } else {
                    dropsCache[npcName] = parsed
                    persistCache()
                    failedCache.remove(npcName)?.let { persistFailures() }

                    writeKotlinFile(parsed)
                    println("✅ Drop table generated: ${sanitizeClassName(parsed.npcName)}DropTable.kt")
                }
            }
        }
    }

    // ---------- networking ----------
    sealed class FetchResult {
        data class Success(val wikitext: String) : FetchResult()
        data class Failure(val reason: String, val details: String? = null) : FetchResult()
    }

    private fun fetchWikitext(npcName: String): FetchResult {
        val encoded = URLEncoder.encode(npcName, StandardCharsets.UTF_8)
        val url = "https://oldschool.runescape.wiki/api.php?action=parse&prop=wikitext&format=json&redirects=1&page=$encoded"

        println("🔹 Fetching: $url")

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "AvalonBot/1.0 (contact: example@example.com)")
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return FetchResult.Failure("HTTP ${response.code}", response.message)
                }
                val body = response.body?.string() ?: return FetchResult.Failure("Empty body")
                val json = Gson().fromJson(body, JsonObject::class.java)
                val wikitext = json["parse"]?.asJsonObject
                    ?.get("wikitext")?.asJsonObject
                    ?.get("*")?.asString

                if (wikitext.isNullOrBlank()) return FetchResult.Failure("'wikitext' not found")
                FetchResult.Success(wikitext)
            }
        } catch (e: Exception) {
            FetchResult.Failure("Request exception", e.message)
        }
    }

    // ---------- models ----------
    data class DropData(
        val npcName: String,
        val sections: Map<String, List<DropEntry>>,
        val rareDropTable: Boolean = false
    )

    data class DropEntry(
        val name: String,
        val qMin: Int? = null,
        val qMax: Int? = null,
        val weight: Int? = null,
        val numerator: Int? = null,
        val denominator: Int? = null,
        val rawRarity: String? = null,
        val rawFields: Map<String, String> = emptyMap()
    )

    data class FailureEntry(
        val npcName: String,
        val reason: String,
        val details: String?,
        val firstSeen: String,
        val lastSeen: String,
        val attempts: Int
    )

    private fun markFailure(npcName: String, reason: String, details: String? = null) {
        val now = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
        val existing = failedCache[npcName]
        val entry = if (existing == null) {
            FailureEntry(npcName, reason, details, now, now, 1)
        } else {
            existing.copy(reason = reason, details = details ?: existing.details, lastSeen = now, attempts = existing.attempts + 1)
        }
        failedCache[npcName] = entry
        persistFailures()
    }

    private fun persistCache() {
        val snapshot = TreeMap(dropsCache)
        cacheFile.writeText(gson.toJson(snapshot))
    }

    private fun persistFailures() {
        failedFile.parentFile.mkdirs()
        failedFile.writeText(gson.toJson(failedCache.toSortedMap()))
    }

    // ---------- parser & corrected emitter ----------
    object DropParser {
        // heading sections
        private val sectionRegex =
            Regex("""(?m)^===[\t ]*(.+?)[\t ]*===(.*?)((?m)^[=]{3}|$)""", RegexOption.DOT_MATCHES_ALL)

        // capture DropsLine and DropsLineClue
        private val dropLineRegex = Regex("""\{\{(DropsLine(?:Clue)?)[\|]([^}]*)\}\}""")
        private val tableHeadRegex = Regex("""\{\{DropsTableHead(?:\|([^}]*))?}}""")
        private val tableBottomRegex = Regex("""\{\{DropsTableBottom}}""")
        private val rareDropRegex = Regex("""\{\{RareDropTable\|([^}]*)}}""")

        fun parseAll(npcName: String, wikitext: String): DropData {
            val sections = LinkedHashMap<String, LinkedHashSet<DropEntry>>() // preserve ordering & dedupe
            var rdt = false
            if (rareDropRegex.containsMatchIn(wikitext)) rdt = true

            // 1) parse heading sections
            for (m in sectionRegex.findAll(wikitext)) {
                val title = m.groupValues[1].trim()
                val body = m.groupValues[2]
                val entries = parseSection(body)
                if (entries.isNotEmpty()) sections.computeIfAbsent(title) { LinkedHashSet() } += entries
            }

            // 2) explicit DropsTableHead..Bottom blocks, but only Regular or un-specified dropversion
            var searchIndex = 0
            while (true) {
                val headMatch = tableHeadRegex.find(wikitext, searchIndex) ?: break
                val headParams = headMatch.groupValues.getOrNull(1) ?: ""
                val dropVersion = extractDropVersion(headParams)
                val tail = tableBottomRegex.find(wikitext, headMatch.range.last) ?: break
                val block = wikitext.substring(headMatch.range.first, tail.range.last)
                val nearestHeading = sectionRegex.findAll(wikitext).lastOrNull { it.range.first < headMatch.range.first }
                val sectionName = nearestHeading?.groupValues?.get(1)?.trim() ?: "Main"

                if (dropVersion.isNotBlank() && !dropVersion.equals("Regular", ignoreCase = true)) {
                    searchIndex = tail.range.last + 1
                    continue
                }

                val entries = parseSection(block)
                if (entries.isNotEmpty()) sections.computeIfAbsent(sectionName) { LinkedHashSet() } += entries
                searchIndex = tail.range.last + 1
            }

            // fallback
            if (sections.isEmpty()) {
                val whole = parseSection(wikitext)
                if (whole.isNotEmpty()) sections["Main"] = LinkedHashSet(whole)
            }

            return DropData(npcName, sections.mapValues { it.value.toList() }, rareDropTable = rdt)
        }

        private fun extractDropVersion(paramStr: String): String {
            if (paramStr.isBlank()) return ""
            // params like "dropversion=Regular"
            val parts = paramStr.split("|").map { it.trim() }
            for (p in parts) {
                if (p.lowercase().startsWith("dropversion")) {
                    val idx = p.indexOf('=')
                    if (idx != -1) return p.substring(idx + 1).trim()
                }
            }
            return ""
        }

        private fun parseSection(body: String): List<DropEntry> {
            val list = mutableListOf<DropEntry>()

            dropLineRegex.findAll(body).forEach { match ->
                val templateName = match.groupValues[1] // DropsLine or DropsLineClue
                val inner = match.groupValues[2]

                // simple field parse (good enough for DropsLine templates)
                val fields = inner.split("|").mapNotNull { part ->
                    val idx = part.indexOf('=')
                    if (idx == -1) null else part.substring(0, idx).trim() to part.substring(idx + 1).trim()
                }.toMap()

                // cleanup name; for DropsLineClue, name may be absent and type field used
                val isClue = templateName.equals("DropsLineClue", ignoreCase = true)

                val name =
                    if (isClue) {
                        // Preserve clue semantics; NEVER let tier become an item name
                        "Clue scroll (${fields["type"] ?: "unknown"})"
                    } else {
                        (fields["name"] ?: fields["item"] ?: "")
                            .trim()
                            .ifEmpty { "UNKNOWN" }
                    }

                val qtyRaw = fields["quantity"] ?: fields["qty"] ?: "1"
                val rarityRaw = fields["rarity"] ?: fields["raritynotes"] ?: ""

                val (qMin, qMax) = parseQuantity(qtyRaw)
                val parsedRarity = parseRarity(rarityRaw)

                val entry = when (parsedRarity) {
                    is ParsedRarity.Always -> DropEntry(name, qMin, qMax, rawRarity = "Always", rawFields = fields)
                    is ParsedRarity.Weight -> DropEntry(name, qMin, qMax, weight = parsedRarity.weight, denominator = 128, rawRarity = rarityRaw, rawFields = fields)
                    is ParsedRarity.Tertiary -> DropEntry(name, qMin, qMax, numerator = parsedRarity.numerator, denominator = parsedRarity.denominator, rawRarity = rarityRaw, rawFields = fields)
                }
                list.add(entry)
            }

            return list
        }

        private fun parseQuantity(raw: String): Pair<Int?, Int?> {
            val cleaned = raw.replace("&ndash;", "–").trim()
            return when {
                cleaned.contains("–") -> {
                    val p = cleaned.split("–").map { it.trim() }
                    p.getOrNull(0)?.toIntOrNull() to p.getOrNull(1)?.toIntOrNull()
                }
                cleaned.contains("-") && cleaned.count { it == '-' } == 1 -> {
                    val p = cleaned.split("-").map { it.trim() }
                    p.getOrNull(0)?.toIntOrNull() to p.getOrNull(1)?.toIntOrNull()
                }
                cleaned.isBlank() -> null to null
                else -> cleaned.toIntOrNull() to cleaned.toIntOrNull()
            }
        }

        // Strip '!' prefixes and normalize
        private sealed class ParsedRarity {
            object Always : ParsedRarity()
            data class Weight(val weight: Int) : ParsedRarity()
            data class Tertiary(val numerator: Int, val denominator: Int) : ParsedRarity()
        }

        private fun parseRarity(raw: String?): ParsedRarity {
            if (raw == null) return ParsedRarity.Weight(DEFAULT_COMMON_WEIGHT)
            // strip bangs that appear in some pages (!) and trim
            val cleaned = raw.replace("!", "").lowercase().trim()
            if (cleaned.isEmpty()) return ParsedRarity.Weight(DEFAULT_COMMON_WEIGHT)
            if ("always" in cleaned) return ParsedRarity.Always

            val frac = Regex("""(\d+)\s*/\s*(\d+)""").find(cleaned)
            if (frac != null) {
                val n = frac.groupValues[1].toIntOrNull() ?: 1
                val d = frac.groupValues[2].toIntOrNull() ?: 128
                return if (d == 128) ParsedRarity.Weight(n.coerceAtLeast(1)) else ParsedRarity.Tertiary(n.coerceAtLeast(1), d.coerceAtLeast(1))
            }

            return when {
                "common" in cleaned -> ParsedRarity.Weight(COMMON_WEIGHT)
                "uncommon" in cleaned -> ParsedRarity.Weight(UNCOMMON_WEIGHT)
                "very rare" in cleaned -> ParsedRarity.Tertiary(1, VERY_RARE_DENOM)
                "rare" in cleaned && !("very" in cleaned) -> ParsedRarity.Weight(RARE_WEIGHT)
                else -> ParsedRarity.Weight(DEFAULT_COMMON_WEIGHT)
            }
        }

        private const val COMMON_WEIGHT = 10
        private const val UNCOMMON_WEIGHT = 4
        private const val RARE_WEIGHT = 1
        private const val DEFAULT_COMMON_WEIGHT = COMMON_WEIGHT
        private const val VERY_RARE_DENOM = 5000
    }

    // -------------------------------------------------------------
    // File Output Helper + sanitizers + condition helpers
    // -------------------------------------------------------------
    private fun writeKotlinFile(data: DropData) {
        val className = sanitizeClassName(data.npcName) + "DropTable"
        val file = File(outDir, "$className.kt")
        val code = DslEmitter.emit(data)

        file.parentFile.mkdirs()
        file.writeText(code)
    }

    private fun sanitizeClassName(name: String): String =
        name.replace(Regex("[^A-Za-z0-9]"), "")
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

    private fun formatConditionFromFields(fields: Map<String, String>): String? {
        // slayerlevel detection
        val slayerLevel = fields["slayerlvl"]?.toIntOrNull()
            ?: fields["slayerlevel"]?.toIntOrNull()
            ?: fields["slayer"]?.toIntOrNull()
        if (slayerLevel != null) return "player.skills.getLevel(Skills.SLAYER) >= $slayerLevel"

        // wilderness: look in raritynotes or leagueRegion
        val rn = (fields["raritynotes"] ?: fields["notes"] ?: fields["leagueRegion"] ?: "")
        if (rn.lowercase().contains("wilderness") || fields["leagueRegion"]?.lowercase()?.contains("wilderness") == true) {
            return "player.world?.isWilderness == true"
        }

        // Konar / slayer task references - emit TODO placeholder (you can replace)
        val rnLower = rn.lowercase()
        if (rnLower.contains("konar") || rnLower.contains("slayer task")) {
            return "false /* TODO: Konar slayer task check */"
        }

        return null
    }

    // ---------- corrected emitter ----------
    object DslEmitter {
        private val canonicalNameOverrides: Map<String, String> = mapOf(
            // common wiki -> server item name fixes
            "mithril axe" to "mithril_hatchet",
            "mithril axe(s)" to "mithril_hatchet",
            "adamantite bar" to "adamant_bar",
            "adamantite bar (noted)" to "adamant_bar",
            "dragon javelin tips" to "dragon_javelin_tips",
            "ensouled dragon head" to "ensouled_dragon_head",
            "draconic visage" to "draconic_visage",
            "starved ancient effigy" to "starved_ancient_effigy",
            "black dragonhide" to "black_dragonhide",
            "vile ashes" to "vile_ashes",
            "infernal ashes" to "infernal_ashes"
            // add more overrides as you discover mismatches
        )

        fun emit(data: DropData): String {
            val b = StringBuilder()
            val className = "${sanitizeClassName(data.npcName)}DropTable"
            val fieldName = camelToFieldName(data.npcName)

            b.appendLine("package com.rs.kotlin.game.npc.drops.tables")
            b.appendLine()
            b.appendLine("import com.rs.java.game.player.Skills")
            b.appendLine("import com.rs.java.game.player.content.treasuretrails.TreasureTrailsManager")
            b.appendLine("import com.rs.kotlin.game.npc.drops.dropTable")
            b.appendLine()
            b.appendLine("object $className {")
            b.appendLine()
            b.appendLine("    val $fieldName = dropTable(rareDropTable = ${data.rareDropTable}, rolls = 1) {")
            b.appendLine()

            // Flatten in input order (sections preserved)
            val flat = data.sections.values.flatten()

            // classify items into sets preserving order and deduping by signature
            val always = LinkedHashMap<String, DropEntry>()
            val charms = LinkedHashMap<String, DropEntry>()
            val main = LinkedHashMap<String, DropEntry>()
            val tertiary = LinkedHashMap<String, DropEntry>()
            val preRoll = LinkedHashMap<String, DropEntry>()

            // helper signature so duplicates are deterministic
            fun signature(e: DropEntry): String {
                return listOf(e.name.lowercase(), e.qMin?.toString() ?: "", e.qMax?.toString() ?: "", e.weight?.toString() ?: "", e.numerator?.toString() ?: "", e.denominator?.toString() ?: "")
                    .joinToString("|")
            }

            // classify
            for (e in flat) {
                val sig = signature(e)
                val nameLower = e.name.lowercase()
                val isClue =
                    e.rawFields["type"]?.lowercase()?.let {
                        it.contains("easy") ||
                                it.contains("medium") ||
                                it.contains("hard") ||
                                it.contains("elite") ||
                                it.contains("master")
                    } == true
                val guaranteed = (e.rawRarity?.equals("Always", true) == true) || isGuaranteedName(e.name)

                // pre-roll detection: wiki uses "Pre-roll" heading; we can also detect gemw/pre-roll flags, but prefer heading -> user requested "only when present"
                val isPreRoll = (e.rawFields["gemw"] == "No" && e.rawFields.containsKey("pre-roll")) || false

                // charms detection
                when (nameLower) {
                    "gold charm", "green charm", "crimson charm", "blue charm" -> {
                        if (!charms.containsKey(sig)) charms[sig] = e
                        continue
                    }
                }

                // Pre-roll explicitly into preRoll if pre-roll marker exists
                if (isPreRoll) {
                    if (!preRoll.containsKey(sig)) preRoll[sig] = e
                    continue
                }

                // Clues always tertiary
                if (isClue) {
                    if (!tertiary.containsKey(sig)) tertiary[sig] = e
                    continue
                }

                // Always items
                if (guaranteed) {
                    if (!always.containsKey(sig)) always[sig] = e
                    continue
                }

                // Tertiary by denominator != 128 (or explicit tertiary numerator/den)
                if ((e.denominator != null && e.denominator != 128) || (e.numerator != null && e.denominator != null && e.denominator != 128)) {
                    if (!tertiary.containsKey(sig)) tertiary[sig] = e
                    continue
                }

                // Main by weight preferentially
                if (e.weight != null) {
                    if (!main.containsKey(sig)) main[sig] = e
                    continue
                }

                // fallback to main
                if (!main.containsKey(sig)) main[sig] = e
            }

            // Emit blocks in your order: always, main, tertiary (with charm and preRoll in right places)
            if (always.isNotEmpty()) {
                b.appendLine("        alwaysDrops {")
                for (entry in always.values) {
                    val item = mapToCanonical(entry.name)
                    val amt = amountText(entry)
                    b.appendLine("            drop(\"item.$item\"$amt)")
                }
                b.appendLine("        }")
                b.appendLine()
            }

            if (charms.isNotEmpty()) {
                b.appendLine("        charmDrops {")
                for (entry in charms.values) {
                    val percent = entry.rawFields["percent"]?.toDoubleOrNull() ?: extractCharmPercentFromRarity(entry.rawRarity)
                    val fn = when (entry.name.lowercase()) {
                        "gold charm" -> "gold"
                        "green charm" -> "green"
                        "crimson charm" -> "crimson"
                        "blue charm" -> "blue"
                        else -> "gold"
                    }
                    val pctText = percent?.let { stripTrailingZeros(it) } ?: "TODO"
                    b.appendLine("            $fn(amount = 1, percent = $pctText)")
                }
                b.appendLine("        }")
                b.appendLine()
            }

            if (main.isNotEmpty()) {
                b.appendLine("        mainDrops(128) {")
                for (entry in main.values) {
                    val item = mapToCanonical(entry.name)
                    val amt = amountText(entry)
                    val weight = if (entry.weight != null) ", weight = ${entry.weight}" else ""
                    b.appendLine("            drop(\"item.$item\"$amt$weight)")
                }
                b.appendLine("        }")
                b.appendLine()
            }

            if (tertiary.isNotEmpty()) {
                b.appendLine("        tertiaryDrops {")
                for (entry in tertiary.values) {
                    val mapped = if (isClueLike(entry)) mapClueNameIfNeeded(entry) else mapToCanonical(entry.name)
                    val num = entry.numerator ?: 1
                    val den = entry.denominator ?: 1
                    val cond = if (isClueLike(entry)) {
                        // clue -> treasure trails condition based on clue type
                        val t = (entry.rawFields["type"] ?: entry.name).lowercase()
                        when {
                            t.contains("hard") -> "!player.treasureTrailsManager.hasClueScrollByLevel(TreasureTrailsManager.HARD)"
                            t.contains("elite") -> "!player.treasureTrailsManager.hasClueScrollByLevel(TreasureTrailsManager.ELITE)"
                            t.contains("medium") -> "!player.treasureTrailsManager.hasClueScrollByLevel(TreasureTrailsManager.MEDIUM)"
                            t.contains("easy") -> "!player.treasureTrailsManager.hasClueScrollByLevel(TreasureTrailsManager.EASY)"
                            t.contains("master") -> "!player.treasureTrailsManager.hasClueScrollByLevel(TreasureTrailsManager.MASTER)"
                            else -> null
                        }
                    } else {
                        formatConditionFromFields(entry.rawFields)
                    }

                    if (cond != null) {
                        b.appendLine("            drop(")
                        b.appendLine("                \"item.$mapped\",")
                        b.appendLine("                numerator = $num,")
                        b.appendLine("                denominator = $den,")
                        b.appendLine("                condition = { player -> $cond }")
                        b.appendLine("            )")
                    } else {
                        b.appendLine("            drop(\"item.$mapped\", numerator = $num, denominator = $den)")
                    }
                }
                b.appendLine("        }")
                b.appendLine()
            }

            if (preRoll.isNotEmpty()) {
                b.appendLine("        preRollDrops {")
                for (entry in preRoll.values) {
                    val item = mapToCanonical(entry.name)
                    val cond = formatConditionFromFields(entry.rawFields)
                    if (cond != null) {
                        b.appendLine("            drop(\"item.$item\", condition = { player -> $cond })")
                    } else {
                        b.appendLine("            drop(\"item.$item\")")
                    }
                }
                b.appendLine("        }")
                b.appendLine()
            }

            b.appendLine("    }.apply { name = \"${escapeQuote(data.npcName)}\" }")
            b.appendLine("}")
            return b.toString()
        }

        private fun mapToCanonical(rawName: String): String {
            val key = rawName.lowercase().trim()
            canonicalNameOverrides[key]?.let { return it }
            // general sanitize
            return rawName.lowercase()
                .replace(Regex("""\s+"""), "_")
                .replace("(", "")
                .replace(")", "")
                .replace("’", "")
                .replace("'", "")
                .replace(",", "")
                .replace("--", "_")
                .replace("–", "_")
                .replace("/", "_")
                .replace(Regex("[^a-z0-9_]+"), "")
        }

        private fun isClueLike(e: DropEntry): Boolean {
            val l = e.name.lowercase()
            return l.contains("clue") || l.contains("scroll") || l.contains("casket") || (e.rawFields["type"]?.lowercase()?.contains("clue") == true)
        }

        private fun mapClueNameIfNeeded(e: DropEntry): String {
            val lower = (e.rawFields["type"] ?: e.name).lowercase()
            return when {
                lower.contains("easy") -> "scroll_box_easy"
                lower.contains("medium") -> "scroll_box_medium"
                lower.contains("hard") -> "scroll_box_hard"
                lower.contains("elite") -> "scroll_box_elite"
                lower.contains("master") -> "scroll_box_master"
                else -> sanitizeItem(e.name)
            }
        }

        private fun isGuaranteedName(name: String): Boolean {
            val l = name.lowercase()
            return l.contains("bones") || l.contains("ashes") || l.contains("vile ashes") || l.contains("infernal ashes")
        }

        private fun sanitizeItem(name: String): String =
            name.lowercase()
                .replace(Regex("""\s+"""), "_")
                .replace("(", "")
                .replace(")", "")
                .replace("’", "")
                .replace("'", "")
                .replace(",", "")
                .replace("--", "_")
                .replace("–", "_")
                .replace("/", "_")
                .replace(Regex("[^a-z0-9_]+"), "")

        private fun amountText(e: DropEntry): String {
            return if (e.qMin != null) {
                if (e.qMax != null && e.qMin != e.qMax) {
                    ", amount = ${e.qMin}..${e.qMax}"
                } else if (e.qMin != 1) {
                    ", amount = ${e.qMin}"
                } else ""
            } else ""
        }

        private fun extractCharmPercentFromRarity(rawRarity: String?): Double? {
            if (rawRarity == null) return null
            val pctMatch = Regex("""([\d.]+)\s*%""").find(rawRarity)
            if (pctMatch != null) return pctMatch.groupValues[1].toDoubleOrNull()
            return null
        }

        private fun sanitizeClassName(name: String): String =
            name.replace(Regex("[^A-Za-z0-9]"), "").replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

        private fun camelToFieldName(name: String): String {
            val s = name.replace(Regex("[^A-Za-z0-9]"), "")
            return s.replaceFirstChar { it.lowercase(Locale.getDefault()) } + "Table"
        }

        private fun escapeQuote(s: String) = s.replace("\"", "\\\"")

        private fun stripTrailingZeros(d: Double): String {
            val s = d.toString()
            return if (s.contains('.')) s.trimEnd('0').trimEnd('.') else s
        }
    }
}
