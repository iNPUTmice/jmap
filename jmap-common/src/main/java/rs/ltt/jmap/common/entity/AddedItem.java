/*
 * Copyright 2019 Daniel Gultsch
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

package rs.ltt.jmap.common.entity;

import com.google.common.base.MoreObjects;
import com.google.gson.annotations.SerializedName;

public class AddedItem<T> {

    @SerializedName("id")
    private T item;

    private long index;

    private AddedItem() {}

    private AddedItem(T id, long index) {
        this.item = id;
        this.index = index;
    }

    public static <T> AddedItem<T> of(T item, long index) {
        return new AddedItem<>(item, index);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("item", item).add("index", index).toString();
    }

    public T getItem() {
        return item;
    }

    public long getIndex() {
        return index;
    }
}
