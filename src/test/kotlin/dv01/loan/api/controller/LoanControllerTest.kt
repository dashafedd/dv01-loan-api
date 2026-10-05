package dv01.loan.api.controller

import dv01.loan.api.controller.error.ApiExceptionHandler
import dv01.loan.api.loader.CsvLoanLoader
import dv01.loan.api.loader.LoadResult
import dv01.loan.api.loader.LoadingReport
import dv01.loan.api.model.loan.Loan
import dv01.loan.api.repository.LoanRepository
import dv01.loan.api.service.LoanQueryService
import org.hamcrest.Matchers.contains
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.nio.file.Path

class LoanControllerTest {

    private val service = LoanQueryService(object : LoanRepository {
        override val loans: List<Loan> = SAMPLE.loans
        override val report: LoadingReport = SAMPLE.report
    })

    private val mockMvc: MockMvc = MockMvcBuilders
        .standaloneSetup(LoanController(service))
        .setControllerAdvice(ApiExceptionHandler())
        .build()

    @Test
    fun should_returnOptions_when_optionsAreRequested() {
        mockMvc.get("$BASE/options").andExpect {
            status { isOk() }
            jsonPath("$.recordCount") { value(298) }
            jsonPath("$.issueMonths") { value(contains("2017-12")) }
            jsonPath("$.grades") { value(contains("A", "B", "C", "D", "E", "F", "G")) }
            jsonPath("$.states.length()") { value(43) }
            jsonPath("$.purposes.length()") { value(12) }
        }
    }

    @Test
    fun should_summarizeAllLoansByGrade_when_noParametersAreGiven() {
        mockMvc.get("$BASE/summary").andExpect {
            status { isOk() }
            jsonPath("$.groupBy") { value("grade") }
            jsonPath("$.filters") { isEmpty() }
            jsonPath("$.totals.loanCount") { value(298) }
            jsonPath("$.groups[*].key") { value(contains("A", "B", "C", "D", "E", "F", "G")) }
        }
    }

    @Test
    fun should_normalizeValues_when_parametersAreLowercaseAndPadded() {
        mockMvc.get("$BASE/summary") {
            param("groupBy", "STATE")
            param("state", " ca , ny ,")
        }.andExpect {
            status { isOk() }
            jsonPath("$.groupBy") { value("state") }
            jsonPath("$.filters.state") { value(contains("CA", "NY")) }
            jsonPath("$.totals.loanCount") { value(69) }
            jsonPath("$.groups[*].key") { value(contains("CA", "NY")) }
        }
    }

    @Test
    fun should_ignoreFilters_when_valuesAreBlank() {
        mockMvc.get("$BASE/summary") {
            param("grade", "")
            param("state", " , ")
            param("from", " ")
            param("ficoMin", "")
        }.andExpect {
            status { isOk() }
            jsonPath("$.filters") { isEmpty() }
            jsonPath("$.totals.loanCount") { value(298) }
        }
    }

    @Test
    fun should_returnBadRequest_when_groupByIsUnknown() {
        mockMvc.get("$BASE/summary") {
            param("groupBy", "status")
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.title") { value("Invalid request parameters") }
            jsonPath("$.errors[*].parameter") { value(contains("groupBy")) }
            jsonPath("$.errors[0].message") { value("must be one of: grade, state, month, ficoBand, purpose") }
        }
    }

    @Test
    fun should_reportEveryInvalidParameter_when_severalAreInvalid() {
        mockMvc.get("$BASE/summary") {
            param("grade", "A,Z")
            param("state", "XX")
            param("from", "2017/12")
            param("ficoMin", "abc")
            param("ficoMax", "851")
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.detail") { value("5 parameter(s) could not be accepted.") }
            jsonPath("$.errors[*].parameter") { value(contains("grade", "state", "from", "ficoMin", "ficoMax")) }
            jsonPath("$.errors[0].message") { value("'Z' is not a grade in this dataset") }
        }
    }

    @Test
    fun should_returnBadRequest_when_rangesAreInverted() {
        mockMvc.get("$BASE/summary") {
            param("from", "2018-01")
            param("to", "2017-12")
            param("ficoMin", "720")
            param("ficoMax", "700")
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.errors[*].parameter") { value(contains("from", "ficoMin")) }
            jsonPath("$.errors[*].message") {
                value(contains("must not be after 'to'", "must not be greater than 'ficoMax'"))
            }
        }
    }

    private companion object {
        const val BASE = "/api/v1/loans"

        val SAMPLE: LoadResult = CsvLoanLoader().load(
            Path.of(
                checkNotNull(LoanControllerTest::class.java.getResource("/sample_300.csv")) {
                    "sample_300.csv is missing from test resources"
                }.toURI(),
            ),
        )
    }

}
