/*
 * Copyright 2020 Daniel Gultsch
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
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
public class PushSubscription extends AbstractIdentifiableEntity {

    private String deviceClientId;

    private String url;

    private Keys keys;

    private String verificationCode;

    private Instant expires;

    private List<String> types;

    @Builder(toBuilder = true)
    public PushSubscription(String id, String deviceClientId, String url, Keys keys, String verificationCode, Instant expires, List<String> types) {
        this.id = id;
        this.deviceClientId = deviceClientId;
        this.url = url;
        this.keys = keys;
        this.verificationCode = verificationCode;
        this.expires = expires;
        this.types = types;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("deviceClientId", deviceClientId)
                .add("url", url)
                .add("keys", keys)
                .add("verificationCode", verificationCode)
                .add("expires", expires)
                .add("types", types)
                .toString();
    }
}
