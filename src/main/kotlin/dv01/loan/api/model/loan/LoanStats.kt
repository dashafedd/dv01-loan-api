package dv01.loan.api.model.loan

import java.math.BigDecimal

/**
 * Aggregated numbers for a set of loans. Used for the totals of a summary.
 * Averages and percentages are rounded to 2 decimal places, and are 0.00 when there are no loans.
 */
data class LoanStats(
    val loanCount: Long,
    val totalLoanAmount: BigDecimal,
    val avgLoanAmount: BigDecimal,
    val avgInterestRate: BigDecimal,
    val weightedAvgInterestRate: BigDecimal
)

/** The same numbers for one group of a summary. */
data class GroupStats(
    val key: String, // the group's value, e.g. "A", "CA" or "2017-12"
    val loanCount: Long,
    val totalLoanAmount: BigDecimal,
    val avgLoanAmount: BigDecimal,
    val avgInterestRate: BigDecimal,
    val weightedAvgInterestRate: BigDecimal,
    val percentOfTotalAmount: BigDecimal, // this group's share of the total loan amount of all matching loans
)