package com.example.nt_05_thienthach

import com.example.nt_05_thienthach.game.*
import org.junit.Assert.*
import org.junit.Test

class FlightUpgradeTest {
    private fun game() = GameEngine(seed = 87, tutorial = true).apply { startTutorial() }
    private fun quiet(g: GameEngine, steps: Int) {
        repeat(steps) { g.meteors.clear(); g.items.clear(); g.advance(GameConfig.STEP) }
    }
    private fun offered() = game().apply { quiet(this, 2700) }
    private fun resume(g: GameEngine) { repeat(30) { g.advance(.1) } }
    @Test fun choiceStartsAt45SecondsAndFreezesSimulation() {
        val g = game(); quiet(g, 2699); assertEquals(Phase.RUNNING, g.phase)
        quiet(g, 1); assertEquals(Phase.CHOOSING, g.phase)
        val time = g.time; val score = g.score; val energy = g.energy
        g.meteors.add(Meteor(9, 100f, 100f, MeteorKind.SMALL))
        repeat(100) { g.advance(.1) }
        assertEquals(time, g.time, 0.0); assertEquals(score, g.score)
        assertEquals(energy, g.energy, 0.0); assertEquals(100f, g.meteors.last().y, 0f)
        assertFalse(g.requestPulse())
    }
    @Test fun choiceIsGuardedAndResumesWithCountdown() {
        val g = game(); assertFalse(g.chooseUpgrade(FlightUpgrade.FOCUS))
        quiet(g, 2700); assertTrue(g.chooseUpgrade(FlightUpgrade.FOCUS))
        assertEquals(Phase.COUNTDOWN, g.phase); assertFalse(g.chooseUpgrade(FlightUpgrade.DAREDEVIL))
        val time = g.time; resume(g); assertEquals(time, g.time, 0.0)
        assertEquals(Phase.RUNNING, g.phase); assertFalse(g.upgradeOptions.contains(FlightUpgrade.FOCUS))
    }
    @Test fun backgroundPauseReturnsToChoiceWithoutBypass() {
        val g = offered(); g.pause(); assertEquals(Phase.PAUSED, g.phase)
        assertFalse(g.chooseUpgrade(FlightUpgrade.FOCUS)); quiet(g, 100)
        g.resume(); assertEquals(Phase.CHOOSING, g.phase)
        assertTrue(g.flightUpgrades.isEmpty()); assertTrue(g.chooseUpgrade(FlightUpgrade.FOCUS))
    }
    @Test fun capacitorExpandsActualPulseButSlowsPassiveRecharge() {
        val g = offered(); g.chooseUpgrade(FlightUpgrade.CAPACITOR); resume(g)
        g.meteors.clear(); g.meteors.add(Meteor(99, 180f, 450f, MeteorKind.SMALL))
        assertTrue(g.requestPulse()); g.advance(GameConfig.STEP)
        assertTrue(g.meteors.none { it.id == 99L }); assertEquals(250f, g.pulseRadius, 0f)
        quiet(g, 60); assertEquals(2.1, g.energy, .0001)
        g.items.add(Item(1, g.shipX, 679f, ItemKind.SHIELD)); g.advance(GameConfig.STEP)
        assertEquals(22.135, g.energy, .001)
    }
    @Test fun daredevilDoublesNearScoreAndClampsArmorAndRepair() {
        val g = offered(); g.chooseUpgrade(FlightUpgrade.DAREDEVIL); resume(g)
        assertEquals(2, g.maxArmor); assertEquals(2, g.armor)
        val bonus = g.bonusScore
        g.meteors.clear(); g.meteors.add(Meteor(99, g.shipX + 34, 679f, MeteorKind.SMALL))
        g.advance(GameConfig.STEP); assertEquals(bonus + 40, g.bonusScore)
        g.items.add(Item(1, g.shipX, 679f, ItemKind.REPAIR)); g.advance(GameConfig.STEP)
        assertEquals(2, g.armor)
    }
    @Test fun focusExtendsComboAndShortensFutureSlowItems() {
        val g = offered(); g.chooseUpgrade(FlightUpgrade.FOCUS); resume(g)
        g.meteors.clear(); g.meteors.add(Meteor(99, g.shipX + 34, 679f, MeteorKind.SMALL))
        g.advance(GameConfig.STEP); assertEquals(8.0, g.comboRemaining, 0.0)
        g.items.add(Item(1, g.shipX, 679f, ItemKind.SLOW)); g.advance(GameConfig.STEP)
        assertEquals(4.0, g.slow, .0001)
    }
    @Test fun choicesAreUniqueCappedAndDoNotLeakIntoNewRun() {
        val g = offered()
        FlightUpgrade.entries.forEachIndexed { index, choice ->
            assertEquals(Phase.CHOOSING, g.phase)
            if (index > 0) assertFalse(g.chooseUpgrade(FlightUpgrade.CAPACITOR))
            assertTrue(g.chooseUpgrade(choice)); resume(g)
            if (index < 2) quiet(g, 2700)
        }
        quiet(g, 2800); assertEquals(Phase.RUNNING, g.phase)
        assertEquals(3, g.result.flightUpgrades.size); assertTrue(g.upgradeOptions.isEmpty())
        val fresh = game(); assertTrue(fresh.flightUpgrades.isEmpty()); assertEquals(3, fresh.maxArmor)
        assertEquals(180f, fresh.pulseRadius, 0f)
    }
    @Test fun selectingDoesNotSpendPersistentUpgradesOrChangeRewardFormula() {
        val g = offered(); val upgrades = g.upgrades
        g.chooseUpgrade(FlightUpgrade.DAREDEVIL)
        assertEquals(upgrades, g.upgrades); assertEquals(g.score / 20, g.result.earnedCredits)
        assertEquals(listOf(FlightUpgrade.DAREDEVIL), g.result.flightUpgrades)
    }
}
