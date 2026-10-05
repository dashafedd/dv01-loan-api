package dv01.loan.api.loader

import java.math.BigDecimal

/**
 * Reason why a row from the file was not loaded.
 */
enum class RejectReason {
    MALFORMED_ROW,
    BAD_ID,
    BAD_AMOUNT,
    BAD_RATE,
    BAD_ISSUE_DATE,
    BAD_FICO,
    BAD_GRADE,
    BAD_STATE,
    BAD_PURPOSE
}

/**
 * Summary of one file load: how many rows were accepted and rejected, and why.
 * Also keeps funded amounts, to check that no rows were lost while reading the file.
 */
class LoadingReport(
    val accepted: Int,
    val rejected: Int,
    val rejectedByReason: Map<RejectReason, Int>,
    val loadMillis: Long,
    val acceptedFundedAmount: BigDecimal,
    val rejectedFundedAmount: BigDecimal,
    val footerFundedAmount: BigDecimal? // total written at the end of the file, null if the file has none
) {
    // accepted + rejected should add up to what the file says it funded.
    val reconciles: Boolean
        get() = footerFundedAmount?.compareTo(acceptedFundedAmount.add(rejectedFundedAmount)) == 0
}