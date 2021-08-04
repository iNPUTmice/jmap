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

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import rs.ltt.jmap.common.util.Property;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

@Builder
@Getter
public class Account {
    private String name;
    private Boolean isPersonal;
    private Boolean isReadOnly;
    //TODO @Singular annotation doesn’t seem to compile. Maybe report with lombok?
    @Getter(AccessLevel.NONE)
    private Map<Class<? extends AccountCapability>, AccountCapability> accountCapabilities;

    public boolean isPersonal() {
        return Property.expected(isPersonal);
    }

    public boolean isReadOnly() {
        return Property.expected(isReadOnly);
    }

    public <T extends AccountCapability> T getCapability(Class<T> clazz) {
        return clazz.cast(accountCapabilities.get(clazz));
    }

    public Collection<AccountCapability> getCapabilities() {
        return accountCapabilities == null ? Collections.emptyList() : accountCapabilities.values();
    }

    public boolean hasCapability(Class<? extends AccountCapability> clazz) {
        return accountCapabilities != null && accountCapabilities.containsKey(clazz);
    }

    public static class AccountBuilder {
        public AccountBuilder accountCapabilities(Map<Class<? extends AccountCapability>, AccountCapability> accountCapabilities) {
            for (final Map.Entry<Class<? extends AccountCapability>, AccountCapability> entry : accountCapabilities.entrySet()) {
                final Class<? extends AccountCapability> key = entry.getKey();
                final AccountCapability value = entry.getValue();
                if (key != value.getClass()) {
                    throw new IllegalArgumentException(String.format("key %s does not match value type %s", key, value.getClass()));
                }
            }
            this.accountCapabilities = accountCapabilities;
            return this;
        }
    }
}
