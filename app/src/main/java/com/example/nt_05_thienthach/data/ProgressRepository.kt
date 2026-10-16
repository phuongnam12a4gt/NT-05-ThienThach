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
                    val vibration: Boolean = true, val reducedMotion: Boolean = false)
data class Progress(val credits: Long = 0, val bestScore: Long = 0, val totalRuns: Long = 0,
                    val bestSurvivalMs: Long = 0, val upgrades: Upgrades = Upgrades(),
                    val tutorialCompleted: Boolean = false, val lastRewardedRunId: String? = null)
data class SavedData(val progress: Progress = Progress(), val settings: Settings = Settings(), val recovered: Boolean = false)
enum class UpgradeKind { ARMOR, SHIELD, SLOW }

object Transactions {
    fun reward(progress: Progress, result: RunResult): Progress {
        if (progress.lastRewardedRunId == result.runId) return progress
        require(result.score >= 0 && result.survivalMs >= 0)
        return progress.copy(
            credits = Math.addExact(progress.credits, result.earnedCredits),
            bestScore = maxOf(progress.bestScore, result.score),
            totalRuns = Math.addExact(progress.totalRuns, 1),
            bestSurvivalMs = maxOf(progress.bestSurvivalMs, result.survivalMs),
            lastRewardedRunId = result.runId)
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
        check((p[Keys.schema] ?: 1) <= 1) { "future_schema" }
        return SavedData(Progress(
            credits = (p[Keys.credits] ?: 0).coerceAtLeast(0),
            bestScore = (p[Keys.best] ?: 0).coerceAtLeast(0),
            totalRuns = (p[Keys.runs] ?: 0).coerceAtLeast(0),
            bestSurvivalMs = (p[Keys.survival] ?: 0).coerceAtLeast(0),
            upgrades = Upgrades((p[Keys.armor] ?: 0).coerceIn(0, 2),
                (p[Keys.shield] ?: 0).coerceIn(0, 3), (p[Keys.slow] ?: 0).coerceIn(0, 3)),
            tutorialCompleted = p[Keys.tutorial] ?: false,
            lastRewardedRunId = p[Keys.rewarded]),
            Settings(p[Keys.music] ?: true, p[Keys.sound] ?: true,
                p[Keys.vibration] ?: true, p[Keys.reduced] ?: false), Recovery.happened)
    }
    private fun write(p: MutablePreferences, progress: Progress) {
        p[Keys.schema] = 1
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
    suspend fun buy(kind: UpgradeKind): SavedData = decode(store.edit { p -> write(p, Transactions.buy(decode(p).progress, kind)) })
    suspend fun completeTutorial(): SavedData = decode(store.edit { p -> write(p, decode(p).progress.copy(tutorialCompleted = true)) })
    suspend fun settings(settings: Settings): SavedData = decode(store.edit { p ->
        decode(p)
        p[Keys.schema] = 1
        p[Keys.music] = settings.music
        p[Keys.sound] = settings.sound
        p[Keys.vibration] = settings.vibration
        p[Keys.reduced] = settings.reducedMotion
    })
    suspend fun reset(): SavedData = decode(store.edit { p -> decode(p); write(p, Progress()) })
}

