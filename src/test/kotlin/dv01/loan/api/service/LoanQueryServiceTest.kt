package dv01.loan.api.service

import dv01.loan.api.loader.CsvLoanLoader
import dv01.loan.api.loader.LoadResult
import dv01.loan.api.loader.LoadingReport
import dv01.loan.api.model.GroupBy
import dv01.loan.api.model.loan.GroupStats
import dv01.loan.api.model.loan.Loan
import dv01.loan.api.model.loan.LoanFilter
import dv01.loan.api.model.loan.LoanStats
import dv01.loan.api.repository.LoanRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.nio.file.Path
import java.time.YearMonth

class LoanQueryServiceTest {

    private val service = LoanQueryService(object : LoanRepository {
        override val loans: List<Loan> = SAMPLE.loans
        override val report: LoadingReport = SAMPLE.report
    })

    @Test
    fun should_summarizeAllLoans_when_filterIsEmpty() {
        val response = service.summary(GroupBy.GRADE, LoanFilter())

        assertEquals(stats(298, "4810575", "16142.87", "12.73", "13.10"), response.totals)
        assertTrue(response.filters.isEmpty())
    }

    @Test
    fun should_returnSameResponse_when_calledRepeatedly() {
        val first = service.summary(GroupBy.GRADE, LoanFilter())
        val second = service.summary(GroupBy.GRADE, LoanFilter())

        assertEquals(first, second)
    }

    @Test
    fun should_groupByGrade_when_groupByIsGrade() {
        val response = service.summary(GroupBy.GRADE, LoanFilter())

        assertEquals("grade", response.groupBy)
        assertEquals(
            listOf(
                group("A", 82, "1263225", "15405.18", "6.81", "6.73", "26.26"),
                group("B", 81, "1108325", "13683.02", "10.63", "10.65", "23.04"),
                group("C", 76, "1460650", "19219.08", "14.05", "14.00", "30.36"),
                group("D", 33, "528625", "16018.94", "18.85", "19.15", "10.99"),
                group("E", 16, "233275", "14579.69", "24.18", "24.49", "4.85"),
                group("F", 8, "166475", "20809.38", "29.32", "29.27", "3.46"),
                group("G", 2, "50000", "25000.00", "30.79", "30.79", "1.04"),
            ),
            response.groups,
        )
    }

    @Test
    fun should_groupByFicoBand_when_groupByIsFicoBand() {
        val response = service.summary(GroupBy.FICO_BAND, LoanFilter())

        assertEquals("ficoBand", response.groupBy)
        assertEquals(
            listOf(
                group("660-679", 62, "922725", "14882.66", "16.56", "17.42", "19.18"),
                group("680-699", 69, "1229350", "17816.67", "14.22", "14.61", "25.56"),
                group("700-719", 56, "859425", "15346.88", "12.38", "12.15", "17.87"),
                group("720-739", 54, "822025", "15222.69", "11.57", "12.30", "17.09"),
                group("740-759", 26, "433550", "16675.00", "9.44", "9.61", "9.01"),
                group("760-779", 16, "330800", "20675.00", "7.94", "8.47", "6.88"),
                group("780-799", 8, "132200", "16525.00", "6.75", "7.42", "2.75"),
                group("800-819", 6, "60500", "10083.33", "5.81", "5.91", "1.26"),
                group("820-839", 1, "20000", "20000.00", "5.32", "5.32", "0.42"),
            ),
            response.groups,
        )
    }

    @Test
    fun should_groupByState_when_groupByIsState() {
        val response = service.summary(GroupBy.STATE, LoanFilter())
        val keys = response.groups.map { it.key }

        assertEquals("state", response.groupBy)
        assertEquals(43, keys.size)
        assertEquals(keys.sorted(), keys)
        assertEquals(
            group("CA", 39, "696450", "17857.69", "12.80", "12.49", "14.48"),
            response.groups.single { it.key == "CA" },
        )
    }

    @Test
    fun should_groupByPurpose_when_groupByIsPurpose() {
        val response = service.summary(GroupBy.PURPOSE, LoanFilter())

        assertEquals("purpose", response.groupBy)
        assertEquals(12, response.groups.size)
        assertEquals(
            group("debt_consolidation", 153, "2696625", "17625.00", "13.74", "13.69", "56.06"),
            response.groups.single { it.key == "debt_consolidation" },
        )
        assertEquals(
            group("renewable_energy", 1, "3000", "3000.00", "16.02", "16.02", "0.06"),
            response.groups.single { it.key == "renewable_energy" },
        )
    }

    @Test
    fun should_returnSingleGroupWithFullShare_when_groupByIsMonth() {
        val response = service.summary(GroupBy.MONTH, LoanFilter())

        assertEquals("month", response.groupBy)
        assertEquals(
            listOf(group("2017-12", 298, "4810575", "16142.87", "12.73", "13.10", "100.00")),
            response.groups,
        )
    }

    @Test
    fun should_addUpToTotals_when_summingGroups() {
        GroupBy.entries.forEach { groupBy ->
            val response = service.summary(groupBy, LoanFilter())

            assertEquals(298, response.groups.sumOf { it.loanCount }, "groupBy=$groupBy")
            assertEquals(BigDecimal("4810575"), response.groups.sumOf { it.totalLoanAmount }, "groupBy=$groupBy")
        }
    }

    @Test
    fun should_computeShareOfFilteredTotal_when_filteringByGrade() {
        val response = service.summary(GroupBy.GRADE, LoanFilter(grades = setOf('F', 'G')))

        assertEquals(mapOf("grade" to listOf("F", "G")), response.filters)
        assertEquals(stats(10, "216475", "21647.50", "29.62", "29.62"), response.totals)
        assertEquals(
            listOf(
                group("F", 8, "166475", "20809.38", "29.32", "29.27", "76.90"),
                group("G", 2, "50000", "25000.00", "30.79", "30.79", "23.10"),
            ),
            response.groups,
        )
    }

    @Test
    fun should_keepOnlyMatchingStates_when_filteringByState() {
        val response = service.summary(GroupBy.STATE, LoanFilter(states = setOf("NY", "CA")))

        assertEquals(mapOf("state" to listOf("CA", "NY")), response.filters)
        assertEquals(stats(69, "1193250", "17293.48", "13.41", "13.73"), response.totals)
        assertEquals(listOf("CA", "NY"), response.groups.map { it.key })
    }

    @Test
    fun should_applyAllCriteria_when_filtersAreCombined() {
        val filter = LoanFilter(grades = setOf('B'), states = setOf("CA"), purposes = setOf("debt_consolidation"))

        val response = service.summary(GroupBy.PURPOSE, filter)

        assertEquals(stats(4, "87500", "21875.00", "11.33", "11.48"), response.totals)
        assertEquals(
            listOf(group("debt_consolidation", 4, "87500", "21875.00", "11.33", "11.48", "100.00")),
            response.groups,
        )
    }

    @Test
    fun should_includeBounds_when_filteringByFicoRange() {
        val response = service.summary(GroupBy.FICO_BAND, LoanFilter(ficoMin = 700, ficoMax = 719))

        assertEquals(mapOf("ficoMin" to 700, "ficoMax" to 719), response.filters)
        assertEquals(stats(56, "859425", "15346.88", "12.38", "12.15"), response.totals)
        assertEquals(listOf("700-719"), response.groups.map { it.key })
    }

    @Test
    fun should_compareFicoMidpoint_when_filteringByFico() {
        // loans reported as 700-704 have midpoint 702
        val exact = service.summary(GroupBy.GRADE, LoanFilter(ficoMin = 702, ficoMax = 702))
        val above = service.summary(GroupBy.GRADE, LoanFilter(ficoMin = 703))
        val below = service.summary(GroupBy.GRADE, LoanFilter(ficoMax = 701))

        assertEquals(13, exact.totals.loanCount)
        assertEquals(154, above.totals.loanCount)
        assertEquals(131, below.totals.loanCount)
    }

    @Test
    fun should_includeBounds_when_filteringByIssueMonth() {
        val december = YearMonth.of(2017, 12)

        val response = service.summary(GroupBy.MONTH, LoanFilter(from = december, to = december))

        assertEquals(mapOf("from" to "2017-12", "to" to "2017-12"), response.filters)
        assertEquals(298, response.totals.loanCount)
    }

    @Test
    fun should_returnEmptySummary_when_noLoanMatchesFilter() {
        val filters = listOf(
            LoanFilter(grades = setOf('Z')),
            LoanFilter(states = setOf("XX")),
            LoanFilter(purposes = setOf("wedding")),
            LoanFilter(from = YearMonth.of(2018, 1)),
            LoanFilter(to = YearMonth.of(2017, 11)),
            LoanFilter(ficoMin = 838),
            LoanFilter(ficoMax = 661),
            LoanFilter(ficoMin = 720, ficoMax = 700),
        )

        filters.forEach { filter ->
            val response = service.summary(GroupBy.GRADE, filter)

            assertEquals(stats(0, "0", "0.00", "0.00", "0.00"), response.totals, "filter=$filter")
            assertTrue(response.groups.isEmpty(), "filter=$filter")
        }
    }

    private fun stats(count: Long, total: String, avg: String, avgRate: String, weightedAvgRate: String): LoanStats =
        LoanStats(count, BigDecimal(total), BigDecimal(avg), BigDecimal(avgRate), BigDecimal(weightedAvgRate))

    private fun group(
        key: String,
        count: Long,
        total: String,
        avg: String,
        avgRate: String,
        weightedAvgRate: String,
        percent: String,
    ): GroupStats = GroupStats(
        key,
        count,
        BigDecimal(total),
        BigDecimal(avg),
        BigDecimal(avgRate),
        BigDecimal(weightedAvgRate),
        BigDecimal(percent),
    )

    private companion object {
        val SAMPLE: LoadResult = CsvLoanLoader().load(
            Path.of(
                checkNotNull(LoanQueryServiceTest::class.java.getResource("/sample_300.csv")) {
                    "sample_300.csv is missing from test resources"
                }.toURI(),
            ),
        )
    }

}
