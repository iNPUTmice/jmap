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
import com.google.common.base.Objects;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class EmailAddress {
    private String name;
    private String email;

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("name", name).add("email", email).toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EmailAddress that = (EmailAddress) o;
        return Objects.equal(name, that.name) && Objects.equal(email, that.email);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, email);
    }
}
