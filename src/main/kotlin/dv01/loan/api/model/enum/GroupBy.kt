package dv01.loan.api.model.enum

import dv01.loan.api.model.FicoBand
import dv01.loan.api.model.loan.Loan

enum class GroupBy(val parameterValue: String) {
    GRADE("grade"),
    STATE("state"),
    MONTH("month"),
    FICO_BAND("ficoBand"),
    PURPOSE("purpose");

    fun getGroupForLoan(loan: Loan): String = when (this) {
        GRADE -> loan.grade.toString()
        STATE -> loan.state
        MONTH -> loan.issueMonth.toString()
        FICO_BAND -> FicoBand.labelFor(loan.ficoMidpoint)
        PURPOSE -> loan.purpose
    }
}