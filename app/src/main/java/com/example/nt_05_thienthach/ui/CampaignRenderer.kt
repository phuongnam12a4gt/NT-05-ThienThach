package com.example.nt_05_thienthach.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import com.example.nt_05_thienthach.game.*
import kotlin.math.*

fun DrawScope.vessel(ship: ShipDefinition, x: Float, y: Float, zoom: Float = 1f, flame: Float = 1f) {
    if(ship.id=="S01") {this.ship(x,y,zoom,flame);return}
    val color=Color(ship.color)
    translate(x,y) {scale(zoom,Offset.Zero) {
        drawCircle(color.copy(alpha=.08f),36f)
        fun polygon(vararg coords: Float) {
            val p=Path();p.moveTo(coords[0],coords[1]);for(i in 2 until coords.size step 2)p.lineTo(coords[i],coords[i+1]);p.close()
            drawPath(p,Brush.linearGradient(listOf(color,Color(0xFF254967)),Offset(-20f,-25f),Offset(20f,20f)))
            drawPath(p,color,style=Stroke(1f))
        }
        val dual=ship.id in listOf("S04","S05","S10")
        for(engineX in if(dual)listOf(-11f,11f) else listOf(0f)) {
            drawPath(Path().apply {moveTo(engineX-4,14f);lineTo(engineX,28+flame*7);lineTo(engineX+4,14f);close()},color.copy(alpha=.8f))
        }
        when(ship.id) {
            "S02" -> polygon(0f,-31f,8f,-2f,22f,15f,5f,10f,0f,20f,-5f,10f,-22f,15f,-8f,-2f)
            "S03" -> polygon(-13f,-22f,13f,-22f,23f,-6f,23f,17f,8f,22f,-8f,22f,-23f,17f,-23f,-6f)
            "S04" -> {polygon(-18f,-20f,-8f,-20f,-8f,15f,-18f,20f);polygon(8f,-20f,18f,-20f,18f,20f,8f,15f);polygon(0f,-16f,12f,8f,0f,17f,-12f,8f)}
            "S05" -> {polygon(0f,-28f,18f,11f,10f,20f,0f,10f,-10f,20f,-18f,11f);drawLine(Color.White,Offset(-10f,-8f),Offset(10f,8f),3f)}
            "S06" -> {polygon(0f,-24f,9f,-4f,25f,-10f,22f,17f,9f,13f,0f,22f,-9f,13f,-22f,17f,-25f,-10f,-9f,-4f);drawCircle(color.copy(alpha=.2f),28f,style=Stroke(2f))}
            "S07" -> polygon(0f,-35f,7f,-4f,15f,18f,0f,12f,-15f,18f,-7f,-4f)
            "S08" -> {polygon(0f,-25f,10f,-6f,22f,-6f,22f,12f,9f,12f,0f,22f,-9f,12f,-22f,12f,-22f,-6f,-10f,-6f);drawLine(Color.White,Offset(-7f,0f),Offset(7f,0f),3f);drawLine(Color.White,Offset(0f,-7f),Offset(0f,7f),3f)}
            "S09" -> polygon(0f,-29f,9f,-6f,23f,-13f,17f,5f,24f,18f,6f,14f,0f,22f,-6f,14f,-24f,18f,-17f,5f,-23f,-13f,-9f,-6f)
            "S10" -> {polygon(-19f,-26f,-7f,-26f,-7f,19f,-19f,19f);polygon(7f,-26f,19f,-26f,19f,19f,7f,19f);polygon(0f,-14f,16f,5f,0f,15f,-16f,5f)}
            "S11" -> {drawCircle(color,23f,style=Stroke(4f));polygon(0f,-29f,10f,13f,0f,20f,-10f,13f)}
            "S12" -> {val p=Path();for(i in 0 until 10){val a=(-PI/2+i*PI/5).toFloat();val rad=if(i%2==0)28f else 12f;val xx=cos(a)*rad;val yy=sin(a)*rad;if(i==0)p.moveTo(xx,yy)else p.lineTo(xx,yy)};p.close();drawPath(p,color);drawPath(p,Ink,style=Stroke(1f))}
        }
        if(ship.id!="S08")drawOval(Color(0xFF092B51),Offset(-3f,-13f),Size(6f,12f))
    }}
}
fun DrawScope.campaignBackdrop(g: GameEngine) {
    val c=g.campaign?:return
    val colors=listOf(0xFF123B4A,0xFF463327,0xFF30204F,0xFF24473E,0xFF23415F,0xFF41402A,0xFF3A284D,0xFF482330,0xFF2D315B,0xFF4D2638)
    drawRect(Color(colors[c.definition.chapter-1]).copy(alpha=.22f),size=Size(360f,800f))
    if(c.icy(g.time))for(i in 0..5)drawLine(Color(0xFF9CDDFF).copy(alpha=.16f),Offset(i*70f,440f),Offset(i*70f-45,800f),2f)
    if(c.wind(g.time)!=0f)repeat(5){i->val xx=if(c.wind(g.time)>0)20f else 340f;val dx=if(xx<100)25 else -25;drawLine(Cyan.copy(alpha=.3f),Offset(xx,460f+i*30),Offset(xx+dx,460f+i*30),2f)}
}
fun DrawScope.campaignObjects(g: GameEngine) {
    val c=g.campaign?:return
    c.beams.forEach {b->val active=!b.charge && g.time-b.born>=1.6+if(c.practice).5 else 0.0
        drawRect(Orange.copy(alpha=if(active).65f else .16f),Offset(b.x-b.width/2,0f),Size(b.width,800f))
        drawLine(Orange,Offset(b.x,100f),Offset(b.x,780f),if(active)3f else 1f,pathEffect=if(active)null else PathEffect.dashPathEffect(floatArrayOf(8f,8f)))
    }
    c.gates.forEach {gate->
        val memory=c.id in listOf(52,54,60,65,86)
        for(lane in if(memory)0..2 else gate.lane..gate.lane) {
            val x=if(memory)lane*120f+60 else gate.x
            val rect=Offset(x-45,gate.y-15)
            drawRect((if(gate.optional)Color(0xFFA6C8FF)else Cyan).copy(alpha=.18f),rect,Size(90f,30f))
            drawRect(Cyan,rect,Size(90f,30f),style=Stroke(2f,pathEffect=if(c.id==54 && lane!=gate.lane)PathEffect.dashPathEffect(floatArrayOf(4f,5f))else null))
            if(memory)repeat(lane+1){j->drawCircle(Ink,2.5f,Offset(x+(j-lane/2f)*10,gate.y))}
        }
    }
    if(c.id==87)drawCircle(Muted.copy(alpha=.3f),18f,Offset(c.ghostX,680f),style=Stroke(1f,pathEffect=PathEffect.dashPathEffect(floatArrayOf(4f,4f))))
    c.tokens.forEach {t->val color=when(t.kind){TokenKind.CARGO->Orange;TokenKind.RESCUE->Color(0xFF8AF2B5);TokenKind.WRONG->Color(0xFFEF7284);else->Cyan}
        if(t.kind==TokenKind.RESCUE){drawCircle(color,17f,Offset(t.x,t.y),style=Stroke(2f));drawLine(color,Offset(t.x-7,t.y),Offset(t.x+7,t.y),3f);drawLine(color,Offset(t.x,t.y-7),Offset(t.x,t.y+7),3f)}
        else {drawRect(color.copy(alpha=.3f),Offset(t.x-11,t.y-11),Size(22f,22f));drawRect(color,Offset(t.x-11,t.y-11),Size(22f,22f),style=Stroke(2f))
            if(t.kind==TokenKind.PIN||t.kind==TokenKind.CHOICE_PIN)drawLine(Ink,Offset(t.x-4,t.y+6),Offset(t.x+4,t.y-6),3f)
            if(t.kind==TokenKind.WRONG){drawLine(Ink,Offset(t.x-6,t.y-6),Offset(t.x+6,t.y+6),2f);drawLine(Ink,Offset(t.x+6,t.y-6),Offset(t.x-6,t.y+6),2f)}
        }
        if(t.hold>0)drawArc(Ink,-90f,(t.hold*360).toFloat(),false,Offset(t.x-22,t.y-22),Size(44f,44f),style=Stroke(3f))
    }
    c.nodes.filter{!it.done}.forEach {n->val open=n.open(g.time);drawCircle(if(n.fake)Muted else if(open)Cyan else Orange,22f,Offset(n.x,n.y),style=Stroke(3f));drawCircle((if(open)Cyan else Orange).copy(alpha=.2f),18f,Offset(n.x,n.y))
        if(n.type>0)drawLine(Ink,Offset(n.x-8,n.y),Offset(n.x+8,n.y),2f)
    }
    if(c.hasAllies){vessel(ShipCatalog.all[5],c.allyX,560f,.6f);repeat(c.allyArmor){drawCircle(Cyan,2f,Offset(c.allyX-12+it*6,540f))}}
    if(c.id==37){vessel(ShipCatalog.all[3],c.secondAllyX,600f,.6f);repeat(c.secondAllyArmor){drawCircle(Cyan,2f,Offset(c.secondAllyX-12+it*6,580f))}}
    if(c.definition.stationArmor>0){drawRoundRect(Color(0xFF506A89),Offset(110f,752f),Size(140f,25f),androidx.compose.ui.geometry.CornerRadius(7f,7f));drawRect(Cyan,Offset(115f,758f),Size(130f*c.stationArmor/c.definition.stationArmor,4f))}
    if(c.definition.cores>0){val x=c.bossX();val y=c.bossY(g.time);val open=c.bossOpen(g.time)
        val bossColor=listOf(Orange,Color(0xFFEF7284),Color(0xFFB79AFF),Cyan)[(c.definition.chapter-1)%4]
        drawRoundRect(bossColor.copy(alpha=.25f),Offset(x-48,y-22),Size(96f,44f),androidx.compose.ui.geometry.CornerRadius(9f,9f))
        for(side in listOf(-1,1))drawLine(bossColor,Offset(x+side*25,y),Offset(x+side*(35+c.definition.chapter*2),y-30),7f)
        drawCircle(if(open)Cyan else bossColor,21f,Offset(x,y),style=Stroke(if(open)3f else 6f))
        repeat(c.definition.cores){i->drawCircle(if(i<c.coreCount)Muted.copy(alpha=.4f)else bossColor,3f,Offset(145f+i*12,210f))}
        if(open)drawCircle(Cyan.copy(alpha=.12f),g.pulseRadius,Offset(x,y),style=Stroke(1f))
    }
}

/** Hazard silhouettes carry meaning even when color perception differs. */
internal fun androidx.compose.ui.graphics.drawscope.DrawScope.campaignHazard(m: com.example.nt_05_thienthach.game.Meteor) {
    val center=Offset(m.x,m.y);val r=m.kind.radius
    val color=when(m.tag){1->Color(0xFF9FDFFF);2->Color(0xFFFF8D8D);3->Color(0xFFFFDF69);4->Color(0xFFC9A1FF);5->Color(0xFF80D3FF);else->Color(0xFF7CFFE5)}
    drawCircle(color.copy(alpha=.12f),r+5,center)
    when(m.tag) {
        2 -> {
            val shape=Path().apply{moveTo(m.x,m.y-r);lineTo(m.x+r,m.y);lineTo(m.x+r*.55f,m.y+r);lineTo(m.x,m.y+r*.55f);lineTo(m.x-r*.55f,m.y+r);lineTo(m.x-r,m.y);close()}
            drawPath(shape,color.copy(alpha=.35f));drawPath(shape,color,style=Stroke(2f))
            drawLine(color,Offset(m.x-r*1.4f,m.y),Offset(m.x+r*1.4f,m.y),2f)
        }
        3 -> {
            drawCircle(color.copy(alpha=.25f),r,center);drawCircle(color,r,center,style=Stroke(2f))
            for(i in 0..7){val a=i*Math.PI/4;val v=Offset(kotlin.math.cos(a).toFloat(),kotlin.math.sin(a).toFloat());drawLine(color,center+v*r,center+v*(r+6),2f)}
        }
        4,6 -> {
            drawCircle(color.copy(alpha=.22f),r,center);drawCircle(color,r,center,style=Stroke(2f))
            drawOval(color,Offset(m.x-r*1.4f,m.y-r*.4f),androidx.compose.ui.geometry.Size(r*2.8f,r*.8f),style=Stroke(2f))
            if(m.rewound)drawLine(color,Offset(m.x+5,m.y-6),Offset(m.x-5,m.y),2f)
        }
        else -> {
            val shape=Path().apply{moveTo(m.x,m.y-r);lineTo(m.x+r,m.y-r*.2f);lineTo(m.x+r*.55f,m.y+r);lineTo(m.x-r*.7f,m.y+r*.6f);lineTo(m.x-r,m.y-r*.3f);close()}
            drawPath(shape,color.copy(alpha=.25f));drawPath(shape,color,style=Stroke(2f))
            drawLine(color,Offset(m.x-r*.4f,m.y-r*.6f),Offset(m.x+3,m.y),2f)
            drawLine(color,Offset(m.x+3,m.y),Offset(m.x-r*.3f,m.y+r*.6f),2f)
        }
    }
}
