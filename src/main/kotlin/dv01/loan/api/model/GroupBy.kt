package dv01.loan.api.model

import dv01.loan.api.model.loan.Loan

/**
 * Loan attributes the summary can be grouped by.
 * parameterValue is the name used in the groupBy request parameter.
 */
enum class GroupBy(val parameterValue: String) {
    GRADE("grade"),
    STATE("state"),
    MONTH("month"),
    FICO_BAND("ficoBand"),
    PURPOSE("purpose");

    /** Returns the group this loan belongs to. */
    fun getGroupForLoan(loan: Loan): String = when (this) {
        GRADE -> loan.grade.toString()
        STATE -> loan.state
        MONTH -> loan.issueMonth.toString()
        FICO_BAND -> FicoBand.labelFor(loan.ficoMidpoint)
        PURPOSE -> loan.purpose
    }

    companion object {
        /** Finds the value by its parameter name, ignoring case. Returns null if there is no match. */
        fun parse(value: String): GroupBy? =
            entries.firstOrNull { it.parameterValue.equals(value, ignoreCase = true) }

        /** Lists all parameter names, for use in error messages. */
        fun allowedValues(): String = entries.joinToString(", ") { it.parameterValue }
    }
}