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

package rs.ltt.jmap.mua.util;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import rs.ltt.jmap.common.entity.EmailAddress;

public class EmailAddressToken {
    private final int start;
    private final int end;
    private final EmailAddress emailAddress;

    public EmailAddressToken(int start, int end, EmailAddress emailAddress) {
        this.start = start;
        this.end = end;
        this.emailAddress = emailAddress;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public EmailAddress getEmailAddress() {
        return emailAddress;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(start, end, emailAddress);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EmailAddressToken that = (EmailAddressToken) o;
        return start == that.start
                && end == that.end
                && Objects.equal(emailAddress, that.emailAddress);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("start", start)
                .add("end", end)
                .add("emailAddress", emailAddress)
                .toString();
    }
}
