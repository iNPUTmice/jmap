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

import lombok.Getter;
import lombok.ToString;
import rs.ltt.jmap.common.entity.Account;
import rs.ltt.jmap.common.entity.Capability;

import java.util.Map;

@Getter
@ToString
public class SessionResource {

    private String username;
    private String apiUrl;
    private String downloadUrl;
    private String uploadUrl;
    private String eventSourceUrl;
    private Map<String, Account> accounts;
    private Map<String, String> primaryAccounts;
    private Map<Class<?extends Capability>, Capability> capabilities;
    private String state;

    public <T extends Capability> T getCapability(Class<T> clazz) {
        return clazz.cast(capabilities.get(clazz));
    }
}
