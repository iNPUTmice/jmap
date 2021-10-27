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
import com.google.gson.annotations.SerializedName;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import rs.ltt.jmap.annotation.JmapEntity;
import rs.ltt.jmap.common.entity.filter.EmailFilterCondition;

@Getter
@JmapEntity(filterCondition = EmailFilterCondition.class)
public class Email extends AbstractIdentifiableEntity
        implements IdentifiableEmailWithKeywords,
                IdentifiableEmailWithMailboxIds,
                IdentifiableEmailWithAddresses,
                IdentifiableEmailWithSubject {

    // Metadata

    private String blobId;

    private String threadId;

    private Map<String, Boolean> mailboxIds;

    private Map<String, Boolean> keywords;

    private Long size;

    private Instant receivedAt;

    // Header
    private List<EmailHeader> headers;

    // The following convenience properties are also specified for the Email object:
    private List<String> messageId;

    private List<String> inReplyTo;

    private List<String> references;

    private List<EmailAddress> sender;

    private List<EmailAddress> from;

    private List<EmailAddress> to;

    private List<EmailAddress> cc;

    private List<EmailAddress> bcc;

    private List<EmailAddress> replyTo;

    private String subject;

    private OffsetDateTime sentAt;

    // The following properties are not directly specified by JMAP but are provided by the library
    // for your convenience
    @SerializedName(Property.USER_AGENT)
    private String userAgent;

    @SerializedName(Property.AUTOCRYPT)
    private List<String> autocrypt;

    @SerializedName(Property.AUTOCRYPT_DRAFT_STATE)
    private String autocryptDraftState;

    @SerializedName(Property.AUTOCRYPT_SETUP_MESSAGE)
    private String autocryptSetupMessage;

    // body data

    private EmailBodyPart bodyStructure;

    private Map<String, EmailBodyValue> bodyValues;

    private List<EmailBodyPart> textBody;

    private List<EmailBodyPart> htmlBody;

    private List<EmailBodyPart> attachments;

    private Boolean hasAttachment;

    private String preview;

    @Builder(toBuilder = true)
    public Email(
            String id,
            String blobId,
            String threadId,
            @Singular Map<String, Boolean> mailboxIds,
            @Singular Map<String, Boolean> keywords,
            Long size,
            Instant receivedAt,
            @Singular List<EmailHeader> headers,
            @Singular("messageId") List<String> messageId,
            @Singular("inReplyTo") List<String> inReplyTo,
            @Singular List<String> references,
            @Singular("sender") List<EmailAddress> sender,
            @Singular("from") List<EmailAddress> from,
            @Singular("to") List<EmailAddress> to,
            @Singular("cc") List<EmailAddress> cc,
            @Singular("bcc") List<EmailAddress> bcc,
            @Singular("replyTo") List<EmailAddress> replyTo,
            String subject,
            OffsetDateTime sentAt,
            String userAgent,
            @Singular("autocrypt") List<String> autocrypt,
            String autocryptDraftState,
            String autocryptSetupMessage,
            EmailBodyPart bodyStructure,
            @Singular Map<String, EmailBodyValue> bodyValues,
            @Singular("textBody") List<EmailBodyPart> textBody,
            @Singular("htmlBody") List<EmailBodyPart> htmlBody,
            List<EmailBodyPart> attachments,
            Boolean hasAttachment,
            String preview) {
        this.id = id;
        this.blobId = blobId;
        this.threadId = threadId;
        this.mailboxIds = mailboxIds;
        this.keywords = keywords;
        this.size = size;
        this.receivedAt = receivedAt;
        this.headers = headers;
        this.messageId = messageId;
        this.inReplyTo = inReplyTo;
        this.references = references;
        this.sender = sender;
        this.from = from;
        this.to = to;
        this.cc = cc;
        this.bcc = bcc;
        this.replyTo = replyTo;
        this.subject = subject;
        this.sentAt = sentAt;
        this.userAgent = userAgent;
        this.autocrypt = autocrypt;
        this.autocryptDraftState = autocryptDraftState;
        this.autocryptSetupMessage = autocryptSetupMessage;
        this.bodyStructure = bodyStructure;
        this.bodyValues = bodyValues;
        this.textBody = textBody;
        this.htmlBody = htmlBody;
        this.attachments = attachments;
        this.hasAttachment = hasAttachment;
        this.preview = preview;
    }

    public static Email of(String id) {
        return Email.builder().id(id).build();
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
                .add("userAgent", userAgent)
                .add("autocrypt", autocrypt)
                .add("autocryptDraftState", autocryptDraftState)
                .add("autocryptSetupMessage", autocryptSetupMessage)
                .add("bodyStructure", bodyStructure)
                .add("bodyValues", bodyValues)
                .add("textBody", textBody)
                .add("htmlBody", htmlBody)
                .add("attachments", attachments)
                .add("hasAttachment", hasAttachment)
                .add("preview", preview)
                .toString();
    }

    public static final class Property {
        public static final String KEYWORDS = "keywords";
        public static final String MAILBOX_IDS = "mailboxIds";
        public static final String THREAD_ID = "threadId";
        public static final String RECEIVED_AT = "receivedAt";
        public static final String USER_AGENT = "header:User-Agent:asText";
        public static final String AUTOCRYPT = "header:Autocrypt:asText:all";
        public static final String AUTOCRYPT_DRAFT_STATE = "header:Autocrypt-Draft-State:asText";
        public static final String AUTOCRYPT_SETUP_MESSAGE =
                "header:Autocrypt-Setup-Message:asText";

        private Property() {}
    }

    public static final class Properties {
        public static final String[] THREAD_ID = new String[] {Property.THREAD_ID};
        public static final String[] MUTABLE =
                new String[] {Property.KEYWORDS, Property.MAILBOX_IDS};

        private Properties() {}
    }
}
