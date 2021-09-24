/*
 * Copyright 2021 Arunkumar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.arunkumar.compass

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import dev.arunkumar.compass.entity.Person
import dev.arunkumar.compass.rule.RealmDispatcherRule
import dev.arunkumar.compass.rule.RealmRule
import dev.arunkumar.compass.thread.RealmDispatcher
import io.realm.kotlin.where
import kotlinx.coroutines.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
public class RealmFlowTest {

  private val realmDispatcherRule: RealmDispatcherRule = RealmDispatcherRule()
  private val realmDispatcher: RealmDispatcher get() = realmDispatcherRule.dispatcher
  private val realmRule: RealmRule = RealmRule { realmDispatcher }

  @get:Rule
  public val rules: TestRule = RuleChain
    .outerRule(realmDispatcherRule)
    .around(realmRule)

  @Test
  public fun assertFlowEmitsChangesOnCollectorCoroutine(): Unit = runBlocking {
    val personsFlow = RealmQuery { where<Person>() }.asFlow()
    launch {
      delay(200)
      Realm { it.transact { copyToRealm(Person(name = "NewItem")) } }
    }
    personsFlow.test {
      val firstPersonsEmission = awaitItem()
      assertTrue("First emission is a snapshot of Realm") {
        firstPersonsEmission.size == 30
      }
      assertTrue("Emitted item can be safely passed around threads") {
        withContext(Dispatchers.IO) {
          firstPersonsEmission[0]
        }
        true
      }
      val secondPersonsEmissions = awaitItem()
      assertTrue("Second emission contains added items") {
        secondPersonsEmissions.size == 31
      }
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  public fun assertAbilityToUseTransformsToReadSubsetOfRealmData(): Unit = runBlocking {
    data class PersonMapped(val id: String, val name: String)

    val personsFlow = RealmQuery { where<Person>() }
      .asFlow { it -> PersonMapped(it.id.toString(), it.name) }

    personsFlow.test {
      val firsEmission = awaitItem()
      firsEmission.forEach { personName ->
        assertTrue("Mapped data was returned in the flow") {
          personName.id.contains(personName.name)
        }
      }
      cancelAndIgnoreRemainingEvents()
    }
  }
}
