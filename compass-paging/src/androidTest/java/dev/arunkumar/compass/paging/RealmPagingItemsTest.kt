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

package dev.arunkumar.compass.paging

import androidx.paging.AsyncPagingDataDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import dev.arunkumar.compass.RealmQuery
import dev.arunkumar.compass.paging.util.NoOpListCallBack
import dev.arunkumar.compass.test.entity.Person
import dev.arunkumar.compass.test.rule.RealmDispatcherRule
import dev.arunkumar.compass.test.rule.RealmRule
import dev.arunkumar.compass.thread.RealmDispatcher
import io.realm.kotlin.where
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@Ignore("To figure out testing pagingItems")
public class RealmPagingItemsTest {

  private val realmDispatcherRule: RealmDispatcherRule = RealmDispatcherRule()
  private val realmDispatcher: RealmDispatcher get() = realmDispatcherRule.dispatcher
  private val realmRule: RealmRule = RealmRule(100) { realmDispatcher }

  @get:Rule
  public val rules: TestRule = RuleChain
    .outerRule(realmDispatcherRule)
    .around(realmRule)

  @Test
  public fun assertPagingItemsEmitsChangesInResponseToDataChanges(): Unit = runBlocking {
    val pagingItems = RealmQuery { where<Person>() }.asPagingItems()
    val differ = AsyncPagingDataDiffer(
      diffCallback = PersonDiffCallback,
      updateCallback = NoOpListCallBack,
      workerDispatcher = realmDispatcher
    )
    pagingItems.test {
      val pagingData = awaitItem()
      println("Items emitted $pagingData")

      launch(Dispatchers.Main) {
        differ.submitData(pagingData)
      }
      delay(1000)
      println("Differ snapshot ${differ.snapshot().items}")
      cancelAndIgnoreRemainingEvents()
    }
  }

  public companion object {
    private val PersonDiffCallback = object : DiffUtil.ItemCallback<Person>() {
      override fun areItemsTheSame(
        oldItem: Person,
        newItem: Person
      ): Boolean = oldItem.id == newItem.id

      override fun areContentsTheSame(
        oldItem: Person,
        newItem: Person
      ): Boolean = oldItem.id == newItem.id && oldItem.name == oldItem.name
    }
  }
}
