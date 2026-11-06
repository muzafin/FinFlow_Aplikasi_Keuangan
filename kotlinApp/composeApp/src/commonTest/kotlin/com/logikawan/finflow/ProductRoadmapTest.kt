package com.logikawan.finflow

import com.logikawan.finflow.feature.settings.*
import kotlin.test.*

class ProductRoadmapTest {
    @Test fun featureIdsAreUniqueAndEveryStatusHasCompleteActionableEntries() {
        assertEquals(productRoadmap.size, productRoadmap.map { it.id }.toSet().size)
        for (status in RoadmapStatus.entries) assertTrue(productRoadmap.any { it.status == status })
        for (feature in productRoadmap) {
            assertTrue(feature.id.matches(Regex("FF-[0-9]{3}")))
            assertTrue(listOf(feature.title, feature.area, feature.description, feature.next).all { it.isNotBlank() })
            assertTrue(feature.prompt().contains(feature.id))
            assertTrue(feature.prompt().contains(feature.status.label))
            assertTrue(feature.prompt().contains(feature.next))
        }
        val all = roadmapPrompt()
        productRoadmap.forEach { assertTrue(all.contains("Fitur FinFlow ${it.id}:")) }
        assertTrue(all.contains(roadmapUpdated))
    }
}
