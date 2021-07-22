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

package rs.ltt.jmap.mua.util;

import rs.ltt.jmap.client.session.Session;
import rs.ltt.jmap.common.entity.Attachment;
import rs.ltt.jmap.common.entity.EmailBodyPart;
import rs.ltt.jmap.common.entity.capability.MailAccountCapability;

import java.util.Collection;

public final class AttachmentUtil {

    private AttachmentUtil() {

    }

    public static EmailBodyPart toEmailBodyPart(final Attachment attachment) {
        return EmailBodyPart.builder()
                .blobId(attachment.getBlobId())
                .charset(attachment.getCharset())
                .type(attachment.getType())
                .name(attachment.getName())
                .size(attachment.getSize())
                .build();
    }

    public static void verifyAttachmentsDoNotExceedLimit(final Session session,
                                                  final String account,
                                                  final Collection<? extends Attachment> attachments) {
        final long combinedAttachmentSize = attachments.stream().map(a -> Math.max(0, a.getSize())).reduce(0L, Long::sum);
        final MailAccountCapability capability = session.getAccountCapability(account, MailAccountCapability.class);
        final Long maxSizeAttachments = capability == null ? null : capability.getMaxSizeAttachmentsPerEmail();
        if (maxSizeAttachments != null && combinedAttachmentSize > maxSizeAttachments) {
            throw new CombinedAttachmentSizeExceedsLimitException(maxSizeAttachments);
        }
    }

    public static class CombinedAttachmentSizeExceedsLimitException extends RuntimeException {
        private final long limit;

        private CombinedAttachmentSizeExceedsLimitException(final long limit) {
            super(String.format("The combined size of all attachments exceeds limit of %d", limit));
            this.limit = limit;
        }

        public long getLimit() {
            return limit;
        }
    }
}
