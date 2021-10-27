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

import com.google.gson.annotations.SerializedName;

public enum ErrorType {
    @SerializedName("urn:ietf:params:jmap:error:unknownCapability")
    UNKNOWN_CAPABILITY,
    @SerializedName("urn:ietf:params:jmap:error:notJSON")
    NOT_JSON,
    @SerializedName("urn:ietf:params:jmap:error:notRequest")
    NOT_REQUEST,
    @SerializedName("urn:ietf:params:jmap:error:limit")
    LIMIT
}
