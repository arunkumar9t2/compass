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
@file:Suppress("NOTHING_TO_INLINE")

package dev.arunkumar.compass

import io.realm.Realm
import io.realm.RealmModel
import io.realm.RealmQuery

public typealias RealmQueryBuilder<T> = Realm.() -> RealmQuery<T>

private inline fun <T : RealmModel> RealmQueryBuilder<T>.buildQuery(realm: Realm): RealmQuery<T> {
  return invoke(realm)
}

@Suppress("FunctionName")
public fun <T : RealmModel> RealmQuery(
  builder: RealmQueryBuilder<T>
): RealmQueryBuilder<T> = builder

public fun <T : RealmModel> RealmQueryBuilder<T>.getAll(): List<T> {
  return RealmFunction { realm -> buildQuery(realm).findAll() }
}
