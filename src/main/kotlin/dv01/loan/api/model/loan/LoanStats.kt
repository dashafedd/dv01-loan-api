package dv01.loan.api.model.loan

import java.math.BigDecimal
import java.math.RoundingMode

data class LoanStats(
    val loanCount: Long,
    val totalLoanAmount: BigDecimal,
    val avgLoanAmount: BigDecimal,
    val avgInterestRate: BigDecimal,
    val weightedAvgInterestRate: BigDecimal,
)

data class GroupStats(
    val key: String,
    val loanCount: Long,
    val totalLoanAmount: BigDecimal,
    val avgLoanAmount: BigDecimal,
    val avgInterestRate: BigDecimal,
    val weightedAvgInterestRate: BigDecimal,
    val percentOfTotalAmount: BigDecimal,
)

class StatsAccumulator {
    var totalAmount: BigDecimal = BigDecimal.ZERO

    private companion object {
        val ONE_HUNDRED: BigDecimal = BigDecimal(100)
        const val SCALE = 2
    }

    private var count = 0L
    private var rateSum: BigDecimal = BigDecimal.ZERO
    private var rateTimesAmountSum: BigDecimal = BigDecimal.ZERO

    fun addLoan(loan: Loan) {
        count++
        totalAmount = totalAmount.add(loan.loanAmount)
        rateSum = rateSum.add(loan.interestRate)
        rateTimesAmountSum = rateTimesAmountSum.add(loan.interestRate.multiply(loan.loanAmount))
    }

    fun getGlobalStats(): LoanStats {
        //converted to Long for divide operation
        val countAsDecimal = BigDecimal.valueOf(count)
        return LoanStats(
            loanCount = count,
            totalLoanAmount = totalAmount,
            avgLoanAmount = divide(totalAmount, countAsDecimal),
            avgInterestRate = divide(rateSum, countAsDecimal),
            weightedAvgInterestRate = divide(rateTimesAmountSum, totalAmount),
        )
    }

    fun getGroupStats(key: String, grandTotalAmount: BigDecimal): GroupStats {
        val stats = getGlobalStats()
        return GroupStats(
            key = key,
            loanCount = stats.loanCount,
            totalLoanAmount = stats.totalLoanAmount,
            avgLoanAmount = stats.avgLoanAmount,
            avgInterestRate = stats.avgInterestRate,
            weightedAvgInterestRate = stats.weightedAvgInterestRate,
            percentOfTotalAmount = divide(totalAmount.multiply(ONE_HUNDRED), grandTotalAmount),
        )
    }

    private fun divide(dividend: BigDecimal, divisor: BigDecimal): BigDecimal =
        if (divisor.signum() == 0) BigDecimal.ZERO.setScale(SCALE)
        else dividend.divide(divisor, SCALE, RoundingMode.HALF_UP)

}