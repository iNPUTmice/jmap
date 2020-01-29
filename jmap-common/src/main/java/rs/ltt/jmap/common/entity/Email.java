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
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Getter
@Builder(toBuilder = true)
public class Email extends AbstractIdentifiableEntity implements IdentifiableEmailWithKeywords, IdentifiableEmailWithMailboxIds, IdentifiableEmailWithAddresses, IdentifiableEmailWithSubject {

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

    //The following properties are not directly specified by JMAP but are provided by the library for your convenience
    @SerializedName(Property.USER_AGENT)
    private String userAgent;

    @SerializedName(Property.AUTOCRYPT)
    @Singular("autocrypt")
    private List<String> autocrypt;

    @SerializedName(Property.AUTOCRYPT_DRAFT_STATE)
    private String autocryptDraftState;

    @SerializedName(Property.AUTOCRYPT_SETUP_MESSAGE)
    private String autocryptSetupMessage;

    //body data

    private EmailBodyPart bodyStructure;

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
        public static final String USER_AGENT = "header:User-Agent:asText";
        public static final String AUTOCRYPT = "header:Autocrypt:asText:all";
        public static final String AUTOCRYPT_DRAFT_STATE = "header:Autocrypt-Draft-State:asText";
        public static final String AUTOCRYPT_SETUP_MESSAGE = "header:Autocrypt-Setup-Message:asText";

        private Property() {

        }
    }

    public static final class Properties {
        public static final String[] THREAD_ID = new String[]{Property.THREAD_ID};
        public static final String[] MUTABLE = new String[]{Property.KEYWORDS, Property.MAILBOX_IDS};

        private Properties() {

        }
    }
}
