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
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Getter
@Builder(toBuilder = true)
public class Email extends AbstractIdentifiableEntity implements IdentifiableEmailWithKeywords, IdentifiableEmailWithMailboxIds {

    public static final String[] MUTABLE_PROPERTIES = {"keywords", "mailboxIds"};

    //Metadata

    private String blobId;

    private String threadId;

    @Singular
    private Map<String, Boolean> mailboxIds;

    @Singular
    private Map<String, Boolean> keywords;

    private Long size;

    private Date receivedAt;

    //Header
    @Singular
    private List<EmailHeader> headers;

    //The following convenience properties are also specified for the Email object:
    @Singular("messageId")
    private List<String> messageId;

    @Singular("inReplyTo")
    private List<String> inReplyTo;

    @Singular
    private List<String> references;

    @Singular("sender")
    private List<EmailAddress> sender;

    @Singular("from")
    private List<EmailAddress> from;

    @Singular("to")
    private List<EmailAddress> to;

    @Singular("cc")
    private List<EmailAddress> cc;

    @Singular("bcc")
    private List<EmailAddress> bcc;

    @Singular("replyTo")
    private List<EmailAddress> replyTo;

    private String subject;

    private Date sentAt;

    //body data

    @Singular("bodyStructure")
    private List<EmailBodyPart> bodyStructure;

    @Singular
    private Map<String, EmailBodyValue> bodyValues;

    @Singular("textBody")
    private List<EmailBodyPart> textBody;

    @Singular("htmlBody")
    private List<EmailBodyPart> htmlBody;

    @Singular
    private List<EmailBodyPart> attachments;

    private Boolean hasAttachment;

    private String preview;

    public static Email of(String id) {
        final Email email = new Email(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
        email.id = id;
        return email;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("blobId", blobId)
                .add("threadId", threadId)
                .add("mailboxIds", mailboxIds)
                .add("keywords", keywords)
                .add("size", size)
                .add("receivedAt", receivedAt)
                .add("headers", headers)
                .add("messageId", messageId)
                .add("inReplyTo", inReplyTo)
                .add("references", references)
                .add("sender", sender)
                .add("from", from)
                .add("to", to)
                .add("cc", cc)
                .add("bcc", bcc)
                .add("replyTo", replyTo)
                .add("subject", subject)
                .add("sentAt", sentAt)
                .add("bodyStructure", bodyStructure)
                .add("bodyValues", bodyValues)
                .add("textBody", textBody)
                .add("htmlBody", textBody)
                .add("attachments", attachments)
                .add("hasAttachment", hasAttachment)
                .add("preview", preview)
                .toString();
    }
}
