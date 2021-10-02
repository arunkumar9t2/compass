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
)

public sealed class UiAction {
  public object LoadTasks : UiAction()
  public object ClearTasks : UiAction()
}

public class TasksViewModel(
  private val tasksRepository: TaskRepository = TaskRepository(),
  private val dispatchers: DispatcherProvider = DefaultDispatcherProvider()
) : ViewModel() {
  private val reducerDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

  private val actions = MutableSharedFlow<UiAction>()
  private val actionsFlow = actions.asSharedFlow()
  public fun perform(action: UiAction) {
    viewModelScope.launch { actions.emit(action) }
  }

  private val loadTasks: Flow<Reducer> = onAction<UiAction.LoadTasks>()
    .onStart { emit(UiAction.LoadTasks) }
    .debounce(300)
    .mapLatest {
      tasksRepository
        .tasks { sort(Task.NAME) }
        .cachedIn(viewModelScope)
    }.flowOn(dispatchers.io)
    .map { tasks -> Reducer { copy(tasks = tasks) } }

  public val state: StateFlow<TasksState> = merge(
    loadTasks
  ).scan(TasksState()) { state, reducer -> reducer(state) }
    .flowOn(reducerDispatcher)
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(10000),
      initialValue = TasksState()
    )

  private inline fun <reified Action : UiAction> onAction() = actionsFlow.filterIsInstance<Action>()

  override fun onCleared() {
    reducerDispatcher.close()
  }
}

private typealias Reducer = TasksState.() -> TasksState

private fun Reducer(builder: TasksState.() -> TasksState): Reducer = builder
