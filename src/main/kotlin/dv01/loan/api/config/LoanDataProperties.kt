package dv01.loan.api.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("loan-data")
data class LoanDataProperties(val path: String)