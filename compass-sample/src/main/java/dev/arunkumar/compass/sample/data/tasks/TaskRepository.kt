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

package dev.arunkumar.compass.sample.data.tasks

import androidx.paging.PagingData
import dev.arunkumar.compass.RealmQuery
import dev.arunkumar.compass.RealmTransaction
import dev.arunkumar.compass.paging.asPagingItems
import io.realm.RealmQuery
import io.realm.kotlin.where
import kotlinx.coroutines.flow.Flow

public class TaskRepository {

  public fun tasks(query: RealmQuery<Task>.() -> Unit): Flow<PagingData<Task>> {
    addTasksIfEmpty()
    return RealmQuery { where<Task>().apply(query) }.asPagingItems()
  }

  private fun addTasksIfEmpty() {
    RealmTransaction {
      if (where<Task>().findAll().isEmpty()) {
        copyToRealmOrUpdate((0..3000).map { Task() })
      }
    }
  }
}
