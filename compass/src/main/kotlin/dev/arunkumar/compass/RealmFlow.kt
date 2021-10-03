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

package dev.arunkumar.compass

import dev.arunkumar.compass.thread.RealmDispatcher
import io.realm.Realm
import io.realm.RealmChangeListener
import io.realm.RealmModel
import io.realm.RealmResults
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive

/**
 * Type representing a lambda with the [Realm] as the receiver and
 * [RealmModel] represented by `T` as the parameter and returning the
 * computation as `R`.
 *
 * Use [RealmModelTransform] to convert a managed [RealmModel] to type
 * `R`. `R` can be any type but usually it is a non managed [RealmModel]
 * instance or a mapped object derived from managed [RealmModel].
 *
 * @see RealmCopyTransform - A transform that uses [Realm.copyFromRealm]
 *     to convert a live managed [RealmModel] to non managed instances.
 * @receiver [Realm]
 */
public typealias RealmModelTransform<T, R> = Realm.(realmModel: T) -> R

/**
 * A `RealmModelTransform` implementation that simply copies
 * the managed [RealmModel] instance to a non managed one using
 * [Realm.copyFromRealm]. This is the default behavior for
 * `RealmQueryBuilder<T>.asFlow`.
 *
 * Note: For heavily nested `RealmModel`s, this transform is undesirable
 * since it brings the entire object into application memory. In
 * those cases consider using a custom transform that maps the given
 * [RealmModel] to a subset of required data.
 */
@Suppress("FunctionName")
public fun <T : RealmModel, R> RealmCopyTransform(): RealmModelTransform<T, R> {
  return { model -> copyFromRealm(model) as R }
}

/**
 * A type representing a lambda that computes an instance of
 * [RealmDispatcher]
 */
private typealias RealmDispatcherProvider = () -> RealmDispatcher

/**
 * Returns a [Flow] of list of `R` generated using
 * [transform] on the [RealmModel] instances satisfying the current
 * [RealmQuery]. The flow emits new list of [RealmModel] instances
 * whenever [RealmQuery] results has a change. The returned items can be
 * safely passed around threads since they are unmanaged due to usage of
 * [RealmCopyTransform] which uses [Realm.copyFromRealm] to create non
 * managed instances.
 *
 * The [RealmQuery] is executed using a [Realm] obtained with
 * [DefaultRealm] and it stays active for the duration of the active
 * collection of the flow.
 *
 * The implementation uses the [RealmDispatcher] instance to run
 * the query and observe changes. By default a new instance of
 * [RealmDispatcher] is created with [dispatcher] but this can be
 * customized by passing a custom instance. When flow collection is
 * cancelled, the [RealmDispatcher] is disposed to release resources by
 * calling [RealmDispatcher.close].
 *
 * Note: For large nested [RealmModel]s consider using overloaded method
 * that accepts [RealmModelTransform] to run a custom transform function
 * to generate a subset of data from [RealmModel] instance.
 *
 * Usage:
 *
 * ```
 * open class Person(var name: String = "", val age: Int = 0) : RealmObject
 *
 * val persons: Flow<List<Person> = RealmQuery { where<Person>() }.asFlow()
 * ```
 *
 * @see RealmQueryBuilder.asFlow
 */
public fun <T : RealmModel> RealmQueryBuilder<T>.asFlow(
  dispatcher: RealmDispatcherProvider = { RealmDispatcher() },
): Flow<List<T>> = asFlow(
  dispatcher = dispatcher,
  transform = RealmCopyTransform()
)

/**
 * Returns a [Flow] of list of `R` generated using
 * [transform] on the [RealmModel] instances satisfying the current
 * [RealmQuery]. The flow emits new list of [RealmModel] instances
 * whenever [RealmQuery] results has a change. The returned items can
 * be safely passed around threads since they are unmanaged as long
 * the [transform] does not return managed instances. For correct
 * implementation of [transform] see [RealmCopyTransform].
 *
 * The [RealmQuery] is executed using a [Realm] obtained with
 * [DefaultRealm] and it stays active for the duration of the active
 * collection of the flow.
 *
 * The implementation uses the [RealmDispatcher] instance to run
 * the query and observe changes. By default a new instance of
 * [RealmDispatcher] is created with [dispatcher] but this can be
 * customized by passing a custom instance. When flow collection is
 * cancelled, the [RealmDispatcher] is disposed to release resources by
 * calling [RealmDispatcher.close].
 *
 * Notes: Use [transform] to map the managed [RealmModel] instance to
 * a non managed one of type `R`. It is a good practice to only read
 * the desired properties from [RealmModel] as reading the whole object
 * brings entire object into application memory.
 *
 * Usage:
 *
 * ```
 * open class Person(var name: String = "", val age: Int = 0) : RealmObject
 * data class PersonName(val name: String)
 *
 * val personNames: Flow<List<PersonName> = RealmQuery { where<Person>() }
 *    .asFlow { PersonName(it.name) }
 * ```
 *
 * @see RealmQueryBuilder.asFlow
 */
public fun <T : RealmModel, R> RealmQueryBuilder<T>.asFlow(
  dispatcher: RealmDispatcherProvider = { RealmDispatcher() },
  transform: RealmModelTransform<T, R>
): Flow<List<R>> {
  return flow {
    emit(dispatcher())
  }.flatMapConcat { realmDispatcher: RealmDispatcher ->
    callbackFlow {
      val realm = DefaultRealm()
      val realmQuery = buildQuery(realm)
      val results = realmQuery.findAll()

      if (!results.isValid) {
        awaitClose {}
        realm.close()
        return@callbackFlow
      }

      fun RealmResults<T>.transform(): List<R> = map { value -> realm.transform(value) }

      // Emit initial result
      trySend(results.transform())

      val realmChangeListener = RealmChangeListener<RealmResults<T>> { listenerResults ->
        if (isActive) {
          // Emit any changes
          trySend(listenerResults.transform())
        }
      }
      results.addChangeListener(realmChangeListener)
      awaitClose {
        results.removeChangeListener(realmChangeListener)
        realm.close()
      }
    }.flowOn(realmDispatcher).onCompletion { realmDispatcher.close() }
  }
}
