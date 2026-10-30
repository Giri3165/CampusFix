package com.campusfix.model;

public enum Priority {
    LOW("Low", "bg-success"),
    MEDIUM("Medium", "bg-warning text-dark"),
    HIGH("High", "bg-danger");

    private final String label;
    private final String badgeClass;

    Priority(String label, String badgeClass) { this.label = label; this.badgeClass = badgeClass; }
    public String getLabel() { return label; }
    public String getBadgeClass() { return badgeClass; }
}
