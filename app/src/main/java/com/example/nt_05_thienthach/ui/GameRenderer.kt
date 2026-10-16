package com.example.nt_05_thienthach.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import com.example.nt_05_thienthach.GameViewModel
import com.example.nt_05_thienthach.game.*
import kotlin.math.*

val Space = Color(0xFF050B1C)
val Panel = Color(0xFF101C32)
val Cyan = Color(0xFF6DEBDE)
val Muted = Color(0xFF94A9C7)
val Orange = Color(0xFFFFB276)
val Ink = Color(0xFFEAF2FF)

fun DrawScope.stars(width: Float, height: Float, time: Float = 0f) {
    repeat(85) { i ->
        val x = ((i * 173 + 41) % 997) / 997f * width
        val y = ((((i * 239 + 67) % 991) / 991f * height) + time * (8 + i % 3 * 9)) % height
        drawCircle(Color(0xFFA5C2EE).copy(alpha = .12f + (i % 4) * .07f), if (i % 6 == 0) 1.4f else .7f, Offset(x, y))
    }
}
fun DrawScope.ship(x: Float, y: Float, size: Float = 1f, flame: Float = 1f) {
    translate(x, y) { scale(size, Offset.Zero) {
        drawCircle(Cyan.copy(alpha = .055f), 39f)
        val fire = Path().apply { moveTo(-6f, 15f); lineTo(0f, 31 + flame * 8); lineTo(6f, 15f); close() }
        drawPath(fire, Color(0xFF6F80FF))
        drawPath(Path().apply { moveTo(-3f, 15f); lineTo(0f, 27 + flame * 5); lineTo(3f, 15f); close() }, Cyan)
        val wings = Path().apply {
            moveTo(0f, -24f); lineTo(18f, 16f); lineTo(7f, 12f); lineTo(0f, 17f)
            lineTo(-7f, 12f); lineTo(-18f, 16f); close()
        }
        drawPath(wings, Brush.linearGradient(listOf(Color(0xFFAEFFF5), Color(0xFF288CBB)), Offset(-16f, -20f), Offset(18f, 20f)))
        drawPath(wings, Cyan, style = Stroke(1f))
        drawPath(Path().apply { moveTo(0f, -24f); lineTo(0f, 17f); lineTo(-7f, 12f); close() }, Color(0xFF458DAF))
        drawOval(Color(0xFF092B51), Offset(-4f, -12f), Size(8f, 15f))
        drawLine(Color.White.copy(alpha = .8f), Offset(-1f, -10f), Offset(-1f, -3f), 1.5f)
        drawLine(Orange, Offset(-12f, 9f), Offset(-8f, 7f), 2f)
        drawLine(Orange, Offset(8f, 7f), Offset(12f, 9f), 2f)
    } }
}
@Composable fun ShipPortrait(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = min(size.width / 320f, size.height / 210f)
        translate(size.width / 2, size.height / 2) { scale(s, Offset.Zero) {
            drawCircle(Brush.radialGradient(listOf(Color(0xFF173B56), Color.Transparent), Offset.Zero, 140f), 140f)
            drawCircle(Cyan.copy(alpha = .11f), 82f, style = Stroke(1f))
            drawCircle(Cyan.copy(alpha = .07f), 108f, style = Stroke(1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 7f))))
            drawLine(Cyan.copy(alpha = .12f), Offset(-140f, 0f), Offset(140f, 0f), 1f)
            drawLine(Cyan.copy(alpha = .12f), Offset(0f, -100f), Offset(0f, 100f), 1f)
            drawCircle(Cyan, 3f, Offset(82f, 0f))
            drawCircle(Orange, 2f, Offset(-64f, -52f))
            rotate(-14f, Offset.Zero) { ship(0f, -4f, 2.6f, 1f) }
        } }
    }
}
@Composable fun GameCanvas(vm: GameViewModel, modifier: Modifier = Modifier) {
    Canvas(modifier.clipToBounds().pointerInput(vm) {
        awaitEachGesture {
            val first = awaitFirstDown(requireUnconsumed = true)
            val scale = size.width / GameConfig.WIDTH
            vm.down(first.id.value, first.position.x / scale)
            try {
                while (true) {
                    val event = awaitPointerEvent()
                    val pointer = event.changes.firstOrNull { it.id == first.id } ?: break
                    if (!pointer.pressed || pointer.isConsumed) break
                    vm.drag(pointer.id.value, pointer.position.x / scale)
                    pointer.consume()
                }
            } finally { vm.up(first.id.value) }
        }
    }) {
        vm.frame.intValue
        val game = vm.engine ?: return@Canvas
        scale(size.width / 360f, Offset.Zero) {
            drawRect(Brush.verticalGradient(listOf(Space, Color(0xFF091A30), Space), 0f, 800f), size = Size(360f, 800f))
            stars(360f, 800f, if (game.reducedMotion) 0f else game.time.toFloat())
            drawCircle(Color(0xFF13324D).copy(alpha = .35f), 160f, Offset(340f, 360f), style = Stroke(1f))
            drawCircle(Color(0xFF13324D).copy(alpha = .18f), 180f, Offset(340f, 360f), style = Stroke(1f))
            val sectorColors = listOf(Color(0xFF143A56), Color(0xFF30204F), Color(0xFF12443C))
            val nebula = sectorColors[GameConfig.level(game.time) % sectorColors.size]
            drawCircle(Brush.radialGradient(listOf(nebula.copy(alpha = .5f), Color.Transparent), Offset(330f, 310f), 250f), 250f, Offset(330f, 310f))
            if (game.combo >= 3 && !game.reducedMotion) repeat(10) { i ->
                val x = ((i * 101 + 17) % 360).toFloat()
                val y = ((game.time * 160 + i * 91) % 800).toFloat()
                drawLine(Cyan.copy(alpha = .12f), Offset(x, y), Offset(x, y + 22), 1f)
            }
            game.wave?.takeIf { it.warning > 0 }?.let { wave ->
                for (lane in 0..2) {
                    val safe = lane == wave.safeLane
                    val color = if (safe) Cyan else Orange
                    val left = lane * 120f
                    drawRect(color.copy(alpha = if (safe) .05f else .035f), Offset(left + 3, 180f), Size(114f, 540f))
                    drawLine(color.copy(alpha = .45f), Offset(left + 3, 180f), Offset(left + 3, 720f), 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(7f, 6f)))
                    repeat(5) { row ->
                        val y = 300f + row * 76
                        if (safe) {
                            drawLine(Cyan.copy(alpha = .5f), Offset(left + 49, y - 7), Offset(left + 60, y + 4), 2f)
                            drawLine(Cyan.copy(alpha = .5f), Offset(left + 60, y + 4), Offset(left + 71, y - 7), 2f)
                        } else {
                            drawLine(Orange.copy(alpha = .35f), Offset(left + 47, y - 8), Offset(left + 73, y + 8), 2f)
                            drawLine(Orange.copy(alpha = .35f), Offset(left + 73, y - 8), Offset(left + 47, y + 8), 2f)
                        }
                    }
                }
            }
            for (meteor in game.meteors) {
                val r = meteor.kind.radius
                translate(meteor.x, meteor.y) {
                    drawLine(Orange.copy(alpha = .08f), Offset.Zero, Offset(-meteor.drift * .4f, -r * 3), r * 1.2f)
                    rotate(meteor.rotation, Offset.Zero) {
                        val rock = Path()
                        repeat(9) { index ->
                            val angle = index * 2 * PI / 9
                            val radius = r * if ((index + meteor.id) % 3 == 0L) .82f else 1f
                            val x = cos(angle).toFloat() * radius
                            val y = sin(angle).toFloat() * radius
                            if (index == 0) rock.moveTo(x, y) else rock.lineTo(x, y)
                        }
                        rock.close()
                        drawPath(rock, Brush.linearGradient(listOf(Color(0xFFAE978E), Color(0xFF4A4555)), Offset(-r, -r), Offset(r, r)))
                        drawPath(rock, Color(0xFFC3AEA0).copy(alpha = .55f), style = Stroke(1f))
                        drawCircle(Color(0xFF3E3D4B).copy(alpha = .6f), r * .23f, Offset(-r * .22f, -r * .18f))
                        drawCircle(Color(0xFF3E3D4B).copy(alpha = .5f), r * .14f, Offset(r * .35f, r * .24f))
                    }
                }
            }
            for (item in game.items) {
                val color = when (item.kind) { ItemKind.SHIELD -> Cyan; ItemKind.REPAIR -> Orange; ItemKind.SLOW -> Color(0xFFBDA4FF) }
                translate(item.x, item.y) {
                    drawCircle(color.copy(alpha = .1f), 22f)
                    drawCircle(Panel, 14f)
                    drawCircle(color, 14f, style = Stroke(1.5f))
                    when (item.kind) {
                        ItemKind.SHIELD -> drawPath(Path().apply {
                            moveTo(0f, -8f); lineTo(7f, -4f); lineTo(5f, 4f); lineTo(0f, 9f)
                            lineTo(-5f, 4f); lineTo(-7f, -4f); close()
                        }, color, style = Stroke(2f))
                        ItemKind.REPAIR -> { drawLine(color, Offset(-7f, 0f), Offset(7f, 0f), 3f); drawLine(color, Offset(0f, -7f), Offset(0f, 7f), 3f) }
                        ItemKind.SLOW -> {
                            drawCircle(color, 8f, style = Stroke(1.5f))
                            drawLine(color, Offset(0f, -5f), Offset.Zero, 2f)
                            drawLine(color, Offset.Zero, Offset(4f, 2f), 2f)
                        }
                    }
                }
            }
            game.particles.forEach { p -> drawCircle((when (p.kind) { 0 -> Orange; 2 -> Color(0xFFBDA4FF); else -> Cyan }).copy(alpha = (p.life / .55f).coerceIn(0f, 1f)), 2f, Offset(p.x, p.y)) }
            if (game.invulnerability <= 0 || (game.time * 12).toInt() % 2 == 0) {
                ship(game.shipX, GameConfig.SHIP_Y, flame = if (game.reducedMotion) .7f else (.7 + sin(game.time * 22) * .3).toFloat())
            }
            if (game.pulseRemaining > 0) {
                val progress = (1 - game.pulseRemaining / GameConfig.PULSE_SECONDS).toFloat().coerceIn(0f, 1f)
                val radius = if (game.reducedMotion) 72f else 20 + GameConfig.PULSE_RADIUS * progress
                drawCircle(Color(0xFFBDA4FF).copy(alpha = (1 - progress) * .8f), radius,
                    Offset(game.pulseX, GameConfig.SHIP_Y), style = Stroke(if (game.reducedMotion) 2f else 4f * (1 - progress) + 1))
                drawCircle(Cyan.copy(alpha = (1 - progress) * .12f), radius, Offset(game.pulseX, GameConfig.SHIP_Y))
            }
            if (game.shield > 0) {
                drawCircle(Cyan.copy(alpha = .09f), 30f, Offset(game.shipX, 680f))
                drawCircle(Cyan.copy(alpha = .8f), 30f, Offset(game.shipX, 680f), style = Stroke(1.5f))
            }
            if (game.invulnerability > 0) drawCircle(Orange.copy(alpha = .45f), 34f, Offset(game.shipX, 680f), style = Stroke(1f))
        }
    }
}


