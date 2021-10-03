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

package dev.arunkumar.compass.paging;

import androidx.paging.DataSource
import dev.arunkumar.compass.DefaultRealm
import dev.arunkumar.compass.RealmCopyTransform
import dev.arunkumar.compass.RealmModelTransform
import dev.arunkumar.compass.RealmQueryBuilder
import io.realm.RealmModel
import io.realm.RealmResults

/**
 * Paging [DataSource] implementation that uses [RealmQuery] produced
 * by [realmQueryBuilder] to fetch matching results and [transform] to
 * transform the source [RealmModel] to any custom type represented by
 * `R`.
 *
 * Implementation notes: The data source relies on [TiledDataSource]
 * callbacks [TiledDataSource.loadRange] to load [RealmResults]
 * produced by the query at any position. In order to confirm to
 * [Realm]'s threading model, [RealmDispatcher] is expected to be
 * used when converting this [DataSource] to Paging 3 factory using
 * [DataSource.Factory.asPagingSourceFactory]. Thread safety/stability
 * is not guaranteed when any other dispatcher is passed.
 *
 * [loadRange] is called on the provided Dispatcher thread and
 * [RealmResults] are immediately cached. Further load range requests
 * simply copy the results from previously fetched [RealmResults].
 * Changes are observed on the [RealmResults] and upon any change,
 * the data source is invalidated to force initial loading on
 * [RealmResults].
 *
 * Thread safety for further transformation of fetched [RealmResults]
 * is guaranteed only when provided [transform] returns non-managed
 * instances. For sample implementation, see [RealmCopyTransform]
 *
 * For a safe default, consider using [asPagingItems] which manages
 * dispatcher lifecyle and transforms
 *
 * @see asPagingItems
 * @param realmQueryBuilder The builder function that will be used to
 *     construct [RealmQuery] instanced. Will be called on loading thread.
 * @param transform The transformation function that will be used to map
 *     live managed [RealmModel] instances to type `R`. The `R` should
 *     be unmanaged [RealmModel] objects or custom types. If managed
 *     instances are returned, then thread safety is not guaranteed.
 */
public class RealmTiledDataSource<T : RealmModel, R : Any> internal constructor(
  private val realmQueryBuilder: RealmQueryBuilder<T>,
  private val transform: RealmModelTransform<T, R>
) : TiledDataSource<R>() {

  internal class NoOpTransformFactory<T : RealmModel>(
    private val realmQueryBuilder: RealmQueryBuilder<T>,
    private val transform: RealmModelTransform<T, T> = RealmCopyTransform()
  ) : DataSource.Factory<Int, T>() {
    override fun create(): RealmTiledDataSource<T, T> = RealmTiledDataSource(
      realmQueryBuilder,
      transform
    )
  }

  /**
   * [DataSource.Factory] implementation to construct
   * [RealmTiledDataSource]
   *
   * @param realmQueryBuilder The builder function that will be used to
   *     construct [RealmQuery] instance. Will be called on loading thread.
   * @param transform The transformation function that will be used to
   *     map live managed [RealmModel] instances to type
   *     `R`. The `R` should be unmanaged [RealmModel]
   *     objects or custom types. If managed instances are
   *     returned, then thread safety is not guaranteed.
   */
  public class Factory<T : RealmModel, R : Any>(
    private val realmQueryBuilder: RealmQueryBuilder<T>,
    private val transform: RealmModelTransform<T, R>
  ) : DataSource.Factory<Int, R>() {
    override fun create(): RealmTiledDataSource<T, R> = RealmTiledDataSource(
      realmQueryBuilder,
      transform
    )
  }

  private val realm by lazy { DefaultRealm() }
  private val realmQuery by lazy { realm.realmQueryBuilder() }
  private val realmChangeListener = { _: RealmResults<T> -> invalidate() }
  private val realmResults by lazy {
    realmQuery.findAll().apply {
      addChangeListener(realmChangeListener)
    }
  }

  init {
    addInvalidatedCallback {
      if (realmResults.isValid) {
        realmResults.removeChangeListener(realmChangeListener)
      }
      realm.close()
    }
  }

  override fun countItems(): Int = when {
    realm.isClosed || !realmResults.isValid -> 0
    else -> realmResults.size
  }

  override fun loadRange(startPosition: Int, count: Int): List<R> {
    val size = countItems()
    if (size == 0) return emptyList()
    return buildList {
      val endPosition = minOf(startPosition + count, size)
      for (position in startPosition until endPosition) {
        realmResults[position]?.let { item ->
          add(realm.transform(item))
        }
      }
    }
  }
}
