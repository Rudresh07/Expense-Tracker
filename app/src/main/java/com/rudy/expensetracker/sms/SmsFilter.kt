package com.rudy.expensetracker.sms

object SmsFilter {

    fun isBankSender(sender: String) = BankSmsPatterns.isBankSender(sender)

    fun isOtp(body: String) =
        BankSmsPatterns.OTP_SIGNALS.any { it.containsMatchIn(body) }

    fun isNonTransaction(body: String) =
        BankSmsPatterns.NON_TXN_SIGNALS.any { it.containsMatchIn(body) }

    fun shouldSkip(body: String): Boolean =
        isOtp(body) || isNonTransaction(body)

    /**
     * Content-based fallback for unrecognised sender IDs.
     * Requires all 3 layers to pass:
     *   1. Currency amount (Rs./INR/₹ + number)
     *   2. Clear debit/credit direction keyword
     *   3. At least one bank authenticator (masked a/c, avl bal, payment rail, a/c ref)
     */
    fun looksLikeBankTransaction(body: String): Boolean {
        if (!BankSmsPatterns.CONTENT_AMOUNT_SIGNAL.containsMatchIn(body)) return false
        if (BankSmsPatterns.CONTENT_TXN_DIRECTION.none { it.containsMatchIn(body) }) return false
        return BankSmsPatterns.CONTENT_BANK_AUTHENTICATORS.any { it.containsMatchIn(body) }
    }

    /** Returns true only if the SMS should be parsed as a transaction. */
    fun shouldProcess(sender: String, body: String): Boolean {
        if (!isBankSender(sender) && !looksLikeBankTransaction(body)) return false
        if (isOtp(body))            return false
        if (isNonTransaction(body)) return false
        return true
    }
}