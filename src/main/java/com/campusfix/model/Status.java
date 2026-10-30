package com.campusfix.model;

import java.util.List;

public enum Status {
    SUBMITTED("Submitted", "bg-secondary"),
    ACKNOWLEDGED("Acknowledged", "bg-info text-dark"),
    ASSIGNED("Assigned", "bg-primary"),
    IN_PROGRESS("In Progress", "bg-warning text-dark"),
    RESOLVED("Resolved", "bg-success"),
    REJECTED("Rejected", "bg-danger");

    private final String label;
    private final String badgeClass;

    Status(String label, String badgeClass) { this.label = label; this.badgeClass = badgeClass; }

    public String getLabel() { return label; }
    public String getBadgeClass() { return badgeClass; }

    /** Students may edit or cancel only before a complaint is assigned or resolved. */
    public boolean isEditableByStudent() { return this == SUBMITTED || this == ACKNOWLEDGED; }

    public boolean isClosed() { return this == RESOLVED || this == REJECTED; }

    /** "Pending" on dashboards = not yet being worked on. */
    public static List<Status> pending() { return List.of(SUBMITTED, ACKNOWLEDGED, ASSIGNED); }
}
