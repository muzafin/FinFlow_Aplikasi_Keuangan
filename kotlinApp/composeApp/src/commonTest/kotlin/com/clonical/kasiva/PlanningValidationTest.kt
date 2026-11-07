package com.clonical.kasiva

import com.clonical.kasiva.core.model.*
import kotlin.test.*

class PlanningValidationTest {
    @Test fun monthEndKeepsOriginalAnchor() {
        val feb=nextOccurrence("2026-01-31","monthly",anchorDay=31)
        assertEquals("2026-02-28",feb)
        assertEquals("2026-03-31",nextOccurrence(feb,"monthly",anchorDay=31))
        assertEquals("2028-02-29",nextOccurrence("2024-02-29","yearly",interval=4,anchorDay=29))
    }
    @Test fun dailyWeeklyAndIntervalAreExact() {
        assertEquals("2027-01-01",nextOccurrence("2026-12-31","daily"))
        assertEquals("2026-11-12",nextOccurrence("2026-10-29","weekly",2))
        assertEquals("2027-01-31",nextOccurrence("2026-10-31","monthly",3,31))
        assertFailsWith<IllegalArgumentException>{nextOccurrence("2026-10-29","monthly",0)}
    }
    @Test fun amountsAndDebtCannotExceedBounds() {
        assertFailsWith<IllegalArgumentException>{PlanningValidation.bill(Bill(title="Uji",amountMinor=0,accountId="cash"))}
        assertFailsWith<IllegalArgumentException>{PlanningValidation.debt(Debt(person="Uji",amountMinor=100,paidMinor=101,accountId="cash"))}
        assertFailsWith<IllegalArgumentException>{PlanningValidation.wish(Wish(title="Uji",targetMinor=9000000000000001))}
    }
    @Test fun cardRejectsFullNumberAndInvalidExpiry() {
        val card=WalletCard(name="Uji",lastFour="1234")
        PlanningValidation.card(card)
        assertFailsWith<IllegalArgumentException>{PlanningValidation.card(card.copy(lastFour="4111111111111111"))}
        assertFailsWith<IllegalArgumentException>{PlanningValidation.card(card.copy(expiryMonth=13,expiryYear=2028))}
    }
    @Test fun invalidDatesAndLinksAreRejected() {
        assertFails{PlanningValidation.bill(Bill(title="Uji",amountMinor=100,accountId="cash",dueDate="2026-02-30"))}
        assertFailsWith<IllegalArgumentException>{PlanningValidation.wish(Wish(title="Uji",targetMinor=100,link="javascript:alert(1)"))}
        assertFailsWith<IllegalArgumentException>{PlanningValidation.note(FinanceNote(title="Uji",body="x".repeat(10001)))}
        assertFailsWith<IllegalArgumentException>{PlanningValidation.recurring(RecurringTransaction(title="Uji",amountMinor=100,accountId="cash",categoryId="bills",startDate="2026-10-29",nextDate="2026-10-28"))}
    }
}
