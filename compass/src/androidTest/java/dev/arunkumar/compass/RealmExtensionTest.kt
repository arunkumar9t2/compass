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
import dev.arunkumar.compass.entity.Person
import dev.arunkumar.compass.rule.RealmRule
import io.realm.kotlin.where
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
public class RealmExtensionTest {
  @get:Rule
  public val reamRule: RealmRule = RealmRule()

  @Test
  public fun assertToRealmListReturnsUnManagedRealmTest() {
    val persons = (0..3).map { Person() }.toRealmList()
    assertFalse("toRealmList returns non managed instances") { persons.isManaged }
  }

  @Test
  public fun assertTransactExtensionPerformsTransactionAndReturns() {
    RealmTransaction {
      val person = Person()
      copyToRealm(person)
      val results = where<Person>().equalTo("id", person.id).findAll()
      assertTrue("New person obj was inserted") {
        results.isNotEmpty()
      }
      assertTrue("Inserted value is the same") {
        val insertedPerson = results.first()!!
        insertedPerson.name == person.name && insertedPerson.id == insertedPerson.id
      }
    }
  }
}
