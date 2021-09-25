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
import dev.arunkumar.compass.test.entity.Person
import dev.arunkumar.compass.test.rule.RealmDispatcherRule
import dev.arunkumar.compass.test.rule.RealmRule
import dev.arunkumar.compass.thread.RealmDispatcher
import io.realm.kotlin.where
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
public class RealmQueryTest {
  private val realmDispatcherRule: RealmDispatcherRule = RealmDispatcherRule()
  private val realmDispatcher: RealmDispatcher get() = realmDispatcherRule.dispatcher
  private val realmRule: RealmRule = RealmRule { realmDispatcher }

  @get:Rule
  public val rules: TestRule = RuleChain
    .outerRule(realmDispatcherRule)
    .around(realmRule)

  @Test
  public fun assertGetAllReturnsLoadedResults(): Unit = runBlocking {
    withContext(realmDispatcher) {
      val persons = RealmQuery { where<Person>() }.getAll()
      assertTrue("Assert getAll returns loaded results") { persons.size == 30 }
      assertTrue("getAll returns with values populated") {
        val person = persons.first()
        person.name.isNotEmpty() && person.id.toString().isNotEmpty()
      }
    }
  }
}
