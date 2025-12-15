package com.rs.java.game.player;

import java.util.concurrent.ConcurrentHashMap;

import com.rs.Settings;
import com.rs.java.game.player.content.clans.ClansManager;
import com.rs.kotlin.Rscm;

public class InterfaceManager {

	/*
	 * Tab Ids Combat = 0 Task system = 1 Stats/kills = 2 Quest Journals = 3
	 * Inventory = 4 Worn equipment = 5 Prayer = 6 Magic spellbook = 7 SOF = 8
	 * Friend list = 9 Friend chat = 10 Clan = 11 Settings = 12 Emotes = 13
	 * Music = 14 Notes = 15
	 * 
	 */

	public static final int FIXED_WINDOW_ID = 548;
	public static final int RESIZABLE_WINDOW_ID = 746;
	public static final int CHAT_BOX_TAB = 13;
	public static final int FIXED_SCREEN_TAB_ID = 27;
	public static final int RESIZABLE_SCREEN_TAB_ID = 28;
	public static final int FIXED_INV_TAB_ID = 166;
	public static final int RESIZABLE_INV_TAB_ID = 108;
	public transient Player player;

	private final ConcurrentHashMap<Integer, int[]> openedinterfaces = new ConcurrentHashMap<Integer, int[]>();
	private final ConcurrentHashMap<Integer, Integer> openedinterfacesb = new ConcurrentHashMap<Integer, Integer>();

	public boolean resizableScreen;
	private int windowsPane;
	private int rootInterface;

	public InterfaceManager(Player player) {
		this.player = player;
	}

	public void sendTab(int tabId, int interfaceId) {
		player.getPackets().sendInterface(true, isResizableScreen() ? RESIZABLE_WINDOW_ID : FIXED_WINDOW_ID, tabId,
				interfaceId);
	}

	public void sendTab(String tab, int interfaceId) {
		int tabId = Rscm.lookup(tab);
		player.getPackets().sendInterface(true, isResizableScreen() ? RESIZABLE_WINDOW_ID : FIXED_WINDOW_ID, tabId,
				interfaceId);
	}

	public void sendTab(String tab, String inter) {
		int tabId = Rscm.lookup(tab);
		int interfaceId = Rscm.lookup(inter);
		player.getPackets().sendInterface(true, isResizableScreen() ? RESIZABLE_WINDOW_ID : FIXED_WINDOW_ID, tabId,
				interfaceId);
	}

	public void sendChatBoxInterface(int interfaceId) {
		player.getPackets().sendInterface(true, 752, CHAT_BOX_TAB, interfaceId);
	}

	public void closeChatBoxInterface() {
		player.getPackets().closeInterface(CHAT_BOX_TAB);
	}

	public void closeHealth(boolean fullScreen) {
		player.getInterfaceManager().closeOverlay(false);
	}
	
	public void sendOverlay(int interfaceId, boolean fullScreen, int x) {
		player.getPackets().sendInterface(true, isResizableScreen() ? RESIZABLE_WINDOW_ID : FIXED_WINDOW_ID, 
				x, interfaceId);
	}

	public void sendOverlay(int interfaceId, boolean fullScreen) {
		sendTab(isResizableScreen() ? "tab.overlay_resizeable" : "tab.overlay", interfaceId);
	}

	public void closeOverlay(boolean fullScreen) {
		player.getPackets().closeInterface(isResizableScreen() ? "tab.overlay_resizeable" : "tab.overlay");
	}
	
	public void closeTab(boolean fullScreen, int tab) {
		player.getPackets().closeInterface(tab);
	}
	
	public void closeTab(int tab) {
		player.getPackets().closeInterface(tab);
	}

	public void closeTab(String tab) {
		int tabId = Rscm.lookup(tab);
		player.getPackets().closeInterface(tabId);
	}

	public void sendQuestTab() {
		sendTab(isResizableScreen() ? "tab.quest_resizeable" : "tab.quest", "interface.custom_quest");
	}

	public void sendSummoningTab() {
		sendTab(resizableScreen ? "tab.summoning_resizeable" : "tab.summoning", "interface.summoning_tab");
	}
	
	public void sendDungTab() {
		sendTab(isResizableScreen() ? "tab.dungeoneering_resizeable" : "tab.dungeoneering", "interface.dungeoneering_party");
	}

	public void sendInterface(int interfaceId) {
		player.getPackets().sendInterface(false, isResizableScreen() ? RESIZABLE_WINDOW_ID : FIXED_WINDOW_ID,
				isResizableScreen() ? RESIZABLE_SCREEN_TAB_ID : FIXED_SCREEN_TAB_ID, interfaceId);
	}

	public void sendInterface(String interfaceName) {
		player.getPackets().sendInterface(false, isResizableScreen() ? RESIZABLE_WINDOW_ID : FIXED_WINDOW_ID,
				isResizableScreen() ? RESIZABLE_SCREEN_TAB_ID : FIXED_SCREEN_TAB_ID, Rscm.lookup(interfaceName));
	}

	public void sendInventoryInterface(int childId) {
		player.getPackets().sendInterface(false, isResizableScreen() ? RESIZABLE_WINDOW_ID : FIXED_WINDOW_ID,
				isResizableScreen() ? RESIZABLE_INV_TAB_ID : FIXED_INV_TAB_ID, childId);
	}

	public void sendInventoryInterface(int childId, boolean noclip) {
		player.getPackets().sendInterface(noclip, isResizableScreen() ? RESIZABLE_WINDOW_ID : FIXED_WINDOW_ID,
				isResizableScreen() ? RESIZABLE_INV_TAB_ID : FIXED_INV_TAB_ID, childId);
	}

	public void sendTimerInterface(Player player) {
		player.getInterfaceManager().sendOverlay(3000, false);
		player.getPackets().sendTextOnComponent(3000, 5, "");
		player.getPackets().sendTextOnComponent(3000, 6, "");
		player.getPackets().sendTextOnComponent(3000, 7, "");
		player.getPackets().sendTextOnComponent(3000, 8, "");
		player.getPackets().sendTextOnComponent(3000, 9, "");
	}

	public final void sendInterfaces() {
		if (player.getDisplayMode() == 2 || player.getDisplayMode() == 3) {
			setResizableScreen(true);
			sendFullScreenInterfaces();
		} else {
			setResizableScreen(false);
			sendFixedInterfaces();
		}
		player.getSkills().resetXPDisplay();
		player.getCombatDefinitions().sendUnlockAttackStylesButtons();
		player.getMusicsManager().unlockMusicPlayer();
		player.getEmotesManager().unlockEmotesBook();
		player.getInventory().unlockInventoryOptions();
		player.getPrayer().refresh();
		//sendTimerInterface();
		ClansManager.unlockBanList(player);
		player.getPackets().sendTextOnComponent(182, 1,
				"When you finished playing " + Settings.SERVER_NAME + ", click the log out button to save your progress properly.");
		if (player.getFamiliar() != null && player.isActive())
			player.getFamiliar().unlock();
		player.getPackets().sendGlobalVar(234, 4);//overwrite questtab icon
		player.getControlerManager().sendInterfaces();
	}

	public void replaceRealChatBoxInterface(int interfaceId) {
		player.getPackets().sendInterface(true, 752, 11, interfaceId);
	}

	public void closeReplacedRealChatBoxInterface() {
		player.getPackets().closeInterface(752, 11);
	}

	public void sendWindowPane() {
		player.getPackets().sendWindowsPane(isResizableScreen() ? 746 : 548, 0);
	}

	public void sendFullScreenInterfaces() {
		player.getPackets().sendWindowsPane(746, 0);
		sendTab("tab.chat_resizeable", "interface.chat");
		sendTab("tab.chat_options_resizeable", "interface.chat_options");
		sendTab(15, 745);
		sendTab(25, 754);
		sendTab("tab.hp_orb_resizeable", "interface.hp_orb");
		sendTab("tab.prayer_orb_resizeable", "interface.prayer_orb");
		sendTab("tab.run_orb_resizeable", "interface.run_orb");
		sendTab("tab.summoning_orb_resizeable", "interface.summoning_orb");
		player.getPackets().sendInterface(true, 752, 9, Rscm.lookup("interface.chatbox"));
		sendTab("tab.summoning_resizeable", "interface.summoning_tab");
		player.getPackets().sendGlobalVar(823, 1);
		sendCombatStyles();
		sendTaskSystem();
		sendSkills();
		sendQuestTab();
		sendInventory();
		sendEquipment();
		sendPrayerBook();
		sendMagicBook();
		sendTab("tab.friendslist_resizeable", "interface.friendslist"); // friend list
		sendTab("tab.friendschat_resizeable", "interface.friendschat"); // 551 ignore now friendchat
		sendTab("tab.clanchat_resizeable", "interface.clanchat"); // 589 old clan chat now new clan chat
		sendSettings();
		sendEmotes();
		sendTab("tab.music_resizeable", "interface.music"); // music
		sendTab("tab.notes_resizeable", "interface.notes"); // notes
		sendTab("tab.logout_resizeable", "interface.logout"); // logout*/
	}

	public void sendFixedInterfaces() {
		player.getPackets().sendWindowsPane(548, 0);
		sendTab("tab.chat", "interface.chat");
		sendTab("tab.chat_options", "interface.chat_options");
		sendTab(23, 745);//unknown
		sendTab(25, 754);//unknown
		sendTab("tab.summoning_orb", "interface.summoning_orb");
		sendTab("tab.hp_orb", "interface.hp_orb");
		sendTab("tab.prayer_orb", "interface.prayer_orb");
		sendTab("tab.run_orb", "interface.run_orb");
		player.getPackets().sendInterface(true, 752, 9, 137);
		player.getPackets().sendInterface(true, 548, 9, 167);
		//sendTab(player.getInterfaceManager().hasRezizableScreen() ? 11 : 0, 1252);
		//sendTab(119, "interface.sof");
		sendMagicBook();
		sendPrayerBook();
		sendEquipment();
		sendInventory();
		sendQuestTab();
		sendSummoningTab();
		sendTab("tab.frienschat", "interface.friendschat");
		sendTab("tab.clanchat", "interface.clanchat");
		sendTab("tab.friendslist", "interface.friendslist");
		sendTab("tab.music", "interface.music");
		sendTab("tab.notes", "interface.notes");
		sendTab("tab.logout", "interface.logout");
		sendSkills();
		sendEmotes();
		sendSettings();
		sendTaskSystem();
		sendCombatStyles();
		//sendTimerInterface();
	}
	
	public void sendTimerInterface() {
		sendTab(player.getInterfaceManager().isResizableScreen() ? 26 : 31, 3039);
		
	}

	public void sendStaffPanel() {
		sendTab(resizableScreen ? 119 : 179, 506);
		player.getPackets().sendGlobalVar(823, 1);
	}
	
	public void sendSof() {
		sendTab(resizableScreen ? 119 : 179, 1139);
		player.getPackets().sendGlobalVar(823, 1);
		player.getPackets().sendHideIComponent(1139, 8, true);
		player.getPackets().sendHideIComponent(1139, 12, true);
	}

	public void sendXPPopup() {
		sendTab(isResizableScreen() ? "tab.xp_drop_resizeable" : "tab.xp_drop", "interface.xp_drop"); // xp
	}

	public void sendXPDisplay() {
		sendXPDisplay(Rscm.lookup("interface.xp_counter")); // xp counter
	}

	public void sendXPDisplay(int interfaceId) {
		sendTab(isResizableScreen() ? "tab.xp_counter_resizeable" : "tab.xp_counter", interfaceId); // xp counter
	}

	public void closeXPPopup() {
		player.getPackets().closeInterface(isResizableScreen() ? "tab.xp_drop_resizeable" : "tab.xp_drop");
	}

	public void closeXPDisplay() {
		player.getPackets().closeInterface(isResizableScreen() ? "tab.xp_counter_resizeable" : "tab.xp_counter");
	}

	public void sendEquipment() {
		sendTab(isResizableScreen() ? "tab.equipment_resizeable" : "tab.equipment", "interface.equipment");
	}

	public void closeInterface(int one, int two) {
		player.getPackets().closeInterface(isResizableScreen() ? two : one);
	}

	public void closeEquipment() {
		player.getPackets().closeInterface(isResizableScreen() ? "tab.equipment_resizeable" : "tab.equipment");
	}

	public void sendInventory() {
		sendTab(isResizableScreen() ? "tab.inventory_resizeable" : "tab.inventory", Inventory.INVENTORY_INTERFACE);
	}

	public void closeInventory() {
		player.getPackets().closeInterface(isResizableScreen() ? "tab.inventory_resizeable" : "tab.inventory");
	}

	public void closeSkills() {
		player.getPackets().closeInterface(isResizableScreen() ? "tab.skills_resizeable" : "tab.skills");
	}

	public void closeCombatStyles() {
		player.getPackets().closeInterface(isResizableScreen() ? "tab.combatstyles_resizeable" : "tab.combatstyles");
	}

	public void closeTaskSystem() {
		player.getPackets().closeInterface(isResizableScreen() ? "tab.achievements_resizeable" : "tab.achievements");
	}

	public void sendCombatStyles() {
		sendTab(isResizableScreen() ? "tab.combatstyles_resizeable" : "tab.combatstyles", "interface.combat_interface");
	}

	public void sendTaskSystem() {
		sendTab(resizableScreen ? "tab.achievements_resizeable" : "tab.achievements", "interface.custom_quest");
	}

	public void sendSkills() {
		sendTab(isResizableScreen() ? "tab.skills_resizeable" : "tab.skills", "interface.skills");
	}

	public void sendSettings() {
		sendSettings(Rscm.lookup("interface.settings"));
	}

	public void sendSettings(int interfaceId) {
		sendTab(isResizableScreen() ? "tab.settings_resizeable" : "tab.settings", interfaceId);
	}

	public void sendPrayerBook() {
		String tab = isResizableScreen() ? "tab.prayerbook_resizeable" : "tab.prayerbook";
		sendTab(tab, "interface.prayerbook");
	}

	public void closePrayerBook() {
		int tabId = isResizableScreen() ? Rscm.lookup("tab.prayerbook_resizeable") : Rscm.lookup("tab.prayerbook");
		player.getPackets().closeInterface(tabId);
	}

	public void sendMagicBook() {
		sendTab(isResizableScreen() ? Rscm.lookup("tab.spellbook_resizeable") : Rscm.lookup("tab.spellbook"), player.getCombatDefinitions().getSpellBook());
		switch (player.getCombatDefinitions().getSpellBook()) {
		case 430:
			player.getPackets().sendHideIComponent("interface.lunar_spellbook", 24, true);
			player.getPackets().sendHideIComponent("interface.lunar_spellbook", 68, true);
			player.getPackets().sendHideIComponent("interface.lunar_spellbook", 70, true);
			player.getPackets().sendHideIComponent("interface.lunar_spellbook", 71, true);
			player.getPackets().sendHideIComponent("interface.lunar_spellbook", 77, true);
			break;
		}
	}

	public void closeMagicBook() {
		player.getPackets().closeInterface(isResizableScreen() ? Rscm.lookup("tab.spellbook_resizeable") : Rscm.lookup("tab.spellbook"));
	}

	public void sendEmotes() {
		sendTab(isResizableScreen() ? "tab.emotes_resizeable" : "tab.emotes", "interface.emotes");
	}

	public void closeEmotes() {
		player.getPackets().closeInterface(isResizableScreen() ? Rscm.lookup("tab.emotes_resizeable") : Rscm.lookup("tab.emotes"));
	}

	public boolean addInterface(int windowId, int tabId, int childId) {
		if (openedinterfaces.containsKey(tabId))
			player.getPackets().closeInterface(tabId);
		openedinterfaces.put(tabId, new int[] { childId, windowId });
		return openedinterfaces.get(tabId)[0] == childId;
	}

	public boolean containsInterface(int tabId, int childId) {
		if (childId == windowsPane)
			return true;
		if (!openedinterfaces.containsKey(tabId))
			return false;
		return openedinterfaces.get(tabId)[0] == childId;
	}

	public int getTabWindow(int tabId) {
		if (!openedinterfaces.containsKey(tabId))
			return FIXED_WINDOW_ID;
		return openedinterfaces.get(tabId)[1];
	}

	public boolean containsInterface(String inter) {
		int childId = Rscm.lookup(inter);
		if (childId == windowsPane)
			return true;
		for (int[] value : openedinterfaces.values())
			if (value[0] == childId)
				return true;
		return false;
	}

	public boolean containsInterface(int childId) {
		if (childId == windowsPane)
			return true;
		for (int[] value : openedinterfaces.values())
			if (value[0] == childId)
				return true;
		return false;
	}

	public boolean containsTab(int tabId) {
		return openedinterfaces.containsKey(tabId);
	}

	public boolean containsTab(String tab) {
		int tabId = Rscm.lookup(tab);
		return openedinterfaces.containsKey(tabId);
	}

	public void removeAll() {
		openedinterfaces.clear();
	}

	public boolean containsScreenInter() {
		return containsTab(isResizableScreen() ? RESIZABLE_SCREEN_TAB_ID : FIXED_SCREEN_TAB_ID);
	}

	public void closeScreenInterface() {
		player.getPackets().closeInterface(isResizableScreen() ? RESIZABLE_SCREEN_TAB_ID : FIXED_SCREEN_TAB_ID);
	}

	public boolean containsInventoryInter() {
		return containsTab(isResizableScreen() ? RESIZABLE_INV_TAB_ID : FIXED_INV_TAB_ID);
	}

	public void closeInventoryInterface() {
		player.getPackets().closeInterface(isResizableScreen() ? RESIZABLE_INV_TAB_ID : FIXED_INV_TAB_ID);
	}

	public boolean containsChatBoxInter() {
		return containsTab(CHAT_BOX_TAB);
	}

	public boolean removeTab(int tabId) {
		return openedinterfaces.remove(tabId) != null;
	}

	public boolean removeInterface(int tabId, int childId) {
		if (!openedinterfaces.containsKey(tabId))
			return false;
		if (openedinterfaces.get(tabId)[0] != childId)
			return false;
		return openedinterfaces.remove(tabId) != null;
	}

	public void sendFadingInterface(int backgroundInterface) {
		if (hasRezizableScreen())
			player.getPackets().sendInterface(true, RESIZABLE_WINDOW_ID, 12, backgroundInterface);
		else
			player.getPackets().sendInterface(true, FIXED_WINDOW_ID, 0, backgroundInterface);
	}

	public void closeFadingInterface() {
		if (hasRezizableScreen())
			player.getPackets().closeInterface(12);
		else
			player.getPackets().closeInterface(0);
	}

	public void sendScreenInterface(int backgroundInterface, int interfaceId) {
		player.getInterfaceManager().closeScreenInterface();

		if (hasRezizableScreen()) {
			player.getPackets().sendInterface(false, RESIZABLE_WINDOW_ID, 40, backgroundInterface);
			player.getPackets().sendInterface(false, RESIZABLE_WINDOW_ID, 41, interfaceId);
		} else {
			player.getPackets().sendInterface(false, FIXED_WINDOW_ID, 200, backgroundInterface);
			player.getPackets().sendInterface(false, FIXED_WINDOW_ID, 201, interfaceId);

		}

		player.setCloseInterfacesEvent(new Runnable() {
			@Override
			public void run() {
				if (hasRezizableScreen()) {
					player.getPackets().closeInterface(40);
					player.getPackets().closeInterface(41);
				} else {
					player.getPackets().closeInterface(200);
					player.getPackets().closeInterface(201);
				}
			}
		});
	}

	public void sendTabInterfaces(boolean hidden) {
		if (hidden) {
			player.getInterfaceManager().closeCombatStyles();
			player.getInterfaceManager().closeSkills();
			player.getInterfaceManager().closeInventory();
			player.getInterfaceManager().closeEquipment();
			player.getInterfaceManager().closePrayerBook();
			player.getInterfaceManager().closeMagicBook();
			player.getInterfaceManager().closeEmotes();
		} else {
			player.getInterfaceManager().sendCombatStyles();
			player.getCombatDefinitions().sendUnlockAttackStylesButtons();
			player.getInterfaceManager().sendQuestTab();
			player.getInterfaceManager().sendSkills();
			player.getInterfaceManager().sendInventory();
			player.getInventory().unlockInventoryOptions();
			player.getInterfaceManager().sendEquipment();
			player.getInterfaceManager().sendPrayerBook();
			player.getPrayer().refresh();
			player.getInterfaceManager().sendMagicBook();
			player.getInterfaceManager().sendEmotes();
			player.getEmotesManager().unlockEmotesBook();
			player.getInterfaceManager().sendTaskSystem();
			player.getInterfaceManager().sendQuestTab();
		}
	}

	public boolean hasRezizableScreen() {
		return isResizableScreen();
	}

	public void setWindowsPane(int windowsPane) {
		this.windowsPane = windowsPane;
	}

	public int getWindowsPane() {
		return windowsPane;
	}

	public void gazeOrbOfOculus() {
		player.getPackets().sendWindowsPane(475, 0);
		player.getPackets().sendInterface(true, 475, 57, 751);
		player.getPackets().sendInterface(true, 475, 55, 752);
		player.setCloseInterfacesEvent(new Runnable() {

			@Override
			public void run() {
				player.getPackets().sendWindowsPane(player.getInterfaceManager().hasRezizableScreen() ? 746 : 548, 0);
				player.getPackets().sendResetCamera();
			}

		});
	}

	//sum globalvarid 168 8
	//
	/*
	 * returns lastGameTab
	 */
	public int openGameTab(int tabId) {
		player.getPackets().sendGlobalVar(168, tabId);
		int lastTab = 4; // tabId
		// tab = tabId;
		return lastTab;
	}

	public boolean isResizableScreen() {
		return resizableScreen;
	}

	public void setResizableScreen(boolean resizableScreen) {
		this.resizableScreen = resizableScreen;
	}

	@SuppressWarnings("unused")
	private void clearChilds(int parentInterfaceId) {
		for (int key : openedinterfaces.keySet()) {
			if (key >> 16 == parentInterfaceId)
				openedinterfaces.remove(key);
		}
	}

	private void clearChildsB(int parentInterfaceId) {
		for (int key : openedinterfaces.keySet()) {
			if (key >> 16 == parentInterfaceId)
				openedinterfaces.remove(key);
		}
	}

	public void setInterface(boolean clickThrought, int parentInterfaceId, int parentInterfaceComponentId,
			int interfaceId) {
		int parentUID = getComponentUId(parentInterfaceId, parentInterfaceComponentId);
		Integer oldInterface = openedinterfacesb.get(parentUID);
		if (oldInterface != null)
			clearChildsB(oldInterface);
		openedinterfacesb.put(parentUID, interfaceId);
		player.getPackets().sendInterface(clickThrought, parentUID, interfaceId);
	}

	public int getComponentUId(int interfaceId, int componentId) {
		return interfaceId << 16 | componentId;
	}

	public int getRootInterface() {
		return rootInterface;
	}
	
	public void setRootInterface(int rootInterface, boolean gc) {
		this.rootInterface = rootInterface;
		player.getPackets().sendRootInterface(rootInterface, gc ? 3 : 0);
	}
	
	public void setDefaultRootInterface2() {
		setRootInterface(resizableScreen ? 746 : 548, false);
	}

	public void setRootInterface(int rootInterface) {
		this.rootInterface = rootInterface;
	}

}
