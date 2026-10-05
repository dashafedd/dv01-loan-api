package dv01.loan.api.controller

import dv01.loan.api.service.LoanFilterOptions
import dv01.loan.api.service.LoanQueryService
import dv01.loan.api.service.SummaryResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Read-only HTTP API over the loaded loan dataset.
 * Handles routing only: parameter validation lives in SummaryRequestParser,
 * filtering and aggregation in LoanQueryService.
 */
@RestController
@RequestMapping("/api/v1/loans")
class LoanController(
    private val service: LoanQueryService,
    private val parser: SummaryRequestParser,
) {

    /**
     * Returns the values present in the dataset (months, grades, states, purposes, FICO bands),
     * i.e. what a client can pass as filters to /summary.
     */
    @GetMapping("/options")
    fun options(): LoanFilterOptions = service.options

    /**
     * Returns aggregated stats for the loans matching the filters, in total and per group.
     * Every parameter is optional; with none given, all loans are summarized by grade.
     *
     * groupBy          - grade (default), state, month, ficoBand or purpose
     * grade, state,
     * purpose          - comma-separated lists; a loan matches if it has any of the values
     * from, to         - inclusive issue month range in yyyy-MM format
     * ficoMin, ficoMax - inclusive range (300-850), compared against the midpoint of the loan's FICO range
     *
     * Parameters are taken as raw strings so that all invalid ones can be reported
     * together in a single 400 response instead of failing on the first.
     */
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
        val request = parser.parse(
            groupBy = groupBy,
            grade = grade,
            state = state,
            purpose = purpose,
            from = from,
            to = to,
            ficoMin = ficoMin,
            ficoMax = ficoMax,
        )
        return service.summary(groupBy = request.groupBy, filter = request.filter)
    }
}
