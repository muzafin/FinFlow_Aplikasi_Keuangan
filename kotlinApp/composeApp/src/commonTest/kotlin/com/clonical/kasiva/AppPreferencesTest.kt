package com.clonical.kasiva

import com.clonical.kasiva.core.model.AppPreferences
import com.clonical.kasiva.core.model.financeMenus
import com.clonical.kasiva.core.model.statisticsSections
import kotlin.test.*

class AppPreferencesTest {
    @Test
    fun nilaiTidakValidDiperbaikiTanpaMelempar() {
        val rusak = AppPreferences(theme = "neon", palette = "ungu", menuOrder = listOf("Anggaran"),
            statisticsOrder = listOf("trend"), cardGradient = "hilang", menuTheme = "hilang",
            language = "xx", walletGroupOrder = listOf("cash"), recordingDay = 99, weekStart = 0)
        val perbaikan = rusak.repaired()
        perbaikan.validate()
        assertEquals("system", perbaikan.theme)
        assertEquals("neutral", perbaikan.palette)
        assertEquals(financeMenus.toSet(), perbaikan.menuOrder.toSet())
        assertEquals(financeMenus.size, perbaikan.menuOrder.size)
        assertEquals(statisticsSections.toSet(), perbaikan.statisticsOrder.toSet())
        assertEquals(statisticsSections.size, perbaikan.statisticsOrder.size)
        assertEquals("teal", perbaikan.cardGradient)
        assertEquals("colorful", perbaikan.menuTheme)
        assertEquals("system", perbaikan.language)
        assertEquals(AppPreferences().walletGroupOrder, perbaikan.walletGroupOrder)
        assertEquals(31, perbaikan.recordingDay)
        assertEquals(1, perbaikan.weekStart)
    }

    @Test
    fun pengaturanValidTidakBerubah() {
        val utuh = AppPreferences().copy(theme = "dark", palette = "ruby", cardGradient = "palette_ruby",
            menuTheme = "palette_ruby", language = "en", homePeriod = "Tahun")
        assertEquals(utuh, utuh.repaired())
    }
}
