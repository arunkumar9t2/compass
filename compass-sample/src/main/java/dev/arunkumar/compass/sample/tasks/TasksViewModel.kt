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

package dev.arunkumar.compass.sample.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dev.arunkumar.compass.sample.common.DefaultDispatcherProvider
import dev.arunkumar.compass.sample.common.DispatcherProvider
import dev.arunkumar.compass.sample.data.tasks.Task
import dev.arunkumar.compass.sample.data.tasks.TaskRepository
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

public data class TasksState(
  val tasks: Flow<PagingData<Task>> = flowOf(PagingData.empty()),
  val sort: Sort = Sort.ASC
)

public enum class Sort { ASC, DESC }

public sealed class UiAction {
  public data class LoadTasks(val sort: Sort = Sort.ASC) : UiAction()
  public object ClearTasks : UiAction()
}

public class TasksViewModel(
  private val tasksRepository: TaskRepository = TaskRepository(),
  dispatchers: DispatcherProvider = DefaultDispatcherProvider(),
  initialState: TasksState = TasksState(),
) : ViewModel() {
  private val reducerDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

  private val actions = MutableSharedFlow<UiAction>()
  private val actionsFlow = actions.asSharedFlow()
  public fun perform(action: UiAction) {
    viewModelScope.launch { actions.emit(action) }
  }

  private val loadTasks: Flow<Reducer> = onAction<UiAction.LoadTasks>()
    .onStart { emit(UiAction.LoadTasks()) }
    .debounce(300)
    .mapLatest {
      val dbSort = if (it.sort == Sort.ASC) io.realm.Sort.ASCENDING else io.realm.Sort.DESCENDING
      it.sort to tasksRepository
        .tasks { sort(Task.NAME, dbSort) }
        .cachedIn(viewModelScope)
    }.flowOn(dispatchers.io)
    .map { (sort, tasks) -> Reducer { copy(tasks = tasks, sort = sort) } }
    .share()

  private val clearTasks: Flow<Reducer> = onAction<UiAction.ClearTasks>()
    .debounce(300)
    .onEach { tasksRepository.clear() }
    .flowOn(dispatchers.io)
    .map { Reducer { this } }
    .share()

  public val state: StateFlow<TasksState> = merge(
    loadTasks,
    clearTasks
  ).scan(initialState) { state, reducer -> reducer(state) }
    .flowOn(reducerDispatcher)
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(10000),
      initialValue = initialState
    )

  private inline fun <reified Action : UiAction> onAction() = actionsFlow.filterIsInstance<Action>()
  private fun <T> Flow<T>.share() = shareIn(viewModelScope, SharingStarted.WhileSubscribed())

  override fun onCleared() {
    reducerDispatcher.close()
  }
}

private typealias Reducer = TasksState.() -> TasksState

private fun Reducer(builder: TasksState.() -> TasksState): Reducer = builder
