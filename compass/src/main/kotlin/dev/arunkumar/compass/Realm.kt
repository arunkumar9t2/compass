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
import io.realm.RealmList
import io.realm.RealmModel

public typealias RealmBlock = (realm: Realm) -> Unit
public typealias RealmFunction<T> = (realm: Realm) -> T
public typealias RealmReceiver = Realm.() -> Unit

@Suppress("FunctionName")
public inline fun DefaultRealm(): Realm = Realm.getDefaultInstance()

@Suppress("FunctionName")
public inline fun Realm(block: RealmBlock) {
  val realm = DefaultRealm()
  block(realm)
  realm.close()
}

@Suppress("FunctionName")
public inline fun RealmTransaction(noinline block: RealmReceiver) {
  val realm = DefaultRealm()
  realm.executeTransaction(block)
  realm.close()
}

@Suppress("FunctionName")
public inline fun <T> RealmFunction(block: RealmFunction<T>): T {
  val realm = DefaultRealm()
  return block(realm).also { realm.close() }
}

public inline fun Realm.transact(crossinline block: RealmReceiver) {
  executeTransaction { it.block() }
}

public inline fun <reified T : RealmModel> Collection<T>.toRealmList(): RealmList<T> {
  return RealmList(*toTypedArray())
}
