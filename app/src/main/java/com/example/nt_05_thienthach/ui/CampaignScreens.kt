package com.example.nt_05_thienthach.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.example.nt_05_thienthach.*
import com.example.nt_05_thienthach.R
import com.example.nt_05_thienthach.game.*

@Composable internal fun catalogText(key: String): String {
    val context=LocalContext.current
    val id=context.resources.getIdentifier(key,"string",context.packageName)
    return if(id!=0)stringResource(id)else key
}
@Composable internal fun CampaignMap(vm: GameViewModel)=MenuPage(vm) {
    val progress=vm.saved.progress
    val unlocked=CampaignCatalog.unlocked(progress.stages)
    var expanded by remember { mutableIntStateOf((unlocked-1)/10+1) }
    Eyebrow(R.string.campaign);BigTitle(R.string.stage_map)
    Text("★ ${CampaignCatalog.totalStars(progress.stages)}/300 · ${vm.selectedShip.name}",color=Orange)
    Action(R.string.campaign_continue,{vm.prepareStage(unlocked)},enabled=!vm.busy)
    for(chapter in 1..10) Sheet {
        TextButton(onClick={expanded=if(expanded==chapter)0 else chapter},modifier=Modifier.fillMaxWidth()) {
            Text("${stringResource(R.string.chapter)} $chapter · ${catalogText("chapter_%02d".format(java.util.Locale.ROOT,chapter))}",color=if(chapter<=(unlocked-1)/10+1)Cyan else Muted)
        }
        if(expanded==chapter) for(row in 0..1) Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)) {
            for(col in 1..5) {
                val id=(chapter-1)*10+row*5+col;val record=progress.stages[id]?:StageRecord();val locked=id>unlocked
                val description="${stringResource(R.string.stage)} $id · ${record.stars}/3 · "+stringResource(if(locked)R.string.stage_locked else if(record.cleared)R.string.stage_complete else R.string.stage_ready)
                OutlinedButton(onClick={vm.prepareStage(id)},enabled=!locked&&!vm.busy,
                    modifier=Modifier.weight(1f).heightIn(min=64.dp).semantics { contentDescription=description },contentPadding=PaddingValues(3.dp),shape=RoundedCornerShape(if(id%10==0)6.dp else 15.dp)) {
                    Column(horizontalAlignment=Alignment.CenterHorizontally) {
                        Text(if(locked)"🔒"else id.toString(),fontWeight=FontWeight.Bold)
                        Text("★".repeat(record.stars)+"☆".repeat(3-record.stars),fontSize=8.sp,color=Orange)
                    }
                }
            }
        }
    }
}
@Composable internal fun StagePreparation(vm: GameViewModel)=MenuPage(vm) {
    val id=vm.selectedStage?:1;val spec=CampaignCatalog.find(id)?:return@MenuPage
    val record=vm.saved.progress.stages[id]?:StageRecord()
    Eyebrow(R.string.stage_ready)
    Text("${stringResource(R.string.stage)} $id · ${catalogText("stage_%03d_name".format(java.util.Locale.ROOT,id))}",fontSize=27.sp,fontWeight=FontWeight.Black,color=Ink)
    Text("★".repeat(record.stars)+"☆".repeat(3-record.stars),color=Orange,fontSize=25.sp)
    Sheet {
        Eyebrow(R.string.stage_special);Text(catalogText("stage_%03d_special".format(java.util.Locale.ROOT,id)),color=Ink,lineHeight=23.sp)
        Eyebrow(R.string.stage_goal);Text(catalogText("stage_%03d_goal".format(java.util.Locale.ROOT,id)),color=Cyan)
        Text("${stringResource(R.string.time_left)}: ${spec.seconds}s",color=Muted)
        Text("${stringResource(R.string.stage_damage)} ${spec.damageLimit}",color=Muted)
        Text(stringResource(R.string.stage_skill)+": "+catalogText("stage_%03d_skill".format(java.util.Locale.ROOT,id)),color=Orange,lineHeight=22.sp)
        Text("${stringResource(R.string.stage_first)}: "+if(record.cleared)"✓"else spec.firstClearCredits.toString(),color=Muted)
    }
    ShipPortrait(Modifier.fillMaxWidth().height(120.dp),vm.selectedShip)
    Text(vm.selectedShip.name,color=Cyan,fontSize=22.sp)
    Text(catalogText("ship_${vm.selectedShip.id.lowercase()}_body"),color=Muted,lineHeight=22.sp)
    Action(R.string.ship_choose,vm::openFleet,primary=false)
    if(id in listOf(57,91,96)) Sheet {
        Eyebrow(R.string.route)
        listOf(R.string.route_safe,R.string.route_energy,R.string.route_risk).forEachIndexed { index,label ->
            TextButton(onClick={vm.selectRoute(index)}) {Text((if(vm.selectedRoute==index)"✓ "else "")+stringResource(label))}
        }
    }
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
        Column(Modifier.weight(1f)){Text(stringResource(R.string.practice),color=Ink);Text(stringResource(R.string.practice_note),color=Muted,fontSize=12.sp)}
        Switch(checked=vm.practice,onCheckedChange={vm.togglePractice()})
    }
    Action(R.string.stage_play,vm::start,enabled=!vm.busy)
}
@Composable internal fun Fleet(vm: GameViewModel)=MenuPage(vm) {
    Eyebrow(R.string.fleet);BigTitle(R.string.ship_choose)
    Stat(stringResource(R.string.wallet),vm.saved.progress.credits.toString(),color=Orange)
    for(ship in ShipCatalog.all) Sheet {
        ShipPortrait(Modifier.fillMaxWidth().height(110.dp),ship)
        Text(ship.name,color=Color(ship.color),fontSize=23.sp,fontWeight=FontWeight.Bold)
        Text(catalogText("ship_${ship.id.lowercase()}_body"),color=Muted,lineHeight=22.sp)
        Text("${stringResource(R.string.armor)} ${ship.armor}+${vm.saved.progress.upgrades.armor} · ${stringResource(R.string.ship_speed)} ${ship.speed.toInt()}",color=Ink,fontSize=12.sp)
        Text("${stringResource(R.string.ship_radius)} ${ship.radius.toInt()} · ${stringResource(R.string.ship_energy)} ${ship.recharge}",color=Muted,fontSize=12.sp)
        if(ship.id==vm.saved.progress.equippedShip) Text("✓ "+stringResource(R.string.ship_equipped),color=Cyan)
        else if(ship.id in vm.saved.progress.ownedShips) OutlinedButton(onClick={vm.equipShip(ship.id)},enabled=!vm.busy,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {Text(stringResource(R.string.ship_equip))}
        else Button(onClick={vm.buyShip(ship.id)},enabled=!vm.busy && vm.saved.progress.credits>=ship.price,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {
            Text(stringResource(if(vm.saved.progress.credits>=ship.price)R.string.ship_buy else R.string.not_enough,ship.price)+" · ${ship.price}")
        }
        if(ship.id=="S12" && ship.id in vm.saved.progress.ownedShips) {
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                AstraConfiguration.entries.forEach { config ->
                    OutlinedButton(onClick={vm.configureAstra(config)},enabled=!vm.busy,modifier=Modifier.weight(1f)) {
                        Text((if(vm.saved.progress.astra==config)"✓ "else "")+stringResource(if(config==AstraConfiguration.ARMOR)R.string.astra_armor else R.string.astra_pulse),fontSize=11.sp)
                    }
                }
            }
        }
    }
    Action(R.string.account_upgrades,{vm.navigate(Screen.UPGRADES)},primary=false)
}
@Composable internal fun CampaignObjectiveHud(vm: GameViewModel) {
    val hud=vm.campaignHud;val spec=CampaignCatalog.find(hud.id)?:return
    Column(Modifier.fillMaxWidth().background(Space.copy(alpha=.88f),RoundedCornerShape(10.dp)).padding(7.dp),verticalArrangement=Arrangement.spacedBy(3.dp)) {
        Text("${stringResource(R.string.stage)} ${hud.id} · ${hud.remaining}s"+if(hud.practice)" · ${stringResource(R.string.practice)}"else "",color=Cyan,fontSize=11.sp,fontWeight=FontWeight.Bold)
        val labels=buildList {
            if(spec.gates>0)add(stringResource(R.string.goal_gate)+" ${hud.gates}/${spec.gates}")
            if(spec.cargo>0)add(stringResource(R.string.cargo)+" ${hud.cargo}/${spec.cargo}")
            if(spec.nodes>0)add(stringResource(R.string.nodes)+" ${hud.nodes}/${spec.nodes}")
            if(spec.near>0)add(stringResource(R.string.stat_near)+" ${vm.campaignHud.near}/${spec.near}")
            if(spec.cores>0)add(stringResource(R.string.cores)+" ${hud.cores}/${spec.cores}")
            if(spec.allyArmor>0)add(stringResource(R.string.ally)+" ${hud.allyArmor}/${spec.allyArmor}"+if(hud.id==37)" · ${hud.secondAlly}"else "")
            if(spec.stationArmor>0)add(stringResource(R.string.station)+" ${hud.stationArmor}/${spec.stationArmor}")
        }
        Text(labels.joinToString(" · "),color=Ink,fontSize=10.sp)
        if(spec.cores>0)Text(stringResource(if(hud.bossOpen)R.string.boss_open else R.string.boss_closed),color=if(hud.bossOpen)Cyan else Orange,fontSize=9.sp)
        if(hud.id in listOf(48,52,60,65,86,98)) {
            Text(stringResource(R.string.memory)+": "+hud.memory,color=Orange,fontSize=11.sp)
            TextButton(onClick=vm::showMemory,contentPadding=PaddingValues(0.dp),modifier=Modifier.heightIn(min=48.dp)) {Text(stringResource(R.string.memory_hint),fontSize=10.sp)}
        }
    }
}
