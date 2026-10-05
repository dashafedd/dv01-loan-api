package dv01.loan.api.repository

import dv01.loan.api.loader.LoadingReport
import dv01.loan.api.model.loan.Loan

interface LoanRepository {
    val loans: List<Loan>
    val report: LoadingReport
}