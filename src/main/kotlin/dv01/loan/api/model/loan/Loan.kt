package dv01.loan.api.model.loan

import java.math.BigDecimal
import java.time.YearMonth

class Loan(
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
    val ficoMidpoint: Int get() = (ficoLow + ficoHigh) / 2
}