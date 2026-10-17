package com.example.nt_05_thienthach.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import com.example.nt_05_thienthach.game.*
import kotlinx.coroutines.flow.map

private object Recovery { @Volatile var happened = false }
private val Context.gameStore by preferencesDataStore(
    name = "meteor_dash",
    corruptionHandler = ReplaceFileCorruptionHandler { Recovery.happened = true; emptyPreferences() }
)

data class Settings(val music: Boolean = true, val sound: Boolean = true,
                    val vibration: Boolean = true, val reducedMotion: Boolean = false, val sensitivity: Int = 1)
data class Progress(val credits: Long = 0, val bestScore: Long = 0, val totalRuns: Long = 0,
                    val bestSurvivalMs: Long = 0, val upgrades: Upgrades = Upgrades(),
                    val tutorialCompleted: Boolean = false, val lastRewardedRunId: String? = null,
                    val ownedShips: Set<String> = setOf("S01"), val equippedShip: String = "S01",
                    val astra: AstraConfiguration = AstraConfiguration.ARMOR,
                    val stages: Map<Int, StageRecord> = emptyMap(), val rewardedChapters: Set<Int> = emptySet(),
                    val rewardedRuns: Set<String> = emptySet())
data class SavedData(val progress: Progress = Progress(), val settings: Settings = Settings(), val recovered: Boolean = false)
enum class UpgradeKind { ARMOR, SHIELD, SLOW }

object Transactions {
    fun reward(progress: Progress, result: RunResult): Progress {
        if (result.practice || progress.lastRewardedRunId == result.runId || result.runId in progress.rewardedRuns) return progress
        require(result.score >= 0 && result.survivalMs >= 0 && ShipCatalog.find(result.shipId) != null)
        val reward = breakdown(progress, result)
        val records = progress.stages.toMutableMap()
        result.stageId?.let { id ->
            val spec = requireNotNull(CampaignCatalog.find(id))
            require(id <= CampaignCatalog.unlocked(progress.stages)) { "locked_stage" }
            require(result.achievementBits in 0..7 && (result.victory == (result.achievementBits and 1 != 0)))
            val old = records[id] ?: StageRecord()
            records[id] = StageRecord(old.bits or result.achievementBits, maxOf(old.bestScore, result.score),
                if (result.victory && (old.bestTimeMs == 0L || result.survivalMs < old.bestTimeMs)) result.survivalMs else old.bestTimeMs,
                Math.addExact(old.attempts, 1))
        }
        val chapters = progress.rewardedChapters + (1..10).filter { c -> ((c-1)*10+1..c*10).all { records[it]?.cleared == true } }
        return progress.copy(
            credits = Math.addExact(progress.credits, reward.total),
            bestScore = if (result.stageId == null) maxOf(progress.bestScore, result.score) else progress.bestScore,
            totalRuns = Math.addExact(progress.totalRuns, 1),
            bestSurvivalMs = if (result.stageId == null) maxOf(progress.bestSurvivalMs, result.survivalMs) else progress.bestSurvivalMs,
            lastRewardedRunId = result.runId, stages = records, rewardedChapters = chapters,
            rewardedRuns = progress.rewardedRuns + result.runId)
    }
    fun breakdown(progress: Progress, result: RunResult): RewardBreakdown {
        if (result.practice || result.runId == progress.lastRewardedRunId || result.runId in progress.rewardedRuns) return RewardBreakdown()
        val spec = result.stageId?.let { requireNotNull(CampaignCatalog.find(it)) }
        if (spec == null || !result.victory) return RewardBreakdown(base = result.earnedCredits)
        val oldBits = progress.stages[spec.id]?.bits ?: 0
        val fresh = result.achievementBits and oldBits.inv() and 7
        val completeChapter = spec.chapter !in progress.rewardedChapters &&
            ((spec.chapter-1)*10+1..spec.chapter*10).all { it == spec.id || progress.stages[it]?.cleared == true }
        return RewardBreakdown(result.earnedCredits, if (fresh and 1 != 0) spec.firstClearCredits else 0,
            (if (fresh and 2 != 0) 15L else 0L) + (if (fresh and 4 != 0) 20L else 0L),
            if (fresh and 1 != 0) spec.bossCredits else 0, if (completeChapter) 100L + 40 * spec.chapter else 0)
    }
    fun buyShip(progress: Progress, id: String): Progress {
        val ship = requireNotNull(ShipCatalog.find(id))
        check(id !in progress.ownedShips) { "already_owned" }
        check(progress.credits >= ship.price) { "insufficient_credits" }
        return progress.copy(credits = progress.credits - ship.price, ownedShips = progress.ownedShips + id)
    }
    fun equipShip(progress: Progress, id: String): Progress {
        require(ShipCatalog.find(id) != null && id in progress.ownedShips) { "unowned_ship" }
        return progress.copy(equippedShip = id)
    }
    fun level(progress: Progress, kind: UpgradeKind) = when (kind) {
        UpgradeKind.ARMOR -> progress.upgrades.armor
        UpgradeKind.SHIELD -> progress.upgrades.shield
        UpgradeKind.SLOW -> progress.upgrades.slow
    }
    fun prices(kind: UpgradeKind) = if (kind == UpgradeKind.ARMOR) GameConfig.armorPrices else GameConfig.effectPrices
    fun buy(progress: Progress, kind: UpgradeKind): Progress {
        val level = level(progress, kind)
        val price = prices(kind).getOrNull(level) ?: error("max_level")
        check(progress.credits >= price) { "insufficient_credits" }
        val upgrades = when (kind) {
            UpgradeKind.ARMOR -> progress.upgrades.copy(armor = level + 1)
            UpgradeKind.SHIELD -> progress.upgrades.copy(shield = level + 1)
            UpgradeKind.SLOW -> progress.upgrades.copy(slow = level + 1)
        }
        return progress.copy(credits = progress.credits - price, upgrades = upgrades)
    }
}

class ProgressRepository internal constructor(private val store: androidx.datastore.core.DataStore<Preferences>) {
    constructor(context: Context) : this(context.gameStore)
    private object Keys {
        val schema = intPreferencesKey("schemaVersion")
        val owned = stringSetPreferencesKey("ownedShips")
        val equipped = stringPreferencesKey("equippedShip")
        val astra = stringPreferencesKey("astraConfiguration")
        val chapters = stringSetPreferencesKey("rewardedChapters")
        val rewardedRuns = stringSetPreferencesKey("rewardedRuns")
        val sensitivity = intPreferencesKey("dragSensitivity")
        val credits = longPreferencesKey("credits")
        val best = longPreferencesKey("bestScore")
        val runs = longPreferencesKey("totalRuns")
        val survival = longPreferencesKey("bestSurvivalMs")
        val armor = intPreferencesKey("armorUpgradeLevel")
        val shield = intPreferencesKey("shieldUpgradeLevel")
        val slow = intPreferencesKey("slowUpgradeLevel")
        val tutorial = booleanPreferencesKey("tutorialCompleted")
        val rewarded = stringPreferencesKey("lastRewardedRunId")
        val music = booleanPreferencesKey("musicEnabled")
        val sound = booleanPreferencesKey("soundEnabled")
        val vibration = booleanPreferencesKey("vibrationEnabled")
        val reduced = booleanPreferencesKey("reducedMotionEnabled")
    }
    val data = store.data.map(::decode)
    private fun decode(p: Preferences): SavedData {
        check((p[Keys.schema] ?: 1) <= 2) { "future_schema" }
        val owned = ((p[Keys.owned] ?: emptySet()).filter { ShipCatalog.find(it) != null } + "S01").toSet()
        val equipped = (p[Keys.equipped] ?: "S01").takeIf { it in owned } ?: "S01"
        val records = CampaignCatalog.all.mapNotNull { stage ->
            val values = p[stringPreferencesKey("campaign_${stage.id}")]?.split(":") ?: return@mapNotNull null
            val bits = (values.getOrNull(0)?.toIntOrNull() ?: 0).coerceIn(0,7).let { if (it and 1 == 0) 0 else it }
            stage.id to StageRecord(bits, (values.getOrNull(1)?.toLongOrNull() ?: 0).coerceAtLeast(0),
                (values.getOrNull(2)?.toLongOrNull() ?: 0).coerceAtLeast(0), (values.getOrNull(3)?.toLongOrNull() ?: 0).coerceAtLeast(0))
        }.toMap()
        return SavedData(Progress(
            credits = (p[Keys.credits] ?: 0).coerceAtLeast(0),
            bestScore = (p[Keys.best] ?: 0).coerceAtLeast(0),
            totalRuns = (p[Keys.runs] ?: 0).coerceAtLeast(0),
            bestSurvivalMs = (p[Keys.survival] ?: 0).coerceAtLeast(0),
            upgrades = Upgrades((p[Keys.armor] ?: 0).coerceIn(0, 2),
                (p[Keys.shield] ?: 0).coerceIn(0, 3), (p[Keys.slow] ?: 0).coerceIn(0, 3)),
            tutorialCompleted = p[Keys.tutorial] ?: false,
            lastRewardedRunId = p[Keys.rewarded], ownedShips = owned, equippedShip = equipped,
            astra = runCatching { AstraConfiguration.valueOf(p[Keys.astra] ?: "ARMOR") }.getOrDefault(AstraConfiguration.ARMOR),
            stages = records, rewardedChapters = (p[Keys.chapters] ?: emptySet()).mapNotNull { it.toIntOrNull()?.takeIf { n -> n in 1..10 } }.toSet(),
            rewardedRuns = (p[Keys.rewardedRuns] ?: emptySet()) + listOfNotNull(p[Keys.rewarded])),
            Settings(p[Keys.music] ?: true, p[Keys.sound] ?: true,
                p[Keys.vibration] ?: true, p[Keys.reduced] ?: false, (p[Keys.sensitivity] ?: 1).coerceIn(0,2)), Recovery.happened)
    }
    private fun write(p: MutablePreferences, progress: Progress) {
        p[Keys.schema] = 2
        p[Keys.owned] = progress.ownedShips
        p[Keys.equipped] = progress.equippedShip
        p[Keys.astra] = progress.astra.name
        p[Keys.chapters] = progress.rewardedChapters.map { it.toString() }.toSet()
        p[Keys.rewardedRuns] = progress.rewardedRuns
        CampaignCatalog.all.forEach { stage ->
            val key = stringPreferencesKey("campaign_${stage.id}")
            progress.stages[stage.id]?.let { p[key] = "${it.bits}:${it.bestScore}:${it.bestTimeMs}:${it.attempts}" } ?: p.remove(key)
        }
        p[Keys.credits] = progress.credits
        p[Keys.best] = progress.bestScore
        p[Keys.runs] = progress.totalRuns
        p[Keys.survival] = progress.bestSurvivalMs
        p[Keys.armor] = progress.upgrades.armor
        p[Keys.shield] = progress.upgrades.shield
        p[Keys.slow] = progress.upgrades.slow
        p[Keys.tutorial] = progress.tutorialCompleted
        progress.lastRewardedRunId?.let { p[Keys.rewarded] = it } ?: p.remove(Keys.rewarded)
    }
    suspend fun reward(result: RunResult): SavedData = decode(store.edit { p -> write(p, Transactions.reward(decode(p).progress, result)) })
    suspend fun buyShip(id: String): SavedData = decode(store.edit { p -> write(p, Transactions.buyShip(decode(p).progress, id)) })
    suspend fun equipShip(id: String): SavedData = decode(store.edit { p -> write(p, Transactions.equipShip(decode(p).progress, id)) })
    suspend fun configureAstra(value: AstraConfiguration): SavedData = decode(store.edit { p -> write(p, decode(p).progress.copy(astra = value)) })
    suspend fun buy(kind: UpgradeKind): SavedData = decode(store.edit { p -> write(p, Transactions.buy(decode(p).progress, kind)) })
    suspend fun completeTutorial(): SavedData = decode(store.edit { p -> write(p, decode(p).progress.copy(tutorialCompleted = true)) })
    suspend fun settings(settings: Settings): SavedData = decode(store.edit { p ->
        decode(p)
        p[Keys.schema] = 2
        p[Keys.music] = settings.music
        p[Keys.sound] = settings.sound
        p[Keys.vibration] = settings.vibration
        p[Keys.reduced] = settings.reducedMotion
        p[Keys.sensitivity] = settings.sensitivity.coerceIn(0,2)
    })
    suspend fun reset(): SavedData = decode(store.edit { p -> decode(p); write(p, Progress()) })
}

