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

import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import rs.ltt.jmap.Namespace;
import rs.ltt.jmap.annotation.JmapAccountCapability;
import rs.ltt.jmap.common.entity.AccountCapability;
import rs.ltt.jmap.common.util.Property;

@JmapAccountCapability(namespace = Namespace.SUBMISSION)
@Getter
@Builder
@ToString
public class SubmissionAccountCapability implements AccountCapability {
    private Long maxDelayedSend;
    private Map<String, String[]> submissionExtensions;

    public long maxDelayedSend() {
        return Property.expected(maxDelayedSend);
    }
}
