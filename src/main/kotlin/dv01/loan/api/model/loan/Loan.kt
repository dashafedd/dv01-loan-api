package dv01.loan.api.model.loan

import java.math.BigDecimal
import java.time.YearMonth

/**
 * One loan from the file, with only the fields the API needs.
 * Values are already validated and normalized by the loader.
 */
data class Loan(
    val id: String,
    val issueMonth: YearMonth,
    val state: String,
    val grade: Char,
    val ficoLow: Int,
    val ficoHigh: Int,
    val loanAmount: BigDecimal,
    val fundedAmount: BigDecimal,
    val interestRate: BigDecimal,
    val purpose: String,
    val status: String,
) {
    // the file gives FICO as a range (e.g. 660-664), the midpoint is used as the loan's single score
    val ficoMidpoint: Int get() = (ficoLow + ficoHigh) / 2
}