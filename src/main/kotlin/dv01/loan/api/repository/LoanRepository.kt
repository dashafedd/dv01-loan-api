package dv01.loan.api.repository

import dv01.loan.api.loader.LoadingReport
import dv01.loan.api.model.loan.Loan

/**
 * Source of the loans the API works with.
 * The service depends on this interface, not on where the loans come from (currently a CSV file).
 */
interface LoanRepository {
    // all loaded loans, already validated
    val loans: List<Loan>

    // how the load went: accepted and rejected rows, and the funded amount check
    val report: LoadingReport
}
