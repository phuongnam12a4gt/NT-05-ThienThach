package com.example.nt_05_thienthach

import com.example.nt_05_thienthach.game.*
import com.example.nt_05_thienthach.data.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*
import java.io.File

class CampaignTest {
    @Test fun hundredExplicitStagesAndTwelveShips() {
        assertEquals((1..100).toList(),CampaignCatalog.all.map{it.id})
        assertEquals(12,ShipCatalog.all.size);assertEquals(12,ShipCatalog.all.map{it.id}.distinct().size)
        assertEquals((10..100 step 10).toList(),CampaignCatalog.all.filter{it.cores>0}.map{it.id})
        assertTrue(CampaignCatalog.all.all{it.seconds in 30..180 && it.chapter in 1..10})
    }
    @Test fun shipsHaveCorrectPricesAndDoNotEquipUnowned() {
        val p=Progress(credits=1000);val bought=Transactions.buyShip(p,"S02")
        assertEquals(750,bought.credits);assertEquals("S01",bought.equippedShip)
        assertEquals("S02",Transactions.equipShip(bought,"S02").equippedShip)
        assertThrows(IllegalStateException::class.java){Transactions.buyShip(bought,"S02")}
        assertThrows(IllegalArgumentException::class.java){Transactions.equipShip(p,"S12")}
        assertThrows(IllegalStateException::class.java){Transactions.buyShip(p,"S12")}
    }
    @Test fun campaignSavesIndependentStarsAndNeverRepeatsRewards() {
        val clear=RunResult("a",100,30000,0,stageId=1,victory=true,achievementBits=5)
        val a=Transactions.reward(Progress(),clear)
        assertEquals(5+56+20,a.credits);assertEquals(2,a.stages[1]!!.stars)
        val b=Transactions.reward(a,clear.copy(runId="b",achievementBits=3))
        assertEquals(a.credits+5+15,b.credits);assertEquals(7,b.stages[1]!!.bits)
        assertEquals(b,Transactions.reward(b,clear));assertEquals(0,b.bestScore)
        assertEquals(2,CampaignCatalog.unlocked(b.stages))
    }
    @Test fun allFirstClearRewardsMatchDesignAndStarsDoNotGateUnlocks() {
        var p=Progress();var fixed=0L
        for(id in 1..100) {
            val result=RunResult("stage$id",0,90000,0,stageId=id,victory=true,achievementBits=7)
            fixed+=Transactions.breakdown(p,result).total;p=Transactions.reward(p,result)
        }
        assertEquals(23900L,fixed);assertEquals(23900L,p.credits);assertEquals(300,CampaignCatalog.totalStars(p.stages))
        assertEquals(100,CampaignCatalog.unlocked(p.stages))
        assertEquals(10,p.rewardedChapters.size)
        assertThrows(IllegalArgumentException::class.java){Transactions.reward(Progress(),RunResult("locked",0,0,0,stageId=2,victory=true,achievementBits=1))}
    }
    @Test fun practiceAndFailuresDoNotUnlockOrPayFixedBonuses() {
        val result=RunResult("practice",500,30000,0,stageId=1,victory=true,achievementBits=7,practice=true)
        assertEquals(Progress(),Transactions.reward(Progress(),result))
        val failed=Transactions.reward(Progress(),result.copy(runId="failed",practice=false,victory=false,achievementBits=0))
        assertEquals(25L,failed.credits);assertEquals(1,CampaignCatalog.unlocked(failed.stages))
    }
    @Test fun radiusArmorRechargeAndAstraConfigurationUseShipStats() {
        val heavy=GameEngine(Upgrades(armor=2),ship=ShipCatalog.find("S03")!!)
        assertEquals(7,heavy.maxArmor);assertEquals(7,heavy.armor)
        assertEquals(160f,GameEngine(ship=ShipCatalog.find("S06")!!).pulseRadius,0f)
        assertEquals(4,GameEngine(ship=ShipCatalog.find("S12")!!).maxArmor)
        assertEquals(210f,GameEngine(ship=ShipCatalog.find("S12")!!,astra=AstraConfiguration.PULSE).pulseRadius,0f)
    }
    @Test fun campaignPauseFreezesTokensGatesAndBossWindows() {
        val g=GameEngine(stage=CampaignCatalog.find(10),tutorial=true).apply{startTutorial()}
        repeat(240){g.advance(GameConfig.STEP)}
        val time=g.time;val tokens=g.campaign!!.tokens.map{it.copy()};g.pause()
        repeat(600){g.advance(GameConfig.STEP)}
        assertEquals(time,g.time,0.0);assertEquals(tokens,g.campaign!!.tokens)
    }
    @Test fun successfulResultFreezesAndCannotUsePulseAgain() {
        val g=GameEngine(stage=CampaignCatalog.find(1),tutorial=true).apply{startTutorial()}
        repeat(1800){g.meteors.clear();g.advance(GameConfig.STEP)}
        assertEquals(Phase.VICTORY,g.phase);val result=g.result
        assertFalse(g.requestPulse());repeat(100){g.advance(.1)};assertEquals(result,g.result)
    }
    @Test fun clearedNodeCannotBePaidTwiceByOnePulse() {
        val g=GameEngine(stage=CampaignCatalog.find(5),tutorial=true).apply{startTutorial()}
        repeat(500){g.meteors.clear();g.advance(GameConfig.STEP)}
        val c=g.campaign!!;val n=c.nodes.first();g.targetX=n.x
        repeat(30){g.meteors.clear();g.advance(GameConfig.STEP)}
        if(n.open(g.time)){g.requestPulse();g.advance(GameConfig.STEP);assertEquals(1,c.nodeCount)}
    }
    @Test fun sentinelSavesOnlyOneLethalHitAndNeverAllyFailure() {
        val g=GameEngine(ship=ShipCatalog.find("S10")!!,tutorial=true).apply{startTutorial()}
        repeat(g.maxArmor){g.damage();repeat(100){g.advance(GameConfig.STEP)}}
        assertEquals(1,g.armor);assertTrue(g.sentinelUsed);assertEquals(Phase.RUNNING,g.phase)
        g.damage();assertEquals(Phase.GAME_OVER,g.phase)
        val campaign=GameEngine(ship=ShipCatalog.find("S10")!!,stage=CampaignCatalog.find(31),tutorial=true).apply{startTutorial()}
        campaign.finishStage(false,DefeatReason.ALLY)
        assertEquals(Phase.GAME_OVER,campaign.phase);assertFalse(campaign.sentinelUsed)
    }
    @Test fun medicRepairsAtSimulationTimeAndStopsAtTwoCampaignHeals() {
        val g=GameEngine(ship=ShipCatalog.find("S08")!!,stage=CampaignCatalog.find(1)!!.copy(seconds=150),tutorial=true).apply{startTutorial();damage()}
        fun until(seconds:Double) {
            var frames=0
            while(g.time<seconds && frames++<12000) {
                if(g.phase==Phase.CHOOSING)g.chooseUpgrade(g.upgradeOptions.first())
                g.meteors.clear();g.items.clear();g.advance(GameConfig.STEP)
            }
            assertTrue(g.time>=seconds)
        }
        until(24.9);assertEquals(2,g.armor)
        until(25.1);assertEquals(3,g.armor)
        g.damage();until(50.1);assertEquals(3,g.armor)
        g.damage();until(75.1);assertEquals(2,g.armor)
    }

    @Test fun voltAndAuroraApplyPickupEnergyTradeoffs() {
        fun pickup(id:String):GameEngine {
            val g=GameEngine(ship=ShipCatalog.find(id)!!,tutorial=true).apply{startTutorial();rechargeEnergy(-100.0)}
            g.items.add(Item(999,180f,680f,ItemKind.SHIELD));g.advance(GameConfig.STEP);return g
        }
        assertEquals(15.07,pickup("S05").energy,.01)
        assertEquals(28+3.4/60,pickup("S11").energy,.01)
        val guardian=pickup("S06");assertTrue(guardian.shield>6)
    }
    @Test fun scriptedAutopilotCompletesCampaignObjectivesWithoutRemovingHazards() {
        val traces=mutableListOf<String>();val lines=mutableListOf<String>();val failed=mutableListOf<Int>()
        for(spec in CampaignCatalog.all) {
            val g=GameEngine(Upgrades(2,3,3),stage=spec,seed=spec.seed,tutorial=true).apply{startTutorial()}
            val c=g.campaign!!
            repeat((spec.seconds*65+2000)) {
                if(g.phase==Phase.CHOOSING)g.chooseUpgrade(g.upgradeOptions.firstOrNull{it==FlightUpgrade.FOCUS}?:g.upgradeOptions.first())
                if(g.phase==Phase.RUNNING) {
                    val positions=mutableListOf<Pair<Double,Float>>()
                    c.gates.filter{!it.resolved}.forEach{positions.add(((if(spec.allyArmor>0&&spec.gates>0)560f else 680f)-it.y)/160.0 to it.x)}
                    c.tokens.filter{it.kind!=TokenKind.WRONG&&(it.kind!=TokenKind.PIN||g.energy<99)}.forEach{positions.add((680-it.y)/160.0 to it.x)}
                    c.nodes.filter{!it.done&&!it.fake}.forEach{positions.add(0.5 to it.x)}
                    if(spec.cores>0 && (c.bossOpen(g.time) || positions.isEmpty()))positions.add(-1.0 to c.bossX())
                    if(spec.near>0)g.meteors.filter{it.y in 400f..680f&&!it.grazed}.minByOrNull{680-it.y}?.let{positions.add(-2.0 to (it.x+if(it.x>180)-34 else 34).coerceIn(20f,340f))}
                    
                    if(spec.stationArmor>0)positions.add(0.1 to 180f)
                    if(c.hasAllies && g.energy>=100)g.meteors.filter{it.y in 350f..560f && (abs(it.x-c.allyX)<50 || spec.id==37 && abs(it.x-c.secondAllyX)<50)}.maxByOrNull{it.y}?.let{positions.add(-2.5 to it.x)}
                    if(spec.id==39) {
                        val future=c.executionMemory[((g.time+4.3)/9).toInt()%c.executionMemory.size]-1
                        val empty=(0..2).firstOrNull{it!=future&&abs((60+it*120)-c.allyX)>65}?:((future+1)%3)
                        positions.add(-3.0 to (60+empty*120f))
                    }
                    val target=positions.filter{it.first>=-.1||it.first<=-1}.minByOrNull{it.first}?.second?:if(c.hasAllies)c.allyX else c.safeX(g)
                    var pulse=false
                    if(spec.id==9 && c.gateCount==2 && c.gates.any{!it.resolved&&hypot(it.x-g.shipX,it.y-680)<=g.pulseRadius})pulse=true
                    if(spec.cores>0&&c.bossOpen(g.time)&&hypot(c.bossX()-g.shipX,c.bossY(g.time)-680)<=g.pulseRadius+18)pulse=true
                    if(c.nodes.any{!it.done&&(it.type==0||it.type==3)&&it.open(g.time)&&hypot(it.x-g.shipX,it.y-680)<=g.pulseRadius+18})pulse=true
                    val danger=g.meteors.filter{it.y in 480f..730f&&hypot(it.x-g.shipX,it.y-680)<g.pulseRadius}
                    if(danger.any{abs(it.x-g.shipX)<it.kind.radius+g.ship.radius+18}&&spec.near==0)pulse=true
                    if(c.hasAllies&&g.meteors.any{(abs(it.x-c.allyX)<40||spec.id==37&&abs(it.x-c.secondAllyX)<40)&&it.y in 495f..560f&&hypot(it.x-g.shipX,it.y-680)<=g.pulseRadius+it.kind.radius})pulse=true
                    if(spec.stationArmor>0&&g.meteors.any{abs(it.x-180)<90&&it.y in 600f..745f&&hypot(it.x-g.shipX,it.y-680)<=g.pulseRadius+it.kind.radius})pulse=true
                    if(pulse&&g.energy>=100)g.requestPulse()
                    val candidates=(listOf(target,g.shipX,30f,90f,150f,210f,270f,330f)).distinct()
                    g.targetX=candidates.minByOrNull{x->
                        val risk=g.meteors.filter{it.y in 500f..730f}.sumOf{m->if(abs(m.x-x)<m.kind.radius+g.ship.radius+(if(spec.near>0)0 else 12))1000.0 else 0.0}
                        val beamRisk=c.beams.sumOf{beam->if(!beam.harmless && !beam.charge && g.time-beam.born<2.2 && abs(x-beam.x)<beam.width/2+g.ship.radius+8)10000.0 else 0.0}
                        val nextX=g.shipX+(x-g.shipX).coerceIn(-g.speedLimit/60,g.speedLimit/60)
                        val stepRisk=g.meteors.sumOf{m->if(CollisionSystem.swept(g.shipX,nextX,m.x,m.y,m.kind.radius+g.ship.radius+2))100000.0 else 0.0}
                        stepRisk+beamRisk+risk+abs(x-target)+(if(spec.cores>0||spec.nodes>0)abs(x-target)*2 else 0f)
                    }?:target
                }
                val beforeA=c.allyArmor;val beforeB=c.secondAllyArmor;val beforeS=c.stationArmor;val beforeP=g.pulsesUsed
                val nearby=if(spec.id in listOf(37,39,97))g.meteors.filter{it.y>450}.joinToString{"${it.x.toInt()}/${it.y.toInt()}/${it.kind}"}else ""
                g.advance(GameConfig.STEP)
                if(spec.id in listOf(37,39,97) && (c.allyArmor<beforeA||c.secondAllyArmor<beforeB||c.stationArmor<beforeS||g.pulsesUsed>beforeP))traces.add("stage=${spec.id} t=${"%.2f".format(g.time)} x=${g.shipX.toInt()} energy=${g.energy.toInt()} P=${g.pulsesUsed} A=${c.allyArmor}/${c.secondAllyArmor} ax=${c.allyX.toInt()}/${c.secondAllyX.toInt()} S=${c.stationArmor} tokens=${c.tokens.joinToString{it.kind.toString()+":"+it.x.toInt()+"/"+it.y.toInt()}} rocks=$nearby")
            }
            val ok=g.phase==Phase.VICTORY
            if(!ok)failed.add(spec.id)
            lines.add("${spec.id}: ${g.phase} ${g.defeatReason} t=${g.time.toInt()} armor=${g.armor} gate=${c.gateCount}/${spec.gates} cargo=${c.cargoCount}/${spec.cargo} node=${c.nodeCount}/${spec.nodes} core=${c.coreCount}/${spec.cores} near=${g.nearMisses}/${spec.near}")
        }
        File("build/reports/campaign-autopilot.txt").apply{parentFile?.mkdirs();writeText(lines.joinToString("\n"))}
        File("build/reports/campaign-diagnostics.txt").writeText(traces.joinToString("\n"))
        assertTrue("Autopilot failures: $failed",failed.isEmpty())
    }
}
