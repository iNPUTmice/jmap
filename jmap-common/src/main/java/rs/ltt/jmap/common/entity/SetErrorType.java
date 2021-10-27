/*
 * Copyright 2020 Daniel Gultsch
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

import com.google.gson.annotations.SerializedName;

public enum SetErrorType {
    @SerializedName("forbidden")
    FORBIDDEN,
    @SerializedName("overQuota")
    OVER_QUOTA,
    @SerializedName("tooLarge")
    TOO_LARGE,
    @SerializedName("rateLimit")
    RATE_LIMIT,
    @SerializedName("notFound")
    NOT_FOUND,
    @SerializedName("invalidPatch")
    INVALID_PATCH,
    @SerializedName("willDestroy")
    WILL_DESTROY,
    @SerializedName("invalidProperties")
    INVALID_PROPERTIES,
    @SerializedName("singleton")
    SINGLETON,
    @SerializedName("mailboxHasEmail")
    MAILBOX_HAS_EMAIL,
    @SerializedName("mailboxHasChild")
    MAILBOX_HAS_CHILD,
    @SerializedName("blobNotFound")
    BLOB_NOT_FOUND,
    @SerializedName("tooManyKeywords")
    TOO_MANY_KEYWORDS,
    @SerializedName("tooManyMailboxes")
    TOO_MANY_MAILBOXES,
    @SerializedName("invalidEmail")
    INVALID_EMAIL,
    @SerializedName("forbiddenFrom")
    FORBIDDEN_FROM,
    @SerializedName("tooManyRecipients")
    TOO_MANY_RECIPIENTS,
    @SerializedName("noRecipients")
    NO_RECIPIENTS,
    @SerializedName("invalidRecipients")
    INVALID_RECIPIENTS,
    @SerializedName("forbiddenMailFrom")
    FORBIDDEN_MAIL_FROM,
    @SerializedName("forbiddenToSend")
    FORBIDDEN_TO_SEND,
    @SerializedName("cannotUnsend")
    CANNOT_UNSEND,
    @SerializedName("alreadyExists")
    ALREADY_EXISTS
}
