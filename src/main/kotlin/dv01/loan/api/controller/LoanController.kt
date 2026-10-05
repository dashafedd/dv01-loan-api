package dv01.loan.api.controller

import dv01.loan.api.service.LoanQueryService

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/loans")
class LoanController (private val service: LoanQueryService)