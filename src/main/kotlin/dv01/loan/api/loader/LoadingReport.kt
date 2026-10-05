package dv01.loan.api.loader

import java.math.BigDecimal

enum class RejectReason {
    MALFORMED_ROW,
    BAD_ID,
    BAD_AMOUNT,
    BAD_RATE,
    BAD_ISSUE_DATE,
    BAD_FICO,
    BAD_GRADE,
}

class LoadingReport(
    val accepted: Int,
    val rejected: Int,
    val rejectedByReason: Map<RejectReason, Int>,
    val loadMillis: Long,
    val acceptedFundedAmount: BigDecimal,
    val rejectedFundedAmount: BigDecimal,
)