package dv01.loan.api.controller

import dv01.loan.api.controller.error.ParameterErrors
import dv01.loan.api.model.enum.GroupBy
import dv01.loan.api.model.loan.LoanFilter
import dv01.loan.api.service.LoanFilterOptions
import dv01.loan.api.service.LoanQueryService
import dv01.loan.api.service.SummaryResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@RestController
@RequestMapping("/api/v1/loans")
class LoanController(private val service: LoanQueryService) {

    private companion object {
        val MONTH_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("uuuu-MM")
        val FICO_RANGE = 300..850
    }

    @GetMapping("/options")
    fun options(): LoanFilterOptions = service.options

    @GetMapping("/summary")
    fun summary(
        @RequestParam(required = false) groupBy: String?,
        @RequestParam(required = false) grade: String?,
        @RequestParam(required = false) state: String?,
        @RequestParam(required = false) purpose: String?,
        @RequestParam(required = false) from: String?,
        @RequestParam(required = false) to: String?,
        @RequestParam(required = false) ficoMin: String?,
        @RequestParam(required = false) ficoMax: String?,
    ): SummaryResponse {
        val errors = ParameterErrors()
        val availableOptions = service.options

        var groupByParsed = GroupBy.GRADE //group by grade by default
        if (groupBy != null) {
            val parsed = GroupBy.parse(groupBy)
            if (parsed == null) {
                errors.reject("groupBy", "must be one of: ${GroupBy.allowedValues()}")
            } else {
                groupByParsed = parsed
            }
        }

        val grades = splitUpper(grade).onEach { value ->
            if (value !in availableOptions.grades) errors.reject("grade", "'$value' is not a grade in this dataset")
        }.mapNotNull { it.firstOrNull() }.toSet()

        val states = splitUpper(state).onEach { value ->
            if (value !in availableOptions.states) errors.reject("state", "'$value' is not a state in this dataset")
        }.toSet()

        val purposes = split(purpose).onEach { value ->
            if (value !in availableOptions.purposes) errors.reject(
                "purpose",
                "'$value' is not a purpose in this dataset"
            )
        }.toSet()

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

        return service.summary(
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

    private fun month(raw: String?, parameter: String, errors: ParameterErrors): YearMonth? {
        if (raw.isNullOrBlank()) return null
        return try {
            YearMonth.parse(raw.trim(), MONTH_FORMAT)
        } catch (_: DateTimeParseException) {
            errors.reject(parameter, "must be a month in yyyy-MM format, e.g. 2017-12")
            null
        }
    }

    private fun fico(raw: String?, parameter: String, errors: ParameterErrors): Int? {
        if (raw.isNullOrBlank()) return null
        val value = raw.trim().toIntOrNull()
        if (value == null || value !in FICO_RANGE) {
            errors.reject(parameter, "must be a whole number between ${FICO_RANGE.first} and ${FICO_RANGE.last}")
            return null
        }
        return value
    }

    private fun split(raw: String?): List<String> =
        raw?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()

    private fun splitUpper(raw: String?) = split(raw).map { it.uppercase() }

}