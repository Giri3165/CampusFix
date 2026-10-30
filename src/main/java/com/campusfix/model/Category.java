package com.campusfix.model;

public enum Category {
    ELECTRICAL("Electrical (fans, lights)"),
    PLUMBING("Plumbing / Water leakage"),
    WIFI("Wi-Fi / Network"),
    LAB_EQUIPMENT("Laboratory equipment"),
    CLEANLINESS("Cleanliness / Washrooms"),
    FURNITURE("Desks and chairs"),
    CIVIL("Building / Civil"),
    OTHER("Other");

    private final String label;

    Category(String label) { this.label = label; }
    public String getLabel() { return label; }
}
