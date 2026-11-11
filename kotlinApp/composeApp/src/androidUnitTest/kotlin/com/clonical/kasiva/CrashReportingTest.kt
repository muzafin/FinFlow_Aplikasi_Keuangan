package com.clonical.kasiva

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CrashReportingTest {
    @Test fun technicalReportPreservesCodeLocationsWithoutPrivateMessagesOrCauses() {
        val original = IllegalStateException("email@example.test balance=123 token=secret", RuntimeException("private cause"))
        original.addSuppressed(RuntimeException("private suppressed error"))
        val report = technicalException(original)
        assertEquals("java.lang.IllegalStateException", report.message)
        assertContentEquals(original.stackTrace, report.stackTrace)
        assertNull(report.cause)
        assertTrue(report.suppressed.isEmpty())
    }
}
