package dv01.loan.api.service

import dv01.loan.api.model.FicoBand
import dv01.loan.api.model.loan.GroupStats
import dv01.loan.api.model.loan.LoanFilter
import dv01.loan.api.model.loan.LoanStats
import dv01.loan.api.model.loan.StatsAccumulator
import dv01.loan.api.model.enum.GroupBy
import dv01.loan.api.repository.LoanRepository
import org.springframework.stereotype.Service

data class SummaryResponse(
    val groupBy: String,
    val filters: Map<String, Any>,
    val totals: LoanStats,
    val groups: List<GroupStats>,
)

data class LoanFilterOptions(
    val recordCount: Int,
    val issueMonths: List<String>,
    val grades: List<String>,
    val states: List<String>,
    val purposes: List<String>,
    val ficoBands: List<String>,
)

@Service
class LoanQueryService(private val repository: LoanRepository) {

    fun summary(groupBy: GroupBy, filter: LoanFilter): SummaryResponse {
        val loansStats = StatsAccumulator()
        val groupedLoansStats = HashMap<String, StatsAccumulator>()

        for (loan in repository.loans) {
            if (filter.matches(loan)) {
                loansStats.addLoan(loan)
                groupedLoansStats.getOrPut(groupBy.getGroupForLoan(loan)) { StatsAccumulator() }.addLoan(loan)
            }
        }

        return SummaryResponse(
            groupBy = groupBy.parameterValue,
            filters = filter.getFilterDescription(),
            totals = loansStats.getGlobalStats(),
            groups = groupedLoansStats.entries
                .sortedBy { it.key }
                .map { (key, accumulator) -> accumulator.getGroupStats(key, loansStats.totalAmount) },
        )
    }

    val options: LoanFilterOptions by lazy {
        val loans = repository.loans
        LoanFilterOptions(
            recordCount = loans.size,
            issueMonths = loans.map { it.issueMonth.toString() }.distinct().sorted(),
            grades = loans.map { it.grade.toString() }.distinct().sorted(),
            states = loans.map { it.state }.distinct().sorted(),
            purposes = loans.map { it.purpose }.distinct().sorted(),
            ficoBands = loans.map { FicoBand.labelFor(it.ficoMidpoint) }.distinct().sorted(),
        )
    }

}