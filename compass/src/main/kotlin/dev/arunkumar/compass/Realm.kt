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

/** Type representing a lambda with [Realm] as the parameter. */
public typealias RealmBlock = (realm: Realm) -> Unit
/**
 * Type representing a lambda with [Realm] as the parameter returning
 * type `T`.
 */
public typealias RealmFunction<T> = (realm: Realm) -> T
/** Type representing a lambda with [Realm] as the receiver. */
public typealias RealmReceiver = Realm.() -> Unit

/**
 * Acquires a new instance of [Realm] using [Realm.getDefaultInstance].
 */
@Suppress("FunctionName")
public inline fun DefaultRealm(): Realm = Realm.getDefaultInstance()

/**
 * Acquires an instance of current default [Realm], runs the [block]
 * with `realm` as the param. The acquired `realm` is closed when the
 * function exits
 */
@Suppress("FunctionName")
public inline fun Realm(block: RealmBlock) {
  val realm = DefaultRealm()
  block(realm)
  realm.close()
}

/**
 * Acquires an instance of current default [Realm], runs the a
 * [RealmTransaction] with the `realm` as the receiver in [block].
 * The acquired `realm` is closed when the function exits. If there
 * is already an active `realm` instance present, consider using
 * [Realm.transact]
 *
 * @see Realm.transact
 */
@Suppress("FunctionName")
public inline fun RealmTransaction(noinline block: RealmReceiver) {
  val realm = DefaultRealm()
  realm.executeTransaction(block)
  realm.close()
}

/**
 * Acquires an instance of current default [Realm], runs the given
 * [block] with the realm and returns the result. The acquired realm is
 * closed before the function exits.
 */
@Suppress("FunctionName")
public inline fun <T> RealmFunction(block: RealmFunction<T>): T {
  val realm = DefaultRealm()
  return block(realm).also { realm.close() }
}

/**
 * Runs [block] with [Realm] as the receiver inside a
 * [Realm.executeTransaction] block.
 *
 * @see Realm.executeTransaction
 */
public inline fun Realm.transact(crossinline block: RealmReceiver) {
  executeTransaction { it.block() }
}

/**
 * Converts a collection to non managed [io.realm.RealmResults]
 * instance.
 */
public inline fun <reified T : RealmModel> Collection<T>.toRealmList(): RealmList<T> {
  return RealmList(*toTypedArray())
}
