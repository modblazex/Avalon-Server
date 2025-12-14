package com.rs.java.game.player.prayer;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;
import java.util.function.Function;

import com.rs.java.game.Animation;
import com.rs.java.game.Entity;
import com.rs.java.game.Graphics;
import com.rs.java.game.minigames.clanwars.ClanWars;
import com.rs.java.game.minigames.clanwars.ClanWars.Rules;
import com.rs.java.game.npc.NPC;
import com.rs.java.game.player.CombatDefinitions;
import com.rs.java.game.player.Player;
import com.rs.java.game.player.Skills;
import com.rs.java.game.player.TickManager;
import com.rs.kotlin.Rscm;

public class PrayerBook implements Serializable {
    @Serial
    private static final long serialVersionUID = -2082861520556582824L;

    private static final int PRAYER_POINTS_PER_LEVEL = 10;
    private static final int MAX_LEECH_BONUS = 15;
    private static final int MIN_LEECH_BONUS = -15;
    private static final int DEFENCE_REQUIREMENT_ANCIENT = 30;
    private static final int[] STAT_VARBIT_IDS = {
            Rscm.lookup("varbit.prayerbook_attack_stat"),
            Rscm.lookup("varbit.prayerbook_strength_stat"),
            Rscm.lookup("varbit.prayerbook_defence_stat"),
            Rscm.lookup("varbit.prayerbook_range_stat"),
            Rscm.lookup("varbit.prayerbook_magic_stat")};

    private transient Player player;
    private PrayerBookType activeBook = PrayerBookType.NORMAL;
    private boolean usingQuickPrayer = false;

    private int prayerPoints;
    private final boolean[] quickNormalPrayers = new boolean[NormalPrayer.values().length];
    private final boolean[] quickAncientPrayers = new boolean[AncientPrayer.values().length];

    private transient final EnumSet<NormalPrayer> activeNormalPrayers = EnumSet.noneOf(NormalPrayer.class);
    private transient final EnumSet<AncientPrayer> activeAncientPrayers = EnumSet.noneOf(AncientPrayer.class);

    private transient final int[] leechBonuses = new int[5];
    private transient final int[] turmoilBonuses = new int[3];
    private transient boolean turmoilActive = false;

    private transient final Map<Integer, Long> lastLeechTimes = new HashMap<>();

    public PrayerBook() {
        this.prayerPoints = 10;
    }

    public void onLogin() {
        initializePrayerBook();
    }

    public void initializePrayerBook() {
        if (player != null && player.isActive()) {
            refresh();
            resetLeechBonuses();
        }
    }


    public void setPlayer(Player player) {
        this.player = player;
    }

    private static final boolean DEBUG = true;

    private void debug(String message) {
        if (DEBUG) {
            player.message("[Prayer Debug] " + message);
            System.out.println("[Prayer Debug] " + message);
        }
    }

    private void debugPrayerState(String action, Prayer prayer) {
        if (!DEBUG) return;

        StringBuilder sb = new StringBuilder();
        sb.append("[Prayer Debug] ").append(action).append(": ").append(prayer != null ? prayer.getName() : "null");
        sb.append("\n, Current Active Prayers:");

        for (Prayer activePrayer : getActivePrayers()) {
            sb.append("\n- ").append(activePrayer.getName());
        }

        player.message(sb.toString());
        System.out.println(sb);
    }

    public boolean hasFullPrayerPoints() {
        return prayerPoints >= getMaxPrayerPoints();
    }

    public boolean isActive(Prayer prayer) {
        if (prayer == null) {
            return false;
        }
        return getActivePrayerSet().contains(prayer);
    }

    public boolean handlePrayerClick(int componentId, int slotId) {
        if (componentId == Rscm.lookup("component.prayerbook:prayer_slots") || componentId == Rscm.lookup("component.prayerbook:quickprayer_slots")) {
            Prayer prayer = isAncientCurses() ? AncientPrayer.forId(slotId) : NormalPrayer.forId(slotId);
            if (prayer != null) {
                return switchPrayer(prayer);
            }
        }
        if (componentId == 43) {
            switchQuickPrayerSettings();
        }
        return false;
    }

    private int getCurrentTick() {
        return player.getGameTicks();
    }

    public boolean switchPrayer(Prayer prayer) {
        boolean currentlyActive = isActive(prayer);
        int currentTick = getCurrentTick();

        if (currentlyActive) {
            boolean result = deactivatePrayer(prayer);
            return result;
        }

        if (!canActivatePrayer(prayer)) {
            return false;
        }
        closeConflictingPrayers(prayer);
        if (prayer == AncientPrayer.TURMOIL && !turmoilActive) {
            turmoilActive = true;
            Arrays.fill(turmoilBonuses, 0);
            updateLeechBonuses();
        }
        if (isQuickPrayerMode()) {
            getQuickPrayerArray()[prayer.getId()] = true;
        } else {
            if (isAncientCurses()) {
                activeAncientPrayers.add((AncientPrayer) prayer);
            } else {
                assert prayer instanceof NormalPrayer;
                activeNormalPrayers.add((NormalPrayer) prayer);
            }
            updateAppearanceIfNeeded(prayer);
            playActivationEffects(prayer);
        }

        this.prayerActivatedTick = currentTick;

        player.getPackets().sendSound(2662, 0, 1);
        recalculatePrayer();
        return true;
    }


    private boolean deactivatePrayer(Prayer prayer) {
        if (isQuickPrayerMode()) {
            getQuickPrayerArray()[prayer.getId()] = false;
        } else {
            if (isAncientCurses()) {
                activeAncientPrayers.remove((AncientPrayer) prayer);
            } else {
                activeNormalPrayers.remove((NormalPrayer) prayer);
            }
            updateAppearanceIfNeeded(prayer);
        }
        if (prayer == AncientPrayer.TURMOIL && turmoilActive) {
            turmoilActive = false;
            Arrays.fill(turmoilBonuses, 0);
            updateLeechBonuses();
        }
        player.getPackets().sendSound(2662, 0, 1);
        recalculatePrayer();
        return true;
    }

    private boolean canActivatePrayer(Prayer prayer) {
        if (player.isDead()) {
            return false;
        }
        if (!hasPrayerPoints()) {
            player.getPackets().sendGameMessage("Please recharge your prayer points.");
            return false;
        }
        if (!meetsLevelRequirement(prayer)) {
            return false;
        }
        if (!meetsSpecialRequirements(prayer)) {
            return false;
        }
        if (isDisabledInClanWars()) {
            return false;
        }
        if (prayer.isProtectionPrayer()) {
            return !isPrayerDelayActive();
        }
        return true;
    }

    private boolean isPrayerDelayActive() {
        if (player.getTickManager().isActive(TickManager.TickKeys.DISABLED_PROTECTION_PRAYER_TICK)) {
            player.getPackets().sendGameMessage("You are currently injured and cannot use protection prayers!");
            return true;
        }
        return false;
    }

    private boolean meetsLevelRequirement(Prayer prayer) {
        int prayerLevel = player.getSkills().getLevelForXp(Skills.PRAYER);
        if (prayerLevel < prayer.getRequiredLevel()) {
            player.message("You need a Prayer level of at least " + prayer.getRequiredLevel() + " to use this prayer.");
            return false;
        }

        if (prayer.getBook() == PrayerBookType.ANCIENT_CURSES) {
            int defenceLevel = player.getSkills().getLevelForXp(Skills.DEFENCE);
            if (defenceLevel < DEFENCE_REQUIREMENT_ANCIENT) {
                player.message("You need a Defence level of at least " + DEFENCE_REQUIREMENT_ANCIENT + " to use ancient curses.");
                return false;
            }
        }
        return true;
    }

    private boolean meetsSpecialRequirements(Prayer prayer) {
        if (!prayer.hasSpecialRequirements(player)) {
            player.getPackets().sendGameMessage("You don't have access to this prayer.");
            return false;
        }
        return true;
    }

    private boolean isDisabledInClanWars() {
        if (player.getCurrentFriendChat() != null) {
            ClanWars war = player.getCurrentFriendChat().getClanWars();
            if (war != null && war.get(Rules.NO_PRAYER) && (war.getFirstPlayers().contains(player) || war.getSecondPlayers().contains(player))) {
                player.getPackets().sendGameMessage("Prayer has been disabled during this war.");
                return true;
            }
        }
        return false;
    }

    private void closePrayers(PrayerConflictGroup... groups) {
        for (PrayerConflictGroup group : groups) {
            closePrayersInGroup(group);
        }
    }

    private void closePrayersInGroup(PrayerConflictGroup group) {
        Prayer[] prayersInGroup = getPrayersInConflictGroup(group);

        for (Prayer prayer : prayersInGroup) {
            if (isQuickPrayerMode()) {
                getQuickPrayerArray()[prayer.getId()] = false;
            } else {
                getActivePrayerSet().remove(prayer);
            }
            if (prayer == AncientPrayer.TURMOIL && turmoilActive) {
                turmoilActive = false;
                Arrays.fill(turmoilBonuses, 0);
                updateLeechBonuses();
            }
        }
        if (group == PrayerConflictGroup.PROTECTION || group == PrayerConflictGroup.OTHER || group == PrayerConflictGroup.OVERHEAD || group == PrayerConflictGroup.SPECIAL) {
            player.getAppearence().generateAppearenceData();
        }
    }

    private Prayer[] getPrayersInConflictGroup(PrayerConflictGroup group) {
        if (isAncientCurses()) {
            return getAncientPrayersInGroup(group);
        } else {
            return getNormalPrayersInGroup(group);
        }
    }

    private Prayer[] getNormalPrayersInGroup(PrayerConflictGroup group) {
        return switch (group) {
            case RESTORATION -> new Prayer[]{NormalPrayer.RAPID_HEAL, NormalPrayer.RAPID_RESTORE};
            case MELEE -> new Prayer[]{
                    NormalPrayer.BURST_OF_STRENGTH, NormalPrayer.SUPERHUMAN_STRENGTH,
                    NormalPrayer.ULTIMATE_STRENGTH, NormalPrayer.CLARITY_OF_THOUGHT,
                    NormalPrayer.IMPROVED_REFLEXES, NormalPrayer.INCREDIBLE_REFLEXES,
                    NormalPrayer.CHIVALRY, NormalPrayer.PIETY};
            case OFFENSIVE -> new Prayer[]{
                    NormalPrayer.BURST_OF_STRENGTH, NormalPrayer.SUPERHUMAN_STRENGTH,
                    NormalPrayer.ULTIMATE_STRENGTH, NormalPrayer.CLARITY_OF_THOUGHT,
                    NormalPrayer.IMPROVED_REFLEXES, NormalPrayer.INCREDIBLE_REFLEXES,
                    NormalPrayer.CHIVALRY, NormalPrayer.PIETY,
                    NormalPrayer.SHARP_EYE, NormalPrayer.HAWK_EYE,
                    NormalPrayer.EAGLE_EYE, NormalPrayer.RIGOUR,
                    NormalPrayer.MYSTIC_WILL, NormalPrayer.MYSTIC_LORE,
                    NormalPrayer.MYSTIC_MIGHT, NormalPrayer.AUGURY};
            case DEFENSIVE_SKINS -> new Prayer[]{
                    NormalPrayer.THICK_SKIN, NormalPrayer.ROCK_SKIN,
                    NormalPrayer.STEEL_SKIN, NormalPrayer.CHIVALRY, NormalPrayer.PIETY};
            case STRENGTH -> new Prayer[]{
                    NormalPrayer.BURST_OF_STRENGTH, NormalPrayer.SUPERHUMAN_STRENGTH,
                    NormalPrayer.ULTIMATE_STRENGTH, NormalPrayer.CHIVALRY, NormalPrayer.PIETY};
            case ATTACK -> new Prayer[]{
                    NormalPrayer.CLARITY_OF_THOUGHT, NormalPrayer.IMPROVED_REFLEXES,
                    NormalPrayer.INCREDIBLE_REFLEXES, NormalPrayer.CHIVALRY, NormalPrayer.PIETY};
            case RANGED -> new Prayer[]{
                    NormalPrayer.SHARP_EYE, NormalPrayer.HAWK_EYE,
                    NormalPrayer.EAGLE_EYE, NormalPrayer.RIGOUR};
            case MAGIC -> new Prayer[]{
                    NormalPrayer.MYSTIC_WILL, NormalPrayer.MYSTIC_LORE,
                    NormalPrayer.MYSTIC_MIGHT, NormalPrayer.AUGURY};
            case PROTECT_ITEM -> new Prayer[]{NormalPrayer.PROTECT_ITEM};
            case OVERHEAD -> new Prayer[]{
                    NormalPrayer.PROTECT_FROM_MAGIC, NormalPrayer.PROTECT_FROM_MISSILES,
                    NormalPrayer.PROTECT_FROM_MELEE, NormalPrayer.RETRIBUTION,
                    NormalPrayer.REDEMPTION, NormalPrayer.SMITE};
            case PROTECTION -> new Prayer[]{
                    NormalPrayer.PROTECT_FROM_MAGIC, NormalPrayer.PROTECT_FROM_MISSILES,
                    NormalPrayer.PROTECT_FROM_MELEE};
            case OTHER -> new Prayer[]{
                    NormalPrayer.REDEMPTION, NormalPrayer.RETRIBUTION,
                    NormalPrayer.SMITE, NormalPrayer.PROTECT_FROM_SUMMONING};
            case SPECIAL -> new Prayer[]{NormalPrayer.RAPID_RENEWAL};
            default -> new Prayer[0];
        };
    }

    private Prayer[] getAncientPrayersInGroup(PrayerConflictGroup group) {
        return switch (group) {
            case SAP_CURSES ->
                    new Prayer[]{AncientPrayer.SAP_WARRIOR, AncientPrayer.SAP_RANGER, AncientPrayer.SAP_MAGE, AncientPrayer.SAP_SPIRIT};
            case LEECH_CURSES -> new Prayer[]{
                    AncientPrayer.LEECH_ATTACK, AncientPrayer.LEECH_RANGED,
                    AncientPrayer.LEECH_MAGIC, AncientPrayer.LEECH_DEFENCE,
                    AncientPrayer.LEECH_STRENGTH, AncientPrayer.LEECH_ENERGY, AncientPrayer.LEECH_ENERGY};
            case SPECIAL_DRAIN -> new Prayer[]{AncientPrayer.SAP_SPIRIT, AncientPrayer.LEECH_SPECIAL};
            case ENERGY_DRAIN -> new Prayer[]{AncientPrayer.LEECH_ENERGY};
            case MELEE -> new Prayer[]{AncientPrayer.TURMOIL};
            case OFFENSIVE -> new Prayer[]{AncientPrayer.SAP_MAGE, AncientPrayer.SAP_RANGER,
                    AncientPrayer.SAP_WARRIOR, AncientPrayer.LEECH_ATTACK,
                    AncientPrayer.LEECH_DEFENCE, AncientPrayer.LEECH_STRENGTH,
                    AncientPrayer.LEECH_RANGED, AncientPrayer.LEECH_MAGIC, AncientPrayer.TURMOIL};
            case ATTACK -> new Prayer[]{AncientPrayer.SAP_WARRIOR, AncientPrayer.LEECH_ATTACK, AncientPrayer.TURMOIL};
            case STRENGTH ->
                    new Prayer[]{AncientPrayer.SAP_WARRIOR, AncientPrayer.LEECH_STRENGTH, AncientPrayer.TURMOIL};
            case DEFENSIVE_SKINS ->
                    new Prayer[]{AncientPrayer.SAP_WARRIOR, AncientPrayer.LEECH_DEFENCE, AncientPrayer.TURMOIL};
            case RANGED -> new Prayer[]{AncientPrayer.SAP_RANGER, AncientPrayer.LEECH_RANGED};
            case MAGIC -> new Prayer[]{AncientPrayer.SAP_MAGE, AncientPrayer.LEECH_MAGIC};
            case PROTECT_ITEM -> new Prayer[]{AncientPrayer.PROTECT_ITEM_CURSE};
            case PROTECTION -> new Prayer[]{
                    AncientPrayer.DEFLECT_MAGIC, AncientPrayer.DEFLECT_MISSILES, AncientPrayer.DEFLECT_MELEE};
            case OVERHEAD -> new Prayer[]{
                    AncientPrayer.DEFLECT_MAGIC, AncientPrayer.DEFLECT_MISSILES,
                    AncientPrayer.DEFLECT_MELEE, AncientPrayer.WRATH, AncientPrayer.SOUL_SPLIT};
            case OTHER -> new Prayer[]{AncientPrayer.DEFLECT_SUMMONING, AncientPrayer.WRATH, AncientPrayer.SOUL_SPLIT};
            case SPECIAL -> new Prayer[]{AncientPrayer.BERSERK};
            default -> new Prayer[0];
        };
    }

    private void closeConflictingPrayers(Prayer prayer) {
        closePrayers(prayer.getConflictGroups());
    }

    private void playActivationEffects(Prayer prayer) {
        if (isQuickPrayerMode()) return;

        Animation animation = prayer.getActivationAnimation();
        Graphics graphics = prayer.getActivationGraphics();

        if (animation != null) {
            player.animate(animation);
        }
        if (graphics != null) {
            player.gfx(graphics);
        }
    }

    private void updateAppearanceIfNeeded(Prayer prayer) {
        if (prayer.isProtectionPrayer() || prayer == NormalPrayer.RETRIBUTION || prayer == NormalPrayer.REDEMPTION || prayer == NormalPrayer.SMITE || prayer == AncientPrayer.DEFLECT_MAGIC || prayer == AncientPrayer.DEFLECT_MISSILES || prayer == AncientPrayer.DEFLECT_MELEE || prayer == AncientPrayer.WRATH || prayer == AncientPrayer.SOUL_SPLIT || prayer == AncientPrayer.TURMOIL) {
            player.getAppearence().generateAppearenceData();
        }
    }

    private void recalculatePrayer() {
        long configValue = 0L;

        if (isQuickPrayerMode()) {
            boolean[] quickPrayers = getQuickPrayerArray();
            for (int i = 0; i < quickPrayers.length; i++) {
                if (quickPrayers[i]) {
                    Prayer prayer = getPrayerForId(i);
                    if (prayer != null) {
                        configValue |= getPrayerConfigValue(prayer);
                    }
                }
            }
        } else {
            for (Prayer prayer : getActivePrayerSet()) {
                configValue |= getPrayerConfigValue(prayer);
            }
        }

        int configId = isAncientCurses() ? (isQuickPrayerMode() ? 1587 : 1582) : (isQuickPrayerMode() ? 1397 : 1395);
        //System.out.println("Sending config id: " + configId + ", configValue: " + configValue);

        player.getPackets().sendVar(configId, (int) configValue);
    }

    private long getPrayerConfigValue(Prayer prayer) {
        return isAncientCurses() ? (1L << prayer.getId()) : prayer.getConfigValue();
    }

    private static final int TURMOIL_MAX_BONUS = 15;
    private static final double TURMOIL_BOOST_RATIO = 0.10;

    public void updateTurmoilBonus(Entity target) {
        if (!turmoilActive) return;

        for (int statIndex : new int[]{0, 1, 2}) {
            int targetLevel = getTargetLevel(target, statIndex);
            if (targetLevel == -1) continue;

            int turmoilBoost = (int) (targetLevel * TURMOIL_BOOST_RATIO);
            turmoilBonuses[statIndex] = Math.min(turmoilBoost, TURMOIL_MAX_BONUS);
        }
        updateLeechBonuses();
    }

    private int getTargetLevel(Entity target, int statIndex) {
        if (target instanceof Player p) {
            switch (statIndex) {
                case BonusIndex.ATTACK:
                    return p.getSkills().getLevel(Skills.ATTACK);
                case BonusIndex.STRENGTH:
                    return p.getSkills().getLevel(Skills.STRENGTH);
                case BonusIndex.DEFENCE:
                    return p.getSkills().getLevel(Skills.DEFENCE);
            }
        } else if (target instanceof NPC npc) {
            switch (statIndex) {
                case BonusIndex.ATTACK:
                    return npc.getCombatData().attackLevel;
                case BonusIndex.STRENGTH:
                    return npc.getCombatData().strengthLevel;
                case BonusIndex.DEFENCE:
                    return npc.getCombatData().defenceLevel;
            }
        }
        return -1;
    }

    private Prayer getPrayerForLeechIndex(int index) {
        return switch (index) {
            case 0 -> AncientPrayer.LEECH_ATTACK;
            case 1 -> AncientPrayer.LEECH_STRENGTH;
            case 2 -> AncientPrayer.LEECH_DEFENCE;
            case 3 -> AncientPrayer.LEECH_RANGED;
            case 4 -> AncientPrayer.LEECH_MAGIC;
            default -> null;
        };
    }

    private double calculateBoost(Function<Prayer, Double> boostGetter) {
        double bonus = 1.0;

        for (int i = 0; i < leechBonuses.length; i++) {
            if (boostGetter.apply(getPrayerForLeechIndex(i)) != null) {
                bonus += leechBonuses[i] / 100.0;
            }
        }

        for (Prayer prayer : getActivePrayers()) {
            Double boost = boostGetter.apply(prayer);
            if (boost != null) {
                bonus += boost;
            }
        }

        return Math.max(0.15, bonus);
    }

    public double getAttackMultiplier() {
        double base = calculateBoost(Prayer::getAttackBoost);
        if (turmoilActive) base += turmoilBonuses[0] / 100.0;
        return base;
    }

    public double getStrengthMultiplier() {
        double base = calculateBoost(Prayer::getStrengthBoost);
        if (turmoilActive) base += turmoilBonuses[1] / 100.0;
        return base;
    }

    public double getDefenceMultiplier() {
        double base = calculateBoost(Prayer::getDefenceBoost);
        if (turmoilActive) base += turmoilBonuses[2] / 100.0;
        return base;
    }

    public double getMagicMultiplier() {
        return calculateBoost(Prayer::getMagicBoost); // no turmoil
    }

    public double getRangedMultiplier() {
        return calculateBoost(Prayer::getRangedBoost); // no turmoil
    }

    public void processLeechDecay(int ticks) {
        if (ticks % 50 == 0) {
            checkLeechDecay(player);
        }
    }

    private transient int prayerActivatedTick = -1;
    private transient int drainCounter = 0;
    private static final int DRAIN_PRECISION = 1000;
    private static final int DRAIN_MULTIPLIER = 10;
    private static final int BASE_RESISTANCE = 60;

    public void processPrayerDrain(int ticks) {
        processLeechDecay(ticks);
        if (!hasActivePrayers()) {
            return;
        }

        if (prayerActivatedTick == ticks) {
            return;
        }
        if (!hasPrayerPoints()) {//i believe it should check for runout next tick like this
            closeAllPrayers();
            player.message("You have run out of Prayer points!");
        }

        int prayerBonus = player.getCombatDefinitions().getBonuses()[CombatDefinitions.PRAYER_BONUS];
        int drainResistance = (2 * prayerBonus + BASE_RESISTANCE);

        int totalDrainRate = 0;
        for (Prayer prayer : getActivePrayers()) {
            totalDrainRate += (int) (prayer.getDrainRate() * DRAIN_MULTIPLIER);
        }

        drainCounter += totalDrainRate * DRAIN_PRECISION;

        int pointsToDrain = 0;
        int resistanceThreshold = drainResistance * DRAIN_PRECISION;
        while (drainCounter >= resistanceThreshold) {
            pointsToDrain++;
            drainCounter -= resistanceThreshold;
        }

        if (pointsToDrain > 0) {
            drainPrayer(pointsToDrain);
        }

        /*debug("[Prayer Drain] Ticks:" + ticks +
                " Drain:" + (totalDrainRate/DRAIN_MULTIPLIER) +
                " Bonus:" + prayerBonus +
                " Res:" + (drainResistance/DRAIN_MULTIPLIER));
        debug("[Prayer Drain] Drained:" + pointsToDrain +
                " Remainder:" + (drainCounter / (double)DRAIN_PRECISION));*/
    }

    public void switchQuickPrayerSettings() {
        usingQuickPrayer = !usingQuickPrayer;

        player.getPackets().sendGlobalVar(181, usingQuickPrayer ? 1 : 0);
        player.getPackets().sendVar(1584, isAncientCurses() ? 1 : 0);

        unlockPrayerBookButtons();

        player.getAppearence().generateAppearenceData();

        recalculatePrayer();

        if (usingQuickPrayer) {
            player.getPackets().sendGlobalVar(168, 6);
        }

        player.getPackets().sendSound(2662, 0, 1);
    }

    private void unlockPrayerBookButtons() {
        int componentId = usingQuickPrayer ? 42 : 8;
        player.getPackets().sendUnlockOptions(271, componentId, 0, 29, 0);
    }

    public void switchQuickPrayers() {
        if (player.isDead() || !hasPrayerPoints()) return;

        if (hasActivePrayers()) {
            closeAllPrayers();
        } else {
            boolean activated = false;
            boolean[] quickPrayers = getQuickPrayerArray();

            for (int i = 0; i < quickPrayers.length; i++) {
                if (quickPrayers[i]) {
                    Prayer prayer = getPrayerForId(i);
                    if (prayer != null && switchPrayer(prayer)) {
                        activated = true;
                    }
                }
            }
            if (activated) {
                player.getPackets().sendGlobalVar(182, 1);
                recalculatePrayer();
                return;
            }
            player.message("You don't have any quick prayers configured.");
        }
    }

    public int getPrayerHeadIcon() {
        if (!hasActivePrayers()) {
            return -1;
        }

        if (isAncientCurses()) {
            return getAncientCursesHeadIcon();
        } else {
            return getNormalPrayersHeadIcon();
        }
    }

    /**
     * OVERHEAD IDS
     * <p>
     * //NORMAL PRAYERS
     * MELEE_PROT = 0
     * RANGE_PROT = 1
     * MAGE_PROT = 2
     * RETRIBUTION = 3
     * SMITE = 4
     * REDEMTION = 5
     * RANGE & MAGE = 6
     * RANGE & MELEE = 22
     * MAGE & MELEE = 23
     * MAGE & MELEE & RANGE = 24
     * SUMMONING PROT = 7
     * SUMMONING & MELEE = 8
     * SUMMONING & RANGE = 9
     * SUMMONING & MAGE = 10
     * EMPTY OVERHEAD = 11
     * <p>
     * //ANCIENT PRAYERS
     * DEFLECT MELEE = 12
     * DEFLECT MAGE = 13
     * DEFLECT RANGE = 14
     * DEFLECT SUMMONING = 15
     * SUMMONING & MELEE = 16
     * SUMMONING & RANGE = 17
     * SUMMONING & MAGE = 18
     * WRATH = 19
     * SOULSPLIT = 20
     * <p>
     * //OTHERS
     * RED SKULL = 21
     * WHITE SKULL = 25
     * SOME BEARED MAN? = 26
     */

    private int getAncientCursesHeadIcon() {
        int value = -1;
        if (isActive(AncientPrayer.DEFLECT_MELEE)) value = 12;
        if (isActive(AncientPrayer.DEFLECT_MAGIC)) value = 13;
        if (isActive(AncientPrayer.DEFLECT_MISSILES)) value = 14;
        if (isActive(AncientPrayer.DEFLECT_SUMMONING)) {
            value = 15;
            if (isActive(AncientPrayer.DEFLECT_MELEE)) value += 1;
            if (isActive(AncientPrayer.DEFLECT_MISSILES)) value += 2;
            if (isActive(AncientPrayer.DEFLECT_MAGIC)) value += 3;
        }
        if (isActive(AncientPrayer.WRATH)) value = 19;
        if (isActive(AncientPrayer.SOUL_SPLIT)) value = 20;
        return value;
    }

    private int getNormalPrayersHeadIcon() {
        int value = -1;
        if (isActive(NormalPrayer.PROTECT_FROM_MELEE)) value += 1;
        if (isActive(NormalPrayer.PROTECT_FROM_MISSILES)) value += 2;
        if (isActive(NormalPrayer.PROTECT_FROM_MAGIC)) value += 3;
        if (isActive(NormalPrayer.RETRIBUTION)) value += 4;
        if (isActive(NormalPrayer.SMITE)) value += 5;
        if (isActive(NormalPrayer.REDEMPTION)) value += 6;
        if (isActive(NormalPrayer.PROTECT_FROM_SUMMONING)) value += 8;
        return value;
    }

    public void reset() {
        closeAllPrayers();
        resetLeechBonuses();
        prayerPoints = player.getSkills().getLevelForXp(Skills.PRAYER) * PRAYER_POINTS_PER_LEVEL;
        refreshPrayerPoints();
    }

    public void closeAllPrayers() {
        if (turmoilActive) {
            turmoilActive = false;
            Arrays.fill(turmoilBonuses, 0);
        }
        activeNormalPrayers.clear();
        activeAncientPrayers.clear();
        recalculatePrayer();
        player.getPackets().sendGlobalVar(182, 0);
        player.getAppearence().generateAppearenceData();
    }

    public void closeProtectionPrayers() {
        if (isAncientCurses()) {
            activeAncientPrayers.remove(AncientPrayer.DEFLECT_MAGIC);
            activeAncientPrayers.remove(AncientPrayer.DEFLECT_MISSILES);
            activeAncientPrayers.remove(AncientPrayer.DEFLECT_MELEE);
        } else {
            activeNormalPrayers.remove(NormalPrayer.PROTECT_FROM_MAGIC);
            activeNormalPrayers.remove(NormalPrayer.PROTECT_FROM_MISSILES);
            activeNormalPrayers.remove(NormalPrayer.PROTECT_FROM_MELEE);
        }
        player.getAppearence().generateAppearenceData();
    }

    public boolean drainPrayer(int amount) {
        if (prayerPoints <= 0) return false;
        int newPrayer = Math.max(0, prayerPoints - amount);
        boolean changed = (newPrayer != prayerPoints);
        prayerPoints = newPrayer;
        if (changed) {
            refreshPrayerPoints();
        }
        return changed;
    }

    public boolean drainPrayer() {
        if (prayerPoints <= 0) return false;
        prayerPoints = 0;
        refreshPrayerPoints();
        return true;
    }

    public void drainPrayerOnHalf() {
        if (prayerPoints <= 0) return;
        int newAmount = prayerPoints / 2;
        if (newAmount == prayerPoints) return;
        prayerPoints = newAmount;
        refreshPrayerPoints();
    }

    public boolean restorePrayer(int amount) {
        int max = getMaxPrayerPoints();
        if (prayerPoints >= max) return false;
        prayerPoints = Math.min(max, prayerPoints + amount);
        refreshPrayerPoints();
        return true;
    }

    public boolean restorePrayer(int amount, boolean updateCheck) {
        int max = getMaxPrayerPoints();
        if (prayerPoints >= max) return false;
        int newPrayer = Math.min(max, prayerPoints + amount);
        boolean changed = (newPrayer != prayerPoints);
        prayerPoints = newPrayer;
        if (changed && (!updateCheck || prayerPoints % 10 == 0)) {
            refreshPrayerPoints();
        }
        return changed;
    }

    public Set<Prayer> getActivePrayers() {
        return Collections.unmodifiableSet(getActivePrayerSet());
    }

    public int[] getActivePrayerIds() {
        Set<? extends Prayer> activeSet = getActivePrayerSet();
        int[] ids = new int[activeSet.size()];
        int index = 0;
        for (Prayer prayer : activeSet) {
            ids[index++] = prayer.getId();
        }
        return ids;
    }

    public boolean isPrayerActive(Prayer prayer) {
        return getActivePrayerSet().contains(prayer);
    }

    public int getActivePrayerCount() {
        return getActivePrayerSet().size();
    }

    public boolean hasProtectionPrayerActive() {
        for (Prayer prayer : getActivePrayers()) {
            if (prayer.isProtectionPrayer()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasProtectFromMelee() {
        return  isActive(NormalPrayer.PROTECT_FROM_MELEE) || isActive(AncientPrayer.DEFLECT_MELEE);
    }

    public boolean hasProtectFromRanging() {
        return  isActive(NormalPrayer.PROTECT_FROM_MISSILES) || isActive(AncientPrayer.DEFLECT_MISSILES);
    }

    public boolean hasProtectFromMagic() {
       return  isActive(NormalPrayer.PROTECT_FROM_MAGIC) || isActive(AncientPrayer.DEFLECT_MAGIC);
    }

    public boolean hasProtectItemPrayerActive() {
        for (Prayer prayer : getActivePrayers()) {
            return prayer.isProtectItemPrayer();
        }
        return false;
    }

    public boolean isMeleeProtecting() {
        return isActive(NormalPrayer.PROTECT_FROM_MELEE) || isActive(AncientPrayer.DEFLECT_MELEE);
    }

    public boolean isMageProtecting() {
        return isActive(NormalPrayer.PROTECT_FROM_MAGIC) || isActive(AncientPrayer.DEFLECT_MAGIC);
    }

    public boolean isRangeProtecting() {
        return isActive(NormalPrayer.PROTECT_FROM_MISSILES) || isActive(AncientPrayer.DEFLECT_MISSILES);
    }

    public void adjustStat(int stat, int percentage) {
        if (stat < 0 || stat >= STAT_VARBIT_IDS.length) return;
        player.getVarsManager().sendVarBit(STAT_VARBIT_IDS[stat], percentage + 30);
    }

    public void increaseLeechBonus(int bonusIndex) {
        if (leechBonuses[bonusIndex] < MAX_LEECH_BONUS) {
            leechBonuses[bonusIndex]++;

            if (turmoilActive && bonusIndex <= BonusIndex.DEFENCE) {
                turmoilBonuses[bonusIndex] = Math.min(turmoilBonuses[bonusIndex] + 1, TURMOIL_MAX_BONUS);
            }

            updateLeechBonuses();
            lastLeechTimes.put(bonusIndex, System.currentTimeMillis());
        }
    }

    public void decreaseLeechBonus(int bonusIndex) {
        if (leechBonuses[bonusIndex] > MIN_LEECH_BONUS) {
            leechBonuses[bonusIndex]--;

            if (turmoilActive && bonusIndex <= BonusIndex.DEFENCE) {
                turmoilBonuses[bonusIndex] = Math.max(turmoilBonuses[bonusIndex] - 1, 0);
            }

            updateLeechBonuses();
        }
    }

    public void checkLeechDecay(Player player) {
        int DECAY_TIMER = 30_000;
        boolean changed = false;
        for (int i = 0; i < leechBonuses.length; i++) {
            int current = leechBonuses[i];
            if (current == 0) continue;

            long lastBoost = lastLeechTimes.getOrDefault(i, 0L);

                if (current > 0) {
                    leechBonuses[i]--;
                } else {
                    leechBonuses[i]++;
                }

                adjustStat(i, leechBonuses[i]);
                int newCurrent = leechBonuses[i];
                if (newCurrent != current && newCurrent > 0)
                    changed = true;
                String statAffected = BonusIndex.nameOf(i);
                if (newCurrent == 0) {
                    player.message("Your " + statAffected + " is now unaffected by sap and leech curses.", true);
                }
        }
        if (changed)
            player.message("The sap or leech curses currently affecting your stats reduce a little.", true);
    }

    public void updateLeechBonuses() {
        for (int i = BonusIndex.ATTACK; i <= BonusIndex.MAGIC; i++) {
            if (turmoilActive && i <= BonusIndex.DEFENCE) {
                adjustStat(i, leechBonuses[i] + turmoilBonuses[i]);
            } else {
                adjustStat(i, leechBonuses[i]);
            }
        }
    }

    public void resetLeechBonuses() {
        Arrays.fill(leechBonuses, 0);
        for (int i = BonusIndex.ATTACK; i <= BonusIndex.MAGIC; i++) {
            adjustStat(i, 0);
        }
    }

    public boolean reachedMax(int bonusIndex) {
        return leechBonuses[bonusIndex] >= MAX_LEECH_BONUS;
    }

    public boolean reachedMin(int bonusIndex) {
        return leechBonuses[bonusIndex] <= MIN_LEECH_BONUS;
    }

    private int getMaxPrayerPoints() {
        return player.getSkills().getLevelForXp(Skills.PRAYER) * PRAYER_POINTS_PER_LEVEL;
    }

    public void refreshPrayerPoints() {
        player.getVarsManager().forceSendVarBit(9816, prayerPoints);
    }

    private EnumSet<? extends Prayer> getActivePrayerSet() {
        return isAncientCurses() ? activeAncientPrayers : activeNormalPrayers;
    }

    private boolean[] getQuickPrayerArray() {
        return isAncientCurses() ? quickAncientPrayers : quickNormalPrayers;
    }

    private Prayer getPrayerForId(int id) {
        if (isAncientCurses()) {
            for (AncientPrayer prayer : AncientPrayer.values()) {
                if (prayer.getId() == id) return prayer;
            }
        } else {
            for (NormalPrayer prayer : NormalPrayer.values()) {
                if (prayer.getId() == id) return prayer;
            }
        }
        return null;
    }

    public void refresh() {
        player.getInterfaceManager().sendPrayerBook();
        refreshPrayerPoints();
        recalculatePrayer();
        player.getAppearence().generateAppearenceData();
        player.getPackets().sendGlobalVar("globalvar.prayer_switch_quickpray", usingQuickPrayer ? 1 : 0);
        player.getPackets().sendVar("var.active_prayerbook", isAncientCurses() ? 1 : 0);
        player.getPackets().sendUnlockOptions(271, usingQuickPrayer ? 42 : 8, 0, 29, 0);
    }

    public boolean isAncientCurses() {
        return activeBook == PrayerBookType.ANCIENT_CURSES;
    }

    public int getPrayerPoints() {
        return prayerPoints;
    }

    public boolean hasPrayerPoints() {
        return prayerPoints > 0;
    }

    public boolean hasActivePrayers() {
        return !getActivePrayerSet().isEmpty();
    }

    public boolean isQuickPrayerMode() {
        return usingQuickPrayer;
    }

    public void setPrayerBook(boolean ancientcurses) {
        if (player == null || player.isDead()) {
            return;
        }

        PrayerBookType newBookType = ancientcurses ? PrayerBookType.ANCIENT_CURSES : PrayerBookType.NORMAL;

        if (this.activeBook == newBookType) {
            return;
        }

        closeAllPrayers();
        this.activeBook = newBookType;
        this.usingQuickPrayer = false;

        Arrays.fill(quickNormalPrayers, false);
        Arrays.fill(quickAncientPrayers, false);
        resetLeechBonuses();
        refresh();

        player.message("You have switched to " + (isAncientCurses() ? "Ancient Curses" : "Normal Prayers") + ".");
    }
}