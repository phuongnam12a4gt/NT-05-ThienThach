package com.example.nt_05_thienthach.game

import java.util.UUID
import kotlin.math.*
import kotlin.random.Random

object GameConfig {
    const val WIDTH = 360f
    const val HEIGHT = 800f
    const val SHIP_Y = 680f
    const val SHIP_RADIUS = 12f
    const val MIN_X = 20f
    const val MAX_X = 340f
    const val STEP = 1.0 / 60.0
    const val MAX_DELTA = .10
    const val MAX_STEPS = 6
    const val BASE_ARMOR = 3
    const val INVULNERABILITY = 1.2
    const val SHIELD_SECONDS = 8.0
    const val SLOW_SECONDS = 5.0
    const val SLOW_FACTOR = .6f
    const val ITEM_SPEED = 140f
    const val ITEM_RADIUS = 12f
    const val MAX_METEORS = 80
    const val MAX_ITEMS = 2
    const val MAX_PARTICLES = 150
    const val FIRST_METEOR = 2.0
    const val FIRST_ITEM = 10.0
    const val ITEM_MIN_INTERVAL = 10.0
    const val ITEM_MAX_INTERVAL = 15.0
    const val SPAWN_ATTEMPTS = 8
    const val SAFE_GAP = 60f
    const val COUNTDOWN = 3.0
    const val NEAR_MARGIN = 20f
    const val NEAR_SCORE = 20L
    const val COMBO_SECONDS = 5.0
    const val MAX_COMBO = 9
    const val ENERGY_MAX = 100.0
    const val ENERGY_PER_SECOND = 3.0
    const val ENERGY_NEAR = 12.0
    const val ENERGY_ITEM = 20.0
    const val PULSE_RADIUS = 180f
    const val PULSE_SECONDS = .55
    const val PULSE_GRACE = .6
    const val FIRST_WAVE = 12.0
    const val WAVE_INTERVAL = 18.0
    const val WAVE_WARNING = 1.6
    const val WAVE_RECOVERY = 4.5
    const val SURVIVAL_TARGET = 30
    const val NEAR_TARGET = 5
    const val ITEM_TARGET = 3
    const val SURVIVAL_BONUS = 100L
    const val NEAR_BONUS = 150L
    const val ITEM_BONUS = 100L
    val armorPrices = listOf(200L, 500L)
    val effectPrices = listOf(100L, 250L, 500L)
    fun level(t: Double) = floor((t + 1e-8) / 20).toInt()
    fun speed(t: Double) = min(1 + level(t) * .08, 2.2).toFloat()
    fun interval(t: Double) = max(.90 - level(t) * .05, .30)
    fun score(t: Double, items: Int) = floor(t * 10 + 1e-8).toLong() + items * 25L
    fun credits(score: Long) = score / 20
}

enum class Phase { READY, COUNTDOWN, RUNNING, PAUSED, CHOOSING, GAME_OVER, VICTORY }
enum class FlightUpgrade { CAPACITOR, DAREDEVIL, FOCUS }

enum class ItemKind { SHIELD, REPAIR, SLOW }
enum class GameEvent { PICKUP, HIT, SHIELD_BREAK, GAME_OVER, NEAR_MISS, PULSE, WAVE, MILESTONE }
enum class Notice { NONE, NEAR_MISS, PICKUP, HIT, SHIELD_BREAK, PULSE, WAVE, MILESTONE }
enum class MeteorKind(val radius: Float, val speed: Float) {
    SMALL(12f, 220f), MEDIUM(22f, 170f), LARGE(34f, 125f)
}
data class Upgrades(val armor: Int = 0, val shield: Int = 0, val slow: Int = 0) {
    val maxArmor get() = GameConfig.BASE_ARMOR + armor
    val shieldSeconds get() = GameConfig.SHIELD_SECONDS + shield
    val slowSeconds get() = GameConfig.SLOW_SECONDS + slow * .5
}
data class Meteor(val id: Long, var x: Float, var y: Float, val kind: MeteorKind,
    var rotation: Float = 0f, var drift: Float = 0f, var grazed: Boolean = false,
    val tag: Int = 0, var rewound: Boolean = false, val createdAt: Double = 0.0, val targetAlly: Boolean = false)
data class Item(val id: Long, val x: Float, var y: Float, val kind: ItemKind)
data class Particle(var x: Float, var y: Float, val vx: Float, val vy: Float, var life: Float, val kind: Int)
data class Wave(val safeLane: Int, var warning: Double = GameConfig.WAVE_WARNING, var launched: Boolean = false)
data class RunResult(val runId: String, val score: Long, val survivalMs: Long, val collectedItems: Int,
    val nearMisses: Int = 0, val bestCombo: Int = 0, val pulsesUsed: Int = 0,
    val bonusScore: Long = 0, val missionsCompleted: Int = 0,
    val flightUpgrades: List<FlightUpgrade> = emptyList(), val endingEnergy: Int = 0,
    val stageId: Int? = null, val victory: Boolean = false, val achievementBits: Int = 0,
    val shipId: String = "S01", val practice: Boolean = false, val defeat: DefeatReason = DefeatReason.NONE) {
    val earnedCredits get() = GameConfig.credits(score)
}

object CollisionSystem {
    fun swept(fromX: Float, toX: Float, x: Float, y: Float, radius: Float): Boolean {
        val dx = x - x.coerceIn(min(fromX, toX), max(fromX, toX))
        val dy = y - GameConfig.SHIP_Y
        return dx * dx + dy * dy <= radius * radius
    }
}

class SpawnSystem(seed: Long) {
    private val random = Random(seed)
    private var id = 0L
    fun itemInterval() = random.nextDouble(GameConfig.ITEM_MIN_INTERVAL, GameConfig.ITEM_MAX_INTERVAL)
    fun safeLane() = random.nextInt(3)
    fun wave(safeLane: Int, existing: List<Meteor>): List<Meteor> {
        require(safeLane in 0..2)
        val result = mutableListOf<Meteor>()
        for (lane in 0..2) if (lane != safeLane) {
            for (offset in listOf(-26f, 0f, 26f)) {
                if (existing.size + result.size >= GameConfig.MAX_METEORS) return result
                val kind = if (offset == 0f) MeteorKind.MEDIUM else MeteorKind.SMALL
                val meteor = Meteor(++id, lane * 120f + 60 + offset, -kind.radius, kind)
                if (hasGap(existing + result + meteor)) result.add(meteor)
            }
        }
        return result
    }
    fun meteor(time: Double, existing: List<Meteor>): Meteor? {
        if (existing.size >= GameConfig.MAX_METEORS) return null
        val roll = random.nextDouble()
        val kind = when {
            GameConfig.level(time) == 0 -> MeteorKind.SMALL
            GameConfig.level(time) < 3 -> if (roll < .65) MeteorKind.SMALL else MeteorKind.MEDIUM
            roll < .5 -> MeteorKind.SMALL
            roll < .85 -> MeteorKind.MEDIUM
            else -> MeteorKind.LARGE
        }
        repeat(GameConfig.SPAWN_ATTEMPTS) {
            val x = random.nextDouble(kind.radius.toDouble(), (360 - kind.radius).toDouble()).toFloat()
            val candidate = Meteor(id + 1, x, -kind.radius, kind)
            if (hasGap(existing + candidate)) {
                id++
                if (time >= 20 && kind != MeteorKind.LARGE && random.nextDouble() < .35)
                    candidate.drift = random.nextDouble(22.0, 42.0).toFloat() * if (random.nextBoolean()) 1 else -1
                return candidate
            }
        }
        return null
    }
    fun item(armor: Int, maxArmor: Int, count: Int): Item? {
        if (count >= GameConfig.MAX_ITEMS) return null
        val roll = random.nextDouble()
        val kind = if (armor >= maxArmor) {
            if (roll < 40.0 / 75) ItemKind.SHIELD else ItemKind.SLOW
        } else when { roll < .4 -> ItemKind.SHIELD; roll < .65 -> ItemKind.REPAIR; else -> ItemKind.SLOW }
        return Item(++id, random.nextDouble(12.0, 348.0).toFloat(), -12f, kind)
    }
    companion object {
        fun hasGap(meteors: List<Meteor>): Boolean {
            val ranges = meteors.filter { it.y in -34f..140f }
                .map { max(0f, it.x - it.kind.radius) to min(360f, it.x + it.kind.radius) }.sortedBy { it.first }
            var end = 0f
            for ((left, right) in ranges) {
                if (left - end >= GameConfig.SAFE_GAP) return true
                end = max(end, right)
            }
            return 360f - end >= GameConfig.SAFE_GAP
        }
    }
}

class GameEngine(val upgrades: Upgrades = Upgrades(), val seed: Long = System.nanoTime(),
                 val runId: String = UUID.randomUUID().toString(), tutorial: Boolean = false,
                 val ship: ShipDefinition = ShipCatalog.nova, val stage: StageDefinition? = null,
                 val astra: AstraConfiguration = AstraConfiguration.ARMOR, val practice: Boolean = false,
                 val route: Int = 0) {
    var phase = if (tutorial) Phase.READY else Phase.COUNTDOWN; private set
    var countdown = GameConfig.COUNTDOWN; private set
    var time = 0.0; private set
    var shipX = 180f; private set
    var targetX = 180f
    var armor = ship.armor + upgrades.armor + if (ship.id == "S12" && astra == AstraConfiguration.ARMOR) 1 else 0; private set
    var invulnerability = 0.0; private set
    var shield = 0.0; private set
    var slow = 0.0; private set
    var collected = 0; private set
    var nearMisses = 0; private set
    var combo = 0; private set
    var bestCombo = 0; private set
    var comboRemaining = 0.0; private set
    val multiplier get() = min(3, 1 + combo / 3)
    var bonusScore = 0L; private set
    var energy = GameConfig.ENERGY_MAX; private set
    var pulsesUsed = 0; private set
    var pulseRemaining = 0.0; private set
    var pulseX = 180f; private set
    var notice = Notice.NONE; private set
    var noticeRemaining = 0.0; private set
    var noticeValue = 0L; private set
    var wave: Wave? = null; private set
    var survivalMission = false; private set
    var nearMission = false; private set
    var itemMission = false; private set
    val missionsCompleted get() = listOf(survivalMission, nearMission, itemMission).count { it }
    val score get() = GameConfig.score(time, collected) + bonusScore
    val meteors = mutableListOf<Meteor>()
    val items = mutableListOf<Item>()
    val particles = mutableListOf<Particle>()
    val events = mutableListOf<GameEvent>()
    val campaign = stage?.let { StageSession(it, practice, route) }
    var horizontalVelocity = 0f; private set
    var armorHits = 0; private set
    var sentinelUsed = false; private set
    var defeatReason = DefeatReason.NONE; private set
    private var medicHeals = 0
    private var nextMedic = 25.0
    private var nextAurora = 0.0
    private var wraithUntil = 0.0
    private val spawner = SpawnSystem(seed)
    private var nextMeteor = GameConfig.FIRST_METEOR
    private var nextItem = GameConfig.FIRST_ITEM
    private var nextWave = GameConfig.FIRST_WAVE
    private var waveRecovery = 0.0
    private var accumulator = 0.0
    private var hitSerial = 0
    private var pulseRequested = false
    private var wasTutorial = false
    private val chosenUpgrades = mutableListOf<FlightUpgrade>()
    val flightUpgrades: List<FlightUpgrade> get() = chosenUpgrades.toList()
    val upgradeOptions get() = FlightUpgrade.entries.filter { it !in chosenUpgrades }
    val maxArmor get() = ship.armor + upgrades.armor + (if (ship.id == "S12" && astra == AstraConfiguration.ARMOR) 1 else 0) - (if (FlightUpgrade.DAREDEVIL in chosenUpgrades) 1 else 0)
    val pulseRadius get() = (if (ship.id == "S06") 160f else 180f) +
        (if (ship.id == "S12" && astra == AstraConfiguration.PULSE) 30f else 0f) +
        (if (FlightUpgrade.CAPACITOR in chosenUpgrades) 70f else 0f)
    val speedLimit get() = if (ship.id == "S12" && astra == AstraConfiguration.ARMOR) 680f else ship.speed
    fun rechargeEnergy(value: Double) { energy = (energy + value).coerceIn(0.0, 100.0) }
    fun grantShield() { shield = upgrades.shieldSeconds + if (ship.id == "S06") 2 else 0 }
    fun repair(value: Int = 1) { armor = min(maxArmor, armor + value) }
    fun finishStage(won: Boolean, reason: DefeatReason = DefeatReason.NONE) {
        if (phase != Phase.RUNNING) return
        phase = if (won) Phase.VICTORY else Phase.GAME_OVER
        defeatReason = reason; pulseRequested = false; accumulator = 0.0
        events.add(if (won) GameEvent.MILESTONE else GameEvent.GAME_OVER)
    }
    fun damage(tag: Int = 0) {
        if (phase != Phase.RUNNING || invulnerability > 0) return
        invulnerability = GameConfig.INVULNERABILITY
        if (shield > 0) { campaign?.onShieldBlock(); shield = 0.0; events.add(GameEvent.SHIELD_BREAK); notify(Notice.SHIELD_BREAK) }
        else {
            armorHits++; campaign?.recordHit(tag)
            armor = max(0, armor - 1); combo = 0; comboRemaining = 0.0
            events.add(GameEvent.HIT); notify(Notice.HIT)
            if (armor == 0 && ship.id == "S10" && !sentinelUsed) { armor = 1; sentinelUsed = true; invulnerability = 1.0 }
            if (armor == 0) { phase = Phase.GAME_OVER; defeatReason = DefeatReason.ARMOR; events.add(GameEvent.GAME_OVER) }
        }
        burst(shipX, GameConfig.SHIP_Y, 0)
    }
    private var pausedFrom = Phase.RUNNING
    fun offerUpgrade() { if (phase == Phase.RUNNING && upgradeOptions.isNotEmpty()) { phase = Phase.CHOOSING; targetX = shipX; pulseRequested = false } }
    fun chooseUpgrade(choice: FlightUpgrade): Boolean {
        if (phase != Phase.CHOOSING || choice !in upgradeOptions) return false
        chosenUpgrades.add(choice)
        armor = min(armor, maxArmor)
        phase = Phase.COUNTDOWN; countdown = GameConfig.COUNTDOWN
        accumulator = 0.0; targetX = shipX; pulseRequested = false
        return true
    }
    var reducedMotion = false
    val result get() = RunResult(runId, score, (time * 1000).roundToLong(), collected,
        nearMisses, bestCombo, pulsesUsed, bonusScore, missionsCompleted, flightUpgrades, energy.toInt(),
        stage?.id, phase == Phase.VICTORY, if (phase == Phase.VICTORY) (1 or (if (armorHits <= (stage?.damageLimit ?: 0)) 2 else 0) or (if (campaign?.skillAchieved(this) == true) 4 else 0)) else 0,
        ship.id, practice, defeatReason)
    fun startTutorial() { if (phase == Phase.READY) phase = Phase.RUNNING }
    fun requestPulse(): Boolean {
        if (phase != Phase.RUNNING || energy < GameConfig.ENERGY_MAX || pulseRequested) return false
        pulseRequested = true
        return true
    }
    fun pause() {
        if (phase == Phase.RUNNING || phase == Phase.COUNTDOWN || phase == Phase.READY || phase == Phase.CHOOSING) {
            pausedFrom = phase; wasTutorial = phase == Phase.READY; phase = Phase.PAUSED
            accumulator = 0.0; targetX = shipX; pulseRequested = false
        }
    }
    fun resume() {
        if (phase == Phase.PAUSED) {
            phase = if (wasTutorial) Phase.READY else if (pausedFrom == Phase.CHOOSING) Phase.CHOOSING else Phase.COUNTDOWN
            countdown = GameConfig.COUNTDOWN; accumulator = 0.0
        }
    }
    fun advance(delta: Double) {
        val dt = delta.coerceIn(0.0, GameConfig.MAX_DELTA)
        if (phase == Phase.COUNTDOWN) {
            countdown = max(0.0, countdown - dt)
            if (countdown < 1e-8) { countdown = 0.0; phase = Phase.RUNNING }
            return
        }
        if (phase != Phase.RUNNING) return
        accumulator += dt
        var steps = 0
        while (accumulator + 1e-10 >= GameConfig.STEP && steps < GameConfig.MAX_STEPS && phase == Phase.RUNNING) {
            step(); accumulator -= GameConfig.STEP; steps++
        }
        if (steps == GameConfig.MAX_STEPS) accumulator = 0.0
    }
    private fun remaining(value: Double, delta: Double) = (value - delta).let { if (it < 1e-8) 0.0 else it }
    private fun notify(type: Notice, value: Long = 0) { notice = type; noticeValue = value; noticeRemaining = 1.4 }
    private fun step() {
        val dt = GameConfig.STEP
        val previousX = shipX
        val desired = ((targetX.coerceIn(GameConfig.MIN_X, GameConfig.MAX_X) - shipX) / dt.toFloat()).coerceIn(-speedLimit, speedLimit)
        val inWarmZone = stage?.id==44 && campaign?.tokens?.any{abs(it.x-shipX)<50}==true || stage?.id==50 && campaign?.bossOpen(time)==true
        horizontalVelocity = if (campaign?.icy(time) == true && !inWarmZone) horizontalVelocity + (desired-horizontalVelocity) * min(1f, dt.toFloat()*4f) else desired
        val wind = (if(stage?.id==62 && abs(shipX-(campaign?.safeX(this)?:180f))<45)0f else campaign?.wind(time) ?: 0f) * if (ship.id == "S07" && time < wraithUntil) .5f else 1f
        shipX = (shipX + (horizontalVelocity + wind) * dt.toFloat()).coerceIn(GameConfig.MIN_X, GameConfig.MAX_X)
        campaign?.beforeStep(this, dt)
        if (ship.id == "S08" && time >= nextMedic) {
            nextMedic += 25.0
            if (armor < maxArmor && medicHeals < if (stage == null) 3 else 2) { repair(); medicHeals++ }
        }
        invulnerability = remaining(invulnerability, dt); shield = remaining(shield, dt); slow = remaining(slow, dt)
        comboRemaining = remaining(comboRemaining, dt)
        if (comboRemaining == 0.0) combo = 0
        pulseRemaining = remaining(pulseRemaining, dt)
        noticeRemaining = remaining(noticeRemaining, dt)
        if (noticeRemaining == 0.0) notice = Notice.NONE
        val recharge = if (ship.id == "S12" && astra == AstraConfiguration.PULSE) 2.6 else ship.recharge
        if (campaign?.rechargeAllowed(this) != false) rechargeEnergy(dt * recharge * if (FlightUpgrade.CAPACITOR in chosenUpgrades) .7 else 1.0)
        if (pulseRequested) {
            pulseRequested = false; energy = 0.0; pulsesUsed++
            pulseX = shipX; pulseRemaining = GameConfig.PULSE_SECONDS
            invulnerability = max(invulnerability, if (ship.id == "S09") .45 else GameConfig.PULSE_GRACE)
            val nearby = meteors.filter { hypot(it.x-shipX, it.y-GameConfig.SHIP_Y) <= pulseRadius + it.kind.radius }
            campaign?.onPulse(this, nearby)
            meteors.removeAll { meteor ->
                val inside = hypot(meteor.x - shipX, meteor.y - GameConfig.SHIP_Y) <= pulseRadius + meteor.kind.radius
                if (inside) burst(meteor.x, meteor.y, 2)
                inside
            }
            if (ship.id == "S09" && campaign?.hasAllies != true) meteors.filter { hypot(it.x-shipX, it.y-GameConfig.SHIP_Y) < pulseRadius + 60 }.forEach {
                val x = (it.x + if (it.x < shipX) -30f else 30f).coerceIn(it.kind.radius, 360-it.kind.radius)
                if (kotlin.math.abs(x-(campaign?.safeX(this) ?: shipX)) >= 65f + it.kind.radius) it.x = x
            }
            burst(shipX, GameConfig.SHIP_Y, 2); events.add(GameEvent.PULSE); notify(Notice.PULSE)
        }
        if (stage == null && time + 1e-8 >= nextWave) {
            wave = Wave(spawner.safeLane()); nextWave = time + GameConfig.WAVE_INTERVAL
            waveRecovery = time + GameConfig.WAVE_WARNING + GameConfig.WAVE_RECOVERY
            events.add(GameEvent.WAVE); notify(Notice.WAVE)
        }
        wave?.let { current ->
            current.warning = remaining(current.warning, dt)
            if (current.warning == 0.0 && !current.launched) {
                meteors.addAll(spawner.wave(current.safeLane, meteors)); current.launched = true
            }
            if (time >= waveRecovery) wave = null
        }
        if (stage == null && time + 1e-8 >= nextMeteor) {
            if (wave == null) spawner.meteor(time, meteors)?.let(meteors::add)
            nextMeteor = time + GameConfig.interval(time)
        }
        if (time + 1e-8 >= nextItem) {
            spawner.item(armor, maxArmor, items.size)?.let(items::add)
            nextItem = time + if (stage != null) 7.0 else spawner.itemInterval()
        }
        val speed = (if (stage != null) (.8f + stage.chapter * .035f) else GameConfig.speed(time)) * if (slow > 0) GameConfig.SLOW_FACTOR else 1f
        meteors.forEach {
            it.y += it.kind.speed * speed * dt.toFloat() * (campaign?.timeFactor(it, time) ?: 1f)
            it.x += it.drift * speed * dt.toFloat()
            if (it.x < it.kind.radius || it.x > 360 - it.kind.radius) {
                it.x = it.x.coerceIn(it.kind.radius, 360 - it.kind.radius); it.drift = -it.drift
            }
            it.rotation += dt.toFloat() * 28
        }
        items.forEach { it.y += GameConfig.ITEM_SPEED * dt.toFloat() }
        val itemIterator = items.iterator()
        while (itemIterator.hasNext()) {
            val item = itemIterator.next()
            if (CollisionSystem.swept(previousX, shipX, item.x, item.y, ship.radius + 12f) || (ship.id == "S04" && hypot(item.x-shipX, item.y-GameConfig.SHIP_Y) <= 45f)) {
                when (item.kind) {
                    ItemKind.SHIELD -> shield = upgrades.shieldSeconds + if (ship.id == "S06") 2 else 0
                    ItemKind.REPAIR -> armor = min(maxArmor, armor + 1)
                    ItemKind.SLOW -> slow = (upgrades.slowSeconds - if (ship.id == "S11") 1 else 0) * if (FlightUpgrade.FOCUS in chosenUpgrades) .8 else 1.0
                }
                collected++; campaign?.onPickup(item.kind)
                if (!(ship.id == "S08" && item.kind == ItemKind.REPAIR)) rechargeEnergy(if (ship.id == "S05") 15.0 else 20.0)
                if (ship.id == "S11" && time >= nextAurora) { rechargeEnergy(8.0); nextAurora = time + 15.0 }
                events.add(GameEvent.PICKUP); notify(Notice.PICKUP, 25); burst(item.x, item.y, 1); itemIterator.remove()
            } else if (item.y - 12 > 800) itemIterator.remove()
        }
        val iterator = meteors.iterator()
        while (iterator.hasNext()) {
            val meteor = iterator.next()
            val radius = meteor.kind.radius + ship.radius
            if (CollisionSystem.swept(previousX, shipX, meteor.x, meteor.y, radius)) {
                iterator.remove()
                damage(meteor.tag)
                if (phase == Phase.GAME_OVER) break
            } else {
                val previousY = meteor.y - meteor.kind.speed * speed * dt.toFloat()
                if (!meteor.grazed && previousY < GameConfig.SHIP_Y && meteor.y >= GameConfig.SHIP_Y) {
                    meteor.grazed = true
                    val distance = abs(meteor.x - shipX)
                    if (invulnerability <= 0 && distance > radius && distance <= radius + GameConfig.NEAR_MARGIN) {
                        nearMisses++; combo = min(GameConfig.MAX_COMBO, combo + 1); bestCombo = max(bestCombo, combo)
                        comboRemaining = (if (ship.id == "S07") 4.0 else GameConfig.COMBO_SECONDS) + if (FlightUpgrade.FOCUS in chosenUpgrades) 3.0 else 0.0
                        wraithUntil = time + 1.0; campaign?.onNear(meteor)
                        val points = GameConfig.NEAR_SCORE * multiplier * if (FlightUpgrade.DAREDEVIL in chosenUpgrades) 2 else 1
                        bonusScore += points; energy = min(GameConfig.ENERGY_MAX, energy + GameConfig.ENERGY_NEAR)
                        events.add(GameEvent.NEAR_MISS); notify(Notice.NEAR_MISS, points); burst(meteor.x, meteor.y, 1)
                    }
                }
                if (meteor.y - meteor.kind.radius > 800) iterator.remove()
            }
        }
        particles.forEach { it.x += it.vx * dt.toFloat(); it.y += it.vy * dt.toFloat(); it.life -= dt.toFloat() }
        particles.removeAll { it.life <= 0 }
        time += dt
        campaign?.afterStep(this, dt)
        if (phase == Phase.RUNNING) {
            if (stage == null && chosenUpgrades.size < FlightUpgrade.entries.size && time + 1e-8 >= (chosenUpgrades.size + 1) * 45.0) {
                phase = Phase.CHOOSING; targetX = shipX; pulseRequested = false
            }
            if (stage == null && !survivalMission && time + 1e-8 >= GameConfig.SURVIVAL_TARGET) { survivalMission = true; mission(GameConfig.SURVIVAL_BONUS) }
            if (stage == null && !nearMission && nearMisses >= GameConfig.NEAR_TARGET) { nearMission = true; mission(GameConfig.NEAR_BONUS) }
            if (stage == null && !itemMission && collected >= GameConfig.ITEM_TARGET) { itemMission = true; mission(GameConfig.ITEM_BONUS) }
        }
    }
    private fun mission(points: Long) { bonusScore += points; events.add(GameEvent.MILESTONE); notify(Notice.MILESTONE, points) }
    private fun burst(x: Float, y: Float, kind: Int) {
        hitSerial++
        val count = if (reducedMotion) 4 else 18
        repeat(count) { index -> if (particles.size < GameConfig.MAX_PARTICLES) {
            val angle = (index * 2 * PI / count + hitSerial).toFloat()
            particles.add(Particle(x, y, cos(angle) * 90, sin(angle) * 90, .55f, kind))
        } }
    }
}

