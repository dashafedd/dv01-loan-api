package dv01.loan.api.model

object FicoBand {
    const val WIDTH = 20
    private const val MAX_FICO = 850

    fun labelFor(ficoMidpoint: Int): String {
        val low = (ficoMidpoint / WIDTH) * WIDTH
        return "$low-${minOf(low + WIDTH - 1, MAX_FICO)}"
    }
}