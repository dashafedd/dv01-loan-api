package dv01.loan.api.model.loan

import java.time.YearMonth

data class LoanFilter(
    val grades: Set<Char> = emptySet(),
    val states: Set<String> = emptySet(),
    val purposes: Set<String> = emptySet(),
    val from: YearMonth? = null,
    val to: YearMonth? = null,
    val ficoMin: Int? = null,
    val ficoMax: Int? = null,
) {
    /**
     * Returns true if loan satisfies every criterion from filter.
     * Empty sets and null bounds are treated as pass
     */
    fun matches(loan: Loan): Boolean =
        (grades.isEmpty() || loan.grade in grades) &&
                (states.isEmpty() || loan.state in states) &&
                (purposes.isEmpty() || loan.purpose in purposes) &&
                (from == null || !loan.issueMonth.isBefore(from)) &&
                (to == null || !loan.issueMonth.isAfter(to)) &&
                (ficoMin == null || loan.ficoMidpoint >= ficoMin) &&
                (ficoMax == null || loan.ficoMidpoint <= ficoMax)

    /**
     * Returns the active filter criteria ignoring anything unset.
     */
    fun getFilterDescription(): Map<String, Any> = buildMap {
        if (grades.isNotEmpty()) put("grade", grades.map(Char::toString).sorted())
        if (states.isNotEmpty()) put("state", states.sorted())
        if (purposes.isNotEmpty()) put("purpose", purposes.sorted())
        from?.let { put("from", it.toString()) }
        to?.let { put("to", it.toString()) }
        ficoMin?.let { put("ficoMin", it) }
        ficoMax?.let { put("ficoMax", it) }
    }
}