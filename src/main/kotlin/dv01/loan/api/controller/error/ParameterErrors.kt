package dv01.loan.api.controller.error

data class ParameterError(val parameter: String, val message: String)

class InvalidRequestException(val errors: List<ParameterError>) :
    RuntimeException(errors.joinToString("; ") { "${it.parameter}: ${it.message}" })

class ParameterErrors {
    private val collected = mutableListOf<ParameterError>()

    fun reject(parameter: String, message: String) {
        collected += ParameterError(parameter, message)
    }

    fun throwIfAny() {
        if (collected.isNotEmpty()) throw InvalidRequestException(collected.toList())
    }
}

