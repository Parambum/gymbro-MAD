package com.gymbro.mad

import com.gymbro.mad.project.Owner
import com.gymbro.mad.project.Roadmap
import com.gymbro.mad.project.Routes
import com.gymbro.mad.ui.nav.Tab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The plan checks itself. Not because the compiler cares, but because the
 * split between the four of us is the thing most likely to drift quietly while
 * everyone is heads-down on their own screen.
 */
class RoadmapTest {

    @Test
    fun `John and Arnav own about sixty percent of the remaining work`() {
        val theirs = listOf(Owner.JOHN, Owner.ARNAV)
            .sumOf { owner -> Roadmap.forOwner(owner).sumOf { it.hours } }
        val share = theirs.toFloat() / Roadmap.hoursTotal

        assertTrue("their share is ${(share * 100).toInt()}%", share in 0.55f..0.65f)
    }

    @Test
    fun `Arnav owns the Train and Cardio tickets`() {
        assertTrue(Roadmap.tickets.filter { it.id.startsWith("P-") }.all { it.owner == Owner.ARNAV })
    }

    @Test
    fun `the base and completed tickets are marked done`() {
        assertTrue(Roadmap.forOwner(Owner.ADITYA).all { it.done })
        val completedByOthers = setOf("J-1", "J-2", "J-3", "J-4", "J-5", "J-6", "A-1", "A-2", "A-3")
        completedByOthers.forEach { id ->
            assertTrue("$id should be done", Roadmap.tickets.first { it.id == id }.done)
        }
        assertTrue(Roadmap.tickets.filter { it.id.startsWith("P-") }.all { it.done })
        assertEquals(Roadmap.hoursTotal - Roadmap.hoursDone, Roadmap.hoursLeft)
    }

    @Test
    fun `dashboard reaches one hundred percent only when every ticket is complete`() {
        assertTrue(Roadmap.tickets.all { it.done })
        assertEquals(0, Roadmap.hoursLeft)
        assertEquals(1f, Roadmap.progress, 0f)
    }

    @Test
    fun `ticket ids are unique`() {
        val ids = Roadmap.tickets.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `every ticket route is a route the app actually has`() {
        val known = setOf(
            Routes.DECK,
            Routes.TRAIN,
            Routes.CARDIO,
            Routes.FUEL,
            Routes.BODY,
            Routes.STATS,
            Routes.HISTORY,
            Routes.PROFILE,
            Routes.EXERCISE_PICKER,
            Routes.FOOD_SEARCH,
            Routes.SESSION_DETAIL,
            Routes.WORKOUT_SUMMARY,
        )
        for (ticket in Roadmap.tickets) {
            val route = ticket.route ?: continue
            assertTrue("${ticket.id} points at an unknown route: $route", route in known)
        }
    }

    @Test
    fun `every tab owner has tickets or has finished them`() {
        for (tab in Tab.entries) {
            val owned = Roadmap.forOwner(tab.owner)
            assertTrue("${tab.label} has an owner with no tickets at all", owned.isNotEmpty())
        }
    }
}
