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

import androidx.paging.PagingSource
import androidx.paging.PagingSource.LoadResult.Page.Companion.COUNT_UNDEFINED
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.arunkumar.compass.RealmQuery
import dev.arunkumar.compass.test.entity.Person
import dev.arunkumar.compass.test.rule.RealmDispatcherRule
import dev.arunkumar.compass.test.rule.RealmRule
import dev.arunkumar.compass.thread.RealmDispatcher
import io.realm.kotlin.where
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
public class RealmDataSourceTest {

  private val realmDispatcherRule: RealmDispatcherRule = RealmDispatcherRule()
  private val realmDispatcher: RealmDispatcher get() = realmDispatcherRule.dispatcher
  private val realmRule: RealmRule = RealmRule(100) { realmDispatcher }

  @get:Rule
  public val rules: TestRule = RuleChain
    .outerRule(realmDispatcherRule)
    .around(realmRule)

  private lateinit var pagingSource: PagingSource<Int, Person>

  @Before
  public fun setup() {
    val realmQueryBuilder = RealmQuery { where<Person>() }
    val factory = RealmTiledDataSource.Factory(realmQueryBuilder)
    pagingSource = factory.asPagingSourceFactory(realmDispatcher).invoke()
  }

  @Test
  public fun assertRealmPagingSourceLoadsResultForRefreshKey(): Unit = runBlocking {
    val loadResult = pagingSource.load(
      PagingSource.LoadParams.Refresh(
        key = null,
        loadSize = 10,
        placeholdersEnabled = false
      )
    )
    assertTrue("Page loaded successfully") { loadResult is PagingSource.LoadResult.Page }
    val loadedPage = loadResult as PagingSource.LoadResult.Page<Int, Person>
    assertTrue("Initial load is 10") { loadedPage.data.size == 10 }
    assertTrue("Previous key is null") { loadResult.prevKey == null }
    assertTrue("Next key is 10") { loadResult.nextKey == 10 }
    assertTrue("itemsBefore is 0") { loadResult.itemsBefore == 0 }
    assertTrue("itemsAfter is 90") { loadResult.itemsAfter == 90 }
  }

  @Test
  public fun assertRealmPagingSourceLoadsResultForAppendParam(): Unit = runBlocking {
    val loadResult = pagingSource.load(
      PagingSource.LoadParams.Append(
        key = 3,
        loadSize = 10,
        placeholdersEnabled = false
      )
    )
    assertTrue("Page loaded successfully") { loadResult is PagingSource.LoadResult.Page }
    val loadedPage = loadResult as PagingSource.LoadResult.Page<Int, Person>
    assertTrue("Initial load is 10") { loadedPage.data.size == 10 }
    assertTrue("Previous key is 3") { loadResult.prevKey == 3 }
    assertTrue("Next key is 13") { loadResult.nextKey == 13 }

    // TODO Try to return accurate before/after counts since we know it
    assertTrue("itemsBefore is 0") { loadResult.itemsBefore == COUNT_UNDEFINED }
    assertTrue("itemsAfter is 90") { loadResult.itemsAfter == COUNT_UNDEFINED }
  }

  @Test
  public fun assertRealmPagingSourceLoadsResultForPrependParam(): Unit = runBlocking {
    val loadResult = pagingSource.load(
      PagingSource.LoadParams.Prepend(
        key = 2,
        loadSize = 10,
        placeholdersEnabled = false
      )
    )
    println(loadResult)
    assertTrue("Page loaded successfully") { loadResult is PagingSource.LoadResult.Page }
    val loadedPage = loadResult as PagingSource.LoadResult.Page<Int, Person>
    assertTrue("Initial load is 2") { loadedPage.data.size == 2 }
    assertTrue("Previous key is null") { loadResult.prevKey == null }
    assertTrue("Next key is 2") { loadResult.nextKey == 2 }

    // TODO Try to return accurate before/after counts since we know it
    assertTrue("itemsBefore is undefined") { loadResult.itemsBefore == COUNT_UNDEFINED }
    assertTrue("itemsAfter is undefined") { loadResult.itemsAfter == COUNT_UNDEFINED }
  }
}
