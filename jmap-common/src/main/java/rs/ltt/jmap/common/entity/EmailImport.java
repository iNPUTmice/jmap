/*
 * Copyright 2020 cketti
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

import java.time.Instant;
import java.util.Map;
import lombok.Builder;
import lombok.NonNull;

@Builder
public class EmailImport {

    @NonNull private String blobId;

    @NonNull private Map<String, Boolean> mailboxIds;

    private Map<String, Boolean> keywords;

    private Instant receivedAt;
}
