package dv01.loan.api.repository

import dv01.loan.api.model.Loan

interface LoanRepository {
    val loans: List<Loan>
}