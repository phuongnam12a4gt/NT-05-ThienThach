package com.example.nt_05_thienthach.game

import kotlin.math.*

enum class TokenKind { CARGO, PIN, RESCUE, WRONG, CHOICE_PIN, CHOICE_SHIELD }
data class MissionToken(val id: Long, var x: Float, var y: Float, val kind: TokenKind, val order: Int = 0, var hold: Double = 0.0, var age: Double = 0.0)
data class RouteGate(var y: Float, val lane: Int, val order: Int, val optional: Boolean = false, var resolved: Boolean = false, var x: Float = 60f+lane*120f, var secondWindow: Boolean = false)
data class MissionNode(val id: Long, val x: Float, val y: Float, val born: Double, val type: Int, var done: Boolean = false, val fake: Boolean = false) {
    fun open(time: Double) = type != 0 || ((time-born) % 12.0 >= 4.0 && (time-born) % 12.0 < 10.0)
}
data class WarningBeam(val x: Float, val born: Double, val width: Float = 34f, val harmless: Boolean = false, var hit: Boolean = false, val charge: Boolean = false, var fired: Boolean = false)

/** Per-run director. All state and timers use simulation time; no Android or IO. */
class StageSession(val definition: StageDefinition, val practice: Boolean, val route: Int) {
    val id get() = definition.id
    val tokens = mutableListOf<MissionToken>()
    val gates = mutableListOf<RouteGate>()
    val nodes = mutableListOf<MissionNode>()
    val beams = mutableListOf<WarningBeam>()
    var gateCount = 0; private set
    var cargoCount = 0; private set
    var nodeCount = 0; private set
    var coreCount = 0; private set
    var allyArmor = if (id == 100) 3 else definition.allyArmor; private set
    var secondAllyArmor = definition.allyArmor; private set
    var stationArmor = definition.stationArmor; private set
    var allyX = 180f; private set
    var secondAllyX = 180f; private set
    val hasAllies get() = definition.allyArmor > 0 || id == 100 && coreCount in 2..3
    var hintsUsed = false; private set
    var rescueUsed = false; private set
    var guides = 0; private set
    var misses = 0; private set
    var wrong = 0; private set
    var boundaryTouches = 0; private set
    var freezeUntil = 0.0; private set
    var bossCycle = 0.0; private set
    var bossPinSent = false; private set
    var beamNext = 8.0
    var minimumEnergy = 100.0; private set
    private var nextPattern = 3.0
    private var nextGate = 4.0
    private var nextCargo = 4.0
    private var nextNode = 4.0
    private var nextSupply = 3.0
    private var eventId = 100000L
    private var beat = 0
    private var cargoAttempts = 0
    private var nodeAttempts = 0
    private var firstCores = 0
    private var pinsCaught = 0
    private var pulsesWithHits = 0
    private var centerKills = 0
    private var currentCoreMissed = false
    private var bossUnlocked = false
    private var lastGatePowered = false
    private var gateMaxOffset = 0f
    private var centralDestroyed = false
    private var awaitingGatePin = false
    private var pinGateCount = 0
    private var cyclePulseStart = 0
    private val nearDrifts = mutableSetOf<Int>()
    private val hitTags = mutableSetOf<Int>()
    private val luresByLane = IntArray(3)
    val ghostX get() = trail.firstOrNull()?.second ?: 180f
    private var pickedSlow = 0
    private var shieldBlocks = 0
    private var lastShield = 0.0
    private var maxPulseKills = 0
    private var maxPulseTypes = 0
    private var droneKills = 0
    private var iceBeforeSplit = 0
    private var fractureHits = 0
    private var driftAllyHits = 0
    private var largeLeaks = 0
    private var allyDamage = 0
    private var allyGrace = 0.0
    private var secondGrace = 0.0
    private var stationGrace = 0.0
    private var closedPulses = 0
    private var earlyNodes = 0
    private var nearKinds = mutableSetOf<MeteorKind>()
    private var rewoundNear = mutableSetOf<Long>()
    private var fastAllyHits = 0
    private var choices = mutableSetOf<TokenKind>()
    private var choiceCount = 0
    private var lureCount = 0
    private var noPulseCycle = false
    private var offered = 0
    private var nearAfterPulse = 0
    private var cargoAtFirst = true
    private var stationInput = 100.0
    private val trail = java.util.ArrayDeque<Pair<Double,Float>>()
    val executionMemory get() = routeSequence().map { it+1 }
    val memory get() = if(id==98)listOf(2,3,5,7,9)else if(id==86)executionMemory.reversed()else executionMemory
    fun showHints() { hintsUsed = true }
    fun icy(t: Double): Boolean = when (id) {
        41 -> t in 15.0..25.0 || t in 35.0..45.0
        in 42..50 -> true
        94 -> (t / 21).toInt() == 2
        98 -> (t / 18).toInt() == 2
        else -> false
    }
    fun wind(t: Double): Float = when (id) {
        61 -> if (t in 12.0..28.0) 95f else if (t in 38.0..54.0) -95f else 0f
        62 -> if ((t/8).toInt()%2 == 0) 100f else -100f
        64,67 -> if ((t/10).toInt()%2 == 0) 85f else -85f
        66 -> if ((t/12).toInt()%2 == 0) 65f else -65f
        70 -> (if (coreCount%2 == 0) 110f else -110f) * if (bossOpen(t)) .25f else 1f
        94 -> if ((t/21).toInt()==1) 90f else 0f
        98 -> if ((t/18).toInt()==3) -85f else 0f
        100 -> if (coreCount in 2..3) 85f else 0f
        else -> 0f
    }
    fun rechargeAllowed(g: GameEngine): Boolean = when (id) {
        22 -> g.shipX in 120f..240f
        30 -> coreCount < 2 || g.shipX in 120f..240f
        68 -> g.time % 12 < 8
        94 -> (g.time/21).toInt() != 3 || g.shipX in 120f..240f
        else -> true
    }
    fun timeFactor(m: Meteor, time: Double): Float {
        if (time < freezeUntil) return 0f
        return when (id) {
            82,89 -> if (m.x < 120) .6f else if (m.x > 240) 1.35f else 1f
            90 -> if (coreCount == 1 && time % 8 < 2) 0f else 1f
            else -> 1f
        }
    }
    fun bossOpen(t: Double): Boolean {
        if(definition.cores<=coreCount || t<bossCycle)return false
        if((id==30 || id==60 || id==90 && coreCount==4) && !bossUnlocked)return false
        val age=(t-bossCycle)%20
        return if(id==90 && coreCount==3)age in 8.0..11.0 || age in 13.0..16.0 else age in 8.0..16.0
    }
    fun bossX(): Float = laneX(routeSequence()[coreCount % routeSequence().size])
    fun bossY(t: Double) = if (bossOpen(t)) 560f else 180f
    fun safeX(g: GameEngine): Float {
        gates.firstOrNull { !it.resolved && !it.optional }?.let { return laneX(it.lane) }
        tokens.firstOrNull { it.kind == TokenKind.CARGO || it.kind == TokenKind.RESCUE }?.let { return it.x }
        if (definition.cores > 0) return bossX()
        return laneX(routeSequence()[(g.time/7).toInt() % routeSequence().size])
    }
    private fun laneX(lane: Int) = 60f + lane.coerceIn(0,2) * 120f
    private fun routeSequence(): List<Int> = when (id) {
        1 -> listOf(1,0,2)
        2,4,14 -> listOf(0,1,2,1,0)
        12 -> listOf(0,0,1,2)
        24,35,51 -> listOf(0,2,1,0,2)
        52 -> listOf(2,0,1)
        65,69 -> listOf(0,1,2,2,1,0)
        86 -> listOf(2,2,1,0,1)
        91 -> if (route==0) listOf(1,1,0,1,1,2) else if (route==1) listOf(0,2,0,2,1,1) else listOf(2,1,0,1,2,0)
        else -> listOf((id+route)%3,(id+1+route)%3,(id+2+route)%3,1,0,2)
    }
    fun beforeStep(g: GameEngine, dt: Double) {
        minimumEnergy = min(minimumEnergy,g.energy)
        if (g.shipX <= 35 || g.shipX >= 325) boundaryTouches++
        
        lastShield = g.shield
        trail.addLast(g.time to g.shipX)
        while (trail.size > 1 && trail.first.first < g.time-(if(id==87)3.0 else 1.2)) trail.removeFirst()
        val follow = if (id in listOf(35,76)) trail.first.second else laneX(routeSequence()[(g.time/9).toInt()%routeSequence().size])
        allyX += (follow-allyX).coerceIn(-110f*dt.toFloat(),110f*dt.toFloat())
        secondAllyX += (laneX(routeSequence()[((g.time-2).coerceAtLeast(0.0)/9).toInt()%routeSequence().size])-secondAllyX).coerceIn(-110f*dt.toFloat(),110f*dt.toFloat())
        if (g.time >= nextPattern && !(id==100 && g.time<bossCycle)) { spawnPattern(g); beat++; nextPattern = g.time + patternInterval(g.time) }
        val finalGateReady = when(id) { 47,51 -> nodeCount >= definition.nodes; 96 -> cargoCount >= definition.cargo; else -> true }
        if ((id==30 || id==60 || id==90 && coreCount==4) && !bossUnlocked && gates.none{!it.resolved}) {
            if(id==30 && abs(g.shipX-bossX())<28)bossUnlocked=true
            else if(id!=30)gates.add(RouteGate(100f,(bossX()/120).toInt().coerceIn(0,2),coreCount+1,true))
        }
        if (g.time >= nextGate && finalGateReady && (definition.gates > gateCount || definition.gates==0 && definition.survive && id in listOf(1,4,11,25,46,48,77,81))) {
            val optional = definition.gates == 0
            gates.add(RouteGate(-30f,routeSequence()[(if(optional) guides+misses else gateCount)%routeSequence().size],gateCount+1,optional))
            nextGate = if(id==94) listOf(4.0,13.0,25.0,34.0,46.0,55.0,67.0,76.0,88.0).getOrElse(gateCount+gates.count{!it.resolved}){g.time+9} else g.time + if (optional) 6.0 else max(4.5, definition.seconds*.55 / max(1,definition.gates))
        }
        if (g.time >= nextCargo && cargoCount < (if(id==19) 4 else definition.cargo)) {
            cargoAttempts++
            val rescue = id in listOf(32,38,93)
            val x = laneX(routeSequence()[(cargoAttempts-1)%routeSequence().size])
            tokens.add(MissionToken(++eventId,x,if(rescue) 420f else 150f,if(rescue) TokenKind.RESCUE else TokenKind.CARGO,cargoAttempts))
            nextCargo = g.time + max(4.0,definition.seconds*.55 / max(1.0,definition.cargo*1.5))
            if(id==56) tokens.add(MissionToken(++eventId,360-x,100f,TokenKind.WRONG))
        }
        if (g.time >= nextNode && nodeCount < definition.nodes && (id != 59 || cargoCount >= definition.cargo)) {
            val x = laneX(routeSequence()[nodeCount%routeSequence().size])
            val type = if(id==51) 1 else if(id in listOf(26,47)) 2 else if(id in listOf(5,23,59,78,99))3 else 0
            nodes.removeAll { !it.done }
            nodes.add(MissionNode(++eventId,if(id==75 && nodeCount==0)140f else x,if(type==0) 560f else 680f,g.time,type));nodeAttempts++
            if(id==75 && nodeCount==0)nodes.add(MissionNode(++eventId,220f,560f,g.time,type))
            if(id==95)nodes.add(MissionNode(++eventId,360-x,560f,g.time,type,fake=true))
            tokens.add(MissionToken(++eventId,x,400f,TokenKind.PIN))
            nextNode = g.time + 12.0
        }
        if (g.time >= nextSupply && (g.energy < 75 || id in listOf(21,25,27,68))) {
            tokens.add(MissionToken(++eventId,if(definition.cores>0) bossX() else if(definition.stationArmor>0)180f else safeX(g),350f,TokenKind.PIN))
            nextSupply=g.time+if(definition.stationArmor>0 || id==37)3.0 else 8.0
        }
        if(definition.cores>0 && g.time >= bossCycle+3 && !bossPinSent) {
            tokens.add(MissionToken(++eventId,bossX(),400f,TokenKind.PIN));bossPinSent=true
        }
        if(definition.cores>0 && g.time >= bossCycle+20) { currentCoreMissed=true;bossCycle+=20;bossPinSent=false }
        if(id==92 && g.time>=12+choiceCount*22 && choiceCount<3 && tokens.none { it.kind in listOf(TokenKind.CHOICE_PIN,TokenKind.CHOICE_SHIELD) }) {
            tokens.add(MissionToken(++eventId,60f,260f,TokenKind.CHOICE_PIN));tokens.add(MissionToken(++eventId,300f,260f,TokenKind.CHOICE_SHIELD))
        }
        if (id in listOf(21,23,30,55,61,65,70,74,80,90,95,100) && g.time>=beamNext) {
            beams.add(WarningBeam(if(id==74) 60f else g.shipX,g.time));beamNext=g.time+9.0
        }
        if (id in listOf(43,50) || id==98 && (g.time/18).toInt()==2) {
            val fracture=g.meteors.filter { it.tag==1 && !it.rewound && it.y>=370 }
            fracture.forEach { rock ->
                rock.rewound=true;g.meteors.remove(rock)
                for(sign in listOf(-1,1)) if(g.meteors.size<GameConfig.MAX_METEORS) g.meteors.add(Meteor(++eventId,rock.x,rock.y,MeteorKind.SMALL,drift=sign*35f,tag=5,createdAt=g.time))
            }
        }
        if(id==83 || id==90 && coreCount==2) g.meteors.filter { it.tag==6 && !it.rewound && it.y>440 }.forEach { it.y-=130;it.rewound=true }
        if(id==18) g.meteors.filter { it.tag==4 }.forEach { it.x=(180+sin((g.time-it.createdAt)*1.8+it.id)*90).toFloat() }
        if(id==64 || id==28 || id==47) tokens.forEach { it.x=(it.x+wind(g.time).let { w -> if(w==0f) sin(g.time).toFloat()*40f else w*.5f }*dt.toFloat()).coerceIn(25f,335f) }
        if(id==87 && g.time>=beamNext) { beams.add(WarningBeam(trail.first.second,g.time,charge=true));beamNext=g.time+7;lureCount++ }
    }
    fun afterStep(g: GameEngine, dt: Double) {
        if(g.phase!=Phase.RUNNING) return
        val gateIt=gates.iterator()
        while(gateIt.hasNext()) {
            val gate=gateIt.next();val before=gate.y;gate.y+=160f*dt.toFloat()
            if(id==45)gate.x=(laneX(gate.lane)+sin(g.time*1.2+gate.order).toFloat()*32).coerceIn(45f,315f)
            val threshold=if(id in listOf(35,76)) 560f else GameConfig.SHIP_Y
            if(!gate.resolved && before<threshold && gate.y>=threshold) {
                gate.resolved=true
                val x=if(id in listOf(35,76)) allyX else g.shipX
                val slowEnough=id!=42 || abs(g.horizontalVelocity)<=120
                val cost=if(id==29 && gateCount==definition.gates-1) 70 else if(id==57 && gateCount==definition.gates-1) 60 else 0
                val offset=abs(x-gate.x)
                val aligned=offset <= (if(id==84 && !gate.secondWindow)35f else 45f)-g.ship.radius
                if(aligned && slowEnough && g.energy>=cost && !(id==9 && gateCount==2 && !lastGatePowered)) {
                    g.rechargeEnergy(-cost.toDouble());gateMaxOffset=max(gateMaxOffset,offset)
                    if(gate.optional) { guides++;if(id==60 || id==90)bossUnlocked=true;if(awaitingGatePin){pinGateCount++;awaitingGatePin=false} } else gateCount++
                } else {
                    misses++;awaitingGatePin=false;if(!aligned)wrong++
                    if(id==84 && !gate.secondWindow) {gate.secondWindow=true;gate.y=180f;gate.resolved=false}
                }
            }
            if(gate.y>840)gateIt.remove()
        }
        val tokenIt=tokens.iterator();var choiceTaken=false
        while(tokenIt.hasNext()) {
            val token=tokenIt.next();token.age+=dt
            val pausedItem=id==85 && token.y in 550f..565f && token.age<5
            if(!pausedItem) token.y+=160f*dt.toFloat()
            if(token.kind==TokenKind.RESCUE && token.y>=680 && token.age<7) token.y=680f
            val touch=hypot(token.x-g.shipX,token.y-GameConfig.SHIP_Y)<=g.ship.radius+18
            if(touch) {
                if(token.kind==TokenKind.RESCUE) {token.hold+=dt;if(token.hold<1.0)continue}
                when(token.kind) {
                    TokenKind.CARGO,TokenKind.RESCUE -> { cargoCount++;if(token.order>cargoCount)cargoAtFirst=false;g.rechargeEnergy(20.0) }
                    TokenKind.PIN -> {pinsCaught++;awaitingGatePin=true;g.rechargeEnergy(100.0);if(id==29 || id==57 || id==96 || id==99)rescueUsed=true}
                    TokenKind.WRONG -> wrong++
                    TokenKind.CHOICE_PIN -> { g.rechargeEnergy(100.0);choices.add(token.kind);choiceTaken=true;choiceCount++ }
                    TokenKind.CHOICE_SHIELD -> { if(id==36)stationArmor=min(definition.stationArmor,stationArmor+1)else g.grantShield();choices.add(token.kind);choiceTaken=true;choiceCount++ }
                }
                g.events.add(GameEvent.PICKUP);tokenIt.remove()
            } else {token.hold=0.0;if(token.y>830)tokenIt.remove()}
        }
        if(choiceTaken)tokens.removeAll { it.kind==TokenKind.CHOICE_PIN || it.kind==TokenKind.CHOICE_SHIELD }
        nodes.filter { !it.done && it.type in 1..2 && g.time-it.born>=1.6 }.forEach { node ->
            if(abs(g.shipX-node.x)<=28) {
                if(node.type==1 || g.energy>=80) {
                    stationInput=min(stationInput,g.energy);if(node.type==2)g.rechargeEnergy(-80.0)
                    node.done=true;nodeCount++;if(g.time-node.born<7)earlyNodes++
                }
            }
        }
        nodes.removeAll { it.done }
        beams.forEach { beam ->
            val age=g.time-beam.born
            if(beam.charge && age>=1.6 && !beam.fired) {
                beam.fired=true
                if(g.meteors.size<80)g.meteors.add(Meteor(++eventId,beam.x,50f,MeteorKind.SMALL,tag=2,createdAt=g.time))
            }
            if(!beam.charge && age in (1.6+if(practice).5 else 0.0)..(2.1+if(practice).5 else 0.0) && abs(g.shipX-beam.x)<beam.width/2+g.ship.radius) { beam.hit=true;if(!beam.harmless)g.damage(7)else wrong++ }
        }
        beams.filter { g.time-it.born>3 && !it.hit && !it.harmless }.forEach { lureCount++;luresByLane[(it.x/120).toInt().coerceIn(0,2)]++ }
        beams.removeAll { g.time-it.born>3 }
        val threatened=g.meteors.filter { meteor ->
            if(hasAllies && hypot(meteor.x-allyX,meteor.y-560f)<meteor.kind.radius+16) {
                if(g.time>=allyGrace){allyArmor=max(0,allyArmor-1);allyDamage++;allyGrace=g.time+1.2;if(meteor.drift!=0f)driftAllyHits++;if(timeFactor(meteor,g.time)>1)fastAllyHits++};true
            } else if(id==37 && hypot(meteor.x-secondAllyX,meteor.y-600f)<meteor.kind.radius+16) {
                if(g.time>=secondGrace){secondAllyArmor=max(0,secondAllyArmor-1);allyDamage++;secondGrace=g.time+1.2};true
            } else if(definition.stationArmor>0 && meteor.y>760 && abs(meteor.x-180)<70+meteor.kind.radius) {
                if(g.time>=stationGrace){stationArmor=max(0,stationArmor-1);stationGrace=g.time+1.2;if(meteor.kind==MeteorKind.LARGE)largeLeaks++};true
            } else false
        };g.meteors.removeAll(threatened.toSet())
        if(g.phase!=Phase.RUNNING)return
        if(hasAllies && (allyArmor<=0 || id==37 && secondAllyArmor<=0)) {g.finishStage(false,DefeatReason.ALLY);return}
        if(definition.stationArmor>0 && stationArmor<=0) {g.finishStage(false,DefeatReason.STATION);return}
        val done=gateCount>=definition.gates && cargoCount>=definition.cargo && nodeCount>=definition.nodes && g.nearMisses>=definition.near && coreCount>=definition.cores
        if(done && (!definition.survive || g.time+1e-8>=definition.seconds)) {g.finishStage(true);return}
        if(g.time+1e-8>=definition.seconds) {g.finishStage(false,DefeatReason.TIMEOUT);return}
        val maxOffers=if(definition.cores>0)2 else if(definition.seconds>=60)1 else 0
        if(offered<maxOffers && g.time>=definition.seconds*(offered+1)/(maxOffers+1.0) && gates.none { it.y>400 && it.y<760 } && (!bossOpen(g.time))) {
            offered++;g.offerUpgrade()
        }
    }
    fun onPulse(g: GameEngine, rocks: List<Meteor>) {
        maxPulseKills=max(maxPulseKills,rocks.size);maxPulseTypes=max(maxPulseTypes,rocks.map { if(it.tag==3)3 else it.kind.ordinal }.distinct().size)
        if(rocks.isNotEmpty())pulsesWithHits++
        centerKills+=rocks.count { it.tag==8 }
        droneKills+=rocks.count { it.tag==2 || it.tag==8 };iceBeforeSplit+=rocks.count { it.tag==1 && !it.rewound }
        if(rocks.any{it.tag==4 && it.kind==MeteorKind.LARGE})centralDestroyed=true
        if(id==9 && gateCount==2 && gates.any{!it.resolved && hypot(it.x-g.shipX,it.y-GameConfig.SHIP_Y)<=g.pulseRadius})lastGatePowered=true
        nodes.filter { !it.done && (it.type==0 || it.type==3) && hypot(it.x-g.shipX,it.y-GameConfig.SHIP_Y)<=g.pulseRadius+18 }.forEach {
            if(it.open(g.time)) {
                it.done=true
                if(it.fake)wrong++else {nodeCount++;if(id==23 && (g.time-it.born)%12 in 4.0..10.0 || id!=23 && g.time-it.born<=7)earlyNodes++}
            }
            else {closedPulses++;if(id==88)freezeUntil=g.time+1.2}
        }
        if(definition.cores>0) {
            if(bossOpen(g.time) && hypot(bossX()-g.shipX,bossY(g.time)-GameConfig.SHIP_Y)<=g.pulseRadius+20) {
                if(!currentCoreMissed)firstCores++;currentCoreMissed=false;coreCount++;bossUnlocked=false;bossCycle=g.time+if(id==100 && coreCount%2==0)5 else 0;bossPinSent=false
                if(id==100 && coreCount%2==0){g.repair();g.rechargeEnergy(100.0);g.meteors.clear();beams.clear()}
            } else closedPulses++
        }
    }
    fun onShieldBlock() { shieldBlocks++ }
    fun recordHit(tag: Int) {hitTags.add(tag);if(tag==5)fractureHits++}
    fun onPickup(kind: ItemKind) {if(kind==ItemKind.SLOW)pickedSlow++}
    fun onNear(meteor: Meteor) {nearKinds.add(meteor.kind);nearDrifts.add(meteor.drift.sign.toInt());if(meteor.rewound)rewoundNear.add(meteor.id)}
    private fun patternInterval(t: Double): Double = when(id) {
        4 -> 3.0
        17 -> if(beat%3<2)1.0 else 4.5
        58 -> 2.7
        79 -> 2.6
        else -> (3.5-definition.chapter*.12).coerceAtLeast(2.2)
    }
    private fun spawnPattern(g: GameEngine) {
        val safe=safeX(g)
        if(id==58 && beat%4==0) {if(beat>0 && g.pulsesUsed==cyclePulseStart)noPulseCycle=true;cyclePulseStart=g.pulsesUsed}
        fun rock(x: Float, y: Float = -34f, kind: MeteorKind = MeteorKind.SMALL, drift: Float = 0f, tag: Int = 0, protect: Boolean = true) {
            if(g.meteors.size>=80)return
            if(protect && abs(x-safe)<65+kind.radius)return
            if(protect && hasAllies) {
                val flight=(560-y)/(kind.speed*(.8f+definition.chapter*.035f))
                val future=if(id in listOf(35,76))allyX else laneX(routeSequence()[((g.time+flight)/9).toInt()%routeSequence().size])
                if(abs(x+drift*flight-future)<90+kind.radius)return
                if(id==37 && (abs(x-secondAllyX)<90+kind.radius || abs(x+drift*flight-laneX(routeSequence()[((g.time+flight-2)/9).toInt().coerceAtLeast(0)%routeSequence().size]))<90+kind.radius))return
            }
            g.meteors.add(Meteor(++eventId,x.coerceIn(kind.radius,360-kind.radius),y,kind,drift=drift,tag=tag,createdAt=g.time))
        }
        fun row(offset: Float=0f,tag: Int=0) {for(x in listOf(24f,62f,100f,138f,176f,214f,252f,290f,328f))rock(x, -34f-offset,tag=tag)}
        fun diagonal(reverse: Boolean=false) {rock(40f,drift=if(reverse)-30f else 30f);rock(320f,-160f,drift=if(reverse)30f else -30f)}
        fun twins() {rock(50f,kind=MeteorKind.MEDIUM);rock(50f,-160f);rock(310f,kind=MeteorKind.MEDIUM);rock(310f,-160f)}
        fun arc(tag: Int=0) {for(j in 0..5)rock(35+j*58f,-34f-j*j*7f,tag=tag)}
        fun drones() {for(x in listOf(40f,180f,320f))rock(x,drift=if(beat%2==0)20f else -20f,tag=2)}
        fun ion() {diagonal(beat%2==0);rock(180f,-220f,MeteorKind.MEDIUM,tag=3)}
        fun warning() {beams.add(WarningBeam(g.shipX,g.time,charge=id in listOf(39,72,87)));row(100f)}
        fun orbit() {rock(90f,kind=MeteorKind.LARGE,tag=4);rock(270f,-100f,tag=4)}
        fun escort() {
            rock(40f,tag=2);rock(320f,-80f,tag=2)
            if(beat%2==0)rock(allyX,-60f,MeteorKind.MEDIUM,protect=false)
            tokens.add(MissionToken(++eventId,allyX,350f,TokenKind.PIN))
        }
        fun defense() {
            rock(40f);rock(320f,-80f)
            val kind=if(beat%2==0)MeteorKind.LARGE else MeteorKind.MEDIUM
            rock(180f,560-kind.speed*(.8f+definition.chapter*.035f)*4,kind,protect=false)
            tokens.add(MissionToken(++eventId,180f,360f,TokenKind.PIN))
        }
        fun frost() {arc(1)}
        fun memory() {row(if(beat%2==0)0f else 100f)}
        fun boss() {when(coreCount%4){0->row();1->diagonal();2->drones();else->arc()}}
        when(id) {
            1 -> {rock(if(beat%2==0)40f else 320f)}
            2 -> row()
            3 -> {rock(40f);rock(320f,-150f)}
            4 -> {row();if(g.time>25)row(160f)}
            5 -> twins()
            6 -> {val side=if(beat%2==0)1 else -1;rock((safe+side*36).coerceIn(24f,336f),protect=false)}
            7 -> {row();if(beat==0||beat==3)g.items.add(Item(++eventId,safe,350f,ItemKind.SHIELD))}
            8 -> {diagonal();if(beat==0||beat==3)g.items.add(Item(++eventId,safe,350f,ItemKind.SLOW))}
            9 -> if(g.time<15)arc() else if(g.time<30)row() else twins()
            10 -> boss()
            11 -> {diagonal();diagonal(true)}
            12 -> {rock(40f);rock(160f,-120f);rock(280f,-240f)}
            13 -> {diagonal();rock(320f,-240f,drift=-36f)}
            14 -> row(if(beat%3==0)180f else 0f)
            15 -> twins()
            16 -> arc()
            17 -> {row();row(90f)}
            18 -> orbit()
            19 -> {row();rock(laneX((beat+1)%3),-200f,MeteorKind.LARGE)}
            20 -> {boss();if(beat%3==0)beams.add(WarningBeam(laneX(beat%3),g.time,60f))}
            21 -> ion()
            22 -> {row();rock(60f,-160f,tag=3)}
            23 -> {ion();rock(300f,-200f,tag=3)}
            24 -> memory()
            25 -> {row();if(beat%3==0)tokens.add(MissionToken(++eventId,360-safe,120f,TokenKind.PIN))}
            26 -> {arc();rock(180f,-220f,tag=3)}
            27 -> {diagonal();row(200f)}
            28 -> {diagonal(true);rock(180f,-200f)}
            29 -> {twins();row(280f)}
            30 -> {boss();ion()}
            31 -> escort()
            32 -> {arc();rock(180f,-250f)}
            33 -> defense()
            34 -> {if(g.time in 14.0..20.0 || g.time in 34.0..40.0)escort() else {rock(40f);rock(320f)}}
            35 -> {diagonal();if(beat%3==0)rock(360-allyX,-200f)}
            36 -> {defense();if(beat%3==0)tokens.add(MissionToken(++eventId,safe,250f,TokenKind.CHOICE_SHIELD))}
            37 -> {escort();if(beat%2==1){rock(secondAllyX,-60f,protect=false);tokens.add(MissionToken(++eventId,secondAllyX,350f,TokenKind.PIN))}}
            38 -> {if(beat%3<2)row()else arc()}
            39 -> warning()
            40 -> {boss();if(beat%2==0)escort()}
            41 -> row()
            42 -> twins()
            43 -> frost()
            44 -> {arc();if(beat%3==0)rock(60f,-200f,tag=1)}
            45 -> diagonal(beat%2==0)
            46 -> {arc(1);arc()}
            47 -> {diagonal(true);row(200f)}
            48 -> memory()
            49 -> {row();if(beat%3==0)beams.add(WarningBeam(laneX((beat+2)%3),g.time))}
            50 -> {if(coreCount<2)row()else frost()}
            51 -> row()
            52 -> memory()
            53 -> {drones();if(beat%3==0)warning()}
            54 -> {arc();rock(180f,-300f,tag=2)}
            55 -> {drones();row(240f)}
            56 -> {twins();rock(180f,-300f,tag=3)}
            57 -> {diagonal();arc()}
            58 -> when(beat%4){0->row();1->arc();2->diagonal();else->drones()}
            59 -> {drones();diagonal(true)}
            60 -> {boss();memory()}
            61 -> diagonal()
            62 -> row()
            63 -> {rock(40f,kind=MeteorKind.LARGE);rock(310f,-160f);rock(180f,-280f,tag=3)}
            64 -> diagonal(true)
            65 -> memory()
            66 -> {row();rock(safe+34f,-240f,protect=false)}
            67 -> escort()
            68 -> {ion();drones()}
            69 -> {memory();diagonal()}
            70 -> when(coreCount){0->diagonal();1->diagonal(true);2->row();else->ion()}
            71 -> {for(j in 0..4)rock(60+j*60f,-34f-abs(j-2)*70f,tag=if(j==2)8 else 2)}
            72 -> warning()
            73 -> {drones();rock(180f,-250f,MeteorKind.LARGE)}
            74 -> {row();if(beat%2==0)beams.add(WarningBeam(60f,g.time,28f,harmless=true))}
            75 -> drones()
            76 -> {escort();diagonal()}
            77 -> {arc();if(beat%2==0)arc(2)}
            78 -> {drones();ion()}
            79 -> when((g.time/15).toInt()%3){0->warning();1->drones();else->diagonal()}
            80 -> {boss();if(coreCount>=3)arc(2)}
            81 -> {row();beams.add(WarningBeam(360-safe,g.time,harmless=true))}
            82 -> twins()
            83 -> {arc(6)}
            84 -> {diagonal();rock(180f,-300f)}
            85 -> row()
            86 -> memory()
            87 -> {drones();row(180f)}
            88 -> {ion();rock(180f,-250f,tag=6)}
            89 -> escort()
            90 -> when(coreCount){0->warning();1->drones();2->arc(6);3->twins();else->memory()}
            91 -> when(route){0->row(100f);1->ion();else->{arc();rock(safe+34,-150f,protect=false)}}
            92 -> {row();if(beat%2==0)diagonal()}
            93 -> {escort();arc()}
            94 -> when((g.time/21).toInt()){0->row();1->diagonal();2->frost();else->ion()}
            95 -> {twins();drones()}
            96 -> if(route==0)row()else if(route==1)ion()else arc()
            97 -> defense()
            98 -> when((g.time/18).toInt()){0->diagonal();1->ion();2->frost();3->row();else->arc(6)}
            99 -> {memory();drones()}
            100 -> when(coreCount/2){0->{row();diagonal()};1->escort();else->{arc(6);drones()}}
        }
    }
    fun skillAchieved(g: GameEngine): Boolean = when(id) {
        1 -> guides>=3
        2 -> misses==0
        3 -> g.armor==g.maxArmor
        4 -> guides>=4
        5 -> nodeCount==2 && g.pulsesUsed==2
        6 -> g.bestCombo>=2
        7 -> shieldBlocks>=1 && g.armorHits==0
        8 -> pickedSlow>=2
        9 -> g.nearMisses>=3
        10 -> firstCores==2
        11 -> guides>=5
        12 -> g.pulsesUsed==0
        13 -> g.nearMisses>=4 && nearDrifts.containsAll(listOf(-1,1))
        14 -> misses==0
        15 -> g.nearMisses>=4 && nearKinds.size>=2
        16 -> cargoAtFirst
        17 -> maxPulseKills>=4
        18 -> !centralDestroyed
        19 -> cargoCount>=4
        20 -> 7 !in hitTags
        21 -> minimumEnergy>=30
        22 -> g.energy>=70
        23 -> earlyNodes>=3
        24 -> wrong==0
        25 -> pinGateCount>=3
        26 -> stationInput>=95
        27 -> g.pulsesUsed>=3
        28 -> cargoAtFirst
        29 -> !rescueUsed
        30 -> firstCores==3
        31 -> allyDamage==0
        32 -> g.time<40
        33 -> largeLeaks==0
        34 -> g.pulsesUsed<=3
        35 -> misses==0 && gateMaxOffset<=20
        36 -> stationArmor>=3
        37 -> allyArmor>=definition.allyArmor-1 && secondAllyArmor>=definition.allyArmor-1
        38 -> wrong==0
        39 -> lureCount>=4 && allyDamage==0
        40 -> allyArmor>=2
        41 -> boundaryTouches==0
        42 -> misses==0
        43 -> iceBeforeSplit>=3
        44 -> cargoAtFirst
        45 -> g.pulsesUsed==0
        46 -> guides>=3
        47 -> nodeCount>=2 && g.time<50
        48 -> guides>=6 && !hintsUsed
        49 -> misses==0 && boundaryTouches==0
        50 -> fractureHits==0
        51 -> nodeAttempts<=3
        52 -> !hintsUsed && wrong==0
        53 -> droneKills>=4
        54 -> wrong==0
        55 -> earlyNodes>=3
        56 -> wrong==0
        57 -> g.time<55
        58 -> noPulseCycle || g.pulsesUsed==0
        59 -> g.pulsesUsed==1
        60 -> wrong==0
        61 -> boundaryTouches==0
        62 -> misses==0
        63 -> maxPulseTypes>=3
        64 -> cargoAtFirst
        65 -> !hintsUsed
        66 -> g.nearMisses>=6 && g.pulsesUsed==0
        67 -> driftAllyHits==0
        68 -> pulsesWithHits>=4
        69 -> cargoAtFirst
        70 -> boundaryTouches==0
        71 -> centerKills>=3
        72 -> luresByLane.any{it>=6} && g.armorHits==0
        73 -> droneKills==0
        74 -> wrong==0
        75 -> maxPulseKills>=2 || g.pulsesUsed<nodeCount
        76 -> allyDamage==0
        77 -> guides>=4 && g.pulsesUsed==0
        78 -> g.pulsesUsed==2
        79 -> g.bestCombo>=6
        80 -> closedPulses==0
        81 -> guides>=6 && g.pulsesUsed==0
        82 -> misses==0
        83 -> rewoundNear.size>=4
        84 -> misses<=1
        85 -> cargoAtFirst
        86 -> !hintsUsed
        87 -> lureCount>=6 && g.armorHits==0
        88 -> g.pulsesUsed==3
        89 -> fastAllyHits==0
        90 -> closedPulses==0
        91 -> !rescueUsed
        92 -> choices.size==2
        93 -> allyDamage==0
        94 -> misses==0
        95 -> wrong==0
        96 -> !rescueUsed
        97 -> stationArmor>=4
        98 -> !hintsUsed && !rescueUsed
        99 -> g.time<85 && !rescueUsed
        100 -> !g.sentinelUsed && !rescueUsed
        else -> false
    }
}
