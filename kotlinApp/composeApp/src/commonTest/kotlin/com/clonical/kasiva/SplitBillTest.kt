package com.clonical.kasiva

import com.clonical.kasiva.feature.level.LevelCurve
import com.clonical.kasiva.feature.planning.buildSplitPlan
import com.clonical.kasiva.feature.planning.splitEvenly
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SplitBillTest {
    @Test fun evenSplitKeepsTheExactTotal() {
        assertEquals(listOf(3334L, 3333L, 3333L), splitEvenly(10_000, 3))
        assertEquals(30_000_00L, splitEvenly(30_000_00, 3).sum())
        assertEquals(listOf(0L, 0L), splitEvenly(0, 2))
    }

    @Test fun planRecordsMyShareAsExpenseAndFriendsAsReceivables() {
        val plan = buildSplitPlan("Makan malam", 300_000_00, "IDR", "acc_cash", "food", "2026-11-11", "2026-11-18",
            100_000_00, listOf("Budi" to 100_000_00L, "Sari" to 100_000_00L))
        val mine = plan.myShare!!
        assertEquals("expense", mine.type); assertEquals("manual", mine.source); assertEquals(100_000_00L, mine.amountMinor)
        assertEquals(listOf("Budi", "Sari"), plan.receivables.map { it.person })
        assertTrue(plan.receivables.all { it.direction == "lent" && it.includeBalance && it.accountId == "acc_cash" && it.dueDate == "2026-11-18" })
    }

    @Test fun payingOnlyForOthersCreatesNoExpense() {
        val plan = buildSplitPlan("Tiket", 50_000_00, "IDR", "acc_cash", "food", "2026-11-11", "2026-11-11", 0, listOf("Budi" to 50_000_00L))
        assertNull(plan.myShare)
        assertEquals(1, plan.receivables.size)
    }

    @Test fun invalidPlansAreRejected() {
        assertFailsWith<IllegalArgumentException> { buildSplitPlan("A", 100, "IDR", "a", "food", "2026-11-11", "2026-11-12", 50, listOf("Budi" to 40L)) }
        assertFailsWith<IllegalArgumentException> { buildSplitPlan("A", 100, "IDR", "a", "food", "2026-11-11", "2026-11-12", 50, listOf(" " to 50L)) }
        assertFailsWith<IllegalArgumentException> { buildSplitPlan("A", 100, "IDR", "a", "food", "2026-11-11", "2026-11-12", 100, emptyList()) }
        assertFailsWith<IllegalArgumentException> { buildSplitPlan("A", 100, "IDR", "a", "food", "2026-11-11", "2026-11-10", 50, listOf("Budi" to 50L)) }
    }

    @Test fun levelCurveMatchesTheServer() {
        assertEquals(1, LevelCurve.levelFor(99)); assertEquals(2, LevelCurve.levelFor(100))
        assertEquals(3, LevelCurve.levelFor(300)); assertEquals(600, LevelCurve.start(4))
    }
}
