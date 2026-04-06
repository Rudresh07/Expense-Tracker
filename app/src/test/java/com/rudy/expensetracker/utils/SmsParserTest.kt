package com.rudy.expensetracker.utils

import com.rudy.expensetracker.sms.BankSmsPatterns
import com.rudy.expensetracker.sms.SmsFilter
import com.rudy.expensetracker.sms.SmsParser
import org.junit.Assert.*
import org.junit.Test

class SmsParserTest {

    // ── HDFC ──────────────────────────────────────────────────────────────────

    @Test fun `hdfc debit transfer`() {
        val msg = "Rs.5,000.00 debited from a/c **1234 on 01-Jan-25 by transfer to AMAZON. Avl Bal:Rs.12,345.67"
        val t = SmsParser.parse(msg)!!
        assertEquals(-5000.0,  t.amount, 0.01)   // expense → stored as negative
        assertEquals(1,        t.transactionType) // 1 = expense
        assertEquals("AMAZON", t.title)
    }

    @Test fun `hdfc credit card spent`() {
        val msg = "INR 299.00 spent on HDFC Bank Credit Card ending 4321 on 2025-01-15:10:30:00 at NETFLIX. Avl limit:Rs 49,701"
        val t = SmsParser.parse(msg)!!
        assertEquals(-299.0,    t.amount, 0.01)
        assertEquals(1,         t.transactionType)
        assertEquals("NETFLIX", t.title)
    }

    @Test fun `hdfc credited`() {
        val msg = "Dear Customer, your a/c XXXXXXXX1234 has been credited with Rs.10,000 on 15-Jan-25."
        val t = SmsParser.parse(msg)!!
        assertEquals(10000.0, t.amount, 0.01)  // income → stored as positive
        assertEquals(0,       t.transactionType) // 0 = income
    }

    @Test fun `bob debited`() {
        val msg = "    Rs.442.00 Debited to A/c ...7862 AT POS TID-OMBIZKCY/E,ref-609520571932. Total Bal:Rs.54807.75. Avlbal Amt:Rs.54807.75(05-04-2026 20:02:27). Not you? Call 1800 5700 / 1800 5000-BOB\n"
        val t = SmsParser.parse(msg)!!
        assertEquals(-442.0, t.amount, 0.01)  // income → stored as positive
        assertEquals(1,       t.transactionType) // 0 = income
    }


    // ── ICICI ─────────────────────────────────────────────────────────────────

    @Test fun `icici debit upi`() {
        val msg = "ICICI Bank Acct XX1234 debited for Rs 2,500.00 on 15-Jan-2025; ZOMATO UPI. Available Bal: Rs 8,200.00. Call 18001080 for dispute."
        val t = SmsParser.parse(msg)!!
        assertEquals(-2500.0,  t.amount, 0.01)
        assertEquals(1,        t.transactionType)
        assertEquals("ZOMATO", t.title)
    }

    @Test fun `icici credit neft`() {
        val msg = "Rs.1000.00 credited to ICICI Bank A/c XX5678 on 15/01/25 by UPI/123456789. Ref No 987654"
        val t = SmsParser.parse(msg)!!
        assertEquals(1000.0, t.amount, 0.01)
        assertEquals(0,      t.transactionType)
    }

    // ── SBI ───────────────────────────────────────────────────────────────────

    @Test fun `sbi debit upi`() {
        val msg = "Your A/c no. XX1234 is debited with Rs.500.00 on 15Jan25 to VPA xyz@upi. Avbl Bal is Rs.9,500.00"
        val t = SmsParser.parse(msg)!!
        assertEquals(-500.0,     t.amount, 0.01)
        assertEquals(1,          t.transactionType)
        assertEquals("xyz@upi",  t.title)
    }

    @Test fun `sbi credit neft`() {
        val msg = "Rs 5000.00 credited to your SBI a/c xx5678 on 15/01/2025 by NEFT from RAHUL KUMAR. UTR: 123456789012"
        val t = SmsParser.parse(msg)!!
        assertEquals(5000.0,        t.amount, 0.01)
        assertEquals(0,             t.transactionType)
        assertEquals("RAHUL KUMAR", t.title)
    }

    // ── Axis ──────────────────────────────────────────────────────────────────

    @Test fun `axis debit upi`() {
        val msg = "INR 1,200.00 debited from Axis Bank Ac XXXXXXXXX1234 on 15-01-25. Info: UPI/SWIGGY. Avl Bal: INR 15,000.00"
        val t = SmsParser.parse(msg)!!
        assertEquals(-1200.0,  t.amount, 0.01)
        assertEquals(1,        t.transactionType)
        assertEquals("SWIGGY", t.title)
    }

    // ── Kotak ─────────────────────────────────────────────────────────────────

    @Test fun `kotak debit`() {
        val msg = "Rs.850 has been debited from Kotak Bank A/c XX9876 on 15-01-2025 for MakeMyTrip. Available balance is Rs.22,150"
        val t = SmsParser.parse(msg)!!
        assertEquals(-850.0,       t.amount, 0.01)
        assertEquals(1,            t.transactionType)
        assertEquals("MakeMyTrip", t.title)
    }

    // ── Paytm ─────────────────────────────────────────────────────────────────

    @Test fun `paytm debit`() {
        val msg = "Rs.200 debited from your Paytm Payments Bank A/c for txn at BIGBASKET on 15 Jan 2025. Balance: Rs.1800"
        val t = SmsParser.parse(msg)!!
        assertEquals(-200.0,      t.amount, 0.01)
        assertEquals(1,           t.transactionType)
        assertEquals("BIGBASKET", t.title)
    }

    // ── YES Bank ──────────────────────────────────────────────────────────────

    @Test fun `yes bank pos`() {
        val msg = "Dear Customer, Rs.750 has been debited from a/c no XX2345 on 15-01-2025 for POS txn at STARBUCKS"
        val t = SmsParser.parse(msg)!!
        assertEquals(-750.0,       t.amount, 0.01)
        assertEquals(1,            t.transactionType)
        assertEquals("STARBUCKS",  t.title)
    }

    // ── Fallback title when no merchant found ─────────────────────────────────

    @Test fun `fallback title when no merchant`() {
        val msg = "Rs.100.00 debited from your account."
        val t = SmsParser.parse(msg)!!
        assertEquals(-100.0,           t.amount, 0.01)
        assertEquals(1,                t.transactionType)
        assertEquals("Bank Transaction", t.title)
    }

    // ── parse returns null when no amount or type detected ────────────────────

    @Test fun `returns null when no amount`() {
        assertNull(SmsParser.parse("Your account has been debited. Please check."))
    }

    @Test fun `returns null when no transaction type`() {
        assertNull(SmsParser.parse("Rs.500 transferred successfully."))
    }

    // ── OTP / non-txn must be filtered ───────────────────────────────────────

    @Test fun `otp is filtered`() {
        val otp = "Your OTP for login is 483920. Valid for 10 minutes. Do not share."
        assertTrue(SmsFilter.isOtp(otp))
        assertNull(SmsParser.parse(otp)) // no amount pattern → parse returns null
    }

    @Test fun `statement ready is filtered`() {
        val promo = "Your HDFC Bank Credit Card statement for Jan 2025 is ready. Total dues: Rs.12,450"
        assertTrue(SmsFilter.isNonTransaction(promo))

    }

    @Test fun `emi reminder is filtered`() {
        val emi = "Reminder: Your EMI of Rs.5,000 is due on 20-Jan-2025."
        assertTrue(SmsFilter.isNonTransaction(emi))
    }

    // ── Sender ID normalisation ───────────────────────────────────────────────

    @Test fun `sender with circle prefix`() {
        assertTrue(BankSmsPatterns.isBankSender("AM-HDFCBK"))
        assertTrue(BankSmsPatterns.isBankSender("VK-ICICIB"))
        assertTrue(BankSmsPatterns.isBankSender("JK-SBIINB"))
        assertFalse(BankSmsPatterns.isBankSender("VM-AMAZON"))
    }

    @Test fun `new bank senders are recognised`() {
        assertTrue(BankSmsPatterns.isBankSender("BANDHN"))   // Bandhan Bank
        assertTrue(BankSmsPatterns.isBankSender("AUBANK"))   // AU Small Finance Bank
        assertTrue(BankSmsPatterns.isBankSender("AIRTEL"))   // Airtel Payments Bank
        assertTrue(BankSmsPatterns.isBankSender("IDBIBK"))   // IDBI Bank
    }

    // ── Bank of Baroda ────────────────────────────────────────────────────────

    @Test fun `bob otp is filtered`() {
        val msg = "888128 is OTP for txn of INR 1600.00 at NTANEETOTHDRCARD on 05/03/2026 on card ending 6953.Valid till 11:32:22- Do not share OTP with anyone -Bank of Baroda"
        assertTrue(SmsFilter.isOtp(msg))
        assertTrue(SmsFilter.shouldSkip(msg))
        // parser must not produce a transaction for OTP messages
        assertNull(SmsParser.parse(msg))
    }

    @Test fun `bob upi credit`() {
        val msg = "Dear BOB UPI User: Your account is credited with INR 5000.00 on 2026-03-20 08:28:22 AM by UPI Ref No 607933845045; AvlBal: Rs7303.10 - BOB"
        assertFalse(SmsFilter.shouldSkip(msg))
        val t = SmsParser.parse(msg)!!
        assertEquals(5000.0, t.amount, 0.01)   // income → positive
        assertEquals(0, t.transactionType)      // 0 = income
    }

    @Test fun `bob upi mandate debit`() {
        val msg = "For upcoming mandate set for 2026-04-05 12:00:00 AM ,your A/C will be debited with 299.00 towards JioHotstar for the UPI Mandate, RRN 642975553524 -BOB"
        assertFalse(SmsFilter.shouldSkip(msg))
        val t = SmsParser.parse(msg)!!
        assertEquals(-299.0, t.amount, 0.01)    // expense → negative
        assertEquals(1, t.transactionType)       // 1 = expense
        assertEquals("JioHotstar", t.title)
    }

    @Test fun `bob dr cr upi transfer`() {
        val msg = "Rs.166.00 Dr. from A/C XXXXXX7862 and Cr. to paytm.s1s58yz@pty. Ref:609272092991. AvlBal:Rs56028.75(2026:04:02 08:32:15). Not you? Call 18005700/5000-BOB"
        assertFalse(SmsFilter.shouldSkip(msg))
        val t = SmsParser.parse(msg)!!
        assertEquals(-166.0, t.amount, 0.01)    // expense → negative
        assertEquals(1, t.transactionType)       // 1 = expense
        assertEquals("paytm.s1s58yz@pty", t.title)
    }
}
