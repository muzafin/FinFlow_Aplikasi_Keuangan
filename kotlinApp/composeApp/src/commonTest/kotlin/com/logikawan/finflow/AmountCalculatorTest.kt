package com.logikawan.finflow

import com.logikawan.finflow.core.util.calculateAmount
import kotlin.test.*

class AmountCalculatorTest {
    @Test fun decimalArithmeticAndPrecedenceAreExact(){assertEquals(30L,calculateAmount("0.1+0.2","IDR"));assertEquals(1100L,calculateAmount("10/4+2*4.25","IDR"));assertEquals(1L,calculateAmount("0.01/2","IDR"))}
    @Test fun invalidAndOverflowNeverBecomeMoney(){listOf("1/0","1-2","1++2","90000000000000*90000000000000").forEach{assertFails{calculateAmount(it,"IDR")}}}
}
