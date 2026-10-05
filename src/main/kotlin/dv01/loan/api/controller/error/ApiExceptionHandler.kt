package dv01.loan.api.controller.error

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * Translates exceptions thrown while handling a request into HTTP error responses.
 * Applies to every controller, so error formatting stays out of the controllers themselves.
 */
@RestControllerAdvice
class ApiExceptionHandler {

    /**
     * Turns a failed parameter validation into a 400 response with the list of title, detail and property name.
     */
    @ExceptionHandler(InvalidRequestException::class)
    fun handleInvalidRequest(exception: InvalidRequestException): ProblemDetail =
        ProblemDetail.forStatus(HttpStatus.BAD_REQUEST).apply {
            title = "Invalid request parameters"
            detail = "${exception.errors.size} parameter(s) could not be accepted."
            setProperty("errors", exception.errors)
        }
}
