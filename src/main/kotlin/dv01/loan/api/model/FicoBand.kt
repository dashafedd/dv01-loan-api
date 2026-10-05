package dv01.loan.api.model

/** Groups FICO scores into fixed bands of 20 points, e.g. 660-679, 680-699. */
object FicoBand {
    const val WIDTH = 20
    private const val MAX_FICO = 850 // highest possible score, the last band ends here

    /** Returns the band a score falls into, e.g. 662 -> "660-679". */
    fun labelFor(ficoMidpoint: Int): String {
        // integer division drops the remainder, so this rounds down to the start of the band
        val low = (ficoMidpoint / WIDTH) * WIDTH
        return "$low-${minOf(low + WIDTH - 1, MAX_FICO)}"
    }
}