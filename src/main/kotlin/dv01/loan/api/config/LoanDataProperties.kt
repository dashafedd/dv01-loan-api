package dv01.loan.api.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Settings under "loan-data" in application.yaml.
 * path - location of the loan CSV file, relative to the directory the app is started from.
 */
@ConfigurationProperties("loan-data")
data class LoanDataProperties(val path: String)