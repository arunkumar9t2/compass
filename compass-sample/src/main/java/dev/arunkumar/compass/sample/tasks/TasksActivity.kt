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

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.arunkumar.compass.sample.common.rememberFlowWithLifecycle
import dev.arunkumar.compass.sample.common.theme.CompassTheme

public class TasksActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      CompassTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colors.background
        ) {
          val viewModel = viewModel<TasksViewModel>()
          val state by rememberFlowWithLifecycle(flow = viewModel.state)
            .collectAsState(initial = viewModel.state.value)
          Tasks(state, viewModel)
        }
      }
    }
  }
}

