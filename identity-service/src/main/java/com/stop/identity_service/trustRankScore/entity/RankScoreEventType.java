package com.stop.identity_service.trustRankScore.entity;

public enum RankScoreEventType {
    MATCH_WIN,         // kazanan takım
    MATCH_LOSE,        // kaybeden takım
    MATCH_LEFT_EARLY,  // erken ayrılma
    PEER_RATING        // anket ortalaması (dinamik hesap)
}
