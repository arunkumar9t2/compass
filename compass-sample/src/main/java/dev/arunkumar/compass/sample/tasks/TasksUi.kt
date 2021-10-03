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

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import dev.arunkumar.compass.sample.data.tasks.Task
import kotlinx.coroutines.flow.Flow
import java.util.*

@Composable
public fun Compass(
  state: TasksState,
  tasksViewModel: TasksViewModel
) {
  val fabShape = MaterialTheme.shapes.small.copy(CornerSize(percent = 50))
  Scaffold(
    scaffoldState = rememberScaffoldState(),
    topBar = {
    },
    isFloatingActionButtonDocked = true,
    floatingActionButton = {
      FloatingActionButton(
        onClick = { tasksViewModel.perform(UiAction.ClearTasks) },
        shape = fabShape,
        content = {
          Icon(Icons.Default.ClearAll, contentDescription = "Clear tasks")
        }
      )
    },
    floatingActionButtonPosition = FabPosition.End,
    bottomBar = {
      BottomAppBar(
        modifier = Modifier.animateContentSize(),
        cutoutShape = fabShape
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.End,
          modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
        ) {
        }
      }
    },
    content = { innerPadding ->
      TasksList(state.tasks, innerPadding, taskContent = { task ->
        if (task != null) {
          TaskItem(
            task = task,
            deleteTask = {
              // tasksViewModel.perform(UiAction.DeleteTask(it))
            },
          )
        }
      })
    }
  )
}


@Composable
public fun TasksList(
  tasks: Flow<PagingData<Task>>,
  contentPadding: PaddingValues,
  taskContent: @Composable (task: Task?) -> Unit,
  modifier: Modifier = Modifier,
) {
  val items = tasks.collectAsLazyPagingItems()
  LazyColumn(
    modifier = modifier.padding(contentPadding),
  ) {
    items(
      items = items,
      key = { task -> task.id.toString() }
    ) { task -> taskContent(task) }
  }
}

@Composable
public fun TaskItem(
  task: Task,
  deleteTask: (taskId: UUID) -> Unit,
  modifier: Modifier = Modifier
) {
  Card(modifier = modifier
    .fillMaxWidth()
    .padding(start = 12.dp, end = 12.dp, top = 12.dp)
    .clickable { deleteTask(task.id) }
  ) {
    Row(
      modifier = Modifier.padding(8.dp)
    ) {
      Column {
        Text(
          text = task.name,
          style = MaterialTheme.typography.body1
        )
      }
    }
  }
}
