package com.rs.java.game.player.controlers;

import com.rs.Settings;
import com.rs.java.game.Animation;
import com.rs.java.game.Entity;
import com.rs.java.game.ForceMovement;
import com.rs.java.game.WorldObject;
import com.rs.java.game.WorldTile;
import com.rs.java.game.npc.NPC;
import com.rs.java.game.player.Player;
import com.rs.java.game.player.Skills;
import com.rs.java.game.player.content.Pots;
import com.rs.core.tasks.WorldTask;
import com.rs.core.tasks.WorldTasksManager;
import com.rs.java.utils.Utils;
import com.rs.kotlin.Rscm;
import com.rs.kotlin.game.world.area.Area;
import com.rs.kotlin.game.world.area.AreaManager;

public class WildernessControler extends Controler {

	private boolean showingSkull;

	@Override
	public void start() {
		checkBoosts(player);
		moved();
	}

	public static void checkBoosts(Player player) {
		boolean changed = false;
		int level = player.getSkills().getLevelForXp(Skills.ATTACK);
		int maxLevel = (int) (level + 5 + (level * 0.15));
		if (maxLevel < player.getSkills().getLevel(Skills.ATTACK)) {
			player.getSkills().set(Skills.ATTACK, maxLevel);
			changed = true;
		}
		level = player.getSkills().getLevelForXp(Skills.STRENGTH);
		maxLevel = (int) (level + 5 + (level * 0.15));
		if (maxLevel < player.getSkills().getLevel(Skills.STRENGTH)) {
			player.getSkills().set(Skills.STRENGTH, maxLevel);
			changed = true;
		}
		level = player.getSkills().getLevelForXp(Skills.DEFENCE);
		maxLevel = (int) (level + 5 + (level * 0.15));
		if (maxLevel < player.getSkills().getLevel(Skills.DEFENCE)) {
			player.getSkills().set(Skills.DEFENCE, maxLevel);
			changed = true;
		}
		level = player.getSkills().getLevelForXp(Skills.RANGE);
		maxLevel = (int) (level + 5 + (level * 0.1));
		if (maxLevel < player.getSkills().getLevel(Skills.RANGE)) {
			player.getSkills().set(Skills.RANGE, maxLevel);
			changed = true;
		}
		level = player.getSkills().getLevelForXp(Skills.MAGIC);
		maxLevel = level + 7;
		if (maxLevel < player.getSkills().getLevel(Skills.MAGIC)) {
			player.getSkills().set(Skills.MAGIC, maxLevel);
			changed = true;
		}
		if (changed)
			player.getPackets().sendGameMessage("Your extreme potion bonus has been reduced.");
		if (player.getOverloadTicksLeft() > 1)
			Pots.resetOverLoadEffect(player);
	}

	@Override
	public boolean login() {
		moved();
		return false;
	}

	@Override
	public boolean keepCombating(Entity target) {
		if (target instanceof NPC)
			return true;
		if (!canAttack(target))
			return false;
		if (target instanceof Player) {
			Player p2 = (Player) target;
			if (player.isCanPvp() && !p2.isCanPvp()) {
				player.getPackets().sendGameMessage("That player is not in the wilderness.");
				return false;
			}
			if (Math.abs(player.getSkills().getCombatLevel() - p2.getSkills().getCombatLevel()) > getWildLevel(
					player)) {
				player.getPackets().sendGameMessage("Your level diffrence is too great!");
				player.getPackets().sendGameMessage("You need to move deeper into the Wilderness.");
				return false;
			}
			if (Math.abs(player.getSkills().getCombatLevel() - p2.getSkills().getCombatLevel()) > getWildLevel(p2)) {
				player.getPackets().sendGameMessage("Your level diffrence is too great!");
				player.getPackets().sendGameMessage("You need to move deeper into the Wilderness.");
				return false;
			}
		}
		if (target instanceof Player) {
			Player p2 = (Player) target;
			if (!player.attackedBy.containsKey(p2))
				player.setWildernessSkull();
		}
		return true;
	}

	@Override
	public boolean canAttack(Entity target) {
		if (target instanceof Player) {
			Player p2 = (Player) target;
			if (player.isCanPvp() && !p2.isCanPvp()) {
				player.getPackets().sendGameMessage("That player is not in the wilderness.");
				return false;
			}
			if (Math.abs(player.getSkills().getCombatLevel() - p2.getSkills().getCombatLevel()) > getWildLevel(
					player)) {
				player.getPackets().sendGameMessage("The difference between your combat level and the combat level of "
						+ p2.getDisplayName() + " is too great.");
				player.getPackets()
						.sendGameMessage("You need to move deeper into the Wilderness before you can attack him.");
				return false;
			}
			if (Math.abs(player.getSkills().getCombatLevel() - p2.getSkills().getCombatLevel()) > getWildLevel(p2)) {
				player.getPackets().sendGameMessage("The difference between your combat level and the combat level of "
						+ p2.getDisplayName() + " is too great.");
				player.getPackets()
						.sendGameMessage("He needs to move deeper into the Wilderness before you can attack him.");
				return false;
			}
			if (canHit(target))
				return true;
			return false;
		}

		return true;
	}

	@Override
	public boolean canHit(Entity target) {
		if (target instanceof NPC)
			return true;
		if (player.getCombatDefinitions().getSpellId() <= 0
				&& Utils.inCircle(new WorldTile(3105, 3933, 0), target, 23)) {
			player.getPackets().sendGameMessage("You can only use magic in the arena.");
			return false;
		}
		Player p2 = (Player) target;
		if (Math.abs(player.getSkills().getCombatLevel() - p2.getSkills().getCombatLevel()) > getWildLevel(player))
			return false;
		if (Math.abs(player.getSkills().getCombatLevel() - p2.getSkills().getCombatLevel()) > getWildLevel(p2))
			return false;
		return true;
	}

	@Override
	public boolean processMagicTeleport(WorldTile toTile) {
		if (getWildLevel(player) >= 21) {
			player.getPackets().sendGameMessage("You can't teleport above level 20 wilderness.");
			return false;
		}
		if (player.getTeleBlockDelay() > Utils.currentTimeMillis()) {
			player.getPackets()
					.sendGameMessage("You are teleblocked for another " + player.getTeleBlockTimeleft() + ".");
			return false;
		}
		loseEP();
		return true;

	}

	@Override
	public boolean processItemTeleport(WorldTile toTile) {
		if (getWildLevel(player) >= 21) {
			player.getPackets().sendGameMessage("You can't teleport above level 20 wilderness.");
			return false;
		}
		if (player.getTeleBlockDelay() > Utils.currentTimeMillis()) {
			player.getPackets()
					.sendGameMessage("You are teleblocked for another " + player.getTeleBlockTimeleft() + ".");
			return false;
		}
		loseEP();
		return true;
	}

	@Override
	public boolean processJewerlyTeleport(WorldTile toTile) {
		if (getWildLevel(player) >= 31) {
			player.getPackets().sendGameMessage("You can't teleport above level 30 wilderness.");
			return false;
		}
		if (player.getTeleBlockDelay() > Utils.currentTimeMillis()) {
			player.getPackets()
					.sendGameMessage("You are teleblocked for another " + player.getTeleBlockTimeleft() + ".");
			return false;
		}
		loseEP();
		return true;
	}

	@Override
	public boolean processObjectTeleport(WorldTile toTile) {
		if (player.getTeleBlockDelay() > Utils.currentTimeMillis()) {
			player.getPackets()
					.sendGameMessage("You are teleblocked for another " + player.getTeleBlockTimeleft() + ".");
			return false;
		}
		loseEP();
		return true;
	}

	public void loseEP() {
		if (player.getEP() >= 10 && player.isInCombat()) {
			player.removeEP( 10);
			player.message("You lose 10% EP for teleporting while being in combat.");
		}
	}

	public void showSkull() {
		player.getInterfaceManager().sendTab(player.getInterfaceManager().hasRezizableScreen() ? "tab.wildy_skull_resizeable" : "tab.wildy_skull_tab", "interface.wilderness_skull");
	}

	public static void showKDRInter(Player player) {
		if (player.toggles("KDRINTER", false)) {
			int kills = player.getKillCount();
			int deaths = player.getDeathCount();

			String ratioText;
			if (deaths == 0) {
				ratioText = String.valueOf(kills);
			} else {
				double dr = (double) kills / deaths;
				ratioText = String.format("%.2f", dr);
			}
			if (!player.getInterfaceManager().containsInterface("interface.kdr_interface"))
				player.getInterfaceManager().sendTab("tab.kdr_tab", "interface.kdr_interface");
			player.getPackets().sendTextOnComponent("interface.kdr_interface", 2, "Kills: " + kills);
			player.getPackets().sendTextOnComponent("interface.kdr_interface", 3, "Deaths: " + deaths);
			player.getPackets().sendTextOnComponent("interface.kdr_interface", 4, "Ratio: " + ratioText);
		}
	}

	public static boolean isDitch(int id) {
		return id >= 1440 && id <= 1444 || id >= 65076 && id <= 65087;
	}

	@Override
	public boolean processObjectClick1(final WorldObject object) {
		if (isDitch(object.getId())) {
			player.lock(3);
			player.animate(new Animation(6132));

			int dx = 0, dy = 0;

			switch (object.getRotation()) {
				case 0: // ditch facing south
					dy = (player.getY() > object.getY()) ? -3 : +3;
					break;
				case 2: // ditch facing north
					dy = (player.getY() < object.getY()) ? +3 : -3;
					break;
				case 1: // ditch facing west
					dx = (player.getX() < object.getX()) ? +3 : -3;
					break;
				case 3: // ditch facing east
					dx = (player.getX() > object.getX()) ? -3 : +3;
					break;
			}

			final WorldTile toTile = new WorldTile(
					player.getX() + dx,
					player.getY() + dy,
					object.getPlane()
			);

			int direction;
			if (dx > 0) direction = ForceMovement.EAST;
			else if (dx < 0) direction = ForceMovement.WEST;
			else if (dy > 0) direction = ForceMovement.NORTH;
			else direction = ForceMovement.SOUTH;

			player.setNextForceMovement(
					new ForceMovement(new WorldTile(player), 1, toTile, 2, direction)
			);

			WorldTasksManager.schedule(new WorldTask() {
				@Override
				public void run() {
					player.setNextWorldTile(toTile);
					player.faceObject(object);
					removeIcon();
					removeControler();
					player.resetReceivedDamage();
				}
			}, 2);

			return false;
		}
		return true;
	}



	@Override
	public boolean processObjectClick2(final WorldObject object) {
		return true;
	}

	@Override
	public void sendInterfaces() {
		if (isAtWild(player)) {
			showSkull();
		}
	}

	@Override
	public boolean sendDeath() {
		final Player instance = player;
		player.resetWalkSteps();
		player.lock(7);
		player.animate(new Animation(836));
		if (player.getFamiliar() != null)
			player.getFamiliar().sendDeath(player);
		player.checkPetDeath();
		WorldTasksManager.schedule(new WorldTask() {
			int loop;

			@Override
			public void run() {
				if (loop == 0) {
				} else if (loop == 1) {
					player.getPackets().sendGameMessage("Oh dear, you have died.");
				} else if (loop == 2) {
					Player killer = player.getMostDamageReceivedSourcePlayer();
					if (killer != null) {
						player.sendItemsOnDeath(killer, true);
						killer.increaseKillCount(player);
						// WildernessKills.addKill(killer, player);
					} else
						player.sendItemsOnDeath(instance, true);
					player.getEquipment().init();
					player.getInventory().init();
					player.reset();
					player.setNextWorldTile(new WorldTile(Settings.RESPAWN_PLAYER_LOCATION));
					player.animate(new Animation(-1));
					removeIcon();
					removeControler();
				} else if (loop == 3) {
					player.getPackets().sendMusicEffect(90);
					stop();
				}
				loop++;
			}
		}, 0, 1);
		return false;
	}

	@Override
	public void moved() {
		boolean isAtWild = isAtWild(player);
		boolean isAtWildSafe = isAtWildSafe(player);
		if (isAtWildSafe) {
			removeIcon();
			removeControler();
		}
		if (isAtWild && !isAtWildSafe) {
			showSkull();
			player.getPackets().sendGlobalVar(1000, player.getSkills().getCombatLevel() + player.getSkills().getSummoningCombatLevel());
			player.getAppearence().generateAppearenceData();
			checkBoosts(player);
		}
	}

	public void removeIcon() {
			player.getPackets().closeInterface(player.getInterfaceManager().hasRezizableScreen() ? Rscm.lookup("tab.wildy_skull_resizeable") : Rscm.lookup("tab.wildy_skull_tab"));
			player.getAppearence().generateAppearenceData();
			player.getEquipment().refresh(null);
			player.getPackets().sendGlobalVar(1000, 0);
	}

	@Override
	public boolean logout() {
		return false; // so doesnt remove script
	}

	@Override
	public void forceClose() {
		removeIcon();
	}

	public static boolean isAtWild(WorldTile tile) {
		for (Area area : AreaManager.getAll(tile)) {
			if (area.environment() == Area.Environment.WILDERNESS
					&& !isAtWildSafe(tile)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isAtWildSafe(WorldTile tile) {
		for (Area area : AreaManager.getAll(tile)) {
			if (area.environment() == Area.Environment.WILDERNESS_SAFE || tile.getY() <= 3524) {
				return true;
			}
		}
		return false;
	}

	public static int getWildLevel(Player player) {
		if ((player.getX() >= 3060 && player.getX() <= 3072 && player.getY() >= 10251 && player.getY() <= 10263))
			return 42;
		if (player.getY() > 9900)
			return (player.getY() - 9912) / 8;
		return (player.getY() - 3520) / 8 + 1;
	}
}
