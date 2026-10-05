package dv01.loan.api.model.loan

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Collects running totals for a set of loans, one loan at a time, and turns them into stats.
 * Only sums are kept, so the loans are read once and averages are calculated at the end.
 */
class LoanStatsAccumulator {
    // total loan amount added so far, readable from outside to calculate each group's share of the total
    var totalAmount: BigDecimal = BigDecimal.ZERO
        private set

    private companion object {
        val ONE_HUNDRED: BigDecimal = BigDecimal(100)
        const val SCALE = 2 // decimal places for averages and percentages
    }

    private var count = 0L
    private var rateSum: BigDecimal = BigDecimal.ZERO

    // sum of rate * amount, the numerator of the weighted average rate
    private var rateTimesAmountSum: BigDecimal = BigDecimal.ZERO

    /** Adds one loan to the running totals. */
    fun addLoan(loan: Loan) {
        count++
        totalAmount = totalAmount.add(loan.loanAmount)
        rateSum = rateSum.add(loan.interestRate)
        rateTimesAmountSum = rateTimesAmountSum.add(loan.interestRate.multiply(loan.loanAmount))
    }

    /** Stats for all loans added so far. */
    fun getGlobalStats(): LoanStats {
        // count as BigDecimal, so it can be used as a divisor
        val countAsDecimal = BigDecimal.valueOf(count)
        return LoanStats(
            loanCount = count,
            totalLoanAmount = totalAmount,
            avgLoanAmount = divide(totalAmount, countAsDecimal),
            avgInterestRate = divide(rateSum, countAsDecimal),
            // weighted by loan amount, so bigger loans affect the rate more
            weightedAvgInterestRate = divide(rateTimesAmountSum, totalAmount),
        )
    }

    /**
     * The same stats for one group, plus the group's share of grandTotalAmount in percent.
     * grandTotalAmount is the total loan amount of all matching loans, not only this group.
     */
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

    /** Divides and rounds to 2 decimal places. Returns 0.00 when the divisor is zero (no loans). */
    private fun divide(dividend: BigDecimal, divisor: BigDecimal): BigDecimal =
        if (divisor.signum() == 0) BigDecimal.ZERO.setScale(SCALE)
        else dividend.divide(divisor, SCALE, RoundingMode.HALF_UP)

}
