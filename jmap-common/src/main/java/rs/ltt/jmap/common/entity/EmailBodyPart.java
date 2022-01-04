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
import com.google.common.base.Optional;
import com.google.common.net.MediaType;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Locale;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

@Getter
@Builder
public class EmailBodyPart implements Attachment {

    private String partId;
    private String blobId;
    private Long size;
    @Singular private List<EmailHeader> headers;
    private String name;
    private String type;
    private String charset;
    private String disposition;
    private String cid;

    @Singular("language")
    private List<String> language;

    private String location;

    @Singular private List<EmailBodyPart> subParts;

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("partId", partId)
                .add("blobId", blobId)
                .add("size", size)
                .add("headers", headers)
                .add("name", name)
                .add("type", type)
                .add("charset", charset)
                .add("disposition", disposition)
                .add("cid", cid)
                .add("language", language)
                .add("location", location)
                .add("subParts", subParts)
                .toString();
    }

    public static class EmailBodyPartBuilder {
        public EmailBodyPartBuilder mediaType(MediaType mediaType) {
            this.type(mediaType.withoutParameters().toString());
            final Optional<Charset> optionalCharset = mediaType.charset();
            if (optionalCharset.isPresent()) {
                final Charset charset = optionalCharset.get();
                this.charset(charset.name().toLowerCase(Locale.ROOT));
            }
            return this;
        }
    }
}
