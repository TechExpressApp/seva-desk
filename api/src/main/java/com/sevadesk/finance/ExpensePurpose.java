package com.sevadesk.finance;

/** What an expense was for. */
public enum ExpensePurpose {
    BUILDING_MAINTENANCE("Building Maintenance"),
    CONSTRUCTION("Construction"),
    DEITY_WORSHIP("Deity Worship"),
    GROCERY("Grocery"),
    MONK_DONATION("Monk Donation"),
    BOOKS("Books"),
    OTHER("Other");

    private final String label;

    ExpensePurpose(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
