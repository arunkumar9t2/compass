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
import dev.arunkumar.compass.rule.RealmDispatcherRule
import dev.arunkumar.compass.rule.RealmRule
import dev.arunkumar.compass.thread.RealmDispatcher
import io.realm.Realm
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
public class RealmExecutorTest {

  @get:Rule
  public val realmRule: RealmRule = RealmRule()

  @get:Rule
  public val realmDispatcherRule: RealmDispatcherRule = RealmDispatcherRule()

  private val realm: Realm get() = realmRule.realm
  private val realmDispatcher: RealmDispatcher get() = realmDispatcherRule.dispatcher

  @Test
  public fun assertChangeListenerAddedOnRealmDispatcherPasses(): Unit = runBlocking {
    withContext(realmDispatcher) {

    }
  }
}
