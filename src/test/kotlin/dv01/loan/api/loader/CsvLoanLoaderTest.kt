package dv01.loan.api.loader

import dv01.loan.api.model.Loan
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.Path
import java.time.YearMonth

class CsvLoanLoaderTest {

    private val loader = CsvLoanLoader()

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun should_throwExceptionAndIncludeMessage_when_fileIsNotFound() {
        val path = Path.of("missing.csv")

        val exception = assertThrows(IllegalArgumentException::class.java) { loader.load(path) }

        assertEquals(
            "Loan data file is not found or not readable: ${path.toAbsolutePath()}.",
            exception.message,
        )
    }

    @Test
    fun should_throwExceptionAndIncludeMessage_when_fileIsNotReadable() {
        val path = Files.createFile(tempDir.resolve("unreadable.csv"))
        assumeTrue(path.toFile().setReadable(false))
        assumeFalse(Files.isReadable(path))

        val exception = assertThrows(IllegalArgumentException::class.java) { loader.load(path) }
        assertEquals(
            "Loan data file is not found or not readable: ${path.toAbsolutePath()}.",
            exception.message,
        )
    }

    @Test
    fun should_throwException_when_fileIsEmpty() {
        assertThrows(IllegalStateException::class.java) { loader.load(writeCsv()) }
    }

    @Test
    fun should_returnNoLoans_when_fileHasOnlyHeader() {
        val loans = loader.load(writeCsv(HEADER))

        assertTrue(loans.isEmpty())
    }

    @Test
    fun should_load298Loans_when_fileHasPreambleAndHeader() {
        val loans = loader.load(samplePath())

        assertEquals(298, loans.size)
    }

    @Test
    fun should_mapAllFields_when_rowIsValid() {
        val loan = loader.load(samplePath()).first()

        assertEquals("126285300", loan.id)
        assertEquals(YearMonth.of(2017, 12), loan.issueMonth)
        assertEquals("CA", loan.state)
        assertEquals('A', loan.grade)
        assertEquals(780, loan.ficoLow)
        assertEquals(784, loan.ficoHigh)
        assertEquals(BigDecimal("40000"), loan.loanAmount)
        assertEquals(BigDecimal("40000"), loan.fundedAmount)
        assertEquals(BigDecimal("6.08"), loan.interestRate)
        assertEquals("debt_consolidation", loan.purpose)
        assertEquals("Fully Paid", loan.status)
    }

    @Test
    fun should_preserveFileOrder_when_loadingRows() {
        val loans = loader.load(samplePath())

        assertEquals("126285300", loans.first().id)
        assertEquals("126248664", loans.last().id)
    }

    @Test
    fun should_mapLastRow_when_fileHasFooter() {
        val loan = loader.load(samplePath()).last()

        assertEquals("126248664", loan.id)
        assertEquals("OR", loan.state)
        assertEquals('C', loan.grade)
        assertEquals(690, loan.ficoLow)
        assertEquals(694, loan.ficoHigh)
        assertEquals(BigDecimal("30000"), loan.loanAmount)
        assertEquals(BigDecimal("13.59"), loan.interestRate)
        assertEquals("credit_card", loan.purpose)
        assertEquals("Fully Paid", loan.status)
    }

    @Test
    fun should_parseEveryField_when_allRowsAreValid() {
        val loans = loader.load(samplePath())

        assertTrue(loans.all { it.id != null })
        assertTrue(loans.all { it.issueMonth == YearMonth.of(2017, 12) })
        assertTrue(loans.all { it.grade != null })
        assertTrue(loans.all { it.ficoLow != null && it.ficoHigh != null })
        assertTrue(loans.all { it.loanAmount != null && it.fundedAmount != null })
        assertTrue(loans.all { it.interestRate != null })
        assertTrue(loans.all { it.state.length == 2 })
        assertTrue(loans.all { it.purpose.isNotEmpty() && it.status.isNotEmpty() })
    }

    @Test
    fun should_stripPercentSignAndPadding_when_parsingInterestRate() {
        val rates = loader.load(samplePath()).mapNotNull { it.interestRate }

        assertEquals(BigDecimal("5.32"), rates.min())
        assertEquals(BigDecimal("30.79"), rates.max())
    }

    @Test
    fun should_parseAmounts_when_loadingRows() {
        val loans = loader.load(samplePath())

        assertEquals(BigDecimal("4810575"), loans.sumOf { it.loanAmount!! })
        assertEquals(BigDecimal("4810575"), loans.sumOf { it.fundedAmount!! })
    }

    @Test
    fun should_parseFicoRange_when_loadingRows() {
        val loans = loader.load(samplePath())

        assertEquals(660, loans.minOf { it.ficoLow!! })
        assertEquals(839, loans.maxOf { it.ficoHigh!! })
    }

    @Test
    fun should_parseGrades_when_loadingRows() {
        val byGrade = loader.load(samplePath()).groupingBy { it.grade }.eachCount()

        assertEquals(
            mapOf('A' to 82, 'B' to 81, 'C' to 76, 'D' to 33, 'E' to 16, 'F' to 8, 'G' to 2),
            byGrade,
        )
    }

    @Test
    fun should_parseStatuses_when_loadingRows() {
        val byStatus = loader.load(samplePath()).groupingBy { it.status }.eachCount()

        assertEquals(
            mapOf("Current" to 162, "Fully Paid" to 103, "Charged Off" to 29, "Late (31-120 days)" to 4),
            byStatus,
        )
    }

    @Test
    fun should_setIdToNull_when_idIsBlank() {
        assertNull(loadRow("id" to "  ").id)
    }

    @Test
    fun should_setLoanAmountToNull_when_loanAmountIsNotPositiveNumber() {
        assertNull(loadRow("loan_amnt" to "").loanAmount)
        assertNull(loadRow("loan_amnt" to "abc").loanAmount)
        assertNull(loadRow("loan_amnt" to "0").loanAmount)
        assertNull(loadRow("loan_amnt" to "-5000").loanAmount)
    }

    @Test
    fun should_setFundedAmountToNull_when_fundedAmountIsNegativeOrNotNumber() {
        assertNull(loadRow("funded_amnt" to "").fundedAmount)
        assertNull(loadRow("funded_amnt" to "abc").fundedAmount)
        assertNull(loadRow("funded_amnt" to "-1").fundedAmount)
    }

    @Test
    fun should_setInterestRateToNull_when_interestRateIsNegativeOrNotNumber() {
        assertNull(loadRow("int_rate" to "").interestRate)
        assertNull(loadRow("int_rate" to "%").interestRate)
        assertNull(loadRow("int_rate" to "abc%").interestRate)
        assertNull(loadRow("int_rate" to "-1.5%").interestRate)
    }

    @Test
    fun should_setIssueMonthToNull_when_issueDateIsInvalid() {
        assertNull(loadRow("issue_d" to "").issueMonth)
        assertNull(loadRow("issue_d" to "2017-12").issueMonth)
        assertNull(loadRow("issue_d" to "Dec-17").issueMonth)
        assertNull(loadRow("issue_d" to "Foo-2017").issueMonth)
        assertNull(loadRow("issue_d" to "13-2017").issueMonth)
    }

    @Test
    fun should_setFicoToNull_when_ficoIsNotInteger() {
        val loan = loadRow("fico_range_low" to "7xx", "fico_range_high" to "704.5")

        assertNull(loan.ficoLow)
        assertNull(loan.ficoHigh)
    }

    @Test
    fun should_setGradeToNull_when_gradeIsBlank() {
        assertNull(loadRow("grade" to "").grade)
    }

    @Test
    fun should_keepOtherFields_when_oneFieldIsInvalid() {
        val loan = loadRow("loan_amnt" to "abc")

        assertNull(loan.loanAmount)
        assertEquals("1", loan.id)
        assertEquals(BigDecimal("10000"), loan.fundedAmount)
        assertEquals(BigDecimal("6.08"), loan.interestRate)
        assertEquals(YearMonth.of(2017, 12), loan.issueMonth)
        assertEquals("CA", loan.state)
    }

    @Test
    fun should_leaveFieldsEmpty_when_columnsAreMissingFromHeader() {
        val loan = loader.load(writeCsv("id,loan_amnt", "1,10000")).single()

        assertEquals("1", loan.id)
        assertEquals(BigDecimal("10000"), loan.loanAmount)
        assertNull(loan.fundedAmount)
        assertNull(loan.interestRate)
        assertNull(loan.issueMonth)
        assertNull(loan.ficoLow)
        assertNull(loan.ficoHigh)
        assertNull(loan.grade)
        assertEquals("", loan.state)
        assertEquals("", loan.purpose)
        assertEquals("", loan.status)
    }

    private fun writeCsv(vararg lines: String): Path =
        Files.write(tempDir.resolve("loans.csv"), lines.toList())

    private fun validRow(vararg overrides: Pair<String, String>): String =
        (VALID_VALUES + overrides).values.joinToString(",")

    private fun loadRow(vararg overrides: Pair<String, String>): Loan =
        loader.load(writeCsv(HEADER, validRow(*overrides))).single()

    private companion object {
        val VALID_VALUES = linkedMapOf(
            "id" to "1",
            "loan_amnt" to "10000",
            "funded_amnt" to "10000",
            "int_rate" to "6.08%",
            "issue_d" to "Dec-2017",
            "fico_range_low" to "700",
            "fico_range_high" to "704",
            "grade" to "A",
            "addr_state" to "CA",
            "purpose" to "credit_card",
            "loan_status" to "Current",
        )
        val HEADER = VALID_VALUES.keys.joinToString(",")
    }

    private fun samplePath(): Path =
        Path.of(checkNotNull(javaClass.getResource("/sample_300.csv")) { "sample_300.csv is missing from test resources" }.toURI())

}
