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

package dev.arunkumar.compass.test.rule

import androidx.test.core.app.ApplicationProvider
import dev.arunkumar.compass.DefaultRealm
import dev.arunkumar.compass.test.entity.Person
import dev.arunkumar.compass.transact
import io.realm.Realm
import io.realm.RealmConfiguration
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.rules.ExternalResource

public class RealmRule(
  private val noOfInitialItems: Int = 30,
  private val dispatcherProvider: () -> CoroutineDispatcher = { Dispatchers.IO },
) : ExternalResource() {

  private val dispatcher get() = dispatcherProvider()
  private lateinit var realm: Realm

  override fun before(): Unit = runBlocking {
    withContext(dispatcher) {
      Realm.init(ApplicationProvider.getApplicationContext())
      val realmConfiguration = RealmConfiguration.Builder()
        .deleteRealmIfMigrationNeeded()
        .allowQueriesOnUiThread(false)
        .allowWritesOnUiThread(false)
        .initialData { realm ->
          realm.insertOrUpdate((1..noOfInitialItems).map { Person() })
        }.build()
      Realm.setDefaultConfiguration(realmConfiguration)
      realm = DefaultRealm()
    }
  }

  override fun after(): Unit = runBlocking {
    withContext(dispatcher) {
      realm.transact { deleteAll() }
      realm.close()
    }
  }
}
