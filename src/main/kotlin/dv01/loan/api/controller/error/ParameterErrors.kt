package dv01.loan.api.controller.error

/** One rejected request parameter and the reason it was rejected. */
data class ParameterError(val parameter: String, val message: String)

/** Thrown when a request has invalid parameters. ApiExceptionHandler turns it into a 400 response. */
class InvalidRequestException(val errors: List<ParameterError>) :
    RuntimeException(errors.joinToString("; ") { "${it.parameter}: ${it.message}" })

/**
 * Collects validation errors for a single request,
 * so that all invalid parameters are reported together instead of only the first one.
 */
class ParameterErrors {
    private val collected = mutableListOf<ParameterError>()

    /** Records an error and lets validation continue. */
    fun reject(parameter: String, message: String) {
        collected += ParameterError(parameter, message)
    }

    /** Throws InvalidRequestException if any error was recorded, otherwise does nothing. */
    fun throwIfAny() {
        if (collected.isNotEmpty()) throw InvalidRequestException(collected.toList())
    }
}
