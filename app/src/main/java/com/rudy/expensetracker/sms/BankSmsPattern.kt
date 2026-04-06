package com.rudy.expensetracker.sms

/**
 * Real-world bank SMS formats observed in India.
 *
 * Sender IDs follow the TRAI DLT format: XX-BANKID
 *   XX  = 2-letter telecom circle code (VM, AM, JK, AP, etc.)
 *   or just 6-char alphanumeric like HDFCBK, SBIINB
 *
 * Examples of real senders:
 *   HDFCBK, AM-HDFCBK, VK-HDFCBK   → HDFC Bank
 *   HDFCCC                           → HDFC Credit Card
 *   SBIINB, AD-SBIINB, JK-SBIINB   → SBI
 *   SBICRD                           → SBI Credit Card
 *   ICICIB, VM-ICICIB               → ICICI Bank
 *   AXISBK, AM-AXISBK               → Axis Bank
 *   AXISCB                           → Axis Credit Card
 *   KOTAKB, AP-KOTAKB               → Kotak Mahindra
 *   INDBNK, VM-INDBNK               → IndusInd Bank / Indian Bank (shared ID)
 *   PAYTMB                          → Paytm Payments Bank
 *   YESBNK, BW-YESBNK               → YES Bank
 *   BOIIND                          → Bank of India
 *   CANBNK                          → Canara Bank
 *   CENTBK                          → Central Bank of India
 *   PNBSMS                          → PNB
 *   UBISMS / UNIONB                 → Union Bank of India
 *   IDFCBK                          → IDFC First Bank
 *   SCBANK                          → Standard Chartered
 *   HSBCIN                          → HSBC
 */

// ─── Real SMS samples per bank ───────────────────────────────────────────────
//
// HDFC Bank
//   "Rs.5,000.00 debited from a/c **1234 on 01-Jan-25 by transfer to AMAZON. Avl Bal:Rs.12,345.67"
//   "INR 299.00 spent on HDFC Bank Credit Card ending 4321 on 2025-01-15:10:30:00 at NETFLIX. Avl limit:Rs 49,701"
//   "Dear Customer, your a/c XXXXXXXX1234 has been credited with Rs.10,000 on 15-Jan-25."
//
// ICICI Bank
//   "ICICI Bank Acct XX1234 debited for Rs 2,500.00 on 15-Jan-2025; ZOMATO UPI. Available Bal: Rs 8,200.00. Call 18001080 for dispute."
//   "Rs.1000.00 credited to ICICI Bank A/c XX5678 on 15/01/25 by UPI/123456789. Ref No 987654"
//
// SBI
//   "Your A/c no. XX1234 is debited with Rs.500.00 on 15Jan25 to VPA xyz@upi. Avbl Bal is Rs.9,500.00"
//   "Rs 5000.00 credited to your SBI a/c xx5678 on 15/01/2025 by NEFT from RAHUL KUMAR. UTR: 123456789012"
//
// Axis Bank
//   "INR 1,200.00 debited from Axis Bank Ac XXXXXXXXX1234 on 15-01-25. Info: UPI/SWIGGY. Avl Bal: INR 15,000.00"
//   "INR 3000 credited to your Axis Bank account XX6789 on 15JAN25 via IMPS. Ref:987654321"
//
// Kotak
//   "Rs.850 has been debited from Kotak Bank A/c XX9876 on 15-01-2025 for MakeMyTrip. Available balance is Rs.22,150"
//   "Rs.25,000 credited to your Kotak Mahindra Bank a/c no. XX1234 on 15/01/2025"
//
// Paytm Payments Bank
//   "Rs.200 debited from your Paytm Payments Bank A/c for txn at BIGBASKET on 15 Jan 2025. Balance: Rs.1800"
//
// IndusInd
//   "INR 999.00 has been debited from your IndusInd Bank A/c No. XX4567 on 15/01/2025 for purchase at AMAZON"
//
// YES Bank
//   "Dear Customer, Rs.750 has been debited from a/c no XX2345 on 15-01-2025 for POS txn at STARBUCKS"
//
// ─── OTP samples (must be filtered OUT) ──────────────────────────────────────
//   "Your OTP for login is 483920. Valid for 10 minutes. Do not share."
//   "Use 739201 as your One Time Password (OTP) for HDFC Bank NetBanking. Do NOT share."
//   "583920 is your SBI transaction password. Do not share with anyone."
//   "Dear customer, 2FA code for your ICICI Bank transaction: 294857. Expires in 5 min."
//
// ─── Promotional/info samples (should also be ignored) ───────────────────────
//   "Your HDFC Bank Credit Card statement for Jan 2025 is ready. Total dues: Rs.12,450"
//   "Reminder: Your EMI of Rs.5,000 is due on 20-Jan-2025."

object BankSmsPatterns {

    // Matches: XX-BANKID or plain BANKID (6 alpha chars typical)
    // The circle prefix (AM-, VM-, JK-, etc.) is stripped before matching.
    // Note: INDBNK is shared by IndusInd Bank and Indian Bank — both are valid banks
    //       so the conflict has no practical impact on filtering.
    private val KNOWN_BANK_IDS = setOf(
        // ── Large private banks ────────────────────────────────────────────────
        "HDFCBK", "HDFCCC",             // HDFC Bank, HDFC Credit Card
        "ICICIB",                        // ICICI Bank
        "AXISBK", "AXISCB",             // Axis Bank, Axis Credit Card
        "KOTAKB",                        // Kotak Mahindra Bank
        "INDBNK",                        // IndusInd Bank / Indian Bank (shared DLT ID)
        "YESBNK",                        // YES Bank
        "IDFCBK",                        // IDFC First Bank
        "BANDHN",                        // Bandhan Bank
        "RBLBNK",                        // RBL Bank
        "FEDERA",                        // Federal Bank
        "SOUBIN",                        // South Indian Bank
        "KTKBNK",                        // Karnataka Bank
        "JKBANK",                        // J&K Bank
        "KVBANK",                        // Karur Vysya Bank
        "TJSBNK",                        // Tamilnad Mercantile Bank
        "CSBBNK",                        // Catholic Syrian Bank
        "DCBBNK",                        // DCB Bank
        "CITBNK",                        // City Union Bank
        "DHNBNK",                        // Dhanlaxmi Bank
        "DBSBNK",                        // DBS Bank India
        "SCBANK",                        // Standard Chartered
        "HSBCIN",                        // HSBC India

        // ── Public sector banks ────────────────────────────────────────────────
        "SBIINB", "SBICRD",             // SBI, SBI Credit Card
        "BOIIND",                        // Bank of India
        "CANBNK",                        // Canara Bank
        "CENTBK",                        // Central Bank of India
        "PNBSMS",                        // Punjab National Bank
        "UBISMS", "UNIONB",             // Union Bank of India (pre/post merger IDs)
        "IOBSMS",                        // Indian Overseas Bank
        "BARODM", "BOBSMS",             // Bank of Baroda
        "IDBIBK",                        // IDBI Bank
        "MAHBNK",                        // Bank of Maharashtra
        "PSBBNK",                        // Punjab & Sind Bank
        "UCOBKS",                        // UCO Bank
        "INDBAN",                        // Indian Bank (post Allahabad Bank merger)

        // ── Small finance banks ────────────────────────────────────────────────
        "AUBANK",                        // AU Small Finance Bank
        "EQUTAS",                        // Equitas Small Finance Bank
        "UJJIVN",                        // Ujjivan Small Finance Bank
        "JANABN",                        // Jana Small Finance Bank
        "ESAFBK",                        // ESAF Small Finance Bank
        "UTKRSB",                        // Utkarsh Small Finance Bank

        // ── Payment banks ──────────────────────────────────────────────────────
        "PAYTMB",                        // Paytm Payments Bank
        "AIRTEL",                        // Airtel Payments Bank
        "IPPBSM",                        // India Post Payments Bank
        "FINOBN",                        // Fino Payments Bank
        "JIOPAY",                        // Jio Payments Bank
    )

    fun isBankSender(sender: String): Boolean {
        // Indian DLT sender IDs come in two formats:
        //   2-part: "AM-HDFCBK"   → substringAfterLast gives "HDFCBK" ✓
        //   3-part: "JX-BOBSMS-T" → substringAfterLast gives "T" ✗
        // Fix: search the full uppercased sender for any known bank ID.
        val upper = sender.uppercase()
        return KNOWN_BANK_IDS.any { upper.contains(it) }
    }

    // OTP signals — checked before any parsing
     val OTP_SIGNALS = listOf(
        Regex("""\botp\b""",                          RegexOption.IGNORE_CASE),
        Regex("""\bone[\s-]?time[\s-]?pass""",        RegexOption.IGNORE_CASE),
        Regex("""\bdo\s+not\s+share\b""",             RegexOption.IGNORE_CASE),
        Regex("""\bpassword\b.*\bvalid\b""",          RegexOption.IGNORE_CASE),
        Regex("""\bverification\s+code\b""",          RegexOption.IGNORE_CASE),
        Regex("""\b2fa\s+code\b""",                   RegexOption.IGNORE_CASE),
        Regex("""\bauth(entication)?\s+code\b""",     RegexOption.IGNORE_CASE),
        Regex("""\bexpires\s+in\s+\d+\s+min""",       RegexOption.IGNORE_CASE),
        // Additional OTP patterns
        Regex("""\benter\s+\d{4,8}\b""",              RegexOption.IGNORE_CASE), // "enter 483920"
        Regex("""\buse\s+\d{4,8}\s+to\b""",           RegexOption.IGNORE_CASE), // "use 123456 to verify"
        Regex("""\bcode\s+is\s+\d{4,8}\b""",          RegexOption.IGNORE_CASE), // "code is 938271"
        Regex("""\bpin\s+is\s+\d{4,8}\b""",           RegexOption.IGNORE_CASE), // "pin is 1234"
        Regex("""\bvalid\s+for\s+\d+\s+min""",        RegexOption.IGNORE_CASE), // "valid for 10 minutes"
        Regex("""\bnever\s+share\b""",                 RegexOption.IGNORE_CASE), // "never share this"
        Regex("""\bsecure\s+code\b""",                 RegexOption.IGNORE_CASE), // "secure code"
        Regex("""\btransaction\s+password\b""",        RegexOption.IGNORE_CASE), // "transaction password"
        Regex("""\blogin\s+(otp|code|pin)\b""",        RegexOption.IGNORE_CASE), // "login otp"
        Regex("""\b\d{4,8}\s+is\s+your\b""",          RegexOption.IGNORE_CASE), // "483920 is your OTP"
    )

    // ── Promotional / non-transaction signals ─────────────────────────────────
     val NON_TXN_SIGNALS = listOf(
        // Statement & billing
        Regex("""\bstatement\s+(is\s+)?ready\b""",              RegexOption.IGNORE_CASE),
        Regex("""\bstatement\s+for\b""",                        RegexOption.IGNORE_CASE),
        Regex("""\btotal\s+dues?\b""",                          RegexOption.IGNORE_CASE),
        Regex("""\bminimum\s+amount\s+due\b""",                 RegexOption.IGNORE_CASE),
        Regex("""\bmin(imum)?\s+due\b""",                       RegexOption.IGNORE_CASE),
        Regex("""\bpayment\s+due\s+(date|on)\b""",              RegexOption.IGNORE_CASE),
        Regex("""\bbill\s+(generated|ready|available)\b""",     RegexOption.IGNORE_CASE),

        // EMI & loan reminders
        Regex("""\bemi\b.*\bdue\b""",                           RegexOption.IGNORE_CASE),
        Regex("""\breminder\b.*\bemi\b""",                      RegexOption.IGNORE_CASE),
        Regex("""\bloan\s+(emi|instalment|installment)\b""",    RegexOption.IGNORE_CASE),
        Regex("""\brepayment\s+due\b""",                        RegexOption.IGNORE_CASE),
        Regex("""\bdue\s+on\s+\d""",                            RegexOption.IGNORE_CASE), // "due on 20-Jan"

        // Credit limit / account updates
        Regex("""\bcredit\s+limit\s+(enhanced|reduced|changed|revised|updated)\b""", RegexOption.IGNORE_CASE),
        Regex("""\byour\s+limit\s+(is|has)\b""",                RegexOption.IGNORE_CASE),
        Regex("""\bcredit\s+score\b""",                         RegexOption.IGNORE_CASE),
        Regex("""\bcibil\b""",                                  RegexOption.IGNORE_CASE),

        // Offers & promotions
        Regex("""\bcongratulations\b""",                        RegexOption.IGNORE_CASE),
        Regex("""\bpre[\s-]?approved\b""",                      RegexOption.IGNORE_CASE),
        Regex("""\bexclusive\s+offer\b""",                      RegexOption.IGNORE_CASE),
        Regex("""\bspecial\s+offer\b""",                        RegexOption.IGNORE_CASE),
        Regex("""\bcashback\s+offer\b""",                       RegexOption.IGNORE_CASE),
        Regex("""\breward\s+points?\b""",                       RegexOption.IGNORE_CASE),
        Regex("""\bclick\s+here\b""",                           RegexOption.IGNORE_CASE),
        Regex("""\bvisit\s+(our|the)\b""",                      RegexOption.IGNORE_CASE),
        Regex("""\bapply\s+now\b""",                            RegexOption.IGNORE_CASE),
        Regex("""\bget\s+up\s+to\b""",                          RegexOption.IGNORE_CASE),
        Regex("""\binterest\s+rate\b""",                        RegexOption.IGNORE_CASE),
        Regex("""\bpersonal\s+loan\s+offer\b""",                RegexOption.IGNORE_CASE),

        // KYC / account notices
        Regex("""\bkyc\b""",                                    RegexOption.IGNORE_CASE),
        Regex("""\bupdate\s+your\s+(kyc|details|profile)\b""",  RegexOption.IGNORE_CASE),
        Regex("""\baccount\s+(blocked|suspended|frozen)\b""",   RegexOption.IGNORE_CASE),
        Regex("""\bpassbook\s+updated\b""",                     RegexOption.IGNORE_CASE),

        // Login / security alerts that aren't OTPs
        Regex("""\blogged\s+in\s+(to|from)\b""",                RegexOption.IGNORE_CASE),
        Regex("""\bnew\s+(device|login)\s+detected\b""",        RegexOption.IGNORE_CASE),
        Regex("""\bpassword\s+(changed|updated|reset)\b""",     RegexOption.IGNORE_CASE),

        // Balance enquiry / mini statement
        Regex("""\bbalance\s+enquiry\b""",                      RegexOption.IGNORE_CASE),
        Regex("""\bmini\s+statement\b""",                       RegexOption.IGNORE_CASE),
    )
}
    