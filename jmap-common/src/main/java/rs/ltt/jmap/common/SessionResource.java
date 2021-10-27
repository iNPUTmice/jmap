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

package rs.ltt.jmap.common;

import java.util.Collection;
import java.util.Map;
import lombok.*;
import rs.ltt.jmap.common.entity.Account;
import rs.ltt.jmap.common.entity.AccountCapability;
import rs.ltt.jmap.common.entity.Capability;

@Builder
@Getter
@ToString
public class SessionResource {

    private String username;
    private String apiUrl;
    private String downloadUrl;
    private String uploadUrl;
    private String eventSourceUrl;
    @Singular private Map<String, Account> accounts;

    @Getter(AccessLevel.NONE)
    private Map<Class<? extends AccountCapability>, String> primaryAccounts;
    // TODO @Singular annotation doesn’t seem to compile. Maybe report with lombok?
    @Getter(AccessLevel.NONE)
    private Map<Class<? extends Capability>, Capability> capabilities;

    private String state;

    public <T extends Capability> T getCapability(Class<T> clazz) {
        return clazz.cast(capabilities.get(clazz));
    }

    public Collection<Capability> getCapabilities() {
        return capabilities.values();
    }

    public String getPrimaryAccount(Class<? extends AccountCapability> clazz) {
        return primaryAccounts.get(clazz);
    }

    public static class SessionResourceBuilder {
        public SessionResourceBuilder capabilities(
                Map<Class<? extends Capability>, Capability> capabilities) {
            for (Map.Entry<Class<? extends Capability>, Capability> entry :
                    capabilities.entrySet()) {
                final Class<? extends Capability> key = entry.getKey();
                final Capability value = entry.getValue();
                if (key != value.getClass()) {
                    throw new IllegalArgumentException(
                            String.format(
                                    "key %s does not match value type %s", key, value.getClass()));
                }
            }
            this.capabilities = capabilities;
            return this;
        }
    }
}
