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

package rs.ltt.jmap.client.util;

import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import okhttp3.Call;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;

public class SettableCallFuture<V> extends AbstractFuture<V> {

    private final Call call;

    private SettableCallFuture(final Call call) {
        this.call = call;
    }

    public static <V> SettableCallFuture<V> create(final Call call) {
        return new SettableCallFuture<>(call);
    }

    @CanIgnoreReturnValue
    public boolean set(@Nullable V value) {
        return super.set(value);
    }

    @CanIgnoreReturnValue
    public boolean setException(@NotNull Throwable throwable) {
        return super.setException(throwable);
    }

    @CanIgnoreReturnValue
    public boolean setFuture(@NotNull ListenableFuture<? extends V> future) {
        return super.setFuture(future);
    }

    @Override
    protected void afterDone() {
        super.afterDone();
        if (wasInterrupted()) {
            call.cancel();
        }
    }
}
