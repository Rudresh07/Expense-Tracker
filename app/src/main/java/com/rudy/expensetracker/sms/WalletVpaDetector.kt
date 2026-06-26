package com.rudy.expensetracker.sms

object WalletVpaDetector {

    private data class WalletPattern(
        val pattern: Regex,
        val humanName: String,
    )

    private val WALLET_PATTERNS = listOf(
        WalletPattern(Regex("^amzn.*@apl$",       RegexOption.IGNORE_CASE), "Amazon Pay"),
        WalletPattern(Regex("^amazon.*@apl$",     RegexOption.IGNORE_CASE), "Amazon Pay"),
        WalletPattern(Regex("^paytm[-.].*@.*$",   RegexOption.IGNORE_CASE), "Paytm"),
        WalletPattern(Regex("^paytm@(paytm|pthdfc|ptaxis|ptsbi|ptyes|ptkotak)$", RegexOption.IGNORE_CASE), "Paytm"),
        WalletPattern(Regex("^phonepe[-.].*@.*$", RegexOption.IGNORE_CASE), "PhonePe"),
        WalletPattern(Regex("^phonepe@(ybl|ibl|axl|timecosmos)$",           RegexOption.IGNORE_CASE), "PhonePe"),
        WalletPattern(Regex("^mobikwik.*@.*$",    RegexOption.IGNORE_CASE), "MobiKwik"),
        WalletPattern(Regex("^freecharge.*@.*$",  RegexOption.IGNORE_CASE), "Freecharge"),
        WalletPattern(Regex("^jiomoney.*@.*$",    RegexOption.IGNORE_CASE), "JioMoney"),
    )

    /** Returns true if the VPA routes through a wallet proxy (Amazon Pay, Paytm, PhonePe, etc.) */
    fun isWalletProxy(vpa: String): Boolean =
        WALLET_PATTERNS.any { it.pattern.matches(vpa) }

    /** Returns a human-readable wallet name for the VPA, or null if not a known wallet. */
    fun humanName(vpa: String): String? =
        WALLET_PATTERNS.firstOrNull { it.pattern.matches(vpa) }?.humanName
}
