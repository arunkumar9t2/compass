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

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import dev.arunkumar.compass.RealmCopyTransform
import dev.arunkumar.compass.RealmModelTransform
import dev.arunkumar.compass.RealmQueryBuilder
import dev.arunkumar.compass.thread.RealmDispatcher
import io.realm.RealmModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion

public val DefaultPagingConfig: PagingConfig = PagingConfig(
  pageSize = 20,
  prefetchDistance = 20 * 3,
  initialLoadSize = 20 * 3,
  enablePlaceholders = false,
)

public fun <T : RealmModel> RealmQueryBuilder<T>.asPagingItems(
  tag: String = "PagingItemsExecutor",
  pagingConfig: PagingConfig = DefaultPagingConfig
): Flow<PagingData<T>> {
  return asPagingItems(
    tag = tag,
    pagingConfig = pagingConfig,
    realmModelTransform = RealmCopyTransform()
  )
}

public fun <T : RealmModel, R : Any> RealmQueryBuilder<T>.asPagingItems(
  tag: String = "PagingItemsExecutor",
  pagingConfig: PagingConfig = DefaultPagingConfig,
  realmModelTransform: RealmModelTransform<T, R>
): Flow<PagingData<R>> {
  val realmQueryBuilder = this
  return flow {
    emit(RealmDispatcher(tag))
  }.flatMapConcat { dispatcher ->
    val factory = RealmTiledDataSource.Factory(
      realmQueryBuilder,
      realmModelTransform
    )
    val pagingSourceFactory = factory.asPagingSourceFactory(dispatcher)
    Pager(
      config = pagingConfig,
      initialKey = 0,
      pagingSourceFactory = pagingSourceFactory
    ).flow.onCompletion { dispatcher.close() }
  }
}
