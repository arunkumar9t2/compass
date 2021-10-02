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

import io.realm.RealmObject
import java.util.*

public open class Task(
  public var id: UUID = UUID.randomUUID(),
  public var name: String = id.toString().substring(0, 6)
) : RealmObject() {
  public companion object {
    public const val ID: String = "id"
    public const val NAME: String = "name"
  }
}
