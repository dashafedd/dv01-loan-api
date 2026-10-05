package dv01.loan.api.service

import dv01.loan.api.model.FicoBand
import dv01.loan.api.model.loan.LoanStatsAccumulator
import dv01.loan.api.model.GroupBy
import dv01.loan.api.model.loan.GroupStats
import dv01.loan.api.model.loan.LoanFilter
import dv01.loan.api.model.loan.LoanStats
import dv01.loan.api.repository.LoanRepository
import org.springframework.stereotype.Service

/** Response of /summary: the stats for all matching loans, and the same stats per group. */
data class SummaryResponse(
    val groupBy: String, // the attribute the loans were grouped by, as named in the request
    val filters: Map<String, Any>, // only the filters that were applied
    val totals: LoanStats,
    val groups: List<GroupStats>, // sorted by group key
)

/** Response of /options: the values present in the dataset, each list sorted and without duplicates. */
data class LoanFilterOptions(
    val recordCount: Int, // number of loaded loans
    val issueMonths: List<String>,
    val grades: List<String>,
    val states: List<String>,
    val purposes: List<String>,
    val ficoBands: List<String>,
)

/**
 * Summaries loan data, provides available filter values.
 * Works on the loans kept in memory by the repository.
 */
@Service
class LoanQueryService(private val repository: LoanRepository) {

    /**
     * Calculates stats for the loans matching the filter, in total and for each group.
     * Reads the loans once: every matching loan is added to the totals and to its group.
     */
    fun summary(groupBy: GroupBy, filter: LoanFilter): SummaryResponse {
        val loansStats = LoanStatsAccumulator()
        // one accumulator per group, created when the first loan of that group is found
        val groupedLoansStats = HashMap<String, LoanStatsAccumulator>()

        for (loan in repository.loans) {
            if (filter.matches(loan)) {
                loansStats.addLoan(loan)
                groupedLoansStats.getOrPut(groupBy.getGroupForLoan(loan)) { LoanStatsAccumulator() }.addLoan(loan)
            }
        }

        return SummaryResponse(
            groupBy = groupBy.parameterValue,
            filters = filter.getFilterDescription(),
            totals = loansStats.getGlobalStats(),
            groups = groupedLoansStats.entries
                .sortedBy { it.key }
                // the grand total is passed in, so each group can calculate its share of it
                .map { (key, accumulator) -> accumulator.getGroupStats(key, loansStats.totalAmount) },
        )
    }

    /**
     * Values a client can use as filters.
     * Calculated on first use and then reused, because the loans never change after startup.
     */
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