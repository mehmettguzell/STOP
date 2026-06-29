package com.stop.identity_service.trustRankScore.entity;

public enum TrustScoreEventType {
    MATCH_COMPLETED,     // +0.3
    MATCH_LEFT_EARLY,    // -1.0
    REPORT_RESOLVED,     // ileride eklenecek
    MODERATION_WARNED,   // ileride eklenecek
    MODERATION_BANNED,   // ileride eklenecek
    ADMIN_OVERRIDE       // manuel
}
