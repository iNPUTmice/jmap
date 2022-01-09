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

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import okhttp3.HttpUrl;
import okio.Buffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rs.ltt.jmap.common.Request;
import rs.ltt.jmap.common.Response;
import rs.ltt.jmap.common.entity.*;
import rs.ltt.jmap.common.entity.Thread;
import rs.ltt.jmap.common.entity.filter.EmailFilterCondition;
import rs.ltt.jmap.common.entity.filter.Filter;
import rs.ltt.jmap.common.method.MethodResponse;
import rs.ltt.jmap.common.method.call.core.GetPushSubscriptionMethodCall;
import rs.ltt.jmap.common.method.call.core.SetPushSubscriptionMethodCall;
import rs.ltt.jmap.common.method.call.email.*;
import rs.ltt.jmap.common.method.call.identity.GetIdentityMethodCall;
import rs.ltt.jmap.common.method.call.mailbox.ChangesMailboxMethodCall;
import rs.ltt.jmap.common.method.call.mailbox.GetMailboxMethodCall;
import rs.ltt.jmap.common.method.call.mailbox.SetMailboxMethodCall;
import rs.ltt.jmap.common.method.call.thread.ChangesThreadMethodCall;
import rs.ltt.jmap.common.method.call.thread.GetThreadMethodCall;
import rs.ltt.jmap.common.method.error.*;
import rs.ltt.jmap.common.method.response.core.SetPushSubscriptionMethodResponse;
import rs.ltt.jmap.common.method.response.email.*;
import rs.ltt.jmap.common.method.response.identity.GetIdentityMethodResponse;
import rs.ltt.jmap.common.method.response.mailbox.ChangesMailboxMethodResponse;
import rs.ltt.jmap.common.method.response.mailbox.GetMailboxMethodResponse;
import rs.ltt.jmap.common.method.response.mailbox.SetMailboxMethodResponse;
import rs.ltt.jmap.common.method.response.thread.ChangesThreadMethodResponse;
import rs.ltt.jmap.common.method.response.thread.GetThreadMethodResponse;
import rs.ltt.jmap.common.websocket.StateChangeWebSocketMessage;
import rs.ltt.jmap.mock.server.util.FuzzyRoleParser;
import rs.ltt.jmap.mua.util.MailboxUtil;

public class MockMailServer extends StubMailServer {

    private static final String VERIFICATION_CODE = "stub-verification-code";

    private static final Logger LOGGER = LoggerFactory.getLogger(MockMailServer.class);

    protected final Map<String, Email> emails = new HashMap<>();
    protected final Map<String, byte[]> inMemoryAttachments = new HashMap<>();
    protected final Map<String, MailboxInfo> mailboxes = new HashMap<>();
    protected final Map<String, PushSubscription> pushSubscriptions = new HashMap<>();

    protected final LinkedHashMap<String, Update> updates = new LinkedHashMap<>();

    private int state = 0;

    private boolean reportCanCalculateQueryChanges = false;

    public MockMailServer(final int numThreads, final int accountIndex) {
        super(accountIndex);
        setup(numThreads, (accountIndex * 2048) + accountIndex);
    }

    @Override
    protected Buffer getDownloadBuffer(final String blobId) throws IOException {
        final byte[] attachment = this.inMemoryAttachments.get(blobId);
        if (attachment != null) {
            return new Buffer().readFrom(new ByteArrayInputStream(attachment));
        }
        return super.getDownloadBuffer(blobId);
    }

    protected void setup(final int numThreads, final int offset) {
        this.mailboxes.putAll(Maps.uniqueIndex(generateMailboxes(), MailboxInfo::getId));
        generateEmail(numThreads, offset);
    }

    protected List<MailboxInfo> generateMailboxes() {
        return Collections.singletonList(
                new MailboxInfo(UUID.randomUUID().toString(), "Inbox", Role.INBOX));
    }

    protected void generateEmail(final int numThreads, final int offset) {
        final String mailboxId = MailboxUtil.find(mailboxes.values(), Role.INBOX).getId();
        int emailCount = offset;
        for (int thread = 0; thread < numThreads; ++thread) {
            final int numInThread = (thread % 4) + 1;
            for (int i = 0; i < numInThread; ++i) {
                final Email email =
                        EmailGenerator.get(account, mailboxId, emailCount, thread, i, numInThread);
                this.emails.put(email.getId(), email);
                emailCount++;
            }
        }
    }

    public MockMailServer(final int numThreads) {
        super(0);
        setup(numThreads, 0);
    }

    public Email generateEmailOnTop() {
        final Email email =
                EmailGenerator.getOnTop(
                        account,
                        MailboxUtil.find(mailboxes.values(), Role.INBOX).getId(),
                        emails.size());
        createEmail(email);
        return email;
    }

    private void createEmail(final Email email) {
        final String oldVersion = getState();
        emails.put(email.getId(), email);
        incrementState();
        final String newVersion = getState();
        this.pushUpdate(oldVersion, Update.created(email, newVersion));
    }

    private void pushUpdate(final String oldVersion, final Update update) {
        this.updates.put(oldVersion, update);
        final ImmutableMap.Builder<Class<? extends AbstractIdentifiableEntity>, String>
                changedBuilder = ImmutableMap.builder();
        changedBuilder.put(Thread.class, update.getNewVersion());
        changedBuilder.put(Email.class, update.getNewVersion());
        changedBuilder.put(Mailbox.class, update.getNewVersion());
        final StateChangeWebSocketMessage stateChange =
                new StateChangeWebSocketMessage(
                        ImmutableMap.of(getAccountId(), changedBuilder.build()),
                        update.getNewVersion());
        final StateChange stateChangeMessage =
                StateChange.builder().changed(getAccountId(), changedBuilder.build()).build();
        pushSubscriptions.values().stream()
                .filter(ps -> VERIFICATION_CODE.equals(ps.getVerificationCode()))
                .map(PushSubscription::getUrl)
                .forEach(url -> Pusher.push(HttpUrl.get(url), stateChangeMessage));
        final String message = GSON.toJson(stateChange);
        pushEnabledWebSockets.forEach(webSocket -> webSocket.send(message));
    }

    protected void incrementState() {
        this.state++;
    }

    protected String getState() {
        return String.valueOf(this.state);
    }

    public void setReportCanCalculateQueryChanges(final boolean reportCanCalculateQueryChanges) {
        this.reportCanCalculateQueryChanges = reportCanCalculateQueryChanges;
    }

    @Override
    protected MethodResponse[] execute(
            SetPushSubscriptionMethodCall methodCall,
            ListMultimap<String, Response.Invocation> previousResponses) {
        final SetPushSubscriptionMethodResponse.SetPushSubscriptionMethodResponseBuilder
                responseBuilder = SetPushSubscriptionMethodResponse.builder();
        final Map<String, PushSubscription> create = methodCall.getCreate();
        final Map<String, Map<String, Object>> update = methodCall.getUpdate();
        final String[] destroy = methodCall.getDestroy();
        if (destroy != null && destroy.length > 0) {
            throw new IllegalStateException(
                    "MockServer does not know how to destroy PushSubscriptions");
        }
        if (create != null && create.size() > 0) {
            processCreatePushSubscription(create, responseBuilder);
        }
        if (update != null && update.size() > 0) {
            processUpdatePushSubscription(update, responseBuilder);
        }
        return new MethodResponse[] {responseBuilder.build()};
    }

    private void processUpdatePushSubscription(
            Map<String, Map<String, Object>> update,
            SetPushSubscriptionMethodResponse.SetPushSubscriptionMethodResponseBuilder
                    responseBuilder) {
        for (final Map.Entry<String, Map<String, Object>> entry : update.entrySet()) {
            final String id = entry.getKey();
            Map<String, Object> patch = entry.getValue();
            try {
                final PushSubscription modifiedPushSubscription = patchPushSubscription(id, patch);
                responseBuilder.updated(id, modifiedPushSubscription);
                this.pushSubscriptions.put(
                        modifiedPushSubscription.getId(), modifiedPushSubscription);
            } catch (final IllegalArgumentException e) {
                responseBuilder.notUpdated(
                        id, new SetError(SetErrorType.INVALID_PROPERTIES, e.getMessage()));
            }
        }
    }

    private PushSubscription patchPushSubscription(
            final String id, final Map<String, Object> patches) {
        final PushSubscription current = this.pushSubscriptions.get(id);
        if (current == null) {
            throw new IllegalArgumentException(
                    String.format("No PushSubscription found with id %s", id));
        }
        for (final Map.Entry<String, Object> patch : patches.entrySet()) {
            final String fullPath = patch.getKey();
            final Object modification = patch.getValue();
            if ("verificationCode".equals(fullPath)) {
                if (modification instanceof String) {
                    final String code = (String) modification;
                    return current.toBuilder().verificationCode(code).build();
                } else {
                    throw new IllegalArgumentException("verificationCode is not the correct type");
                }
            } else {
                throw new IllegalArgumentException("Unable to patch " + fullPath);
            }
        }
        return current;
    }

    private void processCreatePushSubscription(
            Map<String, PushSubscription> create,
            SetPushSubscriptionMethodResponse.SetPushSubscriptionMethodResponseBuilder
                    responseBuilder) {
        for (final Map.Entry<String, PushSubscription> entry : create.entrySet()) {
            final String createId = entry.getKey();
            final String id = UUID.randomUUID().toString();
            final PushSubscription pushSubscription = entry.getValue().toBuilder().id(id).build();
            this.pushSubscriptions.put(id, pushSubscription);
            final String url = pushSubscription.getUrl();
            PushVerification pushVerification =
                    PushVerification.builder()
                            .pushSubscriptionId(id)
                            .verificationCode(VERIFICATION_CODE)
                            .build();
            final HttpUrl httpUrl = url == null ? null : HttpUrl.get(url);
            LOGGER.info("Sending PushVerification to {}", httpUrl);
            if (httpUrl != null) {
                if (!Pusher.push(httpUrl, pushVerification)) {
                    LOGGER.info("Failed to send Push Verification");
                }
            }
            responseBuilder.created(createId, pushSubscription);
        }
    }

    @Override
    protected MethodResponse[] execute(
            GetPushSubscriptionMethodCall methodCall,
            ListMultimap<String, Response.Invocation> previousResponses) {
        return new MethodResponse[] {new UnknownMethodMethodErrorResponse()};
    }

    @Override
    protected MethodResponse[] execute(
            ChangesEmailMethodCall methodCall,
            ListMultimap<String, Response.Invocation> previousResponses) {
        final String since = methodCall.getSinceState();
        if (since != null && since.equals(getState())) {
            return new MethodResponse[] {
                ChangesEmailMethodResponse.builder()
                        .oldState(getState())
                        .newState(getState())
                        .updated(new String[0])
                        .created(new String[0])
                        .destroyed(new String[0])
                        .build()
            };
        } else {
            final Update update = getAccumulatedUpdateSince(since);
            if (update == null) {
                return new MethodResponse[] {new CannotCalculateChangesMethodErrorResponse()};
            } else {
                final Changes changes = update.getChangesFor(Email.class);
                return new MethodResponse[] {
                    ChangesEmailMethodResponse.builder()
                            .oldState(since)
                            .newState(update.getNewVersion())
                            .updated(changes == null ? new String[0] : changes.updated)
                            .created(changes == null ? new String[0] : changes.created)
                            .destroyed(new String[0])
                            .hasMoreChanges(!update.getNewVersion().equals(getState()))
                            .build()
                };
            }
        }
    }

    private Update getAccumulatedUpdateSince(final String oldVersion) {
        final ArrayList<Update> updates = new ArrayList<>();
        for (Map.Entry<String, Update> updateEntry : this.updates.entrySet()) {
            if (updateEntry.getKey().equals(oldVersion) || updates.size() > 0) {
                updates.add(updateEntry.getValue());
            }
        }
        if (updates.isEmpty()) {
            return null;
        }
        return Update.merge(updates);
    }

    @Override
    protected MethodResponse[] execute(
            GetEmailMethodCall methodCall,
            ListMultimap<String, Response.Invocation> previousResponses) {
        final Request.Invocation.ResultReference idsReference = methodCall.getIdsReference();
        final List<String> ids;
        if (idsReference != null) {
            try {
                ids =
                        Arrays.asList(
                                ResultReferenceResolver.resolve(idsReference, previousResponses));
            } catch (final IllegalArgumentException e) {
                return new MethodResponse[] {new InvalidResultReferenceMethodErrorResponse()};
            }
        } else {
            ids = Arrays.asList(methodCall.getIds());
        }
        final String[] properties = methodCall.getProperties();
        Stream<Email> emailStream = ids.stream().map(emails::get);
        if (Arrays.equals(properties, Email.Properties.THREAD_ID)) {
            emailStream =
                    emailStream.map(
                            email ->
                                    Email.builder()
                                            .id(email.getId())
                                            .threadId(email.getThreadId())
                                            .build());
        } else if (Arrays.equals(properties, Email.Properties.MUTABLE)) {
            emailStream =
                    emailStream.map(
                            email ->
                                    Email.builder()
                                            .id(email.getId())
                                            .keywords(email.getKeywords())
                                            .mailboxIds(email.getMailboxIds())
                                            .build());
        }
        return new MethodResponse[] {
            GetEmailMethodResponse.builder()
                    .list(emailStream.toArray(Email[]::new))
                    .state(getState())
                    .build()
        };
    }

    @Override
    protected MethodResponse[] execute(
            QueryChangesEmailMethodCall methodCall,
            ListMultimap<String, Response.Invocation> previousResponses) {
        final String since = methodCall.getSinceQueryState();
        if (since != null && since.equals(getState())) {
            return new MethodResponse[] {
                QueryChangesEmailMethodResponse.builder()
                        .oldQueryState(getState())
                        .newQueryState(getState())
                        .added(Collections.emptyList())
                        .removed(new String[0])
                        .build()
            };
        } else {
            return new MethodResponse[] {new CannotCalculateChangesMethodErrorResponse()};
        }
    }

    @Override
    protected MethodResponse[] execute(
            QueryEmailMethodCall methodCall,
            ListMultimap<String, Response.Invocation> previousResponses) {
        final Filter<Email> filter = methodCall.getFilter();
        Stream<Email> stream = emails.values().stream();
        stream = applyFilter(filter, stream);

        // sort
        stream = stream.sorted(Comparator.comparing(Email::getReceivedAt).reversed());

        if (Boolean.TRUE.equals(methodCall.getCollapseThreads())) {
            stream = stream.filter(distinctByKey(Email::getThreadId));
        }
        final List<String> ids = stream.map(Email::getId).collect(Collectors.toList());
        final String anchor = methodCall.getAnchor();
        final int position;
        if (anchor != null) {
            final Long anchorOffset = methodCall.getAnchorOffset();
            final int anchorPosition = ids.indexOf(anchor);
            if (anchorPosition == -1) {
                return new MethodResponse[] {new AnchorNotFoundMethodErrorResponse()};
            }
            position = Math.toIntExact(anchorPosition + (anchorOffset == null ? 0 : anchorOffset));
        } else {
            position =
                    Math.toIntExact(
                            methodCall.getPosition() == null ? 0 : methodCall.getPosition());
        }
        final int limit =
                Math.toIntExact(methodCall.getLimit() == null ? 40 : methodCall.getLimit());
        final int endPosition = Math.min(position + limit, ids.size());
        final String[] page = ids.subList(position, endPosition).toArray(new String[0]);
        LOGGER.info(
                "query email page between {} and {} (inclusive). Page contains {} items",
                position,
                endPosition - 1,
                page.length);
        final Long total =
                Boolean.TRUE.equals(methodCall.getCalculateTotal()) ? (long) ids.size() : null;
        return new MethodResponse[] {
            QueryEmailMethodResponse.builder()
                    .canCalculateChanges(this.reportCanCalculateQueryChanges)
                    .queryState(getState())
                    .total(total)
                    .ids(page)
                    .position((long) position)
                    .build()
        };
    }

    private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        final Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

    private static Stream<Email> applyFilter(
            final Filter<Email> filter, Stream<Email> emailStream) {
        if (filter instanceof EmailFilterCondition) {
            final EmailFilterCondition emailFilterCondition = (EmailFilterCondition) filter;
            final String inMailbox = emailFilterCondition.getInMailbox();
            if (inMailbox != null) {
                emailStream =
                        emailStream.filter(email -> email.getMailboxIds().containsKey(inMailbox));
            }
            final String[] header = emailFilterCondition.getHeader();
            if (header != null
                    && header.length == 2
                    && header[0].equals("Autocrypt-Setup-Message")) {
                emailStream =
                        emailStream.filter(
                                email -> header[1].equals(email.getAutocryptSetupMessage()));
            }
        }
        return emailStream;
    }

    @Override
    protected MethodResponse[] execute(
            SetEmailMethodCall methodCall,
            ListMultimap<String, Response.Invocation> previousResponses) {
        final String ifInState = methodCall.getIfInState();
        final Map<String, Map<String, Object>> update = methodCall.getUpdate();
        final Map<String, Email> create = methodCall.getCreate();
        final String[] destroy = methodCall.getDestroy();
        if (destroy != null && destroy.length > 0) {
            throw new IllegalStateException("MockMailServer does not know how to destroy");
        }
        final SetEmailMethodResponse.SetEmailMethodResponseBuilder responseBuilder =
                SetEmailMethodResponse.builder();
        final String oldState = getState();
        if (ifInState != null) {
            if (!ifInState.equals(oldState)) {
                return new MethodResponse[] {new StateMismatchMethodErrorResponse()};
            }
        }
        if (update != null) {
            final List<Email> modifiedEmails = new ArrayList<>();
            for (final Map.Entry<String, Map<String, Object>> entry : update.entrySet()) {
                final String id = entry.getKey();
                try {
                    final Email modifiedEmail = patchEmail(id, entry.getValue(), previousResponses);
                    modifiedEmails.add(modifiedEmail);
                    responseBuilder.updated(id, modifiedEmail);
                } catch (final IllegalArgumentException e) {
                    responseBuilder.notUpdated(
                            id, new SetError(SetErrorType.INVALID_PROPERTIES, e.getMessage()));
                }
            }
            for (final Email email : modifiedEmails) {
                emails.put(email.getId(), email);
            }
            incrementState();
            final String newState = getState();
            updates.put(
                    oldState, Update.updated(modifiedEmails, this.mailboxes.keySet(), newState));
        }
        if (create != null && create.size() > 0) {
            processCreateEmail(create, responseBuilder, previousResponses);
        }
        return new MethodResponse[] {responseBuilder.build()};
    }

    private void processCreateEmail(
            Map<String, Email> create,
            SetEmailMethodResponse.SetEmailMethodResponseBuilder responseBuilder,
            ListMultimap<String, Response.Invocation> previousResponses) {
        for (final Map.Entry<String, Email> entry : create.entrySet()) {
            final String createId = entry.getKey();
            final String id = UUID.randomUUID().toString();
            final String threadId = UUID.randomUUID().toString();
            final Email userSuppliedEmail = entry.getValue();
            final Map<String, Boolean> mailboxMap = userSuppliedEmail.getMailboxIds();
            final Email.EmailBuilder emailBuilder =
                    userSuppliedEmail.toBuilder()
                            .id(id)
                            .threadId(threadId)
                            .receivedAt(Instant.now());
            emailBuilder.clearMailboxIds();
            for (Map.Entry<String, Boolean> mailboxEntry : mailboxMap.entrySet()) {
                final String mailboxId =
                        CreationIdResolver.resolveIfNecessary(
                                mailboxEntry.getKey(), previousResponses);
                emailBuilder.mailboxId(mailboxId, mailboxEntry.getValue());
            }
            final List<EmailBodyPart> attachments = userSuppliedEmail.getAttachments();
            emailBuilder.clearAttachments();
            if (attachments != null) {
                for (final EmailBodyPart attachment : attachments) {
                    final String partId = attachment.getPartId();
                    final EmailBodyValue value =
                            partId == null ? null : userSuppliedEmail.getBodyValues().get(partId);
                    if (value != null) {
                        final EmailBodyPart emailBodyPart = injectId(attachment);
                        this.inMemoryAttachments.put(
                                emailBodyPart.getBlobId(),
                                value.getValue().getBytes(StandardCharsets.UTF_8));
                        emailBuilder.attachment(emailBodyPart);
                    } else {
                        emailBuilder.attachment(attachment);
                    }
                }
            }
            final Email email = emailBuilder.build();

            createEmail(email);
            responseBuilder.created(createId, email);
        }
    }

    private static EmailBodyPart injectId(final Attachment attachment) {
        return EmailBodyPart.builder()
                .blobId(UUID.randomUUID().toString())
                .charset(attachment.getCharset())
                .type(attachment.getType())
                .name(attachment.getName())
                .size(attachment.getSize())
                .build();
    }

    @Override
    protected MethodResponse[] execute(
            GetIdentityMethodCall methodCall,
            ListMultimap<String, Response.Invocation> previousResponses) {
        return new MethodResponse[] {
            GetIdentityMethodResponse.builder()
                    .list(
                            new Identity[] {
                                Identity.builder()
                                        .id(getAccountId())
                                        .email(account.getEmail())
                                        .name(account.getName())
                                        .build()
                            })
                    .build()
        };
    }

    @Override
    protected MethodResponse[] execute(
            ChangesMailboxMethodCall methodCall,
            ListMultimap<String, Response.Invocation> previousResponses) {
        final String since = methodCall.getSinceState();
        if (since != null && since.equals(getState())) {
            return new MethodResponse[] {
                ChangesMailboxMethodResponse.builder()
                        .oldState(getState())
                        .newState(getState())
                        .updated(new String[0])
                        .created(new String[0])
                        .destroyed(new String[0])
                        .updatedProperties(new String[0])
                        .build()
            };
        } else {
            final Update update = getAccumulatedUpdateSince(since);
            if (update == null) {
                return new MethodResponse[] {new CannotCalculateChangesMethodErrorResponse()};
            } else {
                final Changes changes = update.getChangesFor(Mailbox.class);
                return new MethodResponse[] {
                    ChangesMailboxMethodResponse.builder()
                            .oldState(since)
                            .newState(update.getNewVersion())
                            .updated(changes.updated)
                            .created(changes.created)
                            .destroyed(new String[0])
                            .hasMoreChanges(!update.getNewVersion().equals(getState()))
                            .build()
                };
            }
        }
    }

    @Override
    protected MethodResponse[] execute(
            GetMailboxMethodCall methodCall,
            ListMultimap<String, Response.Invocation> previousResponses) {
        final Request.Invocation.ResultReference idsReference = methodCall.getIdsReference();
        final List<String> ids;
        if (idsReference != null) {
            try {
                ids =
                        Arrays.asList(
                                ResultReferenceResolver.resolve(idsReference, previousResponses));
            } catch (final IllegalArgumentException e) {
                return new MethodResponse[] {new InvalidResultReferenceMethodErrorResponse()};
            }
        } else {
            final String[] idsParameter = methodCall.getIds();
            ids = idsParameter == null ? null : Arrays.asList(idsParameter);
        }
        Stream<Mailbox> mailboxStream = mailboxes.values().stream().map(this::toMailbox);
        return new MethodResponse[] {
            GetMailboxMethodResponse.builder()
                    .list(
                            mailboxStream
                                    .filter(m -> ids == null || ids.contains(m.getId()))
                                    .toArray(Mailbox[]::new))
                    .state(getState())
                    .build()
        };
    }

    private Mailbox toMailbox(MailboxInfo mailboxInfo) {
        return Mailbox.builder()
                .id(mailboxInfo.getId())
                .name(mailboxInfo.name)
                .role(mailboxInfo.role)
                .totalEmails(
                        emails.values().stream()
                                .filter(e -> e.getMailboxIds().containsKey(mailboxInfo.getId()))
                                .count())
                .unreadEmails(
                        emails.values().stream()
                                .filter(e -> e.getMailboxIds().containsKey(mailboxInfo.getId()))
                                .filter(e -> !e.getKeywords().containsKey(Keyword.SEEN))
                                .count())
                .totalThreads(
                        emails.values().stream()
                                .filter(e -> e.getMailboxIds().containsKey(mailboxInfo.getId()))
                                .map(Email::getThreadId)
                                .distinct()
                                .count())
                .unreadThreads(
                        emails.values().stream()
                                .filter(e -> e.getMailboxIds().containsKey(mailboxInfo.getId()))
                                .filter(e -> !e.getKeywords().containsKey(Keyword.SEEN))
                                .map(Email::getThreadId)
                                .distinct()
                                .count())
                .build();
    }

    protected MethodResponse[] execute(
            final SetMailboxMethodCall methodCall,
            ListMultimap<String, Response.Invocation> previousResponses) {
        final String ifInState = methodCall.getIfInState();
        final SetMailboxMethodResponse.SetMailboxMethodResponseBuilder responseBuilder =
                SetMailboxMethodResponse.builder();
        final Map<String, Mailbox> create = methodCall.getCreate();
        final Map<String, Map<String, Object>> update = methodCall.getUpdate();
        final String oldState = getState();
        if (ifInState != null) {
            if (!ifInState.equals(oldState)) {
                return new MethodResponse[] {new StateMismatchMethodErrorResponse()};
            }
        }

        if (create != null && create.size() > 0) {
            processCreateMailbox(create, responseBuilder);
        }
        if (update != null && update.size() > 0) {
            processUpdateMailbox(update, responseBuilder, previousResponses);
        }
        incrementState();
        final SetMailboxMethodResponse setMailboxResponse = responseBuilder.build();
        updates.put(oldState, Update.of(setMailboxResponse, getState()));
        return new MethodResponse[] {setMailboxResponse};
    }

    private void processCreateMailbox(
            final Map<String, Mailbox> create,
            final SetMailboxMethodResponse.SetMailboxMethodResponseBuilder responseBuilder) {
        for (Map.Entry<String, Mailbox> entry : create.entrySet()) {
            final String createId = entry.getKey();
            final Mailbox mailbox = entry.getValue();
            final String name = mailbox.getName();
            if (mailboxes.values().stream()
                    .anyMatch(mailboxInfo -> mailboxInfo.getName().equals(name))) {
                responseBuilder.notCreated(
                        createId,
                        new SetError(
                                SetErrorType.INVALID_PROPERTIES,
                                "A mailbox with the name " + name + " already exists"));
                continue;
            }
            final String id = UUID.randomUUID().toString();
            final MailboxInfo mailboxInfo = new MailboxInfo(id, name, mailbox.getRole());
            this.mailboxes.put(id, mailboxInfo);
            responseBuilder.created(createId, toMailbox(mailboxInfo));
        }
    }

    private void processUpdateMailbox(
            Map<String, Map<String, Object>> update,
            SetMailboxMethodResponse.SetMailboxMethodResponseBuilder responseBuilder,
            ListMultimap<String, Response.Invocation> previousResponses) {
        for (final Map.Entry<String, Map<String, Object>> entry : update.entrySet()) {
            final String id = entry.getKey();
            try {
                final MailboxInfo modifiedMailbox =
                        patchMailbox(id, entry.getValue(), previousResponses);
                responseBuilder.updated(id, toMailbox(modifiedMailbox));
                this.mailboxes.put(modifiedMailbox.getId(), modifiedMailbox);
            } catch (final IllegalArgumentException e) {
                responseBuilder.notUpdated(
                        id, new SetError(SetErrorType.INVALID_PROPERTIES, e.getMessage()));
            }
        }
    }

    private MailboxInfo patchMailbox(
            final String id,
            final Map<String, Object> patches,
            ListMultimap<String, Response.Invocation> previousResponses) {
        final MailboxInfo currentMailbox = this.mailboxes.get(id);
        for (final Map.Entry<String, Object> patch : patches.entrySet()) {
            final String fullPath = patch.getKey();
            final Object modification = patch.getValue();
            final List<String> pathParts = Splitter.on('/').splitToList(fullPath);
            final String parameter = pathParts.get(0);
            if ("role".equals(parameter)) {
                final Role role = FuzzyRoleParser.parse((String) modification);
                return new MailboxInfo(currentMailbox.getId(), currentMailbox.getName(), role);
            } else {
                throw new IllegalArgumentException("Unable to patch " + fullPath);
            }
        }
        return currentMailbox;
    }

    @Override
    protected MethodResponse[] execute(
            ChangesThreadMethodCall methodCall,
            ListMultimap<String, Response.Invocation> previousResponses) {
        final String since = methodCall.getSinceState();
        if (since != null && since.equals(getState())) {
            return new MethodResponse[] {
                ChangesThreadMethodResponse.builder()
                        .oldState(getState())
                        .newState(getState())
                        .updated(new String[0])
                        .created(new String[0])
                        .destroyed(new String[0])
                        .build()
            };
        } else {
            final Update update = getAccumulatedUpdateSince(since);
            if (update == null) {
                return new MethodResponse[] {new CannotCalculateChangesMethodErrorResponse()};
            } else {
                final Changes changes = update.getChangesFor(Thread.class);
                return new MethodResponse[] {
                    ChangesThreadMethodResponse.builder()
                            .oldState(since)
                            .newState(update.getNewVersion())
                            .updated(changes == null ? new String[0] : changes.updated)
                            .created(changes == null ? new String[0] : changes.created)
                            .destroyed(new String[0])
                            .hasMoreChanges(!update.getNewVersion().equals(getState()))
                            .build()
                };
            }
        }
    }

    @Override
    protected MethodResponse[] execute(
            GetThreadMethodCall methodCall,
            ListMultimap<String, Response.Invocation> previousResponses) {
        final Request.Invocation.ResultReference idsReference = methodCall.getIdsReference();
        final List<String> ids;
        if (idsReference != null) {
            try {
                ids =
                        Arrays.asList(
                                ResultReferenceResolver.resolve(idsReference, previousResponses));
            } catch (final IllegalArgumentException e) {
                return new MethodResponse[] {new InvalidResultReferenceMethodErrorResponse()};
            }
        } else {
            ids = Arrays.asList(methodCall.getIds());
        }
        final Thread[] threads =
                ids.stream()
                        .map(
                                threadId ->
                                        Thread.builder()
                                                .id(threadId)
                                                .emailIds(
                                                        emails.values().stream()
                                                                .filter(
                                                                        email ->
                                                                                email.getThreadId()
                                                                                        .equals(
                                                                                                threadId))
                                                                .sorted(
                                                                        Comparator.comparing(
                                                                                Email
                                                                                        ::getReceivedAt))
                                                                .map(Email::getId)
                                                                .collect(Collectors.toList()))
                                                .build())
                        .toArray(Thread[]::new);
        return new MethodResponse[] {
            GetThreadMethodResponse.builder().list(threads).state(getState()).build()
        };
    }

    private Email patchEmail(
            final String id,
            final Map<String, Object> patches,
            ListMultimap<String, Response.Invocation> previousResponses) {
        final Email.EmailBuilder emailBuilder = emails.get(id).toBuilder();
        for (final Map.Entry<String, Object> patch : patches.entrySet()) {
            final String fullPath = patch.getKey();
            final Object modification = patch.getValue();
            final List<String> pathParts = Splitter.on('/').splitToList(fullPath);
            final String parameter = pathParts.get(0);
            if (parameter.equals("keywords")) {
                if (pathParts.size() == 2 && modification instanceof Boolean) {
                    final String keyword = pathParts.get(1);
                    final Boolean value = (Boolean) modification;
                    emailBuilder.keyword(keyword, value);
                } else {
                    throw new IllegalArgumentException(
                            "Keyword modification was not split into two parts");
                }
            } else if (parameter.equals("mailboxIds")) {
                if (pathParts.size() == 2 && modification instanceof Boolean) {
                    final String mailboxId = pathParts.get(1);
                    final Boolean value = (Boolean) modification;
                    emailBuilder.mailboxId(mailboxId, value);
                } else if (modification instanceof Map) {
                    final Map<String, Boolean> mailboxMap = (Map<String, Boolean>) modification;
                    emailBuilder.clearMailboxIds();
                    for (Map.Entry<String, Boolean> mailboxEntry : mailboxMap.entrySet()) {
                        final String mailboxId =
                                CreationIdResolver.resolveIfNecessary(
                                        mailboxEntry.getKey(), previousResponses);
                        emailBuilder.mailboxId(mailboxId, mailboxEntry.getValue());
                    }
                } else {
                    throw new IllegalArgumentException("Unknown patch object for path " + fullPath);
                }
            } else {
                throw new IllegalArgumentException("Unable to patch " + fullPath);
            }
        }
        return emailBuilder.build();
    }

    protected static class MailboxInfo implements IdentifiableMailboxWithRole {

        private final String id;
        private final String name;
        private final Role role;

        public MailboxInfo(final String id, String name, Role role) {
            this.id = id;
            this.name = name;
            this.role = role;
        }

        @Override
        public Role getRole() {
            return this.role;
        }

        public String getName() {
            return name;
        }

        @Override
        public String getId() {
            return id;
        }
    }
}
