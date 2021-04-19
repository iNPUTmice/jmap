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

package rs.ltt.jmap.client.event;

import rs.ltt.jmap.common.entity.StateChange;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class OnStateChangeListenerManager {

    private final Callback callback;

    private final List<OnStateChangeListener> onStateChangeListeners = new ArrayList<>();

    public OnStateChangeListenerManager(final Callback callback) {
        this.callback = callback;
    }

    public void addOnStateChangeListener(final OnStateChangeListener onStateChangeListener) {
        synchronized (this.onStateChangeListeners) {
            final boolean empty = this.onStateChangeListeners.isEmpty();
            this.onStateChangeListeners.add(onStateChangeListener);
            if (empty) {
                callback.enable();
            }
        }
    }

    public void removeOnStateChangeListener(OnStateChangeListener onStateChangeListener) {
        synchronized (this.onStateChangeListeners) {
            final boolean removed = this.onStateChangeListeners.remove(onStateChangeListener);
            final boolean empty = this.onStateChangeListeners.isEmpty();
            if (removed && empty) {
                callback.disable();
            }
        }
    }

    public void removeAllListeners() {
        synchronized (this.onStateChangeListeners) {
            final boolean nonEmpty = !this.onStateChangeListeners.isEmpty();
            this.onStateChangeListeners.clear();
            if (nonEmpty) {
                callback.disable();
            }
        }
    }

    public boolean isPushNotificationsEnabled() {
        synchronized (this.onStateChangeListeners) {
            return !this.onStateChangeListeners.isEmpty();
        }
    }

    public boolean onStateChange(StateChange stateChange) {
        final AtomicBoolean result = new AtomicBoolean(false);
        synchronized (this.onStateChangeListeners) {
            this.onStateChangeListeners.forEach(listener -> {
                result.compareAndSet(false, listener.onStateChange(stateChange));
            });
        }
        return result.get();
    }


    public interface Callback {
        void disable();

        void enable();
    }
}
