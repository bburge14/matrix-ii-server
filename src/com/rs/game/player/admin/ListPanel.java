package com.rs.game.player.admin;

import com.rs.game.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Generic list panel using interface 623 (Adventurer's Log Settings).
 *
 * Layout:
 *   Component 66 = title
 *   Components 3, 5, 7, ..., 39 = 19 row text slots
 *   Click componentId = text componentId - 1 (e.g., row 3 click = 2)
 *   Done / X = client-side close, server gets no click event
 *
 * Pagination: if list size <= 19, all rows show. Otherwise row 1 (text=3)
 * shows "<< Previous" and row 19 (text=39) shows "Next >>", leaving 17
 * data rows per page.
 *
 * State on player temp attributes:
 *   list_panel_items   : List<String>          - the items
 *   list_panel_callback: ListCallback          - row click handler
 *   list_panel_page    : Integer               - current page (0-indexed)
 *   list_panel_title   : String                - displayed in header
 */
public final class ListPanel {

    public interface ListCallback {
        /** Called when a data row is clicked. Receives the item string and the player. */
        void onRowClicked(Player player, String item);
    }

    private ListPanel() {}

    public static final int IFACE = 623;

    // Text component IDs in display order (left col then right col)
    private static final int[] ROW_TEXT_IDS = {
        3, 5, 7, 9, 11, 13, 15, 17, 19, 21,    // left column (10 rows)
        23, 25, 27, 29, 31, 33, 35, 37, 39      // right column (9 rows)
    };
    private static final int ROWS_PER_PAGE = ROW_TEXT_IDS.length;       // 19
    private static final int DATA_ROWS_WHEN_PAGED = ROWS_PER_PAGE - 2;  // 17 (sacrifice first/last for nav)

    private static final int TITLE_COMPONENT = 66;

    public static void show(Player player, String title, List<String> items, ListCallback callback) {
        if (player.getRights() < 2) return;
        player.getTemporaryAttributtes().put("list_panel_title", title);
        player.getTemporaryAttributtes().put("list_panel_items", new ArrayList<String>(items));
        player.getTemporaryAttributtes().put("list_panel_callback", callback);
        player.getTemporaryAttributtes().put("list_panel_page", Integer.valueOf(0));
        try {
            player.getInterfaceManager().sendCentralInterface(IFACE);
            renderPage(player);
        } catch (Throwable t) {
            player.getPackets().sendGameMessage("ListPanel show failed: " + t.getMessage());
            t.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private static void renderPage(Player player) {
        Object titleObj = player.getTemporaryAttributtes().get("list_panel_title");
        Object itemsObj = player.getTemporaryAttributtes().get("list_panel_items");
        Object pageObj  = player.getTemporaryAttributtes().get("list_panel_page");
        if (!(itemsObj instanceof List)) return;

        List<String> items = (List<String>) itemsObj;
        int page = pageObj instanceof Integer ? (Integer) pageObj : 0;
        boolean paged = items.size() > ROWS_PER_PAGE;
        int dataPerPage = paged ? DATA_ROWS_WHEN_PAGED : ROWS_PER_PAGE;
        int totalPages = paged ? (items.size() + dataPerPage - 1) / dataPerPage : 1;

        // Title with optional page indicator
        String titleText = (titleObj == null ? "List" : titleObj.toString());
        if (paged) titleText += " (Page " + (page + 1) + "/" + totalPages + ")";
        write(player, TITLE_COMPONENT, titleText);

        // Clear all rows first
        for (int textId : ROW_TEXT_IDS) write(player, textId, "");

        if (paged) {
            // Row 1: << Previous (or empty if on page 1)
            write(player, ROW_TEXT_IDS[0], page > 0 ? "<col=ffaa00><< Previous Page</col>" : "");
            // Row 19: Next >> (or empty if on last page)
            write(player, ROW_TEXT_IDS[ROW_TEXT_IDS.length - 1], page < totalPages - 1 ? "<col=ffaa00>Next Page >></col>" : "");
            // Data rows: 17 slots starting at index 1
            int startItem = page * dataPerPage;
            for (int i = 0; i < DATA_ROWS_WHEN_PAGED; i++) {
                int itemIdx = startItem + i;
                if (itemIdx >= items.size()) break;
                write(player, ROW_TEXT_IDS[i + 1], items.get(itemIdx));
            }
        } else {
            // No pagination - all 19 rows are data
            for (int i = 0; i < items.size() && i < ROW_TEXT_IDS.length; i++) {
                write(player, ROW_TEXT_IDS[i], items.get(i));
            }
        }
    }

    /** Called from ButtonHandler on a click to interface 623. */
    @SuppressWarnings("unchecked")
    public static boolean handleClick(Player player, int clickComponentId) {
        if (player.getRights() < 2) return false;
        Object itemsObj    = player.getTemporaryAttributtes().get("list_panel_items");
        Object callbackObj = player.getTemporaryAttributtes().get("list_panel_callback");
        Object pageObj     = player.getTemporaryAttributtes().get("list_panel_page");
        if (!(itemsObj instanceof List) || !(callbackObj instanceof ListCallback)) return false;

        List<String> items = (List<String>) itemsObj;
        ListCallback callback = (ListCallback) callbackObj;
        int page = pageObj instanceof Integer ? (Integer) pageObj : 0;
        boolean paged = items.size() > ROWS_PER_PAGE;
        int dataPerPage = paged ? DATA_ROWS_WHEN_PAGED : ROWS_PER_PAGE;
        int totalPages = paged ? (items.size() + dataPerPage - 1) / dataPerPage : 1;

        // Convert click componentId back to a row index. click = text - 1.
        int rowIdx = -1;
        for (int i = 0; i < ROW_TEXT_IDS.length; i++) {
            if (ROW_TEXT_IDS[i] - 1 == clickComponentId) {
                rowIdx = i;
                break;
            }
        }
        if (rowIdx < 0) return false;

        if (paged) {
            if (rowIdx == 0) {
                // << Previous
                if (page > 0) {
                    player.getTemporaryAttributtes().put("list_panel_page", Integer.valueOf(page - 1));
                    renderPage(player);
                }
                return true;
            }
            if (rowIdx == ROW_TEXT_IDS.length - 1) {
                // Next >>
                if (page < totalPages - 1) {
                    player.getTemporaryAttributtes().put("list_panel_page", Integer.valueOf(page + 1));
                    renderPage(player);
                }
                return true;
            }
            // Data row clicked
            int itemIdx = page * dataPerPage + (rowIdx - 1);
            if (itemIdx >= 0 && itemIdx < items.size()) {
                callback.onRowClicked(player, items.get(itemIdx));
                return true;
            }
        } else {
            if (rowIdx >= 0 && rowIdx < items.size()) {
                callback.onRowClicked(player, items.get(rowIdx));
                return true;
            }
        }
        return false;
    }

    private static void write(Player player, int comp, String text) {
        try { player.getPackets().sendIComponentText(IFACE, comp, text); } catch (Throwable ignored) {}
    }
}
