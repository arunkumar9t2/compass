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

package dev.arunkumar.compass.thread

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Process
import io.realm.Realm
import java.util.concurrent.Executor

/**
 * An [Executor] that can be safely released by calling
 * [AutoCloseable.close] method.
 */
public interface CloseableExecutor : Executor, AutoCloseable

/**
 * [HandlerExecutor] internally manages a [HandlerThread] instance
 * that will be used to execute incoming tasks in [Executor.execute].
 * [HandlerExecutor]s are useful in places where an active [Looper] is
 * required on the created [Thread]. The [HandlerThread] is assigned
 * [Process.THREAD_PRIORITY_BACKGROUND] and should be only used to
 * execute tasks with low priority.
 *
 * Notes: The [HandlerExecutor]'s internal [HandlerThread] event loop is
 * stopped when [close] is called.
 *
 *        As a good practice, always `close` the executor when it is no longer required.
 *
 * @param tag The thread name of the handler thread that will be
 *     created.
 */
public class HandlerExecutor(private val tag: String? = null) : CloseableExecutor {

  private val handlerThread by lazy {
    HandlerThread(
      tag ?: this::class.java.simpleName + hashCode(),
      Process.THREAD_PRIORITY_BACKGROUND
    ).apply { start() }
  }

  private val handler by lazy { Handler(handlerThread.looper) }

  /**
   * Executes the given [command] in an [HandlerThread] that is
   * guranteed to have a [Looper] running actively. In case the calling
   * thread is already the `HandlerThread` that this instance manages,
   * then `command` is directly executed without going through a thread
   * switch.
   */
  override fun execute(command: Runnable) {
    if (Looper.myLooper() == handler.looper) {
      command.run()
    } else {
      handler.post(command)
    }
  }

  /**
   * Releases any internal resources that are used by this executor.
   * Always prefer to call this when [HandlerExecutor] is no longer
   * required.
   */
  override fun close() {
    handlerThread.quitSafely()
  }
}

/**
 * An [Executor] that executes given work in a thread that has
 * the Android [Looper] running. An work posted in [execute]
 * can observe [Realm] objects due to presence of the Looper
 * in the execution thread. The internal `Thread` will have
 * [Process.THREAD_PRIORITY_BACKGROUND], hence it is recommended to use
 * this executor for non critical work.
 *
 * @param tag The thread name of the handler thread that will be
 *     created.
 */
public class RealmExecutor(
  private val tag: String? = null
) : CloseableExecutor by HandlerExecutor(tag)
