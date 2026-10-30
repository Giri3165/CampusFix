package com.campusfix.service;

public record DashboardStats(long total, long pending, long inProgress, long resolved, long rejected) {
}
