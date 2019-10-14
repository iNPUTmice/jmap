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

package rs.ltt.jmap.common.method.call.email;

import rs.ltt.jmap.annotation.JmapMethod;
import rs.ltt.jmap.common.Request;
import rs.ltt.jmap.common.entity.Email;
import rs.ltt.jmap.common.method.call.standard.GetMethodCall;

@JmapMethod("Email/get")
public class GetEmailMethodCall extends GetMethodCall<Email> {

    private Boolean fetchTextBodyValues;
    private Boolean fetchHTMLBodyValues;
    private Boolean fetchAllBodyValues;
    private Long maxBodyValueBytes;

    public GetEmailMethodCall(Request.Invocation.ResultReference resultReference) {
        super(resultReference);
    }

    public GetEmailMethodCall(Request.Invocation.ResultReference resultReference, boolean fetchTextBodyValues) {
        super(resultReference);
        this.fetchTextBodyValues = fetchTextBodyValues;
    }

    public GetEmailMethodCall(String[] ids) {
        super(ids);
    }

    public GetEmailMethodCall(String[] ids, boolean fetchTextBodyValues) {
        super(ids);
        this.fetchTextBodyValues = fetchTextBodyValues;
    }

    public GetEmailMethodCall(Request.Invocation.ResultReference resultReference, String[] properties) {
        super(resultReference);
        this.properties = properties;
    }

    public GetEmailMethodCall(String[] ids, String[] properties) {
        super(ids);
        this.properties = properties;
    }
}
