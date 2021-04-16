/*
 * Copyright 2021 Daniel Gultsch
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
 *
 */

package rs.ltt.jmap.client;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import rs.ltt.jmap.client.event.OnStateChangeListener;
import rs.ltt.jmap.client.event.OnStateChangeListenerManager;

import java.util.concurrent.atomic.AtomicInteger;

public class OnStateChangeListenerManagerTest {

    @Test
    public void andAndRemoveListeners() {
        final AtomicInteger disableCount = new AtomicInteger();
        final AtomicInteger enableCount = new AtomicInteger();
        final OnStateChangeListenerManager onStateChangeListenerManager = new OnStateChangeListenerManager(
                new OnStateChangeListenerManager.Callback() {
                    @Override
                    public void disable() {
                        disableCount.incrementAndGet();
                    }

                    @Override
                    public void enable() {
                        enableCount.incrementAndGet();
                    }
                }
        );
        final OnStateChangeListener a = stateChange -> false;
        final OnStateChangeListener b = stateChange -> false;
        Assertions.assertFalse(
                onStateChangeListenerManager.isPushNotificationsEnabled(),
                "Push Notifications are enabled"
        );
        onStateChangeListenerManager.addOnStateChangeListener(a);
        onStateChangeListenerManager.addOnStateChangeListener(b);
        Assertions.assertTrue(
                onStateChangeListenerManager.isPushNotificationsEnabled(),
                "Push Notifications are disable"
        );
        Assertions.assertEquals(1, enableCount.get());
        Assertions.assertEquals(0, disableCount.get());

        onStateChangeListenerManager.removeOnStateChangeListener(b);

        Assertions.assertTrue(
                onStateChangeListenerManager.isPushNotificationsEnabled(),
                "Push Notifications are disable"
        );
        Assertions.assertEquals(1, enableCount.get());
        Assertions.assertEquals(0, disableCount.get());

        onStateChangeListenerManager.removeOnStateChangeListener(a);

        Assertions.assertEquals(1, enableCount.get(), "Enable count is wrong");
        Assertions.assertEquals(1, disableCount.get(), "Disable count is wrong");

        Assertions.assertFalse(
                onStateChangeListenerManager.isPushNotificationsEnabled(),
                "Push Notifications are enabled"
        );

    }

    @Test
    public void andAndRemoveAll() {
        final AtomicInteger disableCount = new AtomicInteger();
        final AtomicInteger enableCount = new AtomicInteger();
        final OnStateChangeListenerManager onStateChangeListenerManager = new OnStateChangeListenerManager(
                new OnStateChangeListenerManager.Callback() {
                    @Override
                    public void disable() {
                        disableCount.incrementAndGet();
                    }

                    @Override
                    public void enable() {
                        enableCount.incrementAndGet();
                    }
                }
        );
        final OnStateChangeListener a = stateChange -> false;
        final OnStateChangeListener b = stateChange -> false;
        Assertions.assertFalse(
                onStateChangeListenerManager.isPushNotificationsEnabled(),
                "Push Notifications are enabled"
        );
        onStateChangeListenerManager.addOnStateChangeListener(a);
        onStateChangeListenerManager.addOnStateChangeListener(b);

        Assertions.assertTrue(
                onStateChangeListenerManager.isPushNotificationsEnabled(),
                "Push Notifications are disable"
        );

        onStateChangeListenerManager.removeAllListeners();

        Assertions.assertFalse(
                onStateChangeListenerManager.isPushNotificationsEnabled(),
                "Push Notifications are enabled"
        );

        Assertions.assertEquals(1, enableCount.get(), "Enable count is wrong");
        Assertions.assertEquals(1, disableCount.get(), "Disable count is wrong");

    }

    @Test
    public void unnecessaryRemove() {
        final AtomicInteger disableCount = new AtomicInteger();
        final AtomicInteger enableCount = new AtomicInteger();
        final OnStateChangeListenerManager onStateChangeListenerManager = new OnStateChangeListenerManager(
                new OnStateChangeListenerManager.Callback() {
                    @Override
                    public void disable() {
                        disableCount.incrementAndGet();
                    }

                    @Override
                    public void enable() {
                        enableCount.incrementAndGet();
                    }
                }
        );
        final OnStateChangeListener a = stateChange -> false;

        onStateChangeListenerManager.addOnStateChangeListener(a);
        onStateChangeListenerManager.removeOnStateChangeListener(a);
        onStateChangeListenerManager.removeOnStateChangeListener(a);
        onStateChangeListenerManager.removeAllListeners();

        Assertions.assertEquals(1, enableCount.get(), "Enable count is wrong");
        Assertions.assertEquals(1, disableCount.get(), "Disable count is wrong");

    }
}
