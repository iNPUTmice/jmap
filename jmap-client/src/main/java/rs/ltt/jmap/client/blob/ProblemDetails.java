/*
 * Copyright 2021 Daniel Gultsch
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

package rs.ltt.jmap.client.blob;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

public class ProblemDetails {

    private String type;
    private String title;
    private int status;
    private String detail;

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public int getStatus() {
        return status;
    }

    public String getDetail() {
        return detail;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProblemDetails details = (ProblemDetails) o;
        return status == details.status && Objects.equal(type, details.type) && Objects.equal(title, details.title) && Objects.equal(detail, details.detail);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(type, title, status, detail);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("type", type)
                .add("title", title)
                .add("status", status)
                .add("detail", detail)
                .toString();
    }
}
