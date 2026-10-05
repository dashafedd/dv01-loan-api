package dv01.loan.api.repository.impl

import dv01.loan.api.config.LoanDataProperties
import dv01.loan.api.loader.CsvLoanLoader
import dv01.loan.api.loader.LoadResult
import dv01.loan.api.loader.LoadingReport
import dv01.loan.api.model.loan.Loan
import dv01.loan.api.repository.LoanRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.nio.file.Path

/**
 * Keeps the loans from the CSV file in memory.
 * The file is read once, when the app starts, so a missing or unreadable file stops the startup.
 * After that the loans never change.
 */
@Repository
class CsvLoanRepository(loader: CsvLoanLoader, properties: LoanDataProperties) : LoanRepository {
    private companion object {
        val log = LoggerFactory.getLogger(CsvLoanRepository::class.java)
    }

    // path comes from "loan-data.path" in application.yaml
    private val loaded: LoadResult = loader.load(Path.of(properties.path))

    override val loans: List<Loan> = loaded.loans
    override val report: LoadingReport = loaded.report

    // logs the result of the load, and warns if the loaded amounts don't add up to the total in the file
    init {
        log.info(
            "Accepted {} loans in {} ms, {} rejected {}",
            report.accepted, report.loadMillis, report.rejected, report.rejectedByReason
        )
        if (report.footerFundedAmount == null) {
            log.warn("No footer total in the file, can't check funded amounts")
        } else if (!report.reconciles) {
            log.warn(
                "Funded amounts don't match the file footer ({}), some rows may have been skipped",
                report.footerFundedAmount
            )
        }
    }

}