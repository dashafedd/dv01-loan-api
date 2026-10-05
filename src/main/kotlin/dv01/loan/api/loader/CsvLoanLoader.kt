package dv01.loan.api.loader

import dv01.loan.api.model.Loan
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
import java.util.Locale

@Component
class CsvLoanLoader {

    private companion object {
        val log = LoggerFactory.getLogger(CsvLoanLoader::class.java)

        val CSV_FORMAT: CSVFormat = CSVFormat.DEFAULT.builder()
            .setIgnoreEmptyLines(true)
            .build()

        val ISSUE_DATE: DateTimeFormatter = DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("MMM-uuuu")
            .toFormatter(Locale.ENGLISH)
            .withResolverStyle(ResolverStyle.STRICT)

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

    fun load(path: Path): List<Loan> {
        require(Files.isReadable(path)) {
            "Loan data file is not found or not readable: ${path.toAbsolutePath()}."
        }
        log.info("Loading loans from {}", path.toAbsolutePath())

        val loans = ArrayList<Loan>()
        //map column name to position in the record
        var columnsMap: Map<String, Int>? = null

        Files.newBufferedReader(path).use { reader ->
            CSV_FORMAT.parse(reader).use { parser ->
                for (record in parser) {
                    if (record.size() == 0) continue

                    val firstCell = record[0].trim()
                    if (columnsMap == null) {
                        if (firstCell == ID_COLUMN) columnsMap = indexColumns(record)
                        continue
                    }

                    //to check
                    if (firstCell.startsWith("Total amount funded")) {
                        continue
                    }

                    val outcome = parseLoan(record, columnsMap)
                    loans += outcome
                }
            }
        }

        checkNotNull(columnsMap) { "No header row found in ${path.toAbsolutePath()}." }

        log.info("Loaded {} loans from {}", loans.size, path.toAbsolutePath())

        return loans
    }

    private fun parseLoan(record: CSVRecord, columns: Map<String, Int>): Loan {
        fun column(name: String): String = columns[name]?.let { record[it].trim() } ?: ""

        val id = column(ID_COLUMN).takeIf { it.isNotEmpty() } // to check
        val loanAmount = column(LOAN_AMOUNT_COLUMN).toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO }
        val fundedAmount = column(FUNDED_AMOUNT_COLUMN).toBigDecimalOrNull()?.takeIf { it >= BigDecimal.ZERO }
        val rate =
            column(INTEREST_RATE_COLUMN).removeSuffix("%").trim().toBigDecimalOrNull()?.takeIf { it >= BigDecimal.ZERO }
        val issueMonth = runCatching {
            YearMonth.parse(
                column(ISSUE_DATE_COLUMN),
                ISSUE_DATE
            )
        }.getOrNull()
        val state = column(ADDRESS_STATE_COLUMN).uppercase(Locale.ROOT)
        val ficoLow = column(FICO_LOW_COLUMN).toIntOrNull()
        val ficoHigh = column(FICO_HIGH_COLUMN).toIntOrNull()
        val grade = column(GRADE_COLUMN).firstOrNull()?.uppercaseChar()

        return Loan(
            id = id,
            issueMonth = issueMonth,
            state = state,
            grade = grade,
            ficoLow = ficoLow,
            ficoHigh = ficoHigh,
            loanAmount = loanAmount,
            fundedAmount = fundedAmount,
            interestRate = rate,
            purpose = column(PURPOSE_COLUMN),
            status = column(STATUS_COLUMN),
        )
    }

    private fun indexColumns(header: CSVRecord): Map<String, Int> =
        header.toList().withIndex().associate { (index, name) -> name.trim() to index }

}