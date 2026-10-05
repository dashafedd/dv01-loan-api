package dv01.loan.api.controller

import dv01.loan.api.controller.error.ParameterErrors
import dv01.loan.api.model.GroupBy
import dv01.loan.api.model.loan.LoanFilter
import dv01.loan.api.service.LoanQueryService
import org.springframework.stereotype.Component
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/** A /summary request after validation: typed and safe to pass to the service. */
data class SummaryRequest(val groupBy: GroupBy, val filter: LoanFilter)

/**
 * Validates the raw query parameters of /summary and converts them to a SummaryRequest.
 * Uses LoanQueryService because grade, state and purpose are checked against
 * the values actually present in the dataset.
 */
@Component
class SummaryRequestParser(private val service: LoanQueryService) {

    /**
     * Checks every parameter before failing, so the client gets all problems in one response.
     * A null or blank parameter means "no filter" and is never an error.
     * Throws InvalidRequestException (returned as 400) if at least one parameter was rejected.
     */
    fun parse(
        groupBy: String?,
        grade: String?,
        state: String?,
        purpose: String?,
        from: String?,
        to: String?,
        ficoMin: String?,
        ficoMax: String?,
    ): SummaryRequest {
        val errors = ParameterErrors()
        val availableOptions = service.options

        var groupByParsed = GroupBy.GRADE //group by grade by default
        if (!groupBy.isNullOrBlank()) {
            val parsed = GroupBy.parse(groupBy)
            if (parsed == null) {
                errors.reject("groupBy", "must be one of: ${GroupBy.allowedValues()}")
            } else {
                groupByParsed = parsed
            }
        }

        // grade and state are case-insensitive: values are uppercased to match how the loader stores them.
        // A grade is a single letter, so valid values are narrowed to Char for LoanFilter.
        val grades = splitUpper(grade).onEach { value ->
            if (value !in availableOptions.grades) errors.reject("grade", "'$value' is not a grade in this dataset")
        }.mapNotNull { it.firstOrNull() }.toSet()

        val states = splitUpper(state).onEach { value ->
            if (value !in availableOptions.states) errors.reject("state", "'$value' is not a state in this dataset")
        }.toSet()

        // purpose is matched exactly as it appears in the file (lowercase, e.g. debt_consolidation)
        val purposes = split(purpose).onEach { value ->
            if (value !in availableOptions.purposes) errors.reject(
                "purpose",
                "'$value' is not a purpose in this dataset"
            )
        }.toSet()

        // range order is only checked when both bounds parsed, to avoid a second error for the same parameter
        val fromMonth = month(from, "from", errors)
        val toMonth = month(to, "to", errors)
        if (fromMonth != null && toMonth != null && fromMonth > toMonth) {
            errors.reject("from", "must not be after 'to'")
        }

        val minFico = fico(ficoMin, "ficoMin", errors)
        val maxFico = fico(ficoMax, "ficoMax", errors)
        if (minFico != null && maxFico != null && minFico > maxFico) {
            errors.reject("ficoMin", "must not be greater than 'ficoMax'")
        }

        errors.throwIfAny()

        return SummaryRequest(
            groupBy = groupByParsed,
            filter = LoanFilter(
                grades = grades,
                states = states,
                purposes = purposes,
                from = fromMonth,
                to = toMonth,
                ficoMin = minFico,
                ficoMax = maxFico,
            ),
        )
    }

    /** Parses a yyyy-MM month. Returns null when absent, or when invalid (after recording the error). */
    private fun month(raw: String?, parameter: String, errors: ParameterErrors): YearMonth? {
        if (raw.isNullOrBlank()) return null
        return try {
            YearMonth.parse(raw.trim(), MONTH_FORMAT)
        } catch (_: DateTimeParseException) {
            errors.reject(parameter, "must be a month in yyyy-MM format, e.g. 2017-12")
            null
        }
    }

    /** Parses a FICO score within 300-850. Returns null when absent, or when invalid (after recording the error). */
    private fun fico(raw: String?, parameter: String, errors: ParameterErrors): Int? {
        if (raw.isNullOrBlank()) return null
        val value = raw.trim().toIntOrNull()
        if (value == null || value !in FICO_RANGE) {
            errors.reject(parameter, "must be a whole number between ${FICO_RANGE.first} and ${FICO_RANGE.last}")
            return null
        }
        return value
    }

    /** Splits a comma-separated list, trimming values and dropping empty ones. */
    private fun split(raw: String?): List<String> =
        raw?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()

    private fun splitUpper(raw: String?) = split(raw).map { it.uppercase() }

    private companion object {
        val MONTH_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("uuuu-MM")
        val FICO_RANGE = 300..850
    }

}
