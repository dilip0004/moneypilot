package com.yourname.moneypilot.util

import org.junit.Assert.assertEquals
import org.junit.Test

class SmsParserTest {

    @Test
    fun parse_HdfcDebitSms_ExtractsCorrectData() {
        val sms = "Alert: Rs 450.00 spent on HDFC Bank Card x1234 at STARBUCKS on 2025-12-29. Info: internal-ref."
        val result = SmsParser.parse(sms)
        
        assertEquals(450.0, result.amount)
        assertEquals("EXPENSE", result.type)
        assertEquals("STARBUCKS", result.merchant)
        assertEquals("1234", result.accountSuffix)
    }

    @Test
    fun parse_UpiDebitSms_ExtractsCorrectData() {
        val sms = "Money Transferred: INR 1,200.50 sent to vpa merchant@upi from Acc x5678."
        val result = SmsParser.parse(sms)
        
        assertEquals(1200.5, result.amount)
        assertEquals("EXPENSE", result.type)
        assertEquals("merchant@upi", result.merchant)
        assertEquals("5678", result.accountSuffix)
    }

    @Test
    fun parse_SalaryCreditSms_ExtractsAsIncome() {
        val sms = "Your A/c x9999 has been credited with Rs 75,000.00 on 01-Jan-2025 towards SALARY."
        val result = SmsParser.parse(sms)
        
        assertEquals(75000.0, result.amount)
        assertEquals("INCOME", result.type)
        assertEquals("SALARY", result.merchant)
        assertEquals("9999", result.accountSuffix)
    }

    @Test
    fun parse_MalformedSms_HandlesSafely() {
        val sms = "Hello, how are you?"
        val result = SmsParser.parse(sms)
        
        assertEquals(null, result.amount)
        assertEquals("EXPENSE", result.type) // Default
        assertEquals(null, result.merchant)
    }
}
