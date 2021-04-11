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

package rs.ltt.jmap.common.entity.filter;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;

public interface QueryString {

    char L0_DIVIDER = '\u000b';
    char L1_DIVIDER = '\u001c';
    char L2_DIVIDER = '\u001d';
    char L3_DIVIDER = '\u001e';
    char L4_DIVIDER = '\u001f';

    default String asHash() {
        return Hashing.sha256().hashString(this.toQueryString(), Charsets.UTF_8).toString();
    }

    String toQueryString();
}
