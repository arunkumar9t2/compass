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

package dev.arunkumar.compass.test

import dev.arunkumar.compass.test.rule.RealmDispatcherRule
import dev.arunkumar.compass.test.rule.RealmRule
import dev.arunkumar.compass.thread.RealmDispatcher
import org.junit.Rule
import org.junit.rules.RuleChain
import org.junit.rules.TestRule

public abstract class RealmTest {
  protected open val realmDispatcherRule: RealmDispatcherRule = RealmDispatcherRule()
  protected open val realmDispatcher: RealmDispatcher get() = realmDispatcherRule.dispatcher
  protected open val realmRule: RealmRule = RealmRule { realmDispatcher }

  @get:Rule
  public val rules: TestRule
    get() = RuleChain
      .outerRule(realmDispatcherRule)
      .around(realmRule)
}
