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
import com.google.common.collect.Collections2;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import rs.ltt.jmap.common.Request;
import rs.ltt.jmap.common.Response;
import rs.ltt.jmap.common.entity.Thread;
import rs.ltt.jmap.common.entity.*;
import rs.ltt.jmap.common.method.MethodResponse;
import rs.ltt.jmap.common.method.call.email.*;
import rs.ltt.jmap.common.method.call.identity.GetIdentityMethodCall;
import rs.ltt.jmap.common.method.call.mailbox.ChangesMailboxMethodCall;
import rs.ltt.jmap.common.method.call.mailbox.GetMailboxMethodCall;
import rs.ltt.jmap.common.method.call.mailbox.SetMailboxMethodCall;
import rs.ltt.jmap.common.method.call.thread.ChangesThreadMethodCall;
import rs.ltt.jmap.common.method.call.thread.GetThreadMethodCall;
import rs.ltt.jmap.common.method.error.CannotCalculateChangesMethodErrorResponse;
import rs.ltt.jmap.common.method.error.InvalidResultReferenceMethodErrorResponse;
import rs.ltt.jmap.common.method.error.StateMismatchMethodErrorResponse;
import rs.ltt.jmap.common.method.response.email.*;
import rs.ltt.jmap.common.method.response.identity.GetIdentityMethodResponse;
import rs.ltt.jmap.common.method.response.mailbox.ChangesMailboxMethodResponse;
import rs.ltt.jmap.common.method.response.mailbox.GetMailboxMethodResponse;
import rs.ltt.jmap.common.method.response.mailbox.SetMailboxMethodResponse;
import rs.ltt.jmap.common.method.response.thread.ChangesThreadMethodResponse;
import rs.ltt.jmap.common.method.response.thread.GetThreadMethodResponse;
import rs.ltt.jmap.mock.server.util.FuzzyRoleParser;
import rs.ltt.jmap.mua.util.MailboxUtil;

import java.util.Comparator;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MockMailServer extends StubMailServer {
    protected final Map<String, Email> emails = new HashMap<>();
    protected final Map<String, MailboxInfo> mailboxes = new HashMap<>();

    protected final Map<String, Update> updates = new HashMap<>();

    private int state = 0;

    private boolean reportCanCalculateQueryChanges = false;

    public MockMailServer(final int numThreads, final int accountIndex) {
        super(accountIndex);
        setup(numThreads, (accountIndex * 2048) + accountIndex);
    }

    public MockMailServer(final int numThreads) {
        super(0);
        setup(numThreads, 0);
    }

    protected void setup(final int numThreads, final int offset) {
        this.mailboxes.putAll(Maps.uniqueIndex(generateMailboxes(), MailboxInfo::getId));
        generateEmail(numThreads, offset);
    }

    protected List<MailboxInfo> generateMailboxes() {
        return Collections.singletonList(
                new MailboxInfo(UUID.randomUUID().toString(), "Inbox", Role.INBOX)
        );
    }

    protected void generateEmail(final int numThreads, final int offset) {
        final String mailboxId = MailboxUtil.find(mailboxes.values(), Role.INBOX).getId();
        int emailCount = offset;
        for (int thread = 0; thread < numThreads; ++thread) {
            final int numInThread = (thread % 4) + 1;
            for (int i = 0; i < numInThread; ++i) {
                final Email email = EmailGenerator.get(account, mailboxId, emailCount, thread, i, numInThread);
                this.emails.put(email.getId(), email);
                emailCount++;
            }
        }
    }

    public Email generateEmailOnTop() {
        final Email email = EmailGenerator.getOnTop(
                account,
                MailboxUtil.find(mailboxes.values(), Role.INBOX).getId(),
                emails.size()
        );
        final String oldVersion = getState();
        emails.put(email.getId(), email);
        incrementState();
        final String newVersion = getState();
        this.updates.put(oldVersion, Update.created(email, newVersion));
        return email;
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
    protected MethodResponse[] execute(GetIdentityMethodCall methodCall, ListMultimap<String, Response.Invocation> previousResponses) {
        return new MethodResponse[]{
                GetIdentityMethodResponse.builder()
                        .list(new Identity[]{Identity.builder()
                                .id(getAccountId())
                                .email(account.getEmail())
                                .name(account.getName())
                                .build()})
                        .build()

        };
    }

    @Override
    protected MethodResponse[] execute(ChangesEmailMethodCall methodCall, ListMultimap<String, Response.Invocation> previousResponses) {
        final String since = methodCall.getSinceState();
        if (since != null && since.equals(getState())) {
            return new MethodResponse[]{
                    ChangesEmailMethodResponse.builder()
                            .oldState(getState())
                            .newState(getState())
                            .updated(new String[0])
                            .created(new String[0])
                            .destroyed(new String[0])
                            .build()
            };
        } else {
            final Update update = this.updates.get(since);
            if (update == null) {
                return new MethodResponse[]{new CannotCalculateChangesMethodErrorResponse()};
            } else {
                final Changes changes = update.getChangesFor(Email.class);
                return new MethodResponse[]{
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

    @Override
    protected MethodResponse[] execute(GetEmailMethodCall methodCall, ListMultimap<String, Response.Invocation> previousResponses) {
        final Request.Invocation.ResultReference idsReference = methodCall.getIdsReference();
        final List<String> ids;
        if (idsReference != null) {
            try {
                ids = Arrays.asList(ResultReferenceResolver.resolve(idsReference, previousResponses));
            } catch (final IllegalArgumentException e) {
                return new MethodResponse[]{new InvalidResultReferenceMethodErrorResponse()};
            }
        } else {
            ids = Arrays.asList(methodCall.getIds());
        }
        final String[] properties = methodCall.getProperties();
        Stream<Email> emailStream = ids.stream().map(emails::get);
        if (Arrays.equals(properties, Email.Properties.THREAD_ID)) {
            emailStream = emailStream.map(email -> Email.builder().id(email.getId()).threadId(email.getThreadId()).build());
        } else if (Arrays.equals(properties, Email.Properties.MUTABLE)) {
            emailStream = emailStream.map(email -> Email.builder().id(email.getId()).keywords(email.getKeywords()).mailboxIds(email.getMailboxIds()).build());
        }
        return new MethodResponse[]{
                GetEmailMethodResponse.builder()
                        .list(emailStream.toArray(Email[]::new))
                        .state(getState())
                        .build()
        };
    }

    @Override
    protected MethodResponse[] execute(QueryChangesEmailMethodCall methodCall, ListMultimap<String, Response.Invocation> previousResponses) {
        final String since = methodCall.getSinceQueryState();
        if (since != null && since.equals(getState())) {
            return new MethodResponse[]{
                    QueryChangesEmailMethodResponse.builder()
                            .oldQueryState(getState())
                            .newQueryState(getState())
                            .added(Collections.emptyList())
                            .removed(new String[0])
                            .build()
            };
        } else {
            return new MethodResponse[]{new CannotCalculateChangesMethodErrorResponse()};
        }
    }

    @Override
    protected MethodResponse[] execute(QueryEmailMethodCall methodCall, ListMultimap<String, Response.Invocation> previousResponses) {
        final ArrayList<Email> temporaryList = new ArrayList<>(emails.values());
        temporaryList.sort(Comparator.comparing(Email::getReceivedAt).reversed());
        final HashSet<String> threadIds = new HashSet<>();
        temporaryList.removeIf(email -> !threadIds.add(email.getThreadId()));
        return new MethodResponse[]{
                QueryEmailMethodResponse.builder()
                        .canCalculateChanges(this.reportCanCalculateQueryChanges)
                        .queryState(getState())
                        .ids(Collections2.transform(temporaryList, Email::getId).toArray(new String[0]))
                        .position(0L)
                        .build()
        };
    }

    @Override
    protected MethodResponse[] execute(SetEmailMethodCall methodCall, ListMultimap<String, Response.Invocation> previousResponses) {
        final String ifInState = methodCall.getIfInState();
        final Map<String, Map<String, Object>> update = methodCall.getUpdate();
        final Map<String, Email> create = methodCall.getCreate();
        final String[] destroy = methodCall.getDestroy();
        if ((create != null && create.size() > 0) || (destroy != null && destroy.length > 0)) {
            throw new IllegalStateException("MockMailServer does not know how to create and destroy");
        }
        final SetEmailMethodResponse.SetEmailMethodResponseBuilder responseBuilder = SetEmailMethodResponse.builder();
        final String oldState = getState();
        if (ifInState != null) {
            if (!ifInState.equals(oldState)) {
                return new MethodResponse[]{new StateMismatchMethodErrorResponse()};
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
                    responseBuilder.notUpdated(id, new SetError(SetErrorType.INVALID_PROPERTIES, e.getMessage()));
                }
            }
            for (final Email email : modifiedEmails) {
                emails.put(email.getId(), email);
            }
            incrementState();
            final String newState = getState();
            updates.put(oldState, Update.updated(modifiedEmails, this.mailboxes.keySet(), newState));
        }
        return new MethodResponse[]{
                responseBuilder.build()
        };
    }

    private Email patchEmail(final String id, final Map<String, Object> patches, ListMultimap<String, Response.Invocation> previousResponses) {
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
                    throw new IllegalArgumentException("Keyword modification was not split into two parts");
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
                        final String mailboxId = CreationIdResolver.resolveIfNecessary(mailboxEntry.getKey(), previousResponses);
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

    @Override
    protected MethodResponse[] execute(ChangesMailboxMethodCall methodCall, ListMultimap<String, Response.Invocation> previousResponses) {
        final String since = methodCall.getSinceState();
        if (since != null && since.equals(getState())) {
            return new MethodResponse[]{
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
            final Update update = this.updates.get(since);
            if (update == null) {
                return new MethodResponse[]{new CannotCalculateChangesMethodErrorResponse()};
            } else {
                final Changes changes = update.getChangesFor(Mailbox.class);
                return new MethodResponse[]{
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
    protected MethodResponse[] execute(GetMailboxMethodCall methodCall, ListMultimap<String, Response.Invocation> previousResponses) {
        final Request.Invocation.ResultReference idsReference = methodCall.getIdsReference();
        final List<String> ids;
        if (idsReference != null) {
            try {
                ids = Arrays.asList(ResultReferenceResolver.resolve(idsReference, previousResponses));
            } catch (final IllegalArgumentException e) {
                return new MethodResponse[]{new InvalidResultReferenceMethodErrorResponse()};
            }
        } else {
            final String[] idsParameter = methodCall.getIds();
            ids = idsParameter == null ? null : Arrays.asList(idsParameter);
        }
        Stream<Mailbox> mailboxStream = mailboxes.values().stream().map(this::toMailbox);
        return new MethodResponse[]{
                GetMailboxMethodResponse.builder()
                        .list(mailboxStream.filter(m -> ids == null || ids.contains(m.getId())).toArray(Mailbox[]::new))
                        .state(getState())
                        .build()
        };
    }

    private Mailbox toMailbox(MailboxInfo mailboxInfo) {
        return Mailbox.builder()
                .id(mailboxInfo.getId())
                .name(mailboxInfo.name)
                .role(mailboxInfo.role)
                .totalEmails(emails.values().stream()
                        .filter(e -> e.getMailboxIds().containsKey(mailboxInfo.getId()))
                        .count()
                )
                .unreadEmails(emails.values().stream()
                        .filter(e -> e.getMailboxIds().containsKey(mailboxInfo.getId()))
                        .filter(e -> !e.getKeywords().containsKey(Keyword.SEEN))
                        .count())
                .totalThreads(emails.values().stream()
                        .filter(e -> e.getMailboxIds().containsKey(mailboxInfo.getId()))
                        .map(Email::getThreadId)
                        .distinct().count())
                .unreadThreads(emails.values().stream()
                        .filter(e -> e.getMailboxIds().containsKey(mailboxInfo.getId()))
                        .filter(e -> !e.getKeywords().containsKey(Keyword.SEEN))
                        .map(Email::getThreadId)
                        .distinct().count())
                .build();
    }

    protected MethodResponse[] execute(final SetMailboxMethodCall methodCall, ListMultimap<String, Response.Invocation> previousResponses) {
        final String ifInState = methodCall.getIfInState();
        final SetMailboxMethodResponse.SetMailboxMethodResponseBuilder responseBuilder = SetMailboxMethodResponse.builder();
        final Map<String, Mailbox> create = methodCall.getCreate();
        final Map<String, Map<String, Object>> update = methodCall.getUpdate();
        final String oldState = getState();
        if (ifInState != null) {
            if (!ifInState.equals(oldState)) {
                return new MethodResponse[]{new StateMismatchMethodErrorResponse()};
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
        return new MethodResponse[]{
                setMailboxResponse
        };
    }

    private void processCreateMailbox(final Map<String, Mailbox> create, final SetMailboxMethodResponse.SetMailboxMethodResponseBuilder responseBuilder) {
        for (Map.Entry<String, Mailbox> entry : create.entrySet()) {
            final String createId = entry.getKey();
            final Mailbox mailbox = entry.getValue();
            final String name = mailbox.getName();
            if (mailboxes.values().stream().anyMatch(mailboxInfo -> mailboxInfo.getName().equals(name))) {
                responseBuilder.notCreated(
                        createId,
                        new SetError(SetErrorType.INVALID_PROPERTIES, "A mailbox with the name " + name + " already exists")
                );
                continue;
            }
            final String id = UUID.randomUUID().toString();
            final MailboxInfo mailboxInfo = new MailboxInfo(
                    id,
                    name,
                    mailbox.getRole()
            );
            this.mailboxes.put(id, mailboxInfo);
            responseBuilder.created(createId, toMailbox(mailboxInfo));
        }
    }

    private void processUpdateMailbox(Map<String, Map<String, Object>> update, SetMailboxMethodResponse.SetMailboxMethodResponseBuilder responseBuilder, ListMultimap<String, Response.Invocation> previousResponses) {
        for (final Map.Entry<String, Map<String, Object>> entry : update.entrySet()) {
            final String id = entry.getKey();
            try {
                final MailboxInfo modifiedMailbox = patchMailbox(id, entry.getValue(), previousResponses);
                responseBuilder.updated(id, toMailbox(modifiedMailbox));
                this.mailboxes.put(modifiedMailbox.getId(), modifiedMailbox);
            } catch (final IllegalArgumentException e) {
                responseBuilder.notUpdated(id, new SetError(SetErrorType.INVALID_PROPERTIES, e.getMessage()));
            }
        }
    }

    private MailboxInfo patchMailbox(final String id, final Map<String, Object> patches, ListMultimap<String, Response.Invocation> previousResponses) {
        final MailboxInfo currentMailbox = this.mailboxes.get(id);
        for (final Map.Entry<String, Object> patch : patches.entrySet()) {
            final String fullPath = patch.getKey();
            final Object modification = patch.getValue();
            final List<String> pathParts = Splitter.on('/').splitToList(fullPath);
            final String parameter = pathParts.get(0);
            if ("role".equals(parameter)) {
                final Role role = FuzzyRoleParser.parse((String) modification);
                return new MailboxInfo(
                        currentMailbox.getId(),
                        currentMailbox.getName(),
                        role
                );
            } else {
                throw new IllegalArgumentException("Unable to patch " + fullPath);
            }
        }
        return currentMailbox;
    }

    @Override
    protected MethodResponse[] execute(ChangesThreadMethodCall methodCall, ListMultimap<String, Response.Invocation> previousResponses) {
        final String since = methodCall.getSinceState();
        if (since != null && since.equals(getState())) {
            return new MethodResponse[]{
                    ChangesThreadMethodResponse.builder()
                            .oldState(getState())
                            .newState(getState())
                            .updated(new String[0])
                            .created(new String[0])
                            .destroyed(new String[0])
                            .build()
            };
        } else {
            final Update update = this.updates.get(since);
            if (update == null) {
                return new MethodResponse[]{new CannotCalculateChangesMethodErrorResponse()};
            } else {
                final Changes changes = update.getChangesFor(Thread.class);
                return new MethodResponse[]{
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
    protected MethodResponse[] execute(GetThreadMethodCall methodCall, ListMultimap<String, Response.Invocation> previousResponses) {
        final Request.Invocation.ResultReference idsReference = methodCall.getIdsReference();
        final List<String> ids;
        if (idsReference != null) {
            try {
                ids = Arrays.asList(ResultReferenceResolver.resolve(idsReference, previousResponses));
            } catch (final IllegalArgumentException e) {
                return new MethodResponse[]{new InvalidResultReferenceMethodErrorResponse()};
            }
        } else {
            ids = Arrays.asList(methodCall.getIds());
        }
        final Thread[] threads = ids.stream()
                .map(threadId -> Thread.builder()
                        .id(threadId)
                        .emailIds(emails.values().stream()
                                .filter(email -> email.getThreadId().equals(threadId))
                                .sorted(Comparator.comparing(Email::getReceivedAt))
                                .map(Email::getId).collect(Collectors.toList())).build())
                .toArray(Thread[]::new);
        return new MethodResponse[]{
                GetThreadMethodResponse.builder()
                        .list(threads)
                        .state(getState())
                        .build()
        };
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
