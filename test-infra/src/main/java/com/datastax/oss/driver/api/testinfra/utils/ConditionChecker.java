/*
 * Copyright (C) 2017-2017 DataStax Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.datastax.oss.driver.api.testinfra.utils;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Fail.fail;

public class ConditionChecker {

  private static final int DEFAULT_PERIOD_MILLIS = 500;

  private static final int DEFAULT_TIMEOUT_MILLIS = 60000;

  public static class ConditionCheckerBuilder {

    private long timeout = DEFAULT_TIMEOUT_MILLIS;

    private TimeUnit timeoutUnit = TimeUnit.MILLISECONDS;

    private long period = DEFAULT_PERIOD_MILLIS;

    private TimeUnit periodUnit = TimeUnit.MILLISECONDS;

    private final BooleanSupplier predicate;

    ConditionCheckerBuilder(BooleanSupplier predicate) {
      this.predicate = predicate;
    }

    public ConditionCheckerBuilder every(long period, TimeUnit unit) {
      this.period = period;
      periodUnit = unit;
      return this;
    }

    public ConditionCheckerBuilder every(long periodMillis) {
      period = periodMillis;
      periodUnit = TimeUnit.MILLISECONDS;
      return this;
    }

    public ConditionCheckerBuilder before(long timeout, TimeUnit unit) {
      this.timeout = timeout;
      timeoutUnit = unit;
      return this;
    }

    public ConditionCheckerBuilder before(long timeoutMillis) {
      timeout = timeoutMillis;
      timeoutUnit = TimeUnit.MILLISECONDS;
      return this;
    }

    @SuppressWarnings("unchecked")
    public void becomesTrue() {
      new ConditionChecker(predicate, period, periodUnit).await(timeout, timeoutUnit);
    }

    @SuppressWarnings("unchecked")
    public void becomesFalse() {
      new ConditionChecker(() -> !predicate.getAsBoolean(), period, periodUnit)
          .await(timeout, timeoutUnit);
    }
  }

  public static ConditionCheckerBuilder checkThat(BooleanSupplier predicate) {
    return new ConditionCheckerBuilder(predicate);
  }

  private final BooleanSupplier predicate;
  private final Lock lock;
  private final Condition condition;
  private final Timer timer;

  @SuppressWarnings("unchecked")
  public ConditionChecker(BooleanSupplier predicate, long period, TimeUnit periodUnit) {
    this.predicate = predicate;
    lock = new ReentrantLock();
    condition = lock.newCondition();
    timer = new Timer("condition-checker", true);
    timer.schedule(
        new TimerTask() {
          @Override
          public void run() {
            checkCondition();
          }
        },
        0,
        periodUnit.toMillis(period));
  }

  /** Waits until the predicate becomes true, or a timeout occurs, whichever happens first. */
  public void await(long timeout, TimeUnit unit) {
    boolean interrupted = false;
    long nanos = unit.toNanos(timeout);
    lock.lock();
    try {
      while (!evalCondition()) {
        if (nanos <= 0L)
          fail(
              String.format(
                  "Timeout after %s %s while waiting for condition",
                  timeout, unit.toString().toLowerCase()));
        try {
          nanos = condition.awaitNanos(nanos);
        } catch (InterruptedException e) {
          interrupted = true;
        }
      }
    } finally {
      timer.cancel();
      if (interrupted) Thread.currentThread().interrupt();
    }
  }

  private void checkCondition() {
    lock.lock();
    try {
      if (evalCondition()) {
        condition.signal();
      }
    } finally {
      lock.unlock();
    }
  }

  private boolean evalCondition() {
    return predicate.getAsBoolean();
  }
}