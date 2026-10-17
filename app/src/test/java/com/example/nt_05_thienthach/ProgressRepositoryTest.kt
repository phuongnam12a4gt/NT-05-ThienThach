package com.example.nt_05_thienthach

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.example.nt_05_thienthach.data.*
import com.example.nt_05_thienthach.game.*
import java.io.File
import java.io.IOException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ProgressRepositoryTest {
    @get:Rule val temporary = TemporaryFolder()
    private fun store(file: File, scope: CoroutineScope) = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
    private suspend fun <T> withRepository(test: suspend (ProgressRepository, DataStore<Preferences>) -> T): T {
        val file = File(temporary.newFolder(), "game.preferences_pb")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try { val store = store(file, scope); return test(ProgressRepository(store), store) }
        finally { scope.coroutineContext[Job]!!.cancelAndJoin() }
    }
    @Test fun missingKeysUseDefaults() = runBlocking {
        withRepository { repository, _ -> assertEquals(SavedData(), repository.data.first()) }
    }
    @Test fun invalidNumbersAreClamped() = runBlocking {
        withRepository { repository, store ->
            store.edit { p -> p[longPreferencesKey("credits")] = -10; p[intPreferencesKey("armorUpgradeLevel")] = 999; p[intPreferencesKey("slowUpgradeLevel")] = -1 }
            val progress = repository.data.first().progress
            assertEquals(0L, progress.credits); assertEquals(2, progress.upgrades.armor); assertEquals(0, progress.upgrades.slow)
        }
    }
    @Test fun futureSchemaRefusesReadAndWriteWithoutDeleting() = runBlocking {
        withRepository { repository, store ->
            val key = intPreferencesKey("schemaVersion")
            store.edit { it[key] = 99 }
            try { repository.data.first(); fail("Future schema must not load") } catch (_: IllegalStateException) { }
            try { repository.reset(); fail("Future schema must not reset") } catch (_: IllegalStateException) { }
            assertEquals(99, store.data.first()[key])
        }
    }
    @Test fun concurrentRewardCallsOnlyRewardOnce() = runBlocking {
        withRepository { repository, _ ->
            coroutineScope { repeat(30) { launch { repository.reward(RunResult("run", 1000, 100000, 0)) } } }
            val p = repository.data.first().progress
            assertEquals(50L, p.credits); assertEquals(1L, p.totalRuns)
        }
    }
    @Test fun repeatedPurchasesCannotOverspend() = runBlocking {
        withRepository { repository, _ ->
            repository.reward(RunResult("fund", 2000, 200000, 0))
            coroutineScope { repeat(20) { launch { runCatching { repository.buy(UpgradeKind.SHIELD) } } } }
            val p = repository.data.first().progress
            assertEquals(0L, p.credits); assertEquals(1, p.upgrades.shield)
        }
    }
    @Test fun resetPreservesSettingsAndClearsTutorialAndProgress() = runBlocking {
        withRepository { repository, _ ->
            val options = Settings(music = false, sound = false, vibration = false, reducedMotion = true)
            repository.settings(options); repository.completeTutorial()
            repository.reward(RunResult("fund", 20000, 2000000, 0)); repository.buy(UpgradeKind.ARMOR)
            val after = repository.reset()
            assertEquals(Progress(), after.progress); assertEquals(options, after.settings)
        }
    }
    @Test fun dataSurvivesClosingAndReopeningStore() = runBlocking {
        val file = File(temporary.newFolder(), "game.preferences_pb")
        val firstScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val repository = ProgressRepository(store(file, firstScope))
        repository.reward(RunResult("persist", 6000, 600000, 0))
        val expected = repository.buy(UpgradeKind.ARMOR)
        firstScope.coroutineContext[Job]!!.cancelAndJoin()
        val secondScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try { assertEquals(expected, ProgressRepository(store(file, secondScope)).data.first()) }
        finally { secondScope.coroutineContext[Job]!!.cancelAndJoin() }
    }
    @Test fun failedSaveCanRetryWithoutDoubleReward() = runBlocking {
        val state = MutableStateFlow<Preferences>(emptyPreferences())
        var failWrite = true
        val fake = object : DataStore<Preferences> {
            override val data: Flow<Preferences> = state
            override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
                if (failWrite) throw IOException("Simulated full disk")
                return transform(state.value).also { state.value = it }
            }
        }
        val repository = ProgressRepository(fake)
        val result = RunResult("retry", 1000, 100000, 0)
        try { repository.reward(result); fail("Save should fail") } catch (_: IOException) { }
        assertEquals(0L, repository.data.first().progress.credits)
        failWrite = false
        repository.reward(result); repository.reward(result)
        assertEquals(50L, repository.data.first().progress.credits)
        assertEquals(1L, repository.data.first().progress.totalRuns)
    }
    @Test fun schemaOneMigratesWithoutChangingWalletRecordsOrSettings() = runBlocking {
        withRepository { repository, store ->
            store.edit {
                it[intPreferencesKey("schemaVersion")]=1
                it[longPreferencesKey("credits")]=1234L
                it[longPreferencesKey("bestScore")]=9876L
                it[intPreferencesKey("armorUpgradeLevel")]=2
                it[booleanPreferencesKey("musicEnabled")]=false
            }
            val before=repository.data.first()
            assertEquals(1234L,before.progress.credits)
            assertEquals(setOf("S01"),before.progress.ownedShips)
            assertTrue(before.progress.stages.isEmpty())
            val after=repository.equipShip("S01")
            assertEquals(before,after)
            assertEquals(2,store.data.first()[intPreferencesKey("schemaVersion")])
        }
    }
    @Test fun concurrentShipPurchaseAndCampaignRewardRemainAtomic() = runBlocking {
        withRepository { repository, _ ->
            repository.reward(RunResult("fundships",10000,100000,0))
            val victory=RunResult("stagefirst",0,1000,0,stageId=1,victory=true,achievementBits=7)
            coroutineScope { repeat(20) { launch { repository.reward(victory) };launch { runCatching { repository.buyShip("S02") } } } }
            val p=repository.data.first().progress
            assertEquals(setOf("S01","S02"),p.ownedShips)
            assertEquals(500L-250+56+35,p.credits)
            assertEquals(7,p.stages[1]?.bits)
            assertEquals(2L,p.totalRuns)
            val equipped=repository.equipShip("S02")
            assertEquals("S02",equipped.progress.equippedShip)
            val reset=repository.reset()
            assertEquals(Progress(),reset.progress)
        }
    }
    @Test fun fleetAndCampaignPersistWhenStoreReopens() = runBlocking {
        val file=File(temporary.newFolder(),"campaign.preferences_pb")
        val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
        val repo=ProgressRepository(store(file,scope))
        repo.reward(RunResult("fleetfund",10000,100000,0))
        repo.buyShip("S02");repo.equipShip("S02")
        val expected=repo.reward(RunResult("campaignpersist",50,1000,0,stageId=1,victory=true,achievementBits=5))
        scope.coroutineContext[Job]!!.cancelAndJoin()
        val reopened=CoroutineScope(SupervisorJob()+Dispatchers.IO)
        try { assertEquals(expected,ProgressRepository(store(file,reopened)).data.first()) }
        finally { reopened.coroutineContext[Job]!!.cancelAndJoin() }
    }
}
