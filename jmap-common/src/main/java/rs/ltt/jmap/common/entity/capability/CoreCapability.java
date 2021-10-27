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

package rs.ltt.jmap.common.entity.capability;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import rs.ltt.jmap.Namespace;
import rs.ltt.jmap.annotation.JmapCapability;
import rs.ltt.jmap.common.entity.Capability;
import rs.ltt.jmap.common.util.Property;

@JmapCapability(namespace = Namespace.CORE)
@Builder
@Getter
@ToString
public class CoreCapability implements Capability {

    private Long maxSizeUpload;
    private Long maxConcurrentUpload;
    private Long maxCallsInRequest;
    private Long maxObjectsInGet;
    private Long maxObjectsInSet;
    private String[] collationAlgorithms;

    public long maxSizeUpload() {
        return Property.expected(maxSizeUpload);
    }

    public long maxConcurrentUpload() {
        return Property.expected(maxConcurrentUpload);
    }

    public long maxCallsInRequest() {
        return Property.expected(maxCallsInRequest);
    }

    public long maxObjectsInGet() {
        return Property.expected(maxObjectsInGet);
    }

    public long maxObjectsInSet() {
        return Property.expected(maxObjectsInSet);
    }
}
