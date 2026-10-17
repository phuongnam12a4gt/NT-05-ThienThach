package com.example.nt_05_thienthach.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.example.nt_05_thienthach.*
import com.example.nt_05_thienthach.R
import com.example.nt_05_thienthach.data.*
import com.example.nt_05_thienthach.game.*

@Composable fun MeteorDashApp(vm: GameViewModel) {
    var showLanguages by remember { mutableStateOf(false) }
    val openLanguages = { showLanguages = true }

    MaterialTheme(colorScheme = darkColorScheme(primary = Cyan, onPrimary = Space, background = Space,
        surface = Panel, onSurface = Ink, secondary = Orange, onBackground = Ink)) {
        BackHandler(enabled = vm.screen != Screen.HOME) { vm.back() }
        Box(Modifier.fillMaxSize().background(Space)) {
            Canvas(Modifier.matchParentSize()) { stars(size.width, size.height) }
            Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                if (!vm.loaded) Loading(vm)
                else when (vm.screen) {
                    Screen.HOME -> Home(vm, openLanguages)
                    Screen.GAME -> Gameplay(vm)
                    Screen.RESULT -> Results(vm)
                    Screen.UPGRADES -> Upgrades(vm)
                    Screen.SETTINGS -> SettingsScreen(vm, openLanguages)
                    Screen.HELP -> Help(vm)
                    Screen.CAMPAIGN -> CampaignMap(vm)
                    Screen.PREPARE -> StagePreparation(vm)
                    Screen.FLEET -> Fleet(vm)
                }
            }
        }
        if (showLanguages) LanguagePicker { showLanguages = false }
        if (vm.confirmAbandon != null) AlertDialog(
            onDismissRequest = { vm.confirmAbandon = null },
            title = { Text(stringResource(R.string.abandon_title)) }, text = { Text(stringResource(R.string.abandon_body)) },
            confirmButton = { TextButton(onClick = { vm.confirmAbandon?.let(vm::abandon) }) { Text(stringResource(R.string.abandon)) } },
            dismissButton = { TextButton(onClick = { vm.confirmAbandon = null }) { Text(stringResource(R.string.keep_playing)) } })
        if (vm.confirmReset) AlertDialog(onDismissRequest = { vm.confirmReset = false },
            title = { Text(stringResource(R.string.reset_title)) }, text = { Text(stringResource(R.string.reset_body)) },
            confirmButton = { TextButton(onClick = vm::reset) { Text(stringResource(R.string.reset)) } },
            dismissButton = { TextButton(onClick = { vm.confirmReset = false }) { Text(stringResource(R.string.cancel)) } })
        if (vm.resetNotice) AlertDialog(onDismissRequest = { vm.resetNotice = false },
            title = { Text(stringResource(R.string.recovered_title)) }, text = { Text(stringResource(R.string.recovered_body)) },
            confirmButton = { TextButton(onClick = { vm.resetNotice = false }) { Text(stringResource(R.string.understood)) } })
        if (vm.error) AlertDialog(onDismissRequest = vm::clearError,
            title = { Text(stringResource(R.string.error_title)) }, text = { Text(stringResource(R.string.error_body)) },
            confirmButton = { TextButton(onClick = vm::retrySave, enabled = !vm.busy) { Text(stringResource(R.string.retry)) } },
            dismissButton = { if (vm.loaded && (vm.screen != Screen.RESULT || vm.rewardSaved)) TextButton(onClick = vm::clearError) { Text(stringResource(R.string.close)) } })
    }
}
@Composable private fun Loading(vm: GameViewModel) {
    Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(color = Cyan)
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.loading), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.loading_hint), color = Muted, modifier = Modifier.padding(top = 12.dp))
        if (vm.error) TextButton(onClick = vm::reload) { Text(stringResource(R.string.retry)) }
    }
}
@Composable internal fun Eyebrow(id: Int) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Text(stringResource(id), color = Cyan, fontSize = 10.sp, letterSpacing = if (rtl) 0.sp else 2.sp, fontWeight = FontWeight.Bold)
}
@Composable internal fun BigTitle(id: Int) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Text(stringResource(id), modifier = Modifier.fillMaxWidth(), fontSize = 38.sp,
        lineHeight = if (rtl) 50.sp else 42.sp, fontWeight = FontWeight.Black,
        letterSpacing = if (rtl) 0.sp else (-1).sp, color = Ink)
}
@Composable internal fun Action(id: Int, onClick: () -> Unit, enabled: Boolean = true, primary: Boolean = true) {
    if (primary) Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        shape = RoundedCornerShape(16.dp), contentPadding = PaddingValues(16.dp)) { Text(stringResource(id), fontWeight = FontWeight.Bold, fontSize = 14.sp) }
    else OutlinedButton(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        border = BorderStroke(1.dp, Muted.copy(alpha = .23f)), shape = RoundedCornerShape(16.dp), contentPadding = PaddingValues(14.dp)) {
        Text(stringResource(id), color = if (enabled) Ink else Muted, fontWeight = FontWeight.Medium)
    }
}
@Composable internal fun Sheet(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Panel.copy(alpha = .88f))
        .border(1.dp, Muted.copy(alpha = .12f), RoundedCornerShape(22.dp)).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
}
@Composable internal fun MenuPage(vm: GameViewModel, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        TextButton(onClick = vm::back, contentPadding = PaddingValues(0.dp), modifier = Modifier.heightIn(min = 48.dp)) {
            Text((if (LocalLayoutDirection.current == LayoutDirection.Rtl) "→  " else "←  ") + stringResource(R.string.back), color = Muted)
        }
        content()
    }
}
@Composable private fun Home(vm: GameViewModel, openLanguages: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.brand), fontSize = 12.sp, letterSpacing = 3.sp, fontWeight = FontWeight.Bold, color = Ink)
            TextButton(onClick = openLanguages, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("🌐 " + stringResource(R.string.language), color = Cyan, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(4.dp))
        Eyebrow(R.string.update_badge)
        BigTitle(R.string.title)
        Text(stringResource(R.string.new_tagline), color = Muted, fontSize = 14.sp, lineHeight = 22.sp)
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Brush.verticalGradient(listOf(Color(0xFF10253A), Panel.copy(alpha = .6f))))
            .border(1.dp, Cyan.copy(alpha = .15f), RoundedCornerShape(24.dp)).padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.survival_mode), fontSize = 10.sp, color = Cyan, letterSpacing = 1.sp)
                Text("01", fontSize = 10.sp, color = Muted)
            }
            ShipPortrait(Modifier.fillMaxWidth().height(150.dp).clearAndSetSemantics { }, vm.selectedShip)
            Text(vm.selectedShip.name, color = Ink, fontSize = 11.sp, letterSpacing = 1.sp)
            Text(stringResource(R.string.ship_ready), color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
        }
        Action(R.string.campaign_continue, { vm.prepareStage(CampaignCatalog.unlocked(vm.saved.progress.stages)) }, enabled = !vm.busy)
        Action(R.string.campaign, { vm.navigate(Screen.CAMPAIGN) }, primary = false)
        Action(R.string.survival_mode, vm::startEndless, enabled = !vm.busy, primary = false)
        Action(R.string.fleet, vm::openFleet, primary = false)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Stat(stringResource(R.string.best), vm.saved.progress.bestScore.toString(), Modifier.weight(1f), Cyan)
            Stat(stringResource(R.string.wallet), vm.saved.progress.credits.toString(), Modifier.weight(1f), Orange)
        }
        Sheet { Eyebrow(R.string.challenge_title); Text(stringResource(R.string.challenge_body), color = Muted, fontSize = 12.sp, lineHeight = 20.sp) }
        Action(R.string.upgrades, { vm.navigate(Screen.UPGRADES) }, primary = false)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            TextButton(onClick = { vm.navigate(Screen.SETTINGS) }) { Text(stringResource(R.string.settings), color = Muted) }
            TextButton(onClick = { vm.navigate(Screen.HELP) }) { Text(stringResource(R.string.help), color = Muted) }
        }
        Text(stringResource(R.string.offline), color = Muted.copy(alpha = .6f), fontSize = 9.sp, letterSpacing = 1.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}
@Composable internal fun Stat(label: String, value: String, modifier: Modifier = Modifier, color: Color = Cyan) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(Panel.copy(alpha = .8f)).padding(16.dp)) {
        Text(label, color = Muted, fontSize = 10.sp, letterSpacing = 1.sp)
        Text(value, color = color, fontSize = 25.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
    }
}
@Composable private fun Gameplay(vm: GameViewModel) {
    LaunchedEffect(vm) {
        var previous = 0L
        while (true) withFrameNanos { now ->
            if (previous != 0L) vm.tick((now - previous) / 1_000_000_000.0)
            previous = now
        }
    }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                val boardWidth = minOf(maxWidth, maxHeight * .45f)
                Box(Modifier.width(boardWidth).height(boardWidth * (800f / 360f))) {
                    GameCanvas(vm, Modifier.fillMaxSize())
                    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(stringResource(R.string.armor), color = Muted, fontSize = 9.sp)
                                Text(stringResource(R.string.armor_value, vm.hud.armor, vm.hud.maxArmor),
                                    color = if (vm.hud.armor == 1) Orange else Cyan, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(stringResource(R.string.score), color = Muted, fontSize = 9.sp)
                                Text(vm.hud.score.toString(), color = Ink, fontSize = 26.sp, fontWeight = FontWeight.Black)
                            }
                            OutlinedButton(onClick = vm::pause, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                                contentPadding = PaddingValues(8.dp), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, Muted.copy(alpha = .3f))) {
                                Text(stringResource(R.string.pause), color = Ink, fontSize = 10.sp)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (vm.hud.shield > 0) Text(stringResource(R.string.shield_timer, vm.hud.shield), color = Cyan, fontSize = 10.sp)
                            if (vm.hud.slow > 0) Text(stringResource(R.string.slow_timer, vm.hud.slow), color = Color(0xFFBDA4FF), fontSize = 10.sp)
                        }
                        if (vm.campaignHud.id > 0) CampaignObjectiveHud(vm) else FlightChallenge(vm)
                        if (vm.hud.flightUpgrades.isNotEmpty()) Text(flightUpgradeNames(vm.hud.flightUpgrades), color = Cyan, fontSize = 10.sp)
                    }
                    FlightNotice(vm, Modifier.align(Alignment.TopCenter).padding(top = boardWidth * .75f, start = 8.dp, end = 8.dp))
                    Text(stringResource(R.string.drag_hint), color = Muted.copy(alpha = .65f), fontSize = 9.sp,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp))
                    if (vm.hud.phase == Phase.READY) Column(Modifier.align(Alignment.Center).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("\u2194", color = Cyan, fontSize = 52.sp, modifier = Modifier.clearAndSetSemantics { })
                        Text(stringResource(R.string.tutorial_title), color = Ink, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.tutorial_body), color = Muted, fontSize = 13.sp, lineHeight = 21.sp)
                    }
                    if (vm.hud.phase == Phase.COUNTDOWN) Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.get_ready), color = Cyan, fontSize = 10.sp, letterSpacing = 2.sp)
                        Text(vm.hud.countdown.toString(), fontSize = 90.sp, fontWeight = FontWeight.Black, color = Ink)
                    }
                }
            }
            Column(Modifier.fillMaxWidth().background(Panel.copy(alpha = .6f)).padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Button(onClick = vm::pulse, enabled = vm.hud.phase == Phase.RUNNING && vm.hud.energy >= 100,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = RoundedCornerShape(16.dp)) {
                    Text(if (vm.hud.energy >= 100) stringResource(R.string.pulse_ready) else stringResource(R.string.pulse_charging, vm.hud.energy),
                        fontSize = 12.sp, fontWeight = FontWeight.Black)
                }
                LinearProgressIndicator(progress = { vm.hud.energy / 100f }, modifier = Modifier.fillMaxWidth().height(3.dp), color = Cyan, trackColor = Panel)
                Text(stringResource(R.string.pulse_hint), fontSize = 9.sp, color = Muted)
                if (vm.hud.guideStep >= 0 && vm.hud.phase == Phase.RUNNING) {
                    Text(stringResource(when (vm.hud.guideStep) {
                        1 -> R.string.guide_pickup; 2 -> R.string.guide_pulse; 3 -> R.string.guide_near; else -> R.string.tutorial_body
                    }), color = Cyan, fontSize = 12.sp)
                    TextButton(onClick = vm::finishGuide, enabled = !vm.busy) { Text(stringResource(R.string.guide_skip)) }
                }
            }
        }
        if (vm.hud.phase == Phase.CHOOSING) Column(
            Modifier.fillMaxSize().background(Space).verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Eyebrow(R.string.flight_upgrade_title)
            Text(stringResource(R.string.flight_upgrade_body), color = Muted, lineHeight = 22.sp)
            (vm.engine?.upgradeOptions ?: emptyList()).forEach { choice ->
                Sheet {
                    Text(flightUpgradeName(choice), color = Cyan, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(when (choice) {
                        FlightUpgrade.CAPACITOR -> R.string.capacitor_body
                        FlightUpgrade.DAREDEVIL -> R.string.daredevil_body
                        FlightUpgrade.FOCUS -> R.string.focus_body
                    }), color = Ink, lineHeight = 22.sp)
                    Button(onClick = { vm.chooseUpgrade(choice) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.flight_upgrade_choose))
                    }
                }
            }
            TextButton(onClick = vm::pause) { Text(stringResource(R.string.pause)) }
        }
        if (vm.hud.phase == Phase.PAUSED) Column(Modifier.fillMaxSize().background(Space.copy(alpha = .96f))
            .verticalScroll(rememberScrollState()).padding(28.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
            Spacer(Modifier.height(42.dp)); Eyebrow(R.string.pause); BigTitle(R.string.paused_title)
            Text(stringResource(R.string.paused_body), color = Muted, lineHeight = 24.sp)
            Sheet { Text(vm.hud.score.toString(), color = Cyan, fontSize = 42.sp, fontWeight = FontWeight.Bold); Eyebrow(R.string.score) }
            Action(R.string.continue_play, vm::resume)
            Action(R.string.play_again, { vm.confirmAbandon = Screen.GAME }, primary = false)
            Action(R.string.home, { vm.confirmAbandon = Screen.HOME }, primary = false)
        }
    }
}
@Composable private fun Results(vm: GameViewModel) {
    val result = vm.result ?: return
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Spacer(Modifier.height(16.dp)); Eyebrow(R.string.mission_end)
        if (result.stageId != null) {
            Text(stringResource(if(result.victory) R.string.stage_victory else R.string.stage_defeat)+" ${result.stageId}", color = if(result.victory) Cyan else Orange, fontSize=28.sp, fontWeight=FontWeight.Black)
            Text("★".repeat(Integer.bitCount(result.achievementBits))+"☆".repeat(3-Integer.bitCount(result.achievementBits)),color=Orange,fontSize=30.sp)
            if (!result.victory) Text(stringResource(when(result.defeat) { DefeatReason.ALLY->R.string.defeat_ally;DefeatReason.STATION->R.string.defeat_station;DefeatReason.TIMEOUT->R.string.defeat_timeout;else->R.string.defeat_armor }),color=Orange)
            if(result.practice) Text(stringResource(R.string.practice_active),color=Orange)
        } else BigTitle(R.string.result_title)
        if (vm.newRecord) Text(stringResource(R.string.new_record), color = Orange, fontSize = 12.sp, letterSpacing = 2.sp)
        Sheet {
            Eyebrow(R.string.final_score)
            Text(result.score.toString(), fontSize = 66.sp, fontWeight = FontWeight.Black, color = Cyan)
            HorizontalDivider(color = Muted.copy(alpha = .15f))
            Detail(R.string.best, vm.saved.progress.bestScore.toString())
            val seconds = result.survivalMs / 1000
            Detail(R.string.survival, stringResource(R.string.time_format, seconds / 60, seconds % 60))
            Detail(R.string.collected, result.collectedItems.toString())
            Detail(R.string.stat_near, result.nearMisses.toString(), Cyan)
            Detail(R.string.stat_combo, result.bestCombo.toString(), Cyan)
            Detail(R.string.stat_pulse, result.pulsesUsed.toString())
            Detail(R.string.stat_bonus, "+${result.bonusScore}", Orange)
            Detail(R.string.stat_missions, "${result.missionsCompleted}/3", Orange)
            if (vm.rewardSaved) {
                Detail(R.string.earned, "+${vm.resultRewards.total}", Orange)
                Detail(R.string.wallet, vm.saved.progress.credits.toString(), Orange)
            } else Text(stringResource(if (vm.error) R.string.reward_failed else R.string.reward_pending), color = Orange)
        }
        if (result.stageId != null && vm.rewardSaved && !result.practice) Sheet {
            Detail(R.string.reward_base, "+${vm.resultRewards.base}")
            Detail(R.string.stage_first, "+${vm.resultRewards.first}")
            Detail(R.string.stage_star_reward, "+${vm.resultRewards.stars}")
            Detail(R.string.stage_boss_reward, "+${vm.resultRewards.boss}")
            Detail(R.string.stage_chapter_reward, "+${vm.resultRewards.chapter}")
        }
        if(result.stageId != null && vm.rewardSaved) {
            if(result.victory && !result.practice && result.stageId < 100) Action(R.string.stage_next, { vm.prepareStage(result.stageId+1) }, enabled=!vm.busy)
            if(result.victory && result.stageId==100) Text(stringResource(R.string.campaign_finished),color=Cyan,fontSize=20.sp)
            Action(R.string.stage_map, { vm.navigate(Screen.CAMPAIGN) }, enabled=!vm.busy,primary=false)
            Action(R.string.ship_choose, vm::openFleet, enabled=!vm.busy,primary=false)
        }
        if (!vm.rewardSaved && vm.error) Action(R.string.retry_save, vm::retrySave, enabled = !vm.busy)
        Sheet {
            Eyebrow(R.string.next_flight)
            Text(stringResource(when {
                result.endingEnergy >= 100 -> R.string.tip_energy
                result.survivalMs < 30000 -> R.string.tip_survive
                result.nearMisses < 5 -> R.string.tip_near
                else -> R.string.tip_combo
            }), color = Ink, lineHeight = 22.sp)
            if (result.flightUpgrades.isNotEmpty()) Text(flightUpgradeNames(result.flightUpgrades), color = Cyan)
            if (vm.rewardSaved) {
                val progress = vm.saved.progress
                val next = UpgradeKind.entries.mapNotNull { kind ->
                    Transactions.prices(kind).getOrNull(Transactions.level(progress, kind))?.let { kind to it }
                }.minByOrNull { it.second }
                Text(if (next == null) stringResource(R.string.all_upgraded)
                    else stringResource(if (progress.credits >= next.second) R.string.goal_ready else R.string.goal_remaining,
                        (next.second - progress.credits).coerceAtLeast(0)), color = Orange)
            }
        }
        if (vm.rewardSaved) Action(R.string.upgrades, { vm.navigate(Screen.UPGRADES) }, enabled = !vm.busy, primary = false)
        Action(R.string.play_again, vm::start, enabled = vm.rewardSaved && !vm.busy)
        Action(R.string.home, { vm.navigate(Screen.HOME) }, enabled = vm.rewardSaved && !vm.busy, primary = false)
        Text(stringResource(R.string.result_hint), color = Muted, fontSize = 12.sp)
    }
}
@Composable internal fun Detail(id: Int, value: String, color: Color = Ink) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(id), color = Muted, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}
@Composable private fun Upgrades(vm: GameViewModel) = MenuPage(vm) {
    Eyebrow(R.string.hangar); BigTitle(R.string.upgrades_title)
    Text(stringResource(R.string.upgrades_body), color = Muted, lineHeight = 22.sp)
    Stat(stringResource(R.string.wallet), vm.saved.progress.credits.toString(), color = Orange)
    UpgradeKind.entries.forEach { kind ->
        val p = vm.saved.progress
        val level = Transactions.level(p, kind)
        val prices = Transactions.prices(kind)
        val price = prices.getOrNull(level)
        Sheet {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(when (kind) { UpgradeKind.ARMOR -> "⬡"; UpgradeKind.SHIELD -> "◇"; UpgradeKind.SLOW -> "◷" }, color = Cyan, fontSize = 30.sp, modifier = Modifier.clearAndSetSemantics { })
                Text(stringResource(R.string.level, level, prices.size), color = Muted, fontSize = 12.sp)
            }
            Text(stringResource(when (kind) { UpgradeKind.ARMOR -> R.string.armor_name; UpgradeKind.SHIELD -> R.string.shield_name; UpgradeKind.SLOW -> R.string.slow_name }), fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(when (kind) {
                UpgradeKind.ARMOR -> stringResource(R.string.armor_effect, p.upgrades.maxArmor)
                UpgradeKind.SHIELD -> stringResource(R.string.shield_effect, p.upgrades.shieldSeconds.toInt())
                UpgradeKind.SLOW -> stringResource(R.string.slow_effect, String.format(LocalConfiguration.current.locales[0], "%.1f", p.upgrades.slowSeconds))
            }, color = Muted, fontSize = 13.sp)
            if (price != null) Text(stringResource(when (kind) {
                UpgradeKind.ARMOR -> R.string.armor_next; UpgradeKind.SHIELD -> R.string.shield_next; UpgradeKind.SLOW -> R.string.slow_next
            }), color = Cyan, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                repeat(prices.size) { index -> Box(Modifier.weight(1f).height(4.dp).background(if (index < level) Cyan else Muted.copy(alpha = .15f), RoundedCornerShape(2.dp))) }
            }
            Button(onClick = { vm.buy(kind) }, enabled = !vm.busy && price != null && p.credits >= price,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = RoundedCornerShape(12.dp)) {
                Text(when {
                    vm.busy -> stringResource(R.string.saving)
                    price == null -> stringResource(R.string.max_level)
                    p.credits < price -> stringResource(R.string.not_enough, price)
                    else -> stringResource(R.string.buy, price)
                })
            }
        }
    }
}
@Composable private fun SettingsScreen(vm: GameViewModel, openLanguages: () -> Unit) = MenuPage(vm) {
    Eyebrow(R.string.settings_eyebrow); BigTitle(R.string.settings_title)
    Sheet {
        Text(stringResource(R.string.language_hint), color = Muted)
        OutlinedButton(onClick = openLanguages, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text("🌐 " + stringResource(R.string.language))
        }
    }
    Sheet {
        val s = vm.saved.settings
        Toggle(R.string.music, R.string.music_hint, s.music, !vm.busy) { vm.settings(s.copy(music = it)) }
        Toggle(R.string.sound, R.string.sound_hint, s.sound, !vm.busy) { vm.settings(s.copy(sound = it)) }
        Toggle(R.string.vibration, R.string.vibration_hint, s.vibration, !vm.busy) { vm.settings(s.copy(vibration = it)) }
        Toggle(R.string.reduced_motion, R.string.reduced_motion_hint, s.reducedMotion, !vm.busy) { vm.settings(s.copy(reducedMotion = it)) }
    }
    Sheet {
        Text(stringResource(R.string.sensitivity),color=Ink)
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            listOf("0.75×","1×","1.25×").forEachIndexed { index,label ->
                OutlinedButton(onClick={ vm.settings(vm.saved.settings.copy(sensitivity=index)) },enabled=!vm.busy) {Text((if(vm.saved.settings.sensitivity==index) "✓ " else "")+label)}
            }
        }
    }
    Text(stringResource(R.string.privacy), color = Muted, fontSize = 13.sp, lineHeight = 22.sp)
    OutlinedButton(onClick = { vm.confirmReset = true }, enabled = !vm.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        border = BorderStroke(1.dp, Orange.copy(alpha = .4f)), shape = RoundedCornerShape(16.dp)) { Text(stringResource(R.string.reset), color = Orange) }
}
@Composable private fun Toggle(title: Int, hint: Int, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(title), fontWeight = FontWeight.Bold)
            Text(stringResource(hint), color = Muted, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 5.dp))
        }
        val label = stringResource(title)
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled, modifier = Modifier.semantics { contentDescription = label })
    }
}
@Composable private fun Help(vm: GameViewModel) = MenuPage(vm) {
    Eyebrow(R.string.help_eyebrow); BigTitle(R.string.help_title)
    ShipPortrait(Modifier.fillMaxWidth().height(165.dp).clearAndSetSemantics { })
    listOf(R.string.help_controls to R.string.help_controls_body, R.string.help_survive to R.string.help_survive_body).forEach { (title, body) ->
        Sheet { Eyebrow(title); Text(stringResource(body), color = Muted, fontSize = 14.sp, lineHeight = 24.sp) }
    }
    Sheet {
        Eyebrow(R.string.help_items)
        listOf(R.string.help_shield, R.string.help_repair, R.string.help_slow).forEach { Text(stringResource(it), color = Ink, fontSize = 14.sp, lineHeight = 24.sp) }
    }
    Sheet { Eyebrow(R.string.help_rewards); Text(stringResource(R.string.help_rewards_body), color = Muted, lineHeight = 24.sp) }
    listOf(R.string.new_mechanics to R.string.new_mechanics_body, R.string.pulse_help to R.string.pulse_help_body,
        R.string.wave_help to R.string.wave_help_body, R.string.mission_help to R.string.mission_help_body).forEach { (title, body) ->
        Sheet { Eyebrow(title); Text(stringResource(body), color = Muted, fontSize = 14.sp, lineHeight = 24.sp) }
    }
    Sheet { Eyebrow(R.string.flight_upgrade_title); Text(stringResource(R.string.flight_upgrade_help), color = Muted, lineHeight = 24.sp) }
    Text(stringResource(R.string.help_accessibility), color = Muted, fontSize = 12.sp)
    Action(R.string.guide_replay, vm::startGuided)
    Action(R.string.understood, { vm.navigate(Screen.HOME) }, primary = false)
}



@Composable private fun FlightChallenge(vm: GameViewModel) {
    val h = vm.hud
    val target = when (h.missionIndex) { 0 -> GameConfig.SURVIVAL_TARGET; 1 -> GameConfig.NEAR_TARGET; 2 -> GameConfig.ITEM_TARGET; else -> 3 }
    val id = when (h.missionIndex) { 0 -> R.string.mission_survive; 1 -> R.string.mission_near; 2 -> R.string.mission_items; else -> R.string.mission_all }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Space.copy(alpha = .85f)).padding(9.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(if (h.missionIndex == 3) stringResource(id) else stringResource(id, h.missionProgress), color = Muted, fontSize = 10.sp)
        LinearProgressIndicator(progress = { (h.missionProgress.toFloat() / target).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(2.dp), color = Cyan, trackColor = Panel)
        if (h.combo > 0) Text(stringResource(R.string.combo_status, h.combo, h.multiplier, h.comboSeconds),
            color = Orange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}
@Composable private fun FlightNotice(vm: GameViewModel, modifier: Modifier = Modifier) {
    val h = vm.hud
    if (h.phase != Phase.RUNNING) return
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (h.waveWarning > 0) Text(stringResource(R.string.wave_warning, h.waveWarning), color = Orange,
            fontSize = 13.sp, fontWeight = FontWeight.Black, modifier = Modifier.background(Space.copy(alpha = .9f), RoundedCornerShape(10.dp)).padding(10.dp))
        val id = when (h.notice) {
            Notice.NEAR_MISS -> R.string.notice_near; Notice.PICKUP -> R.string.notice_pickup
            Notice.HIT -> R.string.notice_hit; Notice.SHIELD_BREAK -> R.string.notice_shield
            Notice.PULSE -> R.string.notice_pulse; Notice.WAVE -> R.string.notice_wave
            Notice.MILESTONE -> R.string.notice_mission; Notice.NONE -> null
        }
        if (id != null) Text(when (h.notice) {
            Notice.NEAR_MISS, Notice.PICKUP, Notice.MILESTONE -> stringResource(id, h.noticeValue)
            else -> stringResource(id)
        }, color = if (h.notice == Notice.HIT) Orange else Cyan, fontSize = 14.sp, fontWeight = FontWeight.Black,
            modifier = Modifier.background(Space.copy(alpha = .8f), RoundedCornerShape(10.dp)).padding(10.dp))
    }
}


@Composable private fun flightUpgradeName(choice: FlightUpgrade): String = stringResource(when (choice) {
    FlightUpgrade.CAPACITOR -> R.string.capacitor_name
    FlightUpgrade.DAREDEVIL -> R.string.daredevil_name
    FlightUpgrade.FOCUS -> R.string.focus_name
})
@Composable private fun flightUpgradeNames(choices: List<FlightUpgrade>): String {
    val names = choices.map { flightUpgradeName(it) }
    return names.joinToString(" · ")
}
@Composable private fun LanguagePicker(dismiss: () -> Unit) {
    LocalConfiguration.current // Recompose selection when framework locales change.
    val current = AppCompatDelegate.getApplicationLocales().toLanguageTags()
    AlertDialog(onDismissRequest = dismiss,
        title = { Text("🌐 " + stringResource(R.string.language)) },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 440.dp).verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.language_hint), color = Muted, modifier = Modifier.padding(bottom = 12.dp))
                LanguageRow(stringResource(R.string.language_system), current.isEmpty()) {
                    dismiss(); AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
                }
                AppLanguages.supported.forEach { language ->
                    LanguageRow(language.nativeName + " · " + language.market,
                        AppLanguages.resolve(current)?.tag == language.tag) {
                        dismiss(); AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = dismiss) { Text(stringResource(R.string.close)) } })
}
@Composable private fun LanguageRow(label: String, selected: Boolean, choose: () -> Unit) {
    Row(Modifier.fillMaxWidth().selectable(selected, role = androidx.compose.ui.semantics.Role.RadioButton, onClick = choose)
        .heightIn(min = 52.dp).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = null)
        Text(label, modifier = Modifier.weight(1f).padding(start = 8.dp), fontSize = 14.sp)
    }
}
