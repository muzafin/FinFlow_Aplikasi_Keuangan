package com.logikawan.finflow

import com.logikawan.finflow.core.model.AppPreferences
import com.logikawan.finflow.core.util.*
import kotlinx.datetime.LocalDate
import kotlin.test.*

class FinancePeriodTest {
    @Test fun monthBoundaryClampsAndDoesNotDrift() {
        val period=recordingPeriod("2026-01",31)
        assertEquals("2026-01-31",period.start.toString());assertEquals("2026-02-28",period.endExclusive.toString())
        assertEquals("2026-03-31",recordingPeriod("2026-02",31).endExclusive.toString())
        assertTrue("2026-02-27" in period);assertFalse("2026-02-28" in period)
    }
    @Test fun currentCycleAndWeekUseConfiguredStart() {
        assertEquals("2026-09",activeRecordingMonth(LocalDate(2026,10,5),25))
        assertEquals("2026-10",activeRecordingMonth(LocalDate(2026,10,25),25))
        assertEquals("2026-10-26",financePeriod("week",LocalDate(2026,10,29)).start.toString())
        assertEquals("2026-10-25",financePeriod("week",LocalDate(2026,10,29),weekStart=7).start.toString())
    }
    @Test fun preferencesRejectBrokenOrderAndHiddenNavigation() {
        AppPreferences().validate()
        assertFailsWith<IllegalArgumentException>{AppPreferences(menuOrder=listOf("Anggaran")).validate()}
        assertFailsWith<IllegalArgumentException>{AppPreferences(recordingDay=32).validate()}
        assertFailsWith<IllegalArgumentException>{AppPreferences(hiddenMenus=com.logikawan.finflow.core.model.financeMenus).validate()}
    }
}
