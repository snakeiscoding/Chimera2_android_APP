package com.jht.chimera.service.ui.panels;

import java.util.UUID;

// using UUID so that these can be referenced externally if needed
public enum PanelIds {
    // Standard workout stat panels
    BOTTOM_PANEL("31d40440-ccac-425f-9b81-4eea585d99e1"),
    LEFT_PANEL("866b3959-81cc-48d4-ad82-9fc588cc99a7"),
    RIGHT_PANEL("786eda30-576f-4c48-bb0e-96c7edfbbabf"),
    TOP_PANEL("53732eb3-6af6-4980-b115-9d651f6839a5"),

    // Auto Hide POC panels
    TOP_PANEL2("1b21de82-475f-4e44-b0f2-e555d1013688"),
    BOTTOM_PANEL2("74a4f539-7e45-4dcf-a615-16332f1eedbb"),

    // Countdown Panel
    COUNTDOWN_PANEL("07b554d0-d577-431c-acb5-bbb76ef496bb"),

    // Other Panels
    OVERLAY_PANEL("e3be5610-ef23-44af-8d87-658d3866b2a4"),
    REVERSE_BELT_PANEL("4bc2ef92-d9ec-4a34-acb8-fda92280b390"),
    REVERSE_COUNTDOWN_PANEL("bfe6f556-6fb0-4c7e-92a5-0a90acfd6549"),
    TEAM_VIEWER_PANEL("4d578f70-0055-4c52-b5a8-ef400dd089e9"),
    TREAD_SENSE_PANEL("19f98041-b051-4aee-baa8-4be2da4d61b6"),
    VALUE_UPDATE_PANEL("81143ce5-aae2-457b-9d80-add8170609bb"),
    E_STOP_PANEL("fadbd86a-edfe-44f6-8167-0cd6e8505184"),
    BORDER_LEFT_PANEL("81a726e2-d280-473f-b235-09eabc96969f"),
    BORDER_RIGHT_PANEL("03730291-7fd9-43ae-a109-7711a53ca5a6"),
    FLOATING_MINIMIZE_PANEL("83c26e02-0466-404b-9d96-f1644a26af30"),

    //Message center
    MESSAGE_TOP_PANEL("717c9a1f-c72f-4da2-83e3-844bb1cf1ccf"),
    MESSAGE_FULL_PANEL("eaf38ba9-43f6-44ed-bc5d-10a291fd1274"),

    //Debug
    DEBUG_PANEL("f064ea8c-78ad-4fe6-b918-5f6fca78d27d"),
    ; // END OF LIST

    public final UUID id;

    PanelIds(String id) { this.id = UUID.fromString(id); }

    public static PanelIds findById(String id) {
        try {
            UUID uid = UUID.fromString(id);
            for (PanelIds item : values()) {
                if (item.id.equals(uid)) return item;
            }
        } catch (Exception ignored) {}
        return null;
    }
}
