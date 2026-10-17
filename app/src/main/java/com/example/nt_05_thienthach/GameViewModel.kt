package com.example.nt_05_thienthach

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nt_05_thienthach.audio.AudioController
import com.example.nt_05_thienthach.data.*
import com.example.nt_05_thienthach.game.*
import kotlinx.coroutines.*
import kotlin.math.ceil

enum class Screen { HOME, GAME, RESULT, UPGRADES, SETTINGS, HELP }
data class Hud(val phase: Phase = Phase.READY, val score: Long = 0, val armor: Int = 3,
    val maxArmor: Int = 3, val shield: Int = 0, val slow: Int = 0, val countdown: Int = 3, val level: Int = 1, val combo: Int = 0, val multiplier: Int = 1, val energy: Int = 100,
    val comboSeconds: Int = 0, val notice: Notice = Notice.NONE, val noticeValue: Long = 0,
    val waveWarning: Int = 0, val safeLane: Int = -1, val missionIndex: Int = 0,
    val missionProgress: Int = 0, val missionsDone: Int = 0,
    val flightUpgrades: List<FlightUpgrade> = emptyList(), val guideStep: Int = -1)

class GameViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = ProgressRepository(app)
    var saved by mutableStateOf(SavedData())
        private set
    var loaded by mutableStateOf(false)
        private set
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf(false)
        private set
    var screen by mutableStateOf(Screen.HOME)
        private set
    var engine: GameEngine? = null
        private set
    var hud by mutableStateOf(Hud())
        private set
    // Only Canvas reads this state, so gameplay does not recompose the entire UI each frame.
    val frame = mutableIntStateOf(0)
    var result by mutableStateOf<RunResult?>(null)
        private set
    var rewardSaved by mutableStateOf(false)
        private set
    var newRecord by mutableStateOf(false)
        private set
    var confirmAbandon by mutableStateOf<Screen?>(null)
    var confirmReset by mutableStateOf(false)
    var resetNotice by mutableStateOf(false)
    private var loadJob: Job? = null
    private var retry: (() -> Unit)? = null
    private var foreground = false
    private var audioReady = false
    private var recoverySeen = false
    private val audio = AudioController(app) { pause() }
    private var pointerId: Long? = null
    private var pointerStart = 0f
    private var shipStart = 180f
    init {
        reload()
        viewModelScope.launch {
            withContext(Dispatchers.Default) { runCatching { audio.prepare() } }
            audioReady = true
            syncAudio()
        }
    }
    fun reload() {
        error = false
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                repository.data.collect { data ->
                    saved = data; loaded = true
                    if (data.recovered && !recoverySeen) { recoverySeen = true; resetNotice = true }
                    syncAudio()
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { error = true; retry = { reload() } }
        }
    }
    private fun write(action: suspend () -> SavedData, after: () -> Unit = {}) {
        if (busy) return
        busy = true; error = false
        val again = { write(action, after) }
        viewModelScope.launch {
            try { saved = action(); after(); retry = null; syncAudio() }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { error = true; retry = again }
            finally {
                busy = false
                if (screen == Screen.RESULT && !rewardSaved && !error) saveReward()
            }
        }
    }
    fun retrySave() { retry?.invoke() }
    fun clearError() { if (loaded && !(screen == Screen.RESULT && !rewardSaved)) error = false }
    fun navigate(destination: Screen) {
        if (!loaded || busy || (screen == Screen.RESULT && !rewardSaved)) return
        clearPointer()
        if (destination != Screen.GAME) engine = null
        screen = destination
        syncAudio()
    }
    private var guideActive = false
    private var nextRunGuided = false
    fun startGuided() { nextRunGuided = true; start() }
    fun start() {
        if (screen == Screen.GAME && engine != null) return
        if (!loaded || busy || (screen == Screen.RESULT && !rewardSaved)) return
        clearPointer(); result = null; rewardSaved = false; newRecord = false
        guideActive = nextRunGuided || !saved.progress.tutorialCompleted
        nextRunGuided = false
        engine = GameEngine(saved.progress.upgrades, tutorial = guideActive)
        engine?.reducedMotion = saved.settings.reducedMotion
        screen = Screen.GAME
        refreshHud(); syncAudio()
    }
    fun tick(delta: Double) {
        val game = engine ?: return
        if (screen != Screen.GAME || !foreground || game.phase == Phase.PAUSED || game.phase == Phase.READY) return
        game.advance(delta)
        game.events.forEach(audio::event)
        game.events.clear()
        frame.intValue++
        if (game.phase == Phase.CHOOSING) clearPointer()
        if (guideActive && !error && game.collected > 0 && game.pulsesUsed > 0 && game.nearMisses > 0) finishGuide()
        refreshHud()
        if (game.phase == Phase.GAME_OVER && result == null) {
            result = game.result
            newRecord = game.score > saved.progress.bestScore
            screen = Screen.RESULT
            clearPointer()
            saveReward()
        }
        syncAudio()
    }
    private fun saveReward() {
        val frozen = result ?: return
        write({ repository.reward(frozen) }) { rewardSaved = true }
    }
    fun down(id: Long, x: Float) {
        val game = engine ?: return
        if (pointerId != null || (game.phase != Phase.RUNNING && game.phase != Phase.READY)) return
        pointerId = id; pointerStart = x; shipStart = game.shipX
    }
    fun drag(id: Long, x: Float) {
        val game = engine ?: return
        if (pointerId != id) return
        if (game.phase == Phase.READY && kotlin.math.abs(x - pointerStart) > 4) {
            game.startTutorial(); refreshHud(); syncAudio()

        }
        if (game.phase == Phase.RUNNING) game.targetX = (shipStart + x - pointerStart).coerceIn(20f, 340f)
    }
    fun finishGuide() {
        if (busy) return
        write({ repository.completeTutorial() }) { guideActive = false; refreshHud() }
    }
    fun chooseUpgrade(choice: FlightUpgrade) {
        if (!foreground) return
        if (engine?.chooseUpgrade(choice) == true) { clearPointer(); refreshHud(); syncAudio() }
    }
    fun pulse() { engine?.requestPulse() }
    fun up(id: Long) { if (pointerId == id) clearPointer() }
    fun clearPointer() { pointerId = null }
    fun pause() { engine?.pause(); clearPointer(); refreshHud(); audio.stop(); syncAudio() }
    fun resume() { engine?.resume(); clearPointer(); refreshHud(); syncAudio() }
    fun setForeground(active: Boolean) { foreground = active; if (!active) pause() else syncAudio() }
    private fun syncAudio() {
        if (audioReady) audio.update(foreground && screen == Screen.GAME && engine?.phase == Phase.RUNNING, saved.settings)
    }
    private fun refreshHud() {
        val game = engine ?: return
        hud = Hud(game.phase, game.score, game.armor, game.maxArmor,
            ceil(game.shield).toInt(), ceil(game.slow).toInt(), ceil(game.countdown).toInt(), GameConfig.level(game.time) + 1,
            game.combo, game.multiplier, game.energy.toInt(), ceil(game.comboRemaining).toInt(), game.notice, game.noticeValue,
            ceil(game.wave?.warning ?: 0.0).toInt(), game.wave?.safeLane ?: -1,
            when { !game.survivalMission -> 0; !game.nearMission -> 1; !game.itemMission -> 2; else -> 3 },
            when { !game.survivalMission -> game.time.toInt(); !game.nearMission -> game.nearMisses; !game.itemMission -> game.collected; else -> 3 },
            game.missionsCompleted, game.flightUpgrades,
            if (!guideActive) -1 else when {
                game.phase == Phase.READY -> 0
                game.collected == 0 -> 1
                game.pulsesUsed == 0 -> 2
                game.nearMisses == 0 -> 3
                else -> -1
            })
    }
    fun abandon(destination: Screen) {
        confirmAbandon = null
        engine = null
        if (destination == Screen.GAME) start() else navigate(destination)
    }
    fun buy(kind: UpgradeKind) { write({ repository.buy(kind) }) { if (foreground) audio.purchase() } }
    fun settings(value: Settings) { write({ repository.settings(value) }) { engine?.reducedMotion = value.reducedMotion } }
    fun reset() { confirmReset = false; write({ repository.reset() }) { navigateAfterReset() } }
    private fun navigateAfterReset() { engine = null; screen = Screen.HOME }
    fun back() {
        if (confirmAbandon != null) { confirmAbandon = null; return }
        if (confirmReset) { confirmReset = false; return }
        when (screen) {
            Screen.GAME -> if (hud.phase == Phase.PAUSED) resume() else pause()
            Screen.RESULT -> if (rewardSaved && !busy) navigate(Screen.HOME)
            else -> navigate(Screen.HOME)
        }
    }
    override fun onCleared() { audio.release(); super.onCleared() }
}



