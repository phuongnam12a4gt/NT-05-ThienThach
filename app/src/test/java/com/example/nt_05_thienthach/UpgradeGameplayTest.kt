package com.example.nt_05_thienthach

import com.example.nt_05_thienthach.game.*
import org.junit.Assert.*
import org.junit.Test

class UpgradeGameplayTest {
    private fun game() = GameEngine(seed = 731, tutorial = true).apply { startTutorial() }
    private fun tick(game: GameEngine) = game.advance(GameConfig.STEP)
    private fun quiet(game: GameEngine, count: Int) { repeat(count) { game.meteors.clear(); game.items.clear(); tick(game) } }
    private fun near(game: GameEngine, id: Long = 1) {
        game.meteors.clear(); game.meteors.add(Meteor(id, game.shipX + 34f, 679f, MeteorKind.SMALL)); tick(game)
    }
    @Test fun nearMissAwardsPointsOncePerRock() {
        val g = game(); near(g)
        assertEquals(1, g.nearMisses); assertEquals(20L, g.bonusScore)
        repeat(20) { tick(g) }
        assertEquals(1, g.nearMisses); assertEquals(20L, g.bonusScore)
        assertEquals(1, g.events.count { it == GameEvent.NEAR_MISS })
    }
    @Test fun distantPassDoesNotAwardNearMiss() {
        val g = game(); g.meteors.add(Meteor(1, 270f, 679f, MeteorKind.SMALL)); tick(g)
        assertEquals(0, g.nearMisses)
    }
    @Test fun collisionsAreNotNearMisses() {
        val g = game(); g.meteors.add(Meteor(1, 180f, 679f, MeteorKind.SMALL)); tick(g)
        assertEquals(0, g.nearMisses); assertEquals(2, g.armor)
        near(g, 2); assertEquals(0, g.nearMisses)
    }
    @Test fun comboMultiplierHasThresholdsAndCap() {
        val g = game()
        near(g); assertEquals(1, g.multiplier)
        near(g, 2); near(g, 3); assertEquals(2, g.multiplier); assertEquals(80L, g.bonusScore)
        near(g, 4); near(g, 5); near(g, 6); assertEquals(3, g.multiplier)
        repeat(10) { near(g, 7L + it) }
        assertEquals(9, g.combo); assertEquals(9, g.bestCombo); assertEquals(3, g.multiplier)
    }
    @Test fun comboExpiresButRecordAndBonusRemain() {
        val g = game(); near(g)
        quiet(g, 300)
        assertEquals(0, g.combo); assertEquals(1, g.bestCombo); assertEquals(20L, g.bonusScore)
    }
    @Test fun armorHitBreaksCombo() {
        val g = game(); near(g)
        g.meteors.clear(); g.meteors.add(Meteor(2, 180f, 679f, MeteorKind.SMALL)); tick(g)
        assertEquals(0, g.combo); assertEquals(0.0, g.comboRemaining, 0.0)
    }
    @Test fun pulseClearsOnlyNearbyMeteorsAndNoFreeScore() {
        val g = game()
        g.meteors.add(Meteor(1, 180f, 600f, MeteorKind.SMALL))
        g.meteors.add(Meteor(2, 20f, 100f, MeteorKind.LARGE))
        assertTrue(g.requestPulse()); assertFalse(g.requestPulse()); tick(g)
        assertEquals(listOf(2L), g.meteors.map { it.id })
        assertEquals(0L, g.bonusScore); assertEquals(0, g.nearMisses); assertEquals(1, g.pulsesUsed)
        assertEquals(0.0, g.energy, 0.0); assertTrue(g.invulnerability > 0); assertTrue(g.pulseRemaining > 0)
        assertFalse(g.requestPulse())
    }
    @Test fun pulseRequestsCannotRunOutsideRunningOrSurvivePause() {
        val g = game(); assertTrue(g.requestPulse()); g.pause()
        assertFalse(g.requestPulse()); g.resume()
        repeat(30) { g.advance(.1) }; tick(g)
        assertEquals(0, g.pulsesUsed); assertEquals(100.0, g.energy, 0.0)
        assertFalse(GameEngine(tutorial = true).requestPulse())
        assertFalse(GameEngine().requestPulse())
    }
    @Test fun energyRechargesWithTimeNearMissAndItemsWithoutOverfill() {
        val g = game(); g.requestPulse(); tick(g); quiet(g, 40)
        assertEquals(2.0, g.energy, .0001)
        near(g); assertEquals(14.05, g.energy, .001)
        g.items.add(Item(88, 180f, 679f, ItemKind.SHIELD)); tick(g)
        assertEquals(34.1, g.energy, .001)
        quiet(g, 2400); assertEquals(100.0, g.energy, 0.0); assertTrue(g.requestPulse())
    }
    @Test fun waveWarnsBeforeLaunchingAndNormalSpawnsWait() {
        val g = game(); quiet(g, 720); assertNull(g.wave)
        g.meteors.clear(); tick(g)
        val wave = g.wave!!
        assertTrue(wave.warning > 1); assertFalse(wave.launched); assertTrue(g.meteors.isEmpty())
        var steps = 0
        while (!wave.launched && steps < 120) { g.meteors.clear(); tick(g); steps++ }
        assertTrue(wave.launched); assertTrue(steps >= 90); assertEquals(6, g.meteors.size)
        assertTrue(g.meteors.none { it.x >= wave.safeLane * 120 && it.x <= (wave.safeLane + 1) * 120 })
    }
    @Test fun wavesKeepACorridorAndRespectObjectLimit() {
        val spawn = SpawnSystem(1)
        for (lane in 0..2) {
            val rocks = spawn.wave(lane, emptyList())
            assertEquals(6, rocks.size); assertTrue(SpawnSystem.hasGap(rocks))
        }
        val full = List(80) { Meteor(it.toLong(), 10f, 400f, MeteorKind.SMALL) }
        assertTrue(spawn.wave(1, full).isEmpty())
    }
    @Test fun driftIsSeededAndBouncesInsideBoard() {
        val a = SpawnSystem(94); val b = SpawnSystem(94)
        val rocks = List(100) { a.meteor(60.0, emptyList())!! }
        assertEquals(rocks, List(100) { b.meteor(60.0, emptyList())!! })
        assertTrue(rocks.any { it.drift != 0f })
        val g = game(); val rock = Meteor(1, 347.9f, 100f, MeteorKind.SMALL, drift = 42f)
        g.meteors.add(rock); tick(g)
        assertTrue(rock.x <= 348); assertTrue(rock.drift < 0)
    }
    @Test fun survivalMissionRewardsExactlyOnce() {
        val g = game(); quiet(g, 1800)
        assertTrue(g.survivalMission); assertEquals(100L, g.bonusScore); assertEquals(400L, g.score)
        quiet(g, 100); assertEquals(100L, g.bonusScore)
    }
    @Test fun nearMissionRewardsExactlyOnceWithoutRequiringCurrentHudObjective() {
        val g = game(); repeat(5) { near(g, it.toLong()) }
        assertTrue(g.nearMission); assertFalse(g.survivalMission)
        assertEquals(310L, g.bonusScore)
        near(g, 6); assertEquals(370L, g.bonusScore)
        assertEquals(1, g.events.count { it == GameEvent.MILESTONE })
    }
    @Test fun itemMissionAndResultsIncludeBonusWithoutChangingCreditFormula() {
        val g = game()
        repeat(3) { g.items.add(Item(it.toLong(), 180f, 679f, ItemKind.REPAIR)); tick(g) }
        assertTrue(g.itemMission); assertEquals(100L, g.bonusScore); assertEquals(175L, g.score)
        val result = g.result
        assertEquals(1, result.missionsCompleted); assertEquals(100L, result.bonusScore); assertEquals(8L, result.earnedCredits)
    }
    @Test fun pauseFreezesNewTimersAndWaveAndEnergy() {
        val g = game(); quiet(g, 721); near(g); g.requestPulse(); tick(g)
        val energy = g.energy; val comboTime = g.comboRemaining; val pulse = g.pulseRemaining
        val wave = g.wave!!.warning; val notice = g.noticeRemaining
        g.pause(); repeat(100) { tick(g) }
        assertEquals(energy, g.energy, 0.0); assertEquals(comboTime, g.comboRemaining, 0.0)
        assertEquals(pulse, g.pulseRemaining, 0.0); assertEquals(wave, g.wave!!.warning, 0.0)
        assertEquals(notice, g.noticeRemaining, 0.0)
    }
}
