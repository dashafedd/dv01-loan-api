package dv01.loan.api.loader

import dv01.loan.api.model.loan.Loan
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVRecord
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.Path
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.ResolverStyle
import java.util.EnumMap
import java.util.Locale

/** What a load produces: the accepted loans and a report of what was accepted and rejected. */
data class LoadResult(val loans: List<Loan>, val report: LoadingReport)

/**
 * Reads the loan CSV file and converts each row into a Loan.
 * Rows with missing or invalid values are rejected and counted by reason, not loaded.
 * Only the columns listed below are read, the rest of the file is ignored.
 */
@Component
class CsvLoanLoader {

    private companion object {
        val log = LoggerFactory.getLogger(CsvLoanLoader::class.java)
        val FICO_RANGE = 300..850

        // default format handles quoted values that contain commas or line breaks
        val CSV_FORMAT: CSVFormat = CSVFormat.DEFAULT.builder()
            .setIgnoreEmptyLines(true)
            .build()

        val ISSUE_DATE: DateTimeFormatter = DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("MMM-uuuu")
            .toFormatter(Locale.ENGLISH)
            .withResolverStyle(ResolverStyle.STRICT)

        // column names as they appear in the file header
        const val ID_COLUMN = "id"
        const val LOAN_AMOUNT_COLUMN = "loan_amnt"
        const val FUNDED_AMOUNT_COLUMN = "funded_amnt"
        const val INTEREST_RATE_COLUMN = "int_rate"
        const val ISSUE_DATE_COLUMN = "issue_d"
        const val FICO_LOW_COLUMN = "fico_range_low"
        const val FICO_HIGH_COLUMN = "fico_range_high"
        const val GRADE_COLUMN = "grade"
        const val ADDRESS_STATE_COLUMN = "addr_state"
        const val PURPOSE_COLUMN = "purpose"
        const val STATUS_COLUMN = "loan_status"
    }

    /**
     * Loads all loans from the file in a single pass.
     * Fails if the file can't be read or has no header row.
     */
    fun load(path: Path): LoadResult {
        require(Files.isReadable(path)) {
            "Loan data file is not found or not readable: ${path.toAbsolutePath()}."
        }

        log.info("Loading loans from {}", path.toAbsolutePath())

        val loans = ArrayList<Loan>()
        var acceptedFunded = BigDecimal.ZERO
        var rejectedFunded = BigDecimal.ZERO
        var columnsMap: Map<String, Int>? = null //map column name to position in the record
        val rejectedByReason = EnumMap<RejectReason, Int>(RejectReason::class.java)
        var footerTotal: BigDecimal? = null
        val startedAt = System.nanoTime()

        Files.newBufferedReader(path).use { reader ->
            CSV_FORMAT.parse(reader).use { parser ->
                for (record in parser) {
                    if (record.size() == 0) continue

                    val firstCell = record[0].trim()
                    // the file starts with a notes line, so skip everything until the header row
                    if (columnsMap == null) {
                        if (firstCell == ID_COLUMN) columnsMap = indexColumns(record)
                        continue
                    }

                    // the file ends with total lines, they are not loans
                    if (firstCell.startsWith("Total amount funded")) {
                        footerTotal = parseFooterTotal(firstCell) ?: footerTotal
                        continue
                    }

                    // a row with a different number of values than the header can't be read reliably
                    if (record.size() != columnsMap.size) {
                        rejectedByReason.merge(RejectReason.MALFORMED_ROW, 1, Int::plus)
                        continue
                    }

                    // read the funded amount separately, so rejected rows still count in the footer check
                    val funded = columnsMap[FUNDED_AMOUNT_COLUMN]
                        ?.let { record[it].trim().toBigDecimalOrNull() }
                        ?: BigDecimal.ZERO

                    when (val outcome = parseLoan(record, columnsMap)) {
                        is ParseOutcome.Accepted -> {
                            loans += outcome.loan
                            acceptedFunded = acceptedFunded.add(outcome.loan.fundedAmount)
                        }

                        is ParseOutcome.Rejected -> {
                            rejectedByReason.merge(outcome.reason, 1, Int::plus)
                            rejectedFunded = rejectedFunded.add(funded)
                        }
                    }
                }
            }
        }

        checkNotNull(columnsMap) { "No header row found in ${path.toAbsolutePath()}." }

        log.info("Loaded {} loans from {}", loans.size, path.toAbsolutePath())

        val report = LoadingReport(
            accepted = loans.size,
            rejected = rejectedByReason.values.sum(),
            rejectedByReason = rejectedByReason.toMap(),
            loadMillis = (System.nanoTime() - startedAt) / 1_000_000,
            acceptedFundedAmount = acceptedFunded,
            rejectedFundedAmount = rejectedFunded,
            footerFundedAmount = footerTotal,
        )

        return LoadResult(loans, report)
    }

    /**
     * Converts one row into a Loan.
     * Stops at the first invalid value and returns it as the reject reason.
     */
    private fun parseLoan(record: CSVRecord, columns: Map<String, Int>): ParseOutcome {
        // value of the given column in this row, trimmed
        fun column(name: String): String = columns[name]?.let { record[it].trim() } ?: ""

        val id = column(ID_COLUMN).takeIf { it.isNotEmpty() }
            ?: return ParseOutcome.Rejected(RejectReason.BAD_ID)
        val loanAmount = column(LOAN_AMOUNT_COLUMN).toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO }
            ?: return ParseOutcome.Rejected(RejectReason.BAD_AMOUNT)
        val fundedAmount = column(FUNDED_AMOUNT_COLUMN).toBigDecimalOrNull()?.takeIf { it >= BigDecimal.ZERO }
            ?: return ParseOutcome.Rejected(RejectReason.BAD_AMOUNT)
        val rate =
            column(INTEREST_RATE_COLUMN).removeSuffix("%").trim().toBigDecimalOrNull()?.takeIf { it >= BigDecimal.ZERO }
                ?: return ParseOutcome.Rejected(RejectReason.BAD_RATE)
        val issueMonth = runCatching {
            YearMonth.parse(
                column(ISSUE_DATE_COLUMN),
                ISSUE_DATE
            )
        }.getOrNull()
            ?: return ParseOutcome.Rejected(RejectReason.BAD_ISSUE_DATE)

        val state = column(ADDRESS_STATE_COLUMN).uppercase(Locale.ROOT)
            .takeIf { it.length == 2 && it.all(Char::isLetter) }
            ?: return ParseOutcome.Rejected(RejectReason.BAD_STATE)

        val ficoLow = column(FICO_LOW_COLUMN).toIntOrNull()
            ?: return ParseOutcome.Rejected(RejectReason.BAD_FICO)
        val ficoHigh = column(FICO_HIGH_COLUMN).toIntOrNull()
            ?: return ParseOutcome.Rejected(RejectReason.BAD_FICO)
        if (ficoLow !in FICO_RANGE || ficoHigh !in FICO_RANGE || ficoHigh < ficoLow) {
            return ParseOutcome.Rejected(RejectReason.BAD_FICO)
        }
        val grade = column(GRADE_COLUMN).firstOrNull()?.uppercaseChar()
            ?: return ParseOutcome.Rejected(RejectReason.BAD_GRADE)
        val purpose = column(PURPOSE_COLUMN).lowercase(Locale.ROOT).takeIf { it.isNotEmpty() }
            ?: return ParseOutcome.Rejected(RejectReason.BAD_PURPOSE)

        return ParseOutcome.Accepted(
            Loan(
                id = id,
                issueMonth = issueMonth,
                state = state,
                grade = grade,
                ficoLow = ficoLow,
                ficoHigh = ficoHigh,
                loanAmount = loanAmount,
                fundedAmount = fundedAmount,
                interestRate = rate,
                purpose = purpose,
                status = column(STATUS_COLUMN),
            )
        )
    }

    /** Maps each column name in the header to its position, so values can be read by name. */
    private fun indexColumns(header: CSVRecord): Map<String, Int> =
        header.toList().withIndex().associate { (index, name) -> name.trim() to index }

    /** Result of parsing one row: either a loan or the reason it was rejected. */
    private sealed interface ParseOutcome {
        data class Accepted(val loan: Loan) : ParseOutcome
        data class Rejected(val reason: RejectReason) : ParseOutcome
    }

    /**
     * Reads the total from the line
     * Only policy code 1 is used, the policy code 2 total is ignored.
     */
    private fun parseFooterTotal(cell: String): BigDecimal? =
        cell.takeIf { it.startsWith("Total amount funded in policy code 1") }
            ?.substringAfter(':')
            ?.trim()
            ?.toBigDecimalOrNull()

}