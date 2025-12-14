package com.rs.java.game.player.content.customtab;

import com.rs.java.game.player.Player;
import com.rs.java.game.player.Skills;
import com.rs.java.game.player.controlers.EdgevillePvPControler;
import com.rs.java.game.player.controlers.WildernessControler;
import com.rs.java.utils.HexColours;
import com.rs.java.utils.HexColours.Colour;
import com.rs.java.utils.Utils;

public class SettingsTab extends CustomTab {

    public enum SettingsStore {

        TITLE(25) {
            @Override
            public void usage(Player p) {
            }

            @Override
            public String text(Player p) {
                return "Toggles";
            }
        },

        SETTINGS(3) {
            @Override
            public void usage(Player p) {
            }

            @Override
            public String text(Player p) {
                return "<u>Settings";
            }
        },

        BREAKVIALS(4) {
            @Override
            public void usage(Player p) {
                p.toggles.put("BREAK_VIALS", !p.toggles("BREAK_VIALS", false));
            }

            @Override
            public String text(Player p) {
                return "Break Vials: " + (p.toggles("BREAK_VIALS", false) ? "<col=04BB3B>On" : "<col=BB0404>Off");
            }
        },

        IGNORE_LOWPRICE_ITEMS(5) {
            @Override
            public void usage(Player p) {
                p.toggles.put("IGNORE_LOW_VALUE", !p.toggles("IGNORE_LOW_VALUE", false));
            }

            @Override
            public String text(Player p) {
                return "Dont Drop Free Items: " + (p.toggles("IGNORE_LOW_VALUE", false) ? "<col=04BB3B>On" : "<col=BB0404>Off");
            }
        },

        ITEMSLOOK(6) {
            @Override
            public void usage(Player p) {
                p.switchItemsLook();
            }

            @Override
            public String text(Player p) {
                return "Item Visuals: " + (p.isOldItemsLook() ? "<col=04BB3B>2011" : "<col=04BB3B>2012");
            }
        },

        SHIFTDROP(7) {
            @Override
            public void usage(Player p) {
                p.switchShiftDrop();
            }

            @Override
            public String text(Player p) {
                return "Shift Dropping: " + (p.isShiftDrop() ? "<col=04BB3B>On" : "<col=BB0404>Off");
            }
        },

        DRAGSETTING(8) {
            @Override
            public void usage(Player p) {
                p.switchSlowDrag();
                p.message("You have to relog before noticing changes.");
            }

            @Override
            public String text(Player p) {
                return "Slow Drag: " + (p.isSlowDrag() ? "<col=04BB3B>On" : "<col=BB0404>Off");
            }
        },

        ZOOMSETTING(9) {
            @Override
            public void usage(Player p) {
                p.switchZoom();
            }

            @Override
            public String text(Player p) {
                return "Zoom: " + (p.isZoom() ? "<col=04BB3B>On" : "<col=BB0404>Off");
            }
        },

        HEALTH_OVERLAY(10) {
            @Override
            public void usage(Player p) {
                p.toggles.put("HEALTH_OVERLAY", !p.toggles("HEALTH_OVERLAY", false));
                p.temporaryAttribute().remove("overlay_state");
            }

            @Override
            public String text(Player p) {
                return "Health Overlay: " + (p.toggles("HEALTH_OVERLAY", false) ? "<col=04BB3B>On" : "<col=BB0404>Off");
            }
        },
        HITCHANCE(11) {
            @Override
            public void usage(Player p) {
                p.toggles.put("HITCHANCE_OVERLAY", !p.toggles("HITCHANCE_OVERLAY", false));
                p.temporaryAttribute().remove("overlay_state");
            }

            @Override
            public String text(Player p) {
                return "Hitchance Overlay: " + (p.toggles("HITCHANCE_OVERLAY", false) ? "<col=04BB3B>On" : "<col=BB0404>Off");
            }
        },
        LEVEL_BOOST(12) {
            @Override
            public void usage(Player p) {
                p.toggles.put("LEVELSTATUS_OVERLAY", !p.toggles("LEVELSTATUS_OVERLAY", false));
                p.temporaryAttribute().remove("overlay_state");
            }

            @Override
            public String text(Player p) {
                return "Affected stats Overlay: " + (p.toggles("LEVELSTATUS_OVERLAY", false) ? "<col=04BB3B>On" : "<col=BB0404>Off");
            }
        },

        KDRINTER(13) {
            @Override
            public void usage(Player p) {
                p.toggles.put("KDRINTER", !p.toggles("KDRINTER", false));
                if (p.getInterfaceManager().containsTab(31) && !p.toggles("KDRINTER", false)) {
                    p.getInterfaceManager().closeTab(p.getInterfaceManager().isResizableScreen(), 31);
                } else {
                    if (WildernessControler.isAtWild(p) || EdgevillePvPControler.isAtBank(p) || EdgevillePvPControler.isAtPvP(p)) {
                        WildernessControler.showKDRInter(p);
                    }
                }
            }

            @Override
            public String text(Player p) {
                return "KDR Overlay: " + (p.toggles("KDRINTER", false) ? "<col=04BB3B>On" : "<col=BB0404>Off");
            }
        },

        COMBATSETTINGS(15) {
            @Override
            public void usage(Player p) {
            }

            @Override
            public String text(Player p) {
                return "<u>Combat Settings";
            }
        },

        ONEXPPERHIT(16) {
            @Override
            public void usage(Player p) {
                p.toggles.put("ONEXPPERHIT", !p.toggles("ONEXPPERHIT", false));
                p.getSkills().switchXPPopup(true);
                p.getSkills().switchXPPopup(true);
            }

            @Override
            public String text(Player p) {
                return "One XP per Hit: " + (p.toggles("ONEXPPERHIT", false) ? "<col=04BB3B>On" : "<col=BB0404>Off");
            }
        },

        ONEXHITS(17) {
            @Override
            public void usage(Player p) {
                boolean active = p.getVarsManager().getBitValue(1485) == 1;
                p.getVarsManager().sendVarBit(1485, active ? 0 : 1, true);
                p.getVarsManager().forceSendVarBit(9816, p.getPrayer().getPrayerPoints());
                p.refreshHitPoints();
                p.getSkills().switchXPPopup(true);
                p.getSkills().switchXPPopup(true);
            }

            @Override
            public String text(Player p) {
                boolean active = p.getVarsManager().getBitValue(1485) == 1;
                return "1x Hitpoints & prayer: " + (active ? "<col=04BB3B>On" : "<col=BB0404>Off");
            }
        },

        DROPS(19) {
            @Override
            public void usage(Player p) {
            }

            @Override
            public String text(Player p) {
                return "<u>Drop Settings";
            }
        },

        LOOTBEAMS(20) {
            @Override
            public void usage(Player p) {
                p.toggles.put("LOOTBEAMS", !p.toggles("LOOTBEAMS", false));
            }

            @Override
            public String text(Player p) {
                return "Lootbeams: " + (p.toggles("LOOTBEAMS", false) ? "<col=04BB3B>On" : "<col=BB0404>Off");
            }
        },

        UNTRADEABLEMESSAGE(21) {
            @Override
            public void usage(Player p) {
                p.toggles.put("UNTRADEABLEMESSAGE", !p.toggles("UNTRADEABLEMESSAGE", false));
            }

            @Override
            public String text(Player p) {
                return "Untradeable Message: "
                        + (p.toggles("UNTRADEABLEMESSAGE", false) ? "<col=04BB3B>On" : "<col=BB0404>Off");
            }
        },

        DROPVALUE(22) {
            @Override
            public void usage(Player p) {
                p.getTemporaryAttributtes().put("SET_DROPVALUE", Boolean.TRUE);
                p.getPackets().sendRunScript(108, new Object[]{"Enter Amount:"});
            }

            @Override
            public String text(Player p) {
                int dropValue = Integer.parseInt(p.getToggleValue(p.toggles.get("DROPVALUE")));
                return "Valuable Drop: " + (dropValue < 1 ? HexColours.getMessage(Colour.RED, "0 - click to set") : HexColours.getMessage(Colour.GREEN, "" + Utils.getFormattedNumber(dropValue, ',') + " gp"));
            }
        },

        DEVELOPER_SETTINGS(29) {
            @Override
            public void usage(Player p) {
            }

            @Override
            public String text(Player p) {
                return "<u>Developer Settings";
            }
        },
        INTERACTIVE(30) {
            @Override
            public void usage(Player p) {
                p.switchDeveloperMode();
            }

            @Override
            public String text(Player p) {
                return "Developer Mode: " + (p.isDeveloperMode() ? "<col=04BB3B>On" : "<col=BB0404>Off");
            }
        },
        ;

        private int compId;

        private SettingsStore(int compId) {
            this.compId = compId;
        }

        public abstract String text(Player p);

        public abstract void usage(Player p);

    }

    public static void open(Player player) {
        sendComponents(player);
        for (int i = 3; i <= 22; i++)
            player.getPackets().sendHideIComponent(3002, i, true);
        for (int i = 28; i <= 56; i++)
            player.getPackets().sendHideIComponent(3002, i, true);
        player.getTemporaryAttributtes().put("CUSTOMTAB", 2);
        player.getPackets().sendHideIComponent(3002, BACK_BUTTON, false);
        player.getPackets().sendHideIComponent(3002, FORWARD_BUTTON, false);
        player.getPackets().sendSpriteOnIComponent(3002, RED_STAR_COMP, RED_HIGHLIGHTED);
        for (SettingsStore store : SettingsStore.values()) {
            if (store != null) {
                player.getPackets().sendHideIComponent(3002, store.compId, false);
                if (store.text(player) != null) {
                    player.getPackets().sendTextOnComponent(3002, store.compId, store.text(player));
                }
            }
        }
    }

    public static void handleButtons(Player player, int compId) {
        for (SettingsStore store : SettingsStore.values()) {
            if (store != null) {
                if (compId != store.compId)
                    continue;
                store.usage(player);
                open(player);
            }
        }
        switch (compId) {
            case BACK_BUTTON:
                TeleportTab.open(player);
                break;
            case FORWARD_BUTTON:
                QuestTab.open(player);
                break;
            default:
                break;
        }
    }
}
