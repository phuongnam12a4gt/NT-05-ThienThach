package com.example.nt_05_thienthach

import com.example.nt_05_thienthach.game.*
import com.example.nt_05_thienthach.data.*
import org.junit.Assert.*
import org.junit.Test

class GameEngineTest {
    private fun running(upgrades: Upgrades = Upgrades()) = GameEngine(upgrades, seed = 42, tutorial = true).apply { startTutorial() }
    private fun steps(game: GameEngine, n: Int) { repeat(n) { game.advance(GameConfig.STEP) } }
    private fun hit(game: GameEngine, id: Long = 1) { game.meteors.add(Meteor(id, game.shipX, 677f, MeteorKind.SMALL)) }
    private fun item(game: GameEngine, kind: ItemKind, id: Long = 0) { game.items.add(Item(id, game.shipX, 678f, kind)) }

    @Test fun scoreAndRewardRoundDown() {
        assertEquals(0L, GameConfig.score(0.0, 0))
        assertEquals(12L, GameConfig.score(1.29, 0))
        assertEquals(62L, GameConfig.score(1.29, 2))
        assertEquals(3L, GameConfig.credits(79))
    }
    @Test fun difficultyBoundariesAndCaps() {
        assertEquals(0, GameConfig.level(19.99)); assertEquals(1, GameConfig.level(20.0))
        assertEquals(2, GameConfig.level(59.99)); assertEquals(3, GameConfig.level(60.0))
        assertEquals(1.08f, GameConfig.speed(20.0), .0001f)
        assertEquals(2.2f, GameConfig.speed(9999.0), .0001f)
        assertEquals(.3, GameConfig.interval(9999.0), .0001)
    }
    @Test fun sweptCollisionIncludesBoundaryAndFastDrag() {
        assertTrue(CollisionSystem.swept(20f, 340f, 180f, 680f, 24f))
        assertTrue(CollisionSystem.swept(180f, 180f, 204f, 680f, 24f))
        assertFalse(CollisionSystem.swept(180f, 180f, 204.1f, 680f, 24f))
        assertFalse(CollisionSystem.swept(20f, 340f, 180f, 650f, 24f))
    }
    @Test fun touchingDoesNotMoveUntilNextFixedStepAndClamps() {
        val game = running(); game.targetX = 999f
        assertEquals(180f, game.shipX, 0f)
        steps(game, 1); assertEquals(340f, game.shipX, 0f)
        game.targetX = -100f; steps(game, 1); assertEquals(20f, game.shipX, 0f)
    }
    @Test fun countdownDoesNotSimulateAndResumesFromThree() {
        val game = GameEngine(seed = 42)
        repeat(29) { game.advance(.1) }
        assertEquals(Phase.COUNTDOWN, game.phase); assertEquals(0.0, game.time, 0.0)
        game.pause(); game.advance(.1); game.resume()
        assertEquals(3.0, game.countdown, 0.0)
        repeat(30) { game.advance(.1) }
        assertEquals(Phase.RUNNING, game.phase); assertEquals(0L, game.score)
    }
    @Test fun tutorialNeedsExplicitStart() {
        val game = GameEngine(tutorial = true)
        game.advance(.1); assertEquals(Phase.READY, game.phase); assertEquals(0.0, game.time, 0.0)
        game.pause(); game.resume(); assertEquals(Phase.READY, game.phase)
        game.startTutorial(); steps(game, 1); assertTrue(game.time > 0)
    }
    @Test fun pauseFreezesWorldEffectsAndScore() {
        val game = running(); item(game, ItemKind.SHIELD); steps(game, 1)
        game.meteors.add(Meteor(1, 20f, 40f, MeteorKind.SMALL))
        val y = game.meteors[0].y; val time = game.time; val score = game.score; val shield = game.shield
        game.pause(); steps(game, 100)
        assertEquals(y, game.meteors[0].y, 0f); assertEquals(time, game.time, 0.0)
        assertEquals(score, game.score); assertEquals(shield, game.shield, 0.0)
    }
    @Test fun multipleHitsCostOnlyOneArmorAndRemoveMeteors() {
        val game = running(); hit(game, 1); hit(game, 2); steps(game, 1)
        assertEquals(2, game.armor); assertTrue(game.meteors.isEmpty())
        assertEquals(1, game.events.count { it == GameEvent.HIT })
    }
    @Test fun shieldPickupAppliesBeforeMeteorAndOnlyBlocksOnce() {
        val game = running(); hit(game); item(game, ItemKind.SHIELD); steps(game, 1)
        assertEquals(3, game.armor); assertEquals(0.0, game.shield, 0.0)
        assertEquals(1, game.collected); assertTrue(game.events.contains(GameEvent.SHIELD_BREAK))
        hit(game, 2); steps(game, 1); assertEquals(3, game.armor)
    }
    @Test fun invulnerabilityDoesNotConsumeNewShield() {
        val game = running(); hit(game); steps(game, 1)
        item(game, ItemKind.SHIELD); hit(game, 2); steps(game, 1)
        assertTrue(game.shield > 0); assertEquals(2, game.armor)
    }
    @Test fun invulnerabilityExpires() {
        val game = running(); hit(game); steps(game, 1); steps(game, 74)
        hit(game, 2); steps(game, 1); assertEquals(1, game.armor)
    }
    @Test fun repairClampsAndFullRepairStillCounts() {
        val game = running(); item(game, ItemKind.REPAIR); steps(game, 1)
        assertEquals(3, game.armor); assertEquals(1, game.collected)
        hit(game); steps(game, 1); item(game, ItemKind.REPAIR, 2); steps(game, 1)
        assertEquals(3, game.armor); assertEquals(2, game.collected)
    }
    @Test fun repeatedItemsRefreshInsteadOfStacking() {
        val game = running(Upgrades(shield = 2, slow = 2))
        item(game, ItemKind.SHIELD); item(game, ItemKind.SLOW, 1); steps(game, 1)
        assertEquals(10.0, game.shield, 0.0); assertEquals(6.0, game.slow, 0.0)
        steps(game, 30); item(game, ItemKind.SHIELD, 2); steps(game, 1)
        assertEquals(10.0, game.shield, 0.0)
    }
    @Test fun slowAffectsMeteorsButNotItemVelocityOrTime() {
        val game = running(); item(game, ItemKind.SLOW); steps(game, 1)
        game.meteors.add(Meteor(5, 20f, 0f, MeteorKind.SMALL))
        game.items.add(Item(6, 20f, 0f, ItemKind.SHIELD))
        val before = game.time; steps(game, 1)
        assertEquals(220f * .6f / 60, game.meteors[0].y, .001f)
        assertEquals(140f / 60, game.items[0].y, .001f)
        assertEquals(before + GameConfig.STEP, game.time, 1e-9)
    }
    @Test fun largeFrameDeltaIsBounded() {
        val game = running(); game.advance(99.0)
        assertEquals(.1, game.time, 1e-8)
    }
    @Test fun gameOverEmittedOnceAndResultFrozen() {
        val game = running()
        repeat(3) { hit(game, it.toLong()); steps(game, 1); if (it < 2) steps(game, 74) }
        assertEquals(Phase.GAME_OVER, game.phase); assertEquals(0, game.armor)
        val result = game.result; steps(game, 100)
        assertEquals(result, game.result); assertEquals(1, game.events.count { it == GameEvent.GAME_OVER })
    }
    @Test fun noMeteorsForFirstTwoSeconds() {
        val game = running(); steps(game, 120); assertTrue(game.meteors.isEmpty())
        steps(game, 1); assertEquals(1, game.meteors.size)
    }
    @Test fun spawnSeedAndKindWindowsAreReproducible() {
        val a = SpawnSystem(42); val b = SpawnSystem(42)
        repeat(100) {
            val t = it.toDouble(); assertEquals(a.meteor(t, emptyList()), b.meteor(t, emptyList()))
        }
        val spawn = SpawnSystem(3)
        repeat(100) { assertEquals(MeteorKind.SMALL, spawn.meteor(19.99, emptyList())!!.kind) }
        repeat(100) { assertNotEquals(MeteorKind.LARGE, spawn.meteor(59.99, emptyList())!!.kind) }
    }
    @Test fun spawnLimitsAndSafeCorridor() {
        val spawn = SpawnSystem(1)
        assertNull(spawn.item(3, 3, 2))
        assertNull(spawn.meteor(100.0, List(80) { Meteor(it.toLong(), 1f, 300f, MeteorKind.SMALL) }))
        val wall = (0..5).map { Meteor(it.toLong(), it * 68f + 10f, 0f, MeteorKind.LARGE) }
        assertFalse(SpawnSystem.hasGap(wall))
        assertTrue(SpawnSystem.hasGap(wall.take(3)))
        assertNull(spawn.meteor(100.0, wall))
    }
    @Test fun shieldExpiresBeforeCollisionOnExactBoundary() {
        val game = running(); item(game, ItemKind.SHIELD); steps(game, 1)
        repeat(479) { game.meteors.clear(); steps(game, 1) }
        game.meteors.clear(); hit(game); steps(game, 1)
        assertEquals(0.0, game.shield, 0.0); assertEquals(2, game.armor)
        assertFalse(game.events.contains(GameEvent.SHIELD_BREAK))
    }
    @Test fun slowExpiresAtExactDuration() {
        val game = running(); item(game, ItemKind.SLOW); steps(game, 1)
        repeat(300) { game.meteors.clear(); steps(game, 1) }
        assertEquals(0.0, game.slow, 0.0)
    }
    @Test fun fixedStepTimeUsesExpectedDifficultyAtTwentySeconds() {
        val game = running()
        repeat(1200) { game.meteors.clear(); game.items.clear(); steps(game, 1) }
        assertEquals(1, GameConfig.level(game.time))
    }
    @Test fun fullArmorNeverSpawnsRepair() {
        val spawn = SpawnSystem(5)
        repeat(1000) { assertNotEquals(ItemKind.REPAIR, spawn.item(3, 3, 0)!!.kind) }
    }
    @Test fun rewardIsIdempotentAndUpdatesAllRecords() {
        val result = RunResult("run-1", 101L, 9999L, 2)
        val once = Transactions.reward(Progress(), result)
        assertEquals(5L, once.credits); assertEquals(1L, once.totalRuns); assertEquals(101L, once.bestScore)
        assertEquals(9999L, once.bestSurvivalMs); assertEquals(once, Transactions.reward(once, result))
        assertEquals(2L, Transactions.reward(once, result.copy(runId = "run-2")).totalRuns)
    }
    @Test fun purchasesDeductAndRespectMaximum() {
        var p = Progress(credits = 1000)
        p = Transactions.buy(p, UpgradeKind.ARMOR); assertEquals(800L, p.credits)
        p = Transactions.buy(p, UpgradeKind.ARMOR); assertEquals(300L, p.credits)
        assertEquals(5, p.upgrades.maxArmor)
        assertThrows(IllegalStateException::class.java) { Transactions.buy(p, UpgradeKind.ARMOR) }
    }
    @Test fun insufficientCreditsCannotBuy() {
        val p = Progress(credits = 99)
        assertThrows(IllegalStateException::class.java) { Transactions.buy(p, UpgradeKind.SHIELD) }
        assertEquals(99L, p.credits); assertEquals(0, p.upgrades.shield)
    }
}

