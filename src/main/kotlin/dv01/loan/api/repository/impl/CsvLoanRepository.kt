package dv01.loan.api.repository.impl

import dv01.loan.api.config.LoanDataProperties
import dv01.loan.api.loader.CsvLoanLoader
import dv01.loan.api.loader.LoadResult
import dv01.loan.api.loader.LoadingReport
import dv01.loan.api.model.loan.Loan
import dv01.loan.api.repository.LoanRepository
import org.springframework.stereotype.Repository
import java.nio.file.Path

@Repository
class CsvLoanRepository(loader: CsvLoanLoader, properties: LoanDataProperties) : LoanRepository {
    private val loaded: LoadResult = loader.load(Path.of(properties.path))

    override val loans: List<Loan> = loaded.loans
    override val report: LoadingReport = loaded.report
}