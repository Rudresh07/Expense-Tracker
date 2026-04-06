package com.rudy.expensetracker.sms

object SmsFilter {

    fun isBankSender(sender: String) = BankSmsPatterns.isBankSender(sender)

    fun isOtp(body: String) =
        BankSmsPatterns.OTP_SIGNALS.any { it.containsMatchIn(body) }

    fun isNonTransaction(body: String) =
        BankSmsPatterns.NON_TXN_SIGNALS.any { it.containsMatchIn(body) }

    fun shouldSkip(body: String): Boolean =
        isOtp(body) || isNonTransaction(body)


    /** Single call that returns true only if the SMS should be parsed as a transaction */
    fun shouldProcess(sender: String, body: String): Boolean {
        if (!isBankSender(sender))    return false
        if (isOtp(body))              return false
        if (isNonTransaction(body))   return false
        return true
    }
}