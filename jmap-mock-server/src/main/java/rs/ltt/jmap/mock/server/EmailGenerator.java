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

package rs.ltt.jmap.mock.server;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import rs.ltt.jmap.common.entity.Email;
import rs.ltt.jmap.common.entity.EmailAddress;
import rs.ltt.jmap.common.entity.EmailBodyPart;
import rs.ltt.jmap.common.entity.EmailBodyValue;
import rs.ltt.jmap.mua.util.EmailUtil;

public class EmailGenerator {

    private static final ZoneOffset DEFAULT_ZONE_OFFSET = ZoneOffset.ofHours(1);

    private static final Instant START_DATE = Instant.ofEpochSecond(1605800000);

    private static final List<String> SUBJECT_TEMPLATES =
            Arrays.asList(
                    "Lorem ipsum dolor sit",
                    "Maecenas eleifend rhoncus",
                    "Vivamus in convallis",
                    "Aliquam luctus a elit",
                    "Sed sit amet fringilla",
                    "Quisque dictum turpis at");

    private static final List<String> BODY_TEMPLATES =
            Arrays.asList(
                    "Vestibulum imperdiet tortor est, nec aliquam ligula volutpat at. Cras et"
                        + " malesuada sapien. Suspendisse sit amet luctus risus, in ultrices urna."
                        + " Curabitur et maximus ante. Integer sit amet nulla turpis. Nulla"
                        + " pharetra felis et sem tempus laoreet. Suspendisse et facilisis quam,"
                        + " sed bibendum enim.",
                    "In in orci non sapien tempus ullamcorper id a mi. Nullam sit amet nisl"
                        + " viverra, dapibus ante in, bibendum ante. Aenean sodales ipsum nec enim"
                        + " ornare, vitae lacinia mauris venenatis. Integer gravida sollicitudin"
                        + " orci, eget porta nibh tempus ornare. Sed at convallis dui. Donec"
                        + " mattis rhoncus pharetra. Quisque sed libero posuere, malesuada magna"
                        + " non, hendrerit justo. Nulla eget sem eget enim porttitor tempus"
                        + " imperdiet nec ligula.",
                    "Nulla vitae tempus ligula. Lorem ipsum dolor sit amet, consectetur adipiscing"
                            + " elit. Vivamus vel neque vitae turpis molestie mollis. Suspendisse"
                            + " auctor ullamcorper felis, non auctor velit pretium ut. Sed sit amet"
                            + " ultricies dui. Ut bibendum sit amet mi vitae lacinia. Ut non nisi"
                            + " lacus.",
                    "Duis et mattis purus, at sodales tellus. Suspendisse et auctor arcu. Maecenas"
                        + " vel urna blandit, fringilla velit a, consectetur metus. Pellentesque"
                        + " bibendum ullamcorper purus commodo consectetur. Nulla facilisi."
                        + " Integer aliquet posuere ante quis faucibus. Duis augue ante, varius"
                        + " vel molestie et, consequat eget diam. Mauris tincidunt ac est non"
                        + " sollicitudin. Fusce aliquet feugiat facilisis. Maecenas venenatis,"
                        + " lectus vel dapibus pellentesque, lacus orci pharetra est, sed"
                        + " vulputate erat quam eget libero.",
                    "Vivamus rhoncus dictum lectus, sed bibendum nisi. Sed sodales nibh ac nunc"
                            + " porttitor, sit amet accumsan nisi sagittis. Donec imperdiet metus"
                            + " molestie diam scelerisque vestibulum. Sed aliquam eros sapien, sed"
                            + " hendrerit mi tincidunt sit amet. Cras nec massa nulla. Sed id mi"
                            + " imperdiet ante finibus lobortis. Duis suscipit nulla ac vehicula"
                            + " maximus.");

    public static Email getOnTop(
            final EmailAddress account, final String mailboxId, final int index) {
        final String id = UUID.randomUUID().toString();
        final String threadId = UUID.randomUUID().toString();
        final Instant receivedAt =
                START_DATE.plus(Duration.ofDays(1)).plus(Duration.ofMinutes(index));
        return get(account, id, threadId, receivedAt, mailboxId, index, index, 0, 1);
    }

    public static Email get(
            final EmailAddress account,
            final String mailboxId,
            int index,
            int thread,
            int posInThread,
            int numInThread) {
        final String id = String.format("M%d", index);
        final String threadId = String.format("T%d", thread);
        final Instant receivedAt =
                START_DATE.minus(Duration.ofDays(thread)).plus(Duration.ofHours(posInThread));
        return get(
                account,
                id,
                threadId,
                receivedAt,
                mailboxId,
                index,
                thread,
                posInThread,
                numInThread);
    }

    public static Email get(
            final EmailAddress account,
            final String id,
            final String threadId,
            final Instant receivedAt,
            final String mailboxId,
            int index,
            int thread,
            int posInThread,
            int numInThread) {
        final EmailAddress from = NameGenerator.getEmailAddress(index);
        final ArrayList<EmailAddress> recipients = new ArrayList<>();
        final int threadStartsAt = index - posInThread;
        final int threadEndsAt = threadStartsAt + numInThread;
        for (int i = threadStartsAt; i < threadEndsAt; ++i) {
            if (i != index) {
                recipients.add(NameGenerator.getEmailAddress(i));
            }
        }
        recipients.add(account);

        final String subject = SUBJECT_TEMPLATES.get(thread % SUBJECT_TEMPLATES.size());

        final String body = BODY_TEMPLATES.get(index % BODY_TEMPLATES.size());

        final EmailBodyValue emailBodyValue = EmailBodyValue.builder().value(body).build();
        final String partId = "0";
        final EmailBodyPart emailBodyPart =
                EmailBodyPart.builder().partId(partId).type("text/plain").build();
        return Email.builder()
                .id(id)
                .threadId(threadId)
                .receivedAt(receivedAt)
                .sentAt(receivedAt.atOffset(DEFAULT_ZONE_OFFSET))
                .to(recipients)
                .mailboxId(mailboxId, true)
                .from(from)
                .subject(posInThread == 0 ? subject : EmailUtil.subjectWithPrefix(subject))
                .preview(body.substring(0, Math.max(subject.length(), 256)))
                .bodyValue(partId, emailBodyValue)
                .textBody(emailBodyPart)
                .build();
    }
}
