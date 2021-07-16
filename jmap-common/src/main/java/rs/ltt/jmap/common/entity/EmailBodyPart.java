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

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.List;

@Getter
@Builder
public class EmailBodyPart implements Downloadable {

    private String partId;
    private String blobId;
    private Long size;
    @Singular
    private List<EmailHeader> headers;
    private String name;
    private String type;
    private String charset;
    private String disposition;
    private String cid;

    @Singular("language")
    private List<String> language;
    private String location;

    @Singular
    private List<EmailBodyPart> subParts;

}
