package com.rudy.expensetracker.sms

import com.rudy.expensetracker.model.Transaction
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.util.Date
import java.util.Locale

object SmsParser {

    // ── Amount patterns ────────────────────────────────────────────────────────
    private val AMOUNT_PATTERNS = listOf(
        // INR and ₹ first — more specific, less likely to hit balance figures
        Regex("""INR\s+([0-9]{1,7}(?:,[0-9]{3})*(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE),
        Regex("""₹\s*([0-9]{1,7}(?:,[0-9]{3})*(?:\.[0-9]{1,2})?)"""),
        // Rs last — most generic, can appear multiple times (txn + balance)
        Regex("""[Rr]s\.?\s*([0-9]{1,7}(?:,[0-9]{3})*(?:\.[0-9]{1,2})?)"""),
        // BOB mandate: bare amount with no currency prefix e.g. "debited with 299.00"
        Regex("""(?:debited\s+with|with)\s+([0-9]{1,7}(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE),
    )

    // ── Transaction type patterns ──────────────────────────────────────────────
    private val DEBIT_PATTERNS = listOf(
        Regex("""\bdebited\b""",          RegexOption.IGNORE_CASE),
        Regex("""\bdebited\s+with\b""",   RegexOption.IGNORE_CASE),
        Regex("""\bDr\.(\s|$)"""),                                      // BOB "Dr. from A/C"
        Regex("""\bdeducted\b""",         RegexOption.IGNORE_CASE),
        Regex("""\bspent\s+on\b""",       RegexOption.IGNORE_CASE),
        Regex("""\bpaid\b""",             RegexOption.IGNORE_CASE),
        Regex("""\bpayment\s+of\b""",     RegexOption.IGNORE_CASE),
        Regex("""\bpurchase\s+at\b""",    RegexOption.IGNORE_CASE),
        Regex("""\bwithdrawn\b""",        RegexOption.IGNORE_CASE),
        Regex("""\bcharged\b""",          RegexOption.IGNORE_CASE),
        Regex("""\bpos\s+txn\b""",        RegexOption.IGNORE_CASE),
        Regex("""\btxn\s+at\b""",         RegexOption.IGNORE_CASE),
    )

    private val CREDIT_PATTERNS = listOf(
        Regex("""\bcredited\b""",         RegexOption.IGNORE_CASE),
        Regex("""\breceived\b""",         RegexOption.IGNORE_CASE),
        Regex("""\bdeposited\b""",        RegexOption.IGNORE_CASE),
        Regex("""\brefund(ed)?\b""",      RegexOption.IGNORE_CASE),
        Regex("""\bcash\s*back\b""",      RegexOption.IGNORE_CASE),
        Regex("""\breversed\b""",         RegexOption.IGNORE_CASE),
    )

    // ── Merchant / payee patterns ──────────────────────────────────────────────
    // Ordered by specificity — first match wins.
    private val MERCHANT_PATTERNS = listOf(
        // HDFC CC: "spent on HDFC Bank Credit Card ending 4321 on ... at NETFLIX"
        Regex("""spent\s+on\s+[A-Za-z ]+\s+ending\s+\d+\s+on\s+[\d\-:]+\s+at\s+([A-Za-z0-9 &._\-]{2,40})""", RegexOption.IGNORE_CASE) to 1,

        // HDFC: "by transfer to AMAZON"
        Regex("""by\s+transfer\s+to\s+([A-Za-z0-9 &._\-]{2,40})""", RegexOption.IGNORE_CASE) to 1,

        // BOB: "Cr. to paytm.s1s58yz@pty" — stop at whitespace/Ref/end
        Regex("""[Cc]r\.\s+to\s+([A-Za-z0-9._@\-]{3,50})(?=\s|\.|$)""") to 1,

        // BOB mandate: "towards JioHotstar for ..."
        Regex("""towards\s+([A-Za-z0-9 &._\-]{2,40})(?:\s+for\b|\s+on\b|\.|$)""", RegexOption.IGNORE_CASE) to 1,

        // SBI UPI: "to VPA xyz@upi"
        Regex("""to\s+VPA\s+([A-Za-z0-9._@\-]{3,50})""", RegexOption.IGNORE_CASE) to 1,

        // Axis / generic "Info: UPI/SWIGGY"
        Regex("""[Ii]nfo:\s*(?:UPI|NEFT|IMPS|POS)/([A-Za-z0-9 &._\-]{2,40})""") to 1,

        // Paytm: "txn at BIGBASKET"
        Regex("""txn\s+at\s+([A-Za-z0-9 &._\-]{2,40})""", RegexOption.IGNORE_CASE) to 1,

        // YES Bank: "POS txn at STARBUCKS"
        Regex("""pos\s+txn\s+at\s+([A-Za-z0-9 &._\-]{2,40})""", RegexOption.IGNORE_CASE) to 1,

        // SBI NEFT credit: "by NEFT from RAHUL KUMAR"
        Regex("""by\s+(?:NEFT|IMPS|UPI)\s+from\s+([A-Za-z ]{3,40})""", RegexOption.IGNORE_CASE) to 1,

        // ICICI after semicolon: "; ZOMATO UPI"
        Regex(""";\s*([A-Za-z0-9 &._\-]{2,30}?)\s+(?:UPI|NEFT|IMPS|POS)\b""", RegexOption.IGNORE_CASE) to 1,

        // Kotak / IndusInd: "for MakeMyTrip." or "for MakeMyTrip on"
        Regex("""for\s+([A-Za-z0-9 &._\-]{2,40})(?:\s+on\b|\.)""", RegexOption.IGNORE_CASE) to 1,

        // HDFC generic: "at MERCHANT"
        Regex("""\bat\s+([A-Za-z0-9 &._\-]{2,40})""", RegexOption.IGNORE_CASE) to 1,

        // Generic fallback: "to NAME" before period/end
        Regex("""to\s+([A-Za-z0-9 &._\-]{3,30})(?:\.|$)""", RegexOption.IGNORE_CASE) to 1,
    )

    // ── Account number patterns ───────────────────────────────────────────────
    private val ACCOUNT_PATTERNS = listOf(
        Regex("""(?:a/c|ac|acct|account)(?:\s+no\.?)?\s+[xX*]{0,10}(\d{4})""", RegexOption.IGNORE_CASE),
        Regex("""[Xx*]{2,}\s*(\d{4})"""),
    )

    // ── Available balance patterns ────────────────────────────────────────────
    private val BALANCE_PATTERNS = listOf(
        Regex("""(?:avl|avail(?:able)?)\s*(?:bal(?:ance)?)?:?\s*(?:rs\.?|inr|₹)\s*([0-9,]+(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE),
        Regex("""balance(?:\s+is)?:?\s*(?:rs\.?|inr|₹)\s*([0-9,]+(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE),
    )

    // ── Public entry point ────────────────────────────────────────────────────
    fun parse(body: String, categoryId: Int = 1): Transaction? {
        val amount          = parseAmount(body)  ?: return null
        val transactionType = detectType(body)   ?: return null
        val merchant        = parseMerchant(body)
        val finalAmount     = if (transactionType == 0) amount else -amount

        return Transaction(
            category        = categoryId,
            title           = merchant ?: "Bank Transaction",
            transactionType = transactionType,
            time            = currentTimeString(),
            amount          = finalAmount,
            date            = currentDateString(),
            note            = body
        )
    }

    private fun currentTimeString(): String {
        val now = LocalTime.now()
        val hour = now.hour
        val minute = now.minute
        val amPm = if (hour >= 12) "PM" else "AM"
        val formattedHour = if (hour % 12 == 0) 12 else hour % 12
        return String.format("%02d:%02d %s", formattedHour, minute, amPm)
    }

    private fun currentDateString(): String {
        return SimpleDateFormat("dd MM yyyy", Locale.getDefault()).format(Date())
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    fun parseAmount(body: String): Double? {
        for (pattern in AMOUNT_PATTERNS) {
            val match = pattern.find(body) ?: continue
            val raw = match.groupValues[1].replace(",", "")
            return raw.toDoubleOrNull() ?: continue
        }
        return null
    }

    fun detectType(body: String): Int? = when {
        DEBIT_PATTERNS.any  { it.containsMatchIn(body) } -> 1
        CREDIT_PATTERNS.any { it.containsMatchIn(body) } -> 0
        else -> null
    }

    fun parseMerchant(body: String): String? {
        for ((pattern, group) in MERCHANT_PATTERNS) {
            val value = pattern.find(body)?.groupValues?.getOrNull(group)
                ?.trim()?.trimEnd('.', ',')
                ?: continue
            if (value.length >= 2) return sanitizeMerchant(value)
        }
        return null
    }

    fun parseAccount(body: String): String? {
        for (pattern in ACCOUNT_PATTERNS) {
            return pattern.find(body)?.groupValues?.get(1) ?: continue
        }
        return null
    }

    fun parseBalance(body: String): Double? {
        for (pattern in BALANCE_PATTERNS) {
            val raw = pattern.find(body)?.groupValues?.get(1)
                ?.replace(",", "") ?: continue
            return raw.toDoubleOrNull() ?: continue
        }
        return null
    }

    private fun sanitizeMerchant(raw: String): String {
        val isUpiHandle = raw.contains('@')   // e.g. paytm.s1s58yz@pty, xyz@upi
        return raw
            .replace(Regex("""\s+on\s+\d.*$""",              RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+(UPI|NEFT|IMPS|POS)\s*$""", RegexOption.IGNORE_CASE), "")
            // Skip dot-strip for UPI handles — dots are part of the address
            .let { if (isUpiHandle) it else it.replace(Regex("""\..*$"""), "") }
            .replace(Regex("""\s+Avl\b.*$""",                RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+Available\b.*$""",          RegexOption.IGNORE_CASE), "")
            .trim()
            .take(40)
    }
}