package com.stop.identity_service.trustRankScore;

import java.math.BigDecimal;

public final class ScoreDeltaConstants {

    private ScoreDeltaConstants() {}

    // ========================
    // TRUST SCORE DELTA'LARI
    // ========================

    /** Maçı tamamladığında kazanılan trust */
    public static final BigDecimal TRUST_MATCH_COMPLETED  = new BigDecimal("0.3");

    /** Maçtan erken ayrılma */
    public static final BigDecimal TRUST_MATCH_LEFT_EARLY = new BigDecimal("-1.0");

    // ========================
    // RANK SCORE DELTA'LARI
    // ========================

    /** Kazanan takımda olmak */
    public static final BigDecimal RANK_MATCH_WIN        = new BigDecimal("0.3");

    /** Kaybeden takımda olmak */
    public static final BigDecimal RANK_MATCH_LOSE       = new BigDecimal("-0.3");

    /** Maçtan erken ayrılma */
    public static final BigDecimal RANK_MATCH_LEFT_EARLY = new BigDecimal("-1.0");
}
