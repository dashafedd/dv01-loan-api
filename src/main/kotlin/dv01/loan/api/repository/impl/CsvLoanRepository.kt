package dv01.loan.api.repository.impl

import dv01.loan.api.config.LoanDataProperties

import dv01.loan.api.model.Loan
import dv01.loan.api.repository.LoanRepository
import org.springframework.stereotype.Repository
import java.nio.file.Path

@Repository
class CsvLoanRepository(loader: loan.api.loader.CsvLoanLoader, properties: LoanDataProperties) : LoanRepository {
    override val loans: List<Loan> = loader.load(Path.of(properties.path))
}