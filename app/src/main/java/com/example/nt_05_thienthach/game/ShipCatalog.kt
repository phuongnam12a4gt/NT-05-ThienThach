package com.example.nt_05_thienthach.game

data class ShipDefinition(val id: String, val name: String, val price: Long, val armor: Int,
    val radius: Float, val speed: Float, val recharge: Double, val color: Long)
object ShipCatalog {
    val all = listOf(
        ShipDefinition("S01", "Nova–01", 0, 3, 12f, 720f, 3.0, 0xFF6DEBDE),
        ShipDefinition("S02", "Swift", 250, 2, 10f, 900f, 3.0, 0xFF75BBFF),
        ShipDefinition("S03", "Bulwark", 650, 5, 15f, 600f, 2.5, 0xFFFFB276),
        ShipDefinition("S04", "Scavenger", 1200, 3, 13f, 680f, 3.0, 0xFFF3D96B),
        ShipDefinition("S05", "Volt", 2000, 2, 11f, 780f, 4.2, 0xFFFFE66A),
        ShipDefinition("S06", "Guardian", 3000, 4, 14f, 640f, 2.8, 0xFF8EE8FF),
        ShipDefinition("S07", "Wraith", 4300, 2, 9f, 820f, 3.0, 0xFFBF9FFF),
        ShipDefinition("S08", "Medic", 5900, 3, 12f, 700f, 2.8, 0xFF8AF2B5),
        ShipDefinition("S09", "Tempest", 7800, 3, 12f, 760f, 3.0, 0xFFFF8AB7),
        ShipDefinition("S10", "Sentinel", 10000, 4, 14f, 620f, 2.4, 0xFFB7C8E4),
        ShipDefinition("S11", "Aurora", 12800, 3, 11f, 740f, 3.4, 0xFF88FFEB),
        ShipDefinition("S12", "Astra", 16000, 3, 12f, 760f, 3.1, 0xFFFFD6FF))
    val nova get() = all.first()
    fun find(id: String) = all.firstOrNull { it.id == id }
}
enum class AstraConfiguration { ARMOR, PULSE }
