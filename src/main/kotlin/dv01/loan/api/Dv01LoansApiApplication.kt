package dv01.loan.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class Dv01LoansApiApplication

fun main(args: Array<String>) {
    runApplication<Dv01LoansApiApplication>(*args)
}
