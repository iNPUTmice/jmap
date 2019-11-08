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

package rs.ltt.jmap.mua;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.*;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rs.ltt.jmap.client.JmapClient;
import rs.ltt.jmap.client.JmapRequest.Call;
import rs.ltt.jmap.client.MethodResponses;
import rs.ltt.jmap.client.api.MethodErrorResponseException;
import rs.ltt.jmap.client.session.SessionCache;
import rs.ltt.jmap.client.session.SessionFileCache;
import rs.ltt.jmap.common.Request;
import rs.ltt.jmap.common.entity.Thread;
import rs.ltt.jmap.common.entity.*;
import rs.ltt.jmap.common.entity.filter.EmailFilterCondition;
import rs.ltt.jmap.common.entity.filter.Filter;
import rs.ltt.jmap.common.entity.query.EmailQuery;
import rs.ltt.jmap.common.method.MethodErrorResponse;
import rs.ltt.jmap.common.method.call.email.GetEmailMethodCall;
import rs.ltt.jmap.common.method.call.email.QueryChangesEmailMethodCall;
import rs.ltt.jmap.common.method.call.email.QueryEmailMethodCall;
import rs.ltt.jmap.common.method.call.email.SetEmailMethodCall;
import rs.ltt.jmap.common.method.call.identity.GetIdentityMethodCall;
import rs.ltt.jmap.common.method.call.mailbox.GetMailboxMethodCall;
import rs.ltt.jmap.common.method.call.mailbox.SetMailboxMethodCall;
import rs.ltt.jmap.common.method.call.submission.SetEmailSubmissionMethodCall;
import rs.ltt.jmap.common.method.call.thread.GetThreadMethodCall;
import rs.ltt.jmap.common.method.error.AnchorNotFoundMethodErrorResponse;
import rs.ltt.jmap.common.method.response.email.*;
import rs.ltt.jmap.common.method.response.identity.ChangesIdentityMethodResponse;
import rs.ltt.jmap.common.method.response.identity.GetIdentityMethodResponse;
import rs.ltt.jmap.common.method.response.mailbox.ChangesMailboxMethodResponse;
import rs.ltt.jmap.common.method.response.mailbox.GetMailboxMethodResponse;
import rs.ltt.jmap.common.method.response.mailbox.SetMailboxMethodResponse;
import rs.ltt.jmap.common.method.response.submission.SetEmailSubmissionMethodResponse;
import rs.ltt.jmap.common.method.response.thread.ChangesThreadMethodResponse;
import rs.ltt.jmap.common.method.response.thread.GetThreadMethodResponse;
import rs.ltt.jmap.common.util.Patches;
import rs.ltt.jmap.mua.cache.*;
import rs.ltt.jmap.mua.util.*;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class Mua {

    private static final Logger LOGGER = LoggerFactory.getLogger(Mua.class);
    private final JmapClient jmapClient;
    private final Cache cache;
    private final String accountId;
    private Long queryPageSize = null;
    private ListeningExecutorService ioExecutorService = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());

    private Mua(JmapClient jmapClient, Cache cache, String accountId) {
        this.jmapClient = jmapClient;
        this.cache = cache;
        this.accountId = accountId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public JmapClient getJmapClient() {
        return jmapClient;
    }

    public void shutdown() {
        ioExecutorService.shutdown();
        jmapClient.close();
    }

    public ListenableFuture<Status> refreshIdentities() {
        final ListenableFuture<String> identityStateFuture = ioExecutorService.submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return cache.getIdentityState();
            }
        });
        return Futures.transformAsync(identityStateFuture, new AsyncFunction<String, Status>() {
            @Override
            public ListenableFuture<Status> apply(@NullableDecl final String state) throws Exception {
                if (state == null) {
                    return loadIdentities();
                } else {
                    return updateIdentities(state);
                }
            }
        }, MoreExecutors.directExecutor());
    }

    private ListenableFuture<Status> loadIdentities() {
        JmapClient.MultiCall multiCall = jmapClient.newMultiCall();
        ListenableFuture<Status> future = loadIdentities(multiCall);
        multiCall.execute();
        return future;
    }

    private ListenableFuture<Status> loadIdentities(JmapClient.MultiCall multiCall) {
        final SettableFuture<Status> settableFuture = SettableFuture.create();
        final ListenableFuture<MethodResponses> responseFuture = multiCall.call(new GetIdentityMethodCall(accountId)).getMethodResponses();
        responseFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    final GetIdentityMethodResponse response = responseFuture.get().getMain(GetIdentityMethodResponse.class);
                    final Identity[] identities = response.getList();
                    cache.setIdentities(response.getTypedState(), identities);
                    settableFuture.set(Status.of(identities.length > 0));
                } catch (Exception e) {
                    settableFuture.setException(extractException(e));
                }
            }
        }, ioExecutorService);
        return settableFuture;
    }

    private static Throwable extractException(final Exception exception) {
        if (exception instanceof ExecutionException) {
            final Throwable cause = exception.getCause();
            if (cause != null) {
                return cause;
            }
        }
        return exception;
    }

    private ListenableFuture<Status> updateIdentities(final String state) {
        JmapClient.MultiCall multiCall = jmapClient.newMultiCall();
        ListenableFuture<Status> future = updateIdentities(state, multiCall);
        multiCall.execute();
        return future;
    }

    private ListenableFuture<Status> updateIdentities(final String state, JmapClient.MultiCall multiCall) {
        Preconditions.checkNotNull(state, "State can not be null when updating identities");
        final SettableFuture<Status> settableFuture = SettableFuture.create();
        final UpdateUtil.MethodResponsesFuture methodResponsesFuture = UpdateUtil.identities(multiCall, accountId, state);
        methodResponsesFuture.changes.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ChangesIdentityMethodResponse changesResponse = methodResponsesFuture.changes.get().getMain(ChangesIdentityMethodResponse.class);
                    GetIdentityMethodResponse createdResponse = methodResponsesFuture.created.get().getMain(GetIdentityMethodResponse.class);
                    GetIdentityMethodResponse updatedResponse = methodResponsesFuture.updated.get().getMain(GetIdentityMethodResponse.class);
                    final Update<Identity> update = Update.of(changesResponse, createdResponse, updatedResponse);
                    if (update.hasChanges()) {
                        cache.updateIdentities(update);
                    }
                    settableFuture.set(Status.of(update));
                } catch (Exception e) {
                    settableFuture.setException(extractException(e));
                }
            }
        }, ioExecutorService);
        return settableFuture;
    }

    public ListenableFuture<Status> refreshMailboxes() {
        final ListenableFuture<String> mailboxStateFuture = ioExecutorService.submit(new Callable<String>() {
            @Override
            public String call() {
                return cache.getMailboxState();
            }
        });

        return Futures.transformAsync(mailboxStateFuture, new AsyncFunction<String, Status>() {
            @Override
            public ListenableFuture<Status> apply(@NullableDecl final String state) throws Exception {
                if (state == null) {
                    return loadMailboxes();
                } else {
                    return updateMailboxes(state);
                }
            }
        }, MoreExecutors.directExecutor());
    }

    private ListenableFuture<Status> loadMailboxes() {
        final JmapClient.MultiCall multiCall = jmapClient.newMultiCall();
        final ListenableFuture<Status> future = loadMailboxes(multiCall);
        multiCall.execute();
        return future;
    }

    private ListenableFuture<Status> loadMailboxes(JmapClient.MultiCall multiCall) {
        final SettableFuture<Status> settableFuture = SettableFuture.create();
        final ListenableFuture<MethodResponses> getMailboxMethodResponsesFuture = multiCall.call(new GetMailboxMethodCall(accountId)).getMethodResponses();
        getMailboxMethodResponsesFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    GetMailboxMethodResponse response = getMailboxMethodResponsesFuture.get().getMain(GetMailboxMethodResponse.class);
                    Mailbox[] mailboxes = response.getList();
                    cache.setMailboxes(response.getTypedState(), mailboxes);
                    settableFuture.set(Status.of(mailboxes.length > 0));
                } catch (InterruptedException | ExecutionException | CacheWriteException e) {
                    settableFuture.setException(extractException(e));
                }

            }
        }, ioExecutorService);
        return settableFuture;
    }

    private ListenableFuture<Status> updateMailboxes(final String state) {
        final JmapClient.MultiCall multiCall = jmapClient.newMultiCall();
        final ListenableFuture<Status> future = updateMailboxes(state, multiCall);
        multiCall.execute();
        return future;
    }

    private ListenableFuture<Status> updateMailboxes(final String state, final JmapClient.MultiCall multiCall) {
        Preconditions.checkNotNull(state, "State can not be null when updating mailboxes");
        final SettableFuture<Status> settableFuture = SettableFuture.create();
        final UpdateUtil.MethodResponsesFuture methodResponsesFuture = UpdateUtil.mailboxes(multiCall, accountId, state);
        methodResponsesFuture.changes.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    final ChangesMailboxMethodResponse changesResponse = methodResponsesFuture.changes.get().getMain(ChangesMailboxMethodResponse.class);
                    final GetMailboxMethodResponse createdResponse = methodResponsesFuture.created.get().getMain(GetMailboxMethodResponse.class);
                    final GetMailboxMethodResponse updatedResponse = methodResponsesFuture.updated.get().getMain(GetMailboxMethodResponse.class);
                    final Update<Mailbox> update = Update.of(changesResponse, createdResponse, updatedResponse);
                    if (update.hasChanges()) {
                        cache.updateMailboxes(update, changesResponse.getUpdatedProperties());
                    }
                    settableFuture.set(Status.of(update));
                } catch (InterruptedException | ExecutionException | CacheWriteException | CacheConflictException e) {
                    settableFuture.setException(extractException(e));
                }
            }
        }, ioExecutorService);
        return settableFuture;
    }

    /**
     * Stores an email as a draft. This method will take care of adding the draft and seen keyword and moving the email
     * to the draft mailbox.
     *
     * @param email The email that should be saved as a draft
     * @return
     */
    public ListenableFuture<Boolean> draft(final Email email) {
        return Futures.transformAsync(getMailboxes(), new AsyncFunction<Collection<? extends IdentifiableMailboxWithRole>, Boolean>() {
            @Override
            public ListenableFuture<Boolean> apply(@NullableDecl Collection<? extends IdentifiableMailboxWithRole> mailboxes) {
                return draft(email, MailboxUtil.find(mailboxes, Role.DRAFTS));
            }
        }, MoreExecutors.directExecutor());
    }

    private ListenableFuture<Collection<? extends IdentifiableMailboxWithRole>> getMailboxes() {
        return ioExecutorService.submit(new Callable<Collection<? extends IdentifiableMailboxWithRole>>() {
            @Override
            public Collection<? extends IdentifiableMailboxWithRole> call() throws Exception {
                return cache.getSpecialMailboxes();
            }
        });
    }

    /**
     * Stores an email as a draft. This method will take care of adding the draft and seen keyword and moving the email
     * to the draft mailbox.
     *
     * @param email  The email that should be saved as a draft
     * @param drafts A reference to the Drafts mailbox. Can be null and a new Draft mailbox will automatically be created.
     *               Do not pass null if a Drafts mailbox exists on the server as this call will attempt to create one
     *               and fail.
     * @return
     */
    public ListenableFuture<Boolean> draft(final Email email, final IdentifiableMailboxWithRole drafts) {
        final JmapClient.MultiCall multiCall = jmapClient.newMultiCall();
        final ListenableFuture<Boolean> future = draft(email, drafts, multiCall);
        multiCall.execute();
        return future;
    }

    private ListenableFuture<Boolean> draft(final Email email, final IdentifiableMailboxWithRole drafts, final JmapClient.MultiCall multiCall) {
        Preconditions.checkNotNull(email, "Email can not be null when attempting to create a draft");
        Preconditions.checkState(email.getId() == null, "id is a server-set property");
        Preconditions.checkState(email.getBlobId() == null, "blobId is a server-set property");
        Preconditions.checkState(email.getThreadId() == null, "threadId is a server-set property");
        final Email.EmailBuilder emailBuilder = email.toBuilder();
        final ListenableFuture<MethodResponses> mailboxCreateFuture;
        if (drafts == null) {
            mailboxCreateFuture = createMailbox(Role.DRAFTS, null, multiCall);
        } else {
            mailboxCreateFuture = null;
        }
        if (drafts == null) {
            emailBuilder.mailboxId(CreateUtil.createIdReference(Role.DRAFTS), true);
        } else if (!email.getMailboxIds().containsKey(drafts.getId())) {
            emailBuilder.mailboxId(drafts.getId(), true);
        }
        if (!email.getKeywords().containsKey(Keyword.DRAFT)) {
            emailBuilder.keyword(Keyword.DRAFT, true);
        }
        if (!email.getKeywords().containsKey(Keyword.SEEN)) {
            emailBuilder.keyword(Keyword.SEEN, true);
        }
        final ListenableFuture<MethodResponses> future = multiCall.call(new SetEmailMethodCall(accountId, ImmutableMap.of("e0", emailBuilder.build()))).getMethodResponses();
        return Futures.transformAsync(future, new AsyncFunction<MethodResponses, Boolean>() {
            @Override
            public ListenableFuture<Boolean> apply(@NullableDecl MethodResponses methodResponses) throws Exception {
                if (mailboxCreateFuture != null) {
                    SetMailboxMethodResponse setMailboxResponse = mailboxCreateFuture.get().getMain(SetMailboxMethodResponse.class);
                    SetMailboxException.throwIfFailed(setMailboxResponse);
                }
                SetEmailMethodResponse setEmailMethodResponse = methodResponses.getMain(SetEmailMethodResponse.class);
                SetEmailException.throwIfFailed(setEmailMethodResponse);
                return Futures.immediateFuture(setEmailMethodResponse.getUpdatedCreatedCount() > 0);
            }
        }, MoreExecutors.directExecutor());

    }

    /**
     * Utility method to create a mailbox of a certain role. This method is usually used in conjunction with a
     * modification of emails in a thread. For example a generalized 'move to archive' method can call this method
     * ahead of time to create the archive mailbox.
     * <p>
     * When called with an ObjectsState the SetMailboxMethodCall will be guarded with an ifInState parameter. In that case
     * an automatic call to updateMailboxes is made as well - The idea behind that approach is that if the ifInState fails
     * we will at least have an up to date state on the next attempt.
     *
     * @param role         The role of the mailbox we want to create.
     * @param objectsState If an ObjectsState is giving the create call will be guarded with a ifInState
     * @param multiCall    The MultiCall that will later have the SetEmailMethodCall added to it.
     * @return
     */
    private ListenableFuture<MethodResponses> createMailbox(@NonNullDecl final Role role, @NullableDecl ObjectsState objectsState, final JmapClient.MultiCall multiCall) {
        final SetMailboxMethodCall setMailboxMethodCall = new SetMailboxMethodCall(
                null, //TODO this is the account; we need to do something useful with that
                objectsState == null ? null : objectsState.mailboxState,
                ImmutableMap.of(CreateUtil.createId(role), MailboxUtil.create(role)),
                null,
                null
        );
        final ListenableFuture<MethodResponses> future = multiCall.call(setMailboxMethodCall).getMethodResponses();
        if (objectsState != null && objectsState.mailboxState != null) {
            updateMailboxes(objectsState.mailboxState, multiCall);
        }
        return future;
    }

    public ListenableFuture<Boolean> submit(final Email email, final Identity identity) {
        return Futures.transformAsync(getMailboxes(), new AsyncFunction<Collection<? extends IdentifiableMailboxWithRole>, Boolean>() {
            @Override
            public ListenableFuture<Boolean> apply(@NullableDecl Collection<? extends IdentifiableMailboxWithRole> mailboxes) {
                Preconditions.checkNotNull(mailboxes, "SpecialMailboxes collection must not be null but can be empty");
                final IdentifiableMailboxWithRole drafts = MailboxUtil.find(mailboxes, Role.DRAFTS);
                final String draftMailboxId;
                if (drafts == null || !email.getMailboxIds().containsKey(drafts.getId())) {
                    draftMailboxId = null;
                } else {
                    draftMailboxId = drafts.getId();
                }
                final IdentifiableMailboxWithRole sent = MailboxUtil.find(mailboxes, Role.SENT);
                return submit(email.getId(), identity, draftMailboxId, sent);
            }
        }, MoreExecutors.directExecutor());
    }

    /**
     * Submits (sends / EmailSubmission) a previously drafted email. The email will be removed from the Drafts mailbox
     * and put into the Sent mailbox after successful submission. Additionally the draft keyword will be removed.
     *
     * @param emailId        The id of the email that should be submitted
     * @param identity       The identity used to submit that email
     * @param draftMailboxId The id of the draft mailbox. After successful submission the email will be removed from
     *                       this mailbox. Can be null to skip this operation and not remove the email from that mailbox.
     *                       If not null the caller should ensure that the id belongs to the draft mailbox and the email
     *                       is in that mailbox.
     * @param sent           A reference to the Sent mailbox. Can be null and a new sent mailbox will automatically be created.
     *                       Do not pass null if a Sent mailbox exists on the server as this call will attempt to create one and
     *                       fail.
     * @return
     */
    public ListenableFuture<Boolean> submit(final String emailId, final Identity identity, @NullableDecl String draftMailboxId, final IdentifiableMailboxWithRole sent) {
        final JmapClient.MultiCall multiCall = jmapClient.newMultiCall();
        final ListenableFuture<Boolean> future = submit(emailId, identity, draftMailboxId, sent, multiCall);
        multiCall.execute();
        return future;
    }

    //TODO this need IdentifiableEmailWithMailboxes
    private ListenableFuture<Boolean> submit(@NonNullDecl final String emailId, @NonNullDecl final Identity identity, @NullableDecl String draftMailboxId, @NullableDecl final IdentifiableMailboxWithRole sent, final JmapClient.MultiCall multiCall) {
        Preconditions.checkNotNull(emailId, "emailId can not be null when attempting to submit");
        Preconditions.checkNotNull(identity, "identity can not be null when attempting to submit an email");
        final ListenableFuture<MethodResponses> mailboxCreateFuture;
        if (sent == null) {
            mailboxCreateFuture = createMailbox(Role.SENT, null, multiCall);
        } else {
            mailboxCreateFuture = null;
        }
        final Patches.Builder patchesBuilder = Patches.builder();
        patchesBuilder.remove("keywords/" + Keyword.DRAFT);
        patchesBuilder.set("mailboxIds/" + (sent == null ? CreateUtil.createIdReference(Role.SENT) : sent.getId()), true);
        if (draftMailboxId != null) {
            patchesBuilder.remove("mailboxIds/" + draftMailboxId);
        }
        final ListenableFuture<MethodResponses> setEmailSubmissionFuture = multiCall.call(new SetEmailSubmissionMethodCall(
                accountId,
                ImmutableMap.of(
                        "es0",
                        EmailSubmission.builder()
                                .emailId(emailId)
                                .identityId(identity.getId())
                                .build()
                ),
                ImmutableMap.of(
                        "#es0",
                        patchesBuilder.build()
                )
        )).getMethodResponses();
        return Futures.transformAsync(setEmailSubmissionFuture, new AsyncFunction<MethodResponses, Boolean>() {
            @Override
            public ListenableFuture<Boolean> apply(@NullableDecl MethodResponses methodResponses) throws Exception {
                if (mailboxCreateFuture != null) {
                    SetMailboxMethodResponse setMailboxResponse = mailboxCreateFuture.get().getMain(SetMailboxMethodResponse.class);
                    SetMailboxException.throwIfFailed(setMailboxResponse);
                }
                SetEmailSubmissionMethodResponse setEmailSubmissionMethodResponse = methodResponses.getMain(SetEmailSubmissionMethodResponse.class);
                SetEmailSubmissionException.throwIfFailed(setEmailSubmissionMethodResponse);
                return Futures.immediateFuture(setEmailSubmissionMethodResponse.getUpdatedCreatedCount() > 0);
            }
        }, MoreExecutors.directExecutor());

    }

    /**
     * Submits (sends / EmailSubmission) a previously drafted email. The email will be removed from the Drafts mailbox
     * and put into the Sent mailbox after successful submission. Additionally the draft keyword will be removed.
     *
     * @param emailId  The id of the email that should be submitted
     * @param identity The identity used to submit that email
     * @return
     */
    public ListenableFuture<Boolean> submit(final String emailId, final Identity identity) {
        return Futures.transformAsync(getMailboxes(), new AsyncFunction<Collection<? extends IdentifiableMailboxWithRole>, Boolean>() {
            @Override
            public ListenableFuture<Boolean> apply(@NullableDecl Collection<? extends IdentifiableMailboxWithRole> mailboxes) {
                Preconditions.checkNotNull(mailboxes, "SpecialMailboxes collection must not be null but can be empty");
                final IdentifiableMailboxWithRole drafts = MailboxUtil.find(mailboxes, Role.DRAFTS);
                final IdentifiableMailboxWithRole sent = MailboxUtil.find(mailboxes, Role.SENT);
                return submit(emailId, identity, drafts == null ? null : drafts.getId(), sent);
            }
        }, MoreExecutors.directExecutor());
    }

    public ListenableFuture<Boolean> send(final Email email, final Identity identity) {
        return Futures.transformAsync(getMailboxes(), new AsyncFunction<Collection<? extends IdentifiableMailboxWithRole>, Boolean>() {
            @Override
            public ListenableFuture<Boolean> apply(@NullableDecl Collection<? extends IdentifiableMailboxWithRole> mailboxes) {
                Preconditions.checkNotNull(mailboxes, "SpecialMailboxes collection must not be null but can be empty");
                final IdentifiableMailboxWithRole draft = MailboxUtil.find(mailboxes, Role.DRAFTS);
                final IdentifiableMailboxWithRole sent = MailboxUtil.find(mailboxes, Role.SENT);
                return send(email, identity, draft, sent);
            }
        }, MoreExecutors.directExecutor());
    }

    private ListenableFuture<Boolean> send(final Email email, final Identity identity, final IdentifiableMailboxWithRole drafts, final IdentifiableMailboxWithRole sent) {
        final JmapClient.MultiCall multiCall = jmapClient.newMultiCall();
        ListenableFuture<List<Boolean>> future = Futures.allAsList(
                draft(email, drafts, multiCall),
                submit("#e0", identity, drafts == null ? CreateUtil.createIdReference(Role.DRAFTS) : drafts.getId(), sent, multiCall)
        );
        multiCall.execute();
        return Futures.transform(future, new Function<List<Boolean>, Boolean>() {
            @NullableDecl
            @Override
            public Boolean apply(@NullableDecl List<Boolean> booleans) {
                return booleans != null && !booleans.contains(false);
            }
        }, MoreExecutors.directExecutor());
    }

    public ListenableFuture<Boolean> setKeyword(final Collection<? extends IdentifiableEmailWithKeywords> emails, final String keyword) {
        return Futures.transformAsync(getObjectsState(), new AsyncFunction<ObjectsState, Boolean>() {
            @Override
            public ListenableFuture<Boolean> apply(@NullableDecl ObjectsState objectsState) throws Exception {
                return setKeyword(emails, keyword, objectsState);
            }
        }, MoreExecutors.directExecutor());
    }

    private ListenableFuture<ObjectsState> getObjectsState() {
        return ioExecutorService.submit(new Callable<ObjectsState>() {
            @Override
            public ObjectsState call() {
                return cache.getObjectsState();
            }
        });
    }

    private ListenableFuture<Boolean> setKeyword(Collection<? extends IdentifiableEmailWithKeywords> emails, String keyword, ObjectsState objectsState) {
        final ImmutableMap.Builder<String, Map<String, Object>> emailPatchObjectMapBuilder = ImmutableMap.builder();
        for (IdentifiableEmailWithKeywords email : emails) {
            if (!email.getKeywords().containsKey(keyword)) {
                emailPatchObjectMapBuilder.put(email.getId(), Patches.set("keywords/" + keyword, true));
            }
        }
        final ImmutableMap<String, Map<String, Object>> patches = emailPatchObjectMapBuilder.build();

        return applyEmailPatches(patches, objectsState);
    }

    private ListenableFuture<Boolean> applyEmailPatches(final Map<String, Map<String, Object>> patches, final ObjectsState objectsState) {
        if (patches.size() == 0) {
            return Futures.immediateFuture(false);
        }
        JmapClient.MultiCall multiCall = jmapClient.newMultiCall();
        ListenableFuture<Boolean> future = applyEmailPatches(patches, objectsState, true, multiCall);
        multiCall.execute();
        return future;
    }

    /**
     * Utility method to update emails with a given patch set. Optionally this call will be guarded with an ifInState
     * parameter. If it is this method will also chain an updateEmailCall right after it. The rational for that is that
     * when the first call fails with a miss matched state a second attempt (not triggered automatically) will most likely
     * succeed.
     * <p>
     * When calling this method after creating a mailbox ifInState should be set to false; Otherwise the createMailbox
     * call will increase the state and the subsequent setEmail call would fail.
     *
     * @param patches      The map of patches
     * @param objectsState An ObjectsState that will be used to guard the set call. Only used when ifInState is true
     * @param ifInState    Whether or not to guard the call
     * @param multiCall
     * @return
     */
    private ListenableFuture<Boolean> applyEmailPatches(final Map<String, Map<String, Object>> patches,
                                                        final ObjectsState objectsState,
                                                        final boolean ifInState,
                                                        final JmapClient.MultiCall multiCall) {
        if (ifInState) {
            Preconditions.checkNotNull(objectsState);
        }
        final ListenableFuture<MethodResponses> future = multiCall.call(new SetEmailMethodCall(accountId, ifInState ? objectsState.emailState : null, patches)).getMethodResponses();
        if (ifInState && objectsState.emailState != null) {
            updateEmails(objectsState.emailState, multiCall);
        }
        return Futures.transformAsync(future, new AsyncFunction<MethodResponses, Boolean>() {
            @Override
            public ListenableFuture<Boolean> apply(@NullableDecl MethodResponses methodResponses) throws Exception {
                SetEmailMethodResponse setEmailMethodResponse = methodResponses.getMain(SetEmailMethodResponse.class);
                SetEmailException.throwIfFailed(setEmailMethodResponse);
                return Futures.immediateFuture(setEmailMethodResponse.getUpdatedCreatedCount() > 0);
            }
        }, ioExecutorService);
    }

    private ListenableFuture<Status> updateEmails(final String state, final JmapClient.MultiCall multiCall) {
        Preconditions.checkNotNull(state, "state can not be null when updating emails");
        final SettableFuture<Status> settableFuture = SettableFuture.create();
        final UpdateUtil.MethodResponsesFuture methodResponsesFuture = UpdateUtil.emails(multiCall, accountId, state);
        methodResponsesFuture.changes.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    final ChangesEmailMethodResponse changesResponse = methodResponsesFuture.changes.get().getMain(ChangesEmailMethodResponse.class);
                    final GetEmailMethodResponse createdResponse = methodResponsesFuture.created.get().getMain(GetEmailMethodResponse.class);
                    final GetEmailMethodResponse updatedResponse = methodResponsesFuture.updated.get().getMain(GetEmailMethodResponse.class);
                    final Update<Email> update = Update.of(changesResponse, createdResponse, updatedResponse);
                    if (update.hasChanges()) {
                        cache.updateEmails(update, Email.MUTABLE_PROPERTIES);
                    }
                    settableFuture.set(Status.of(update));
                } catch (InterruptedException | ExecutionException | CacheWriteException | CacheConflictException e) {
                    settableFuture.setException(extractException(e));
                }
            }
        }, ioExecutorService);
        return settableFuture;
    }

    public ListenableFuture<Boolean> removeKeyword(final Collection<? extends IdentifiableEmailWithKeywords> emails, final String keyword) {
        return Futures.transformAsync(getObjectsState(), new AsyncFunction<ObjectsState, Boolean>() {
            @Override
            public ListenableFuture<Boolean> apply(@NullableDecl ObjectsState objectsState) {
                return removeKeyword(emails, keyword, objectsState);
            }
        }, MoreExecutors.directExecutor());
    }

    private ListenableFuture<Boolean> removeKeyword(final Collection<? extends IdentifiableEmailWithKeywords> emails, final String keyword, final ObjectsState objectsState) {
        final ImmutableMap.Builder<String, Map<String, Object>> emailPatchObjectMapBuilder = ImmutableMap.builder();
        for (IdentifiableEmailWithKeywords email : emails) {
            if (email.getKeywords().containsKey(keyword)) {
                emailPatchObjectMapBuilder.put(email.getId(), Patches.remove("keywords/" + keyword));
            }
        }
        final ImmutableMap<String, Map<String, Object>> patches = emailPatchObjectMapBuilder.build();
        return applyEmailPatches(patches, objectsState);
    }

    public ListenableFuture<Boolean> createMailbox(Mailbox mailbox) {
        ListenableFuture<MethodResponses> future = jmapClient.call(new SetMailboxMethodCall(accountId, ImmutableMap.of("new-mailbox-0", mailbox)));
        return Futures.transformAsync(future, new AsyncFunction<MethodResponses, Boolean>() {
            @Override
            public ListenableFuture<Boolean> apply(@NullableDecl MethodResponses methodResponses) throws Exception {
                SetMailboxMethodResponse response = methodResponses.getMain(SetMailboxMethodResponse.class);
                SetMailboxException.throwIfFailed(response);
                return Futures.immediateFuture(response.getUpdatedCreatedCount() > 0);
            }
        }, MoreExecutors.directExecutor());
    }

    /**
     * Copies the individual emails in this collection (usually applied to an entire thread) to the mailbox with the
     * role IMPORTANT. If a mailbox with that role doesn’t exist it will be created.
     *
     * @param emails A collection of emails. Usually all messages in a thread
     * @return
     */
    public ListenableFuture<Boolean> copyToImportant(@NonNullDecl final Collection<? extends IdentifiableEmailWithMailboxIds> emails) {
        return Futures.transformAsync(getMailboxes(), new AsyncFunction<Collection<? extends IdentifiableMailboxWithRole>, Boolean>() {
            @Override
            public ListenableFuture<Boolean> apply(@NullableDecl Collection<? extends IdentifiableMailboxWithRole> mailboxes) throws Exception {
                Preconditions.checkNotNull(mailboxes, "SpecialMailboxes collection must not be null but can be empty");
                final IdentifiableMailboxWithRole important = MailboxUtil.find(mailboxes, Role.IMPORTANT);
                return copyToImportant(emails, important);
            }
        }, MoreExecutors.directExecutor());
    }

    private ListenableFuture<Boolean> copyToImportant(@NonNullDecl final Collection<? extends IdentifiableEmailWithMailboxIds> emails, @NullableDecl final IdentifiableMailboxWithRole important) {
        return Futures.transformAsync(getObjectsState(), new AsyncFunction<ObjectsState, Boolean>() {
            @Override
            public ListenableFuture<Boolean> apply(@NullableDecl ObjectsState objectsState) {
                return copyToImportant(emails, important, objectsState);
            }
        }, MoreExecutors.directExecutor());
    }

    private ListenableFuture<Boolean> copyToImportant(@NonNullDecl final Collection<? extends IdentifiableEmailWithMailboxIds> emails, @NullableDecl final IdentifiableMailboxWithRole important, final ObjectsState objectsState) {
        Preconditions.checkNotNull(emails, "emails can not be null when attempting to copy them to important");
        if (important != null) {
            Preconditions.checkArgument(important.getRole() == Role.IMPORTANT, "Supplied important mailbox must have the role IMPORTANT");
        }
        final JmapClient.MultiCall multiCall = jmapClient.newMultiCall();

        final ListenableFuture<MethodResponses> mailboxCreateFuture;
        if (important == null) {
            mailboxCreateFuture = createMailbox(Role.IMPORTANT, objectsState, multiCall);
        } else {
            mailboxCreateFuture = null;
        }

        ImmutableMap.Builder<String, Map<String, Object>> emailPatchObjectMapBuilder = ImmutableMap.builder();
        for (IdentifiableEmailWithMailboxIds email : emails) {
            final Map<String, Boolean> mailboxIds = new HashMap<>(email.getMailboxIds());
            if (important == null) {
                mailboxIds.put(CreateUtil.createIdReference(Role.IMPORTANT), true);
            } else {
                mailboxIds.put(important.getId(), true);
            }
            emailPatchObjectMapBuilder.put(email.getId(), Patches.set("mailboxIds", mailboxIds));
        }
        final ImmutableMap<String, Map<String, Object>> patches = emailPatchObjectMapBuilder.build();
        if (patches.size() == 0) {
            return Futures.immediateFuture(false);
        }

        final ListenableFuture<Boolean> patchesFuture = applyEmailPatches(patches,
                objectsState,
                important != null, //if do not have to create a mailbox in the same call, calling ifInState on the emails is fine
                multiCall
        );

        multiCall.execute();

        return Futures.transformAsync(patchesFuture, new AsyncFunction<Boolean, Boolean>() {
            @Override
            public ListenableFuture<Boolean> apply(@NullableDecl Boolean patchesResults) throws Exception {
                if (mailboxCreateFuture != null) {
                    SetMailboxMethodResponse setMailboxResponse = mailboxCreateFuture.get().getMain(SetMailboxMethodResponse.class);
                    SetMailboxException.throwIfFailed(setMailboxResponse);
                }
                return Futures.immediateFuture(patchesResults);
            }
        }, MoreExecutors.directExecutor());
    }

    /**
     * Copies the individual emails in this collection (usually applied to an entire thread) to a given mailbox.
     * If a certain email of this collection is already in that mailbox it will be skipped.
     * <p>
     * This method is usually run as a 'add label' action.
     *
     * @param emails  A collection of emails. Usually all messages in a thread
     * @param mailbox The mailbox those emails should be copied to.
     * @return
     */
    public ListenableFuture<Boolean> copyToMailbox(final Collection<? extends IdentifiableEmailWithMailboxIds> emails, final IdentifiableMailboxWithRole mailbox) {
        return Futures.transformAsync(getObjectsState(), new AsyncFunction<ObjectsState, Boolean>() {
            @Override
            public ListenableFuture<Boolean> apply(@NullableDecl ObjectsState objectsState) throws Exception {
                return copyToMailbox(emails, mailbox, objectsState);
            }
        }, MoreExecutors.directExecutor());
    }

    private ListenableFuture<Boolean> copyToMailbox(final Collection<? extends IdentifiableEmailWithMailboxIds> emails, final IdentifiableMailboxWithRole mailbox, final ObjectsState objectsState) {
        ImmutableMap.Builder<String, Map<String, Object>> emailPatchObjectMapBuilder = ImmutableMap.builder();
        for (IdentifiableEmailWithMailboxIds email : emails) {
            if (email.getMailboxIds().containsKey(mailbox.getId())) {
                continue;
            }
            Patches.Builder patchesBuilder = Patches.builder();
            patchesBuilder.set("mailboxIds/" + mailbox.getId(), true);
            emailPatchObjectMapBuilder.put(email.getId(), patchesBuilder.build());
        }
        final ImmutableMap<String, Map<String, Object>> patches = emailPatchObjectMapBuilder.build();

        return applyEmailPatches(patches, objectsState);
    }

    /**
     * Removes the emails in this collection from both the Trash and Archive mailbox (if they are in either of those)
     * and puts all emails into the Inbox instead.
     *
     * @param emails A collection of emails; usually all emails in a thread
     * @return
     */
    public ListenableFuture<Boolean> moveToInbox(final Collection<? extends IdentifiableEmailWithMailboxIds> emails) {
        return Futures.transformAsync(getMailboxes(), new AsyncFunction<Collection<? extends IdentifiableMailboxWithRole>, Boolean>() {
            @Override
            public ListenableFuture<Boolean> apply(@NullableDecl Collection<? extends IdentifiableMailboxWithRole> mailboxes) throws Exception {
                Preconditions.checkNotNull(mailboxes, "SpecialMailboxes collection must not be null but can be empty");
                final IdentifiableMailboxWithRole archive = MailboxUtil.find(mailboxes, Role.ARCHIVE);
                final IdentifiableMailboxWithRole trash = MailboxUtil.find(mailboxes, Role.TRASH);
                final IdentifiableMailboxWithRole inbox = MailboxUtil.find(mailboxes, Role.INBOX);
                return moveToInbox(emails, archive, trash, inbox);
            }
        }, MoreExecutors.directExecutor());
    }

    private ListenableFuture<Boolean> moveToInbox(final Collection<? extends IdentifiableEmailWithMailboxIds> emails, final IdentifiableMailboxWithRole archive, final IdentifiableMailboxWithRole trash, final IdentifiableMailboxWithRole inbox) {
        return Futures.transformAsync(getObjectsState(), new AsyncFunction<ObjectsState, Boolean>() {
            @Override
            public ListenableFuture<Boolean> apply(@NullableDecl ObjectsState objectsState) throws Exception {
                return moveToInbox(emails, archive, trash, inbox, objectsState);
            }
        }, MoreExecutors.directExecutor());
    }

    private ListenableFuture<Boolean> moveToInbox(final Collection<? extends IdentifiableEmailWithMailboxIds> emails, IdentifiableMailboxWithRole archive, IdentifiableMailboxWithRole trash, IdentifiableMailboxWithRole inbox, final ObjectsState objectsState) {
        Preconditions.checkNotNull(emails, "emails can not be null when attempting to move them to inbox");

        final JmapClient.MultiCall multiCall = jmapClient.newMultiCall();

        final ListenableFuture<MethodResponses> mailboxCreateFuture;
        if (inbox == null) {
            mailboxCreateFuture = createMailbox(Role.INBOX, objectsState, multiCall);
        } else {
            mailboxCreateFuture = null;
        }

        ImmutableMap.Builder<String, Map<String, Object>> emailPatchObjectMapBuilder = ImmutableMap.builder();
        for (IdentifiableEmailWithMailboxIds email : emails) {
            Map<String, Boolean> mailboxIds = new HashMap<>(email.getMailboxIds());

            if (archive != null) {
                mailboxIds.remove(archive.getId());
            }
            if (trash != null) {
                mailboxIds.remove(trash.getId());
            }
            if (inbox == null) {
                mailboxIds.put(CreateUtil.createIdReference(Role.INBOX), true);
            } else {
                mailboxIds.put("mailboxIds/" + inbox.getId(), true);
            }
            emailPatchObjectMapBuilder.put(email.getId(), Patches.set("mailboxIds", mailboxIds));
        }
        final ImmutableMap<String, Map<String, Object>> patches = emailPatchObjectMapBuilder.build();
        if (patches.size() == 0) {
            return Futures.immediateFuture(false);
        }

        final ListenableFuture<Boolean> patchesFuture = applyEmailPatches(
                patches,
                objectsState,
                inbox != null,
                multiCall
        );

        multiCall.execute();

        return Futures.transformAsync(patchesFuture, new AsyncFunction<Boolean, Boolean>() {
            @Override
            public ListenableFuture<Boolean> apply(@NullableDecl Boolean patchesResults) throws Exception {
                if (mailboxCreateFuture != null) {
                    SetMailboxMethodResponse setMailboxResponse = mailboxCreateFuture.get().getMain(SetMailboxMethodResponse.class);
                    SetMailboxException.throwIfFailed(setMailboxResponse);
                }
                return Futures.immediateFuture(patchesResults);
            }
        }, MoreExecutors.directExecutor());
    }

    /**
     * Moves the individual emails in this collection (usually applied to an entire thread) from the inbox to the archive.
     * Any email that is not in the inbox will be skipped.
     *
     * @param emails A collection of emails. Usually all messages in a thread
     * @return
     */
    public ListenableFuture<Boolean> archive(final Collection<? extends IdentifiableEmailWithMailboxIds> emails) {
        return Futures.transformAsync(getMailboxes(), new AsyncFunction<Collection<? extends IdentifiableMailboxWithRole>, Boolean>() {
            @Override
            public ListenableFuture<Boolean> apply(@NullableDecl Collection<? extends IdentifiableMailboxWithRole> mailboxes) throws Exception {
                Preconditions.checkNotNull(mailboxes, "SpecialMailboxes collection must not be null but can be empty");
                final IdentifiableMailboxWithRole inbox = MailboxUtil.find(mailboxes, Role.INBOX);
                Preconditions.checkState(inbox != null, "Inbox mailbox not found. Calling archive (remove from inbox) on a collection of emails even though there is no inbox does not make sense");
                final IdentifiableMailboxWithRole archive = MailboxUtil.find(mailboxes, Role.ARCHIVE);
                return archive(emails, inbox, archive);
            }
        }, MoreExecutors.directExecutor());
    }

    private ListenableFuture<Boolean> archive(final Collection<? extends IdentifiableEmailWithMailboxIds> emails, @NonNullDecl final IdentifiableMailboxWithRole inbox, @NullableDecl final IdentifiableMailboxWithRole archive) {
        return Futures.transformAsync(getObjectsState(), new AsyncFunction<ObjectsState, Boolean>() {
            @Override
            public ListenableFuture<Boolean> apply(@NullableDecl ObjectsState objectsState) throws Exception {
                return archive(emails, inbox, archive, objectsState);
            }
        }, MoreExecutors.directExecutor());
    }

    private ListenableFuture<Boolean> archive(final Collection<? extends IdentifiableEmailWithMailboxIds> emails, @NonNullDecl final IdentifiableMailboxWithRole inbox, @NullableDecl final IdentifiableMailboxWithRole archive, final ObjectsState objectsState) {
        Preconditions.checkNotNull(emails, "emails can not be null when attempting to archive them");

        final JmapClient.MultiCall multiCall = jmapClient.newMultiCall();

        final ListenableFuture<MethodResponses> mailboxCreateFuture;
        if (archive == null) {
            mailboxCreateFuture = createMailbox(Role.ARCHIVE, objectsState, multiCall);
        } else {
            mailboxCreateFuture = null;
        }

        ImmutableMap.Builder<String, Map<String, Object>> emailPatchObjectMapBuilder = ImmutableMap.builder();
        for (IdentifiableEmailWithMailboxIds email : emails) {
            if (!email.getMailboxIds().containsKey(inbox.getId())) {
                continue;
            }
            Map<String, Boolean> mailboxIds = new HashMap<>(email.getMailboxIds());
            mailboxIds.remove(inbox.getId());
            if (archive == null) {
                mailboxIds.put(CreateUtil.createIdReference(Role.ARCHIVE), true);
            } else {
                mailboxIds.put(archive.getId(), true);
            }
            emailPatchObjectMapBuilder.put(email.getId(), Patches.set("mailboxIds", mailboxIds));
        }
        final ImmutableMap<String, Map<String, Object>> patches = emailPatchObjectMapBuilder.build();
        if (patches.size() == 0) {
            return Futures.immediateFuture(false);
        }

        final ListenableFuture<Boolean> patchesFuture = applyEmailPatches(patches,
                objectsState,
                archive != null,
                multiCall
        );

        multiCall.execute();

        return Futures.transformAsync(patchesFuture, new AsyncFunction<Boolean, Boolean>() {
            @Override
            public ListenableFuture<Boolean> apply(@NullableDecl Boolean patchesResults) throws Exception {
                if (mailboxCreateFuture != null) {
                    SetMailboxMethodResponse setMailboxResponse = mailboxCreateFuture.get().getMain(SetMailboxMethodResponse.class);
                    SetMailboxException.throwIfFailed(setMailboxResponse);
                }
                return Futures.immediateFuture(patchesResults);
            }
        }, MoreExecutors.directExecutor());
    }

    /**
     * Removes the individual emails in this collection (usually applied to an entire thread) from a given mailbox. If a
     * certain email was not in this mailbox it will be skipped. If removing an email from this mailbox would otherwise
     * lead to the email having no mailbox it will be moved to the Archive mailbox.
     * <p>
     * This method is usually run as a 'remove label' action.
     *
     * @param emails  A collection of emails. Usually all messages in a thread
     * @param mailbox The mailbox from which those emails should be removed
     * @param archive A reference to the Archive mailbox. Can be null and a new Archive mailbox will automatically be
     *                created.
     *                Do not pass null if an Archive mailbox exists on the server as this call will attempt to create
     *                one and fail.
     */
    public ListenableFuture<Boolean> removeFromMailbox(Collection<? extends IdentifiableEmailWithMailboxIds> emails, @NonNullDecl Mailbox mailbox, @NullableDecl final IdentifiableMailboxWithRole archive) {
        Preconditions.checkNotNull(mailbox, "Mailbox can not be null when attempting to remove it from a collection of emails");
        if (archive != null) {
            Preconditions.checkArgument(archive.getRole() == Role.ARCHIVE, "Supplied archive mailbox must have the role ARCHIVE");
        }
        return removeFromMailbox(emails, mailbox.getId(), archive);
    }

    private ListenableFuture<Boolean> removeFromMailbox(final Collection<? extends IdentifiableEmailWithMailboxIds> emails, final String mailboxId, @NullableDecl final IdentifiableMailboxWithRole archive) {
        return Futures.transformAsync(getObjectsState(), new AsyncFunction<ObjectsState, Boolean>() {
            @Override
            public ListenableFuture<Boolean> apply(@NullableDecl ObjectsState objectsState) throws Exception {
                return removeFromMailbox(emails, mailboxId, archive, objectsState);
            }
        }, MoreExecutors.directExecutor());
    }

    private ListenableFuture<Boolean> removeFromMailbox(Collection<? extends IdentifiableEmailWithMailboxIds> emails, String mailboxId, @NullableDecl final IdentifiableMailboxWithRole archive, final ObjectsState objectsState) {
        Preconditions.checkNotNull(emails, "emails can not be null when attempting to remove them from a mailbox");
        Preconditions.checkNotNull(mailboxId, "mailboxId can not be null when attempting to remove emails");
        final JmapClient.MultiCall multiCall = jmapClient.newMultiCall();
        final ListenableFuture<MethodResponses> mailboxCreateFuture;
        if (archive == null) {
            mailboxCreateFuture = createMailbox(Role.ARCHIVE, objectsState, multiCall);
        } else {
            mailboxCreateFuture = null;
        }
        ImmutableMap.Builder<String, Map<String, Object>> emailPatchObjectMapBuilder = ImmutableMap.builder();
        for (IdentifiableEmailWithMailboxIds email : emails) {
            if (!email.getMailboxIds().containsKey(mailboxId)) {
                continue;
            }
            Map<String, Boolean> mailboxIds = new HashMap<>(email.getMailboxIds());
            mailboxIds.remove(mailboxId);
            if (mailboxIds.size() == 0) {
                if (archive == null) {
                    mailboxIds.put(CreateUtil.createIdReference(Role.ARCHIVE), true);
                } else {
                    mailboxIds.put(archive.getId(), true);
                }
            }
            emailPatchObjectMapBuilder.put(email.getId(), Patches.set("mailboxIds", mailboxIds));

        }
        final ImmutableMap<String, Map<String, Object>> patches = emailPatchObjectMapBuilder.build();
        if (patches.size() == 0) {
            return Futures.immediateFuture(false);
        }

        final ListenableFuture<Boolean> patchesFuture = applyEmailPatches(patches,
                objectsState,
                archive != null,
                multiCall
        );

        multiCall.execute();

        return Futures.transformAsync(patchesFuture, new AsyncFunction<Boolean, Boolean>() {
            @Override
            public ListenableFuture<Boolean> apply(@NullableDecl Boolean patchesResults) throws Exception {
                if (mailboxCreateFuture != null) {
                    SetMailboxMethodResponse setMailboxResponse = mailboxCreateFuture.get().getMain(SetMailboxMethodResponse.class);
                    SetMailboxException.throwIfFailed(setMailboxResponse);
                }
                return Futures.immediateFuture(patchesResults);
            }
        }, MoreExecutors.directExecutor());
    }

    /**
     * Removes the individual emails in this collection (usually applied to an entire thread) from a given mailbox. If a
     * certain email was not in this mailbox it will be skipped. If removing an email from this mailbox would otherwise
     * lead to the email having no mailbox it will be moved to the Archive mailbox.
     *
     * @param emails    A collection of emails. Usually all messages in a thread
     * @param mailboxId The id of the mailbox from which those emails should be removed
     * @return
     */
    public ListenableFuture<Boolean> removeFromMailbox(final Collection<? extends IdentifiableEmailWithMailboxIds> emails, final String mailboxId) {
        return Futures.transformAsync(getMailboxes(), new AsyncFunction<Collection<? extends IdentifiableMailboxWithRole>, Boolean>() {
            @Override
            public ListenableFuture<Boolean> apply(@NullableDecl Collection<? extends IdentifiableMailboxWithRole> mailboxes) throws Exception {
                Preconditions.checkNotNull(mailboxes, "SpecialMailboxes collection must not be null but can be empty");
                final IdentifiableMailboxWithRole archive = MailboxUtil.find(mailboxes, Role.ARCHIVE);
                return removeFromMailbox(emails, mailboxId, archive);
            }
        }, MoreExecutors.directExecutor());
    }

    /**
     * Moves all emails in this collection (usually applied to an entire thread) to the trash mailbox. The emails will
     * be removed from all other mailboxes. If a certain email in this collection is already only in the trash mailbox
     * this email will not be processed.
     *
     * @param emails A collection of emails. Usually all messages in a thread
     */
    public ListenableFuture<Boolean> moveToTrash(final Collection<? extends IdentifiableEmailWithMailboxIds> emails) {
        return Futures.transformAsync(getMailboxes(), new AsyncFunction<Collection<? extends IdentifiableMailboxWithRole>, Boolean>() {
            @Override
            public ListenableFuture<Boolean> apply(@NullableDecl Collection<? extends IdentifiableMailboxWithRole> mailboxes) {
                Preconditions.checkNotNull(mailboxes, "SpecialMailboxes collection must not be null but can be empty");
                return moveToTrash(emails, MailboxUtil.find(mailboxes, Role.TRASH));
            }
        }, MoreExecutors.directExecutor());
    }

    /**
     * Moves all emails in this collection (usually applied to an entire thread) to the trash mailbox. The emails will
     * be removed from all other mailboxes. If a certain email in this collection is already only in the trash mailbox
     * this email will not be processed.
     *
     * @param emails A collection of emails. Usually all messages in a thread
     * @param trash  A reference to the Trash mailbox. Can be null and a new trash mailbox will automatically be created.
     *               Do not pass null if a Trash mailbox exists on the server as this call will attempt to create one
     *               and fail.
     */

    public ListenableFuture<Boolean> moveToTrash(final Collection<? extends IdentifiableEmailWithMailboxIds> emails, @NullableDecl final IdentifiableMailboxWithRole trash) {
        return Futures.transformAsync(getObjectsState(), new AsyncFunction<ObjectsState, Boolean>() {
            @Override
            public ListenableFuture<Boolean> apply(@NullableDecl ObjectsState objectsState) throws Exception {
                return moveToTrash(emails, trash, objectsState);
            }
        }, MoreExecutors.directExecutor());
    }

    private ListenableFuture<Boolean> moveToTrash(Collection<? extends IdentifiableEmailWithMailboxIds> emails, @NullableDecl final IdentifiableMailboxWithRole trash, final ObjectsState objectsState) {
        final JmapClient.MultiCall multiCall = jmapClient.newMultiCall();
        final ListenableFuture<MethodResponses> mailboxCreateFuture;
        if (trash == null) {
            mailboxCreateFuture = createMailbox(Role.TRASH, objectsState, multiCall);
        } else {
            mailboxCreateFuture = null;
        }
        ImmutableMap.Builder<String, Map<String, Object>> emailPatchObjectMapBuilder = ImmutableMap.builder();
        for (IdentifiableEmailWithMailboxIds email : emails) {
            if (trash != null && email.getMailboxIds().size() == 1 && email.getMailboxIds().containsKey(trash.getId())) {
                continue;
            }
            Patches.Builder patchesBuilder = Patches.builder();
            if (trash == null) {
                patchesBuilder.set("mailboxIds", ImmutableMap.of(CreateUtil.createIdReference(Role.TRASH), true));
            } else {
                patchesBuilder.set("mailboxIds", ImmutableMap.of(trash.getId(), true));
            }
            emailPatchObjectMapBuilder.put(email.getId(), patchesBuilder.build());
        }
        final ImmutableMap<String, Map<String, Object>> patches = emailPatchObjectMapBuilder.build();
        if (patches.size() == 0) {
            return Futures.immediateFuture(false);
        }
        final ListenableFuture<Boolean> patchesFuture = applyEmailPatches(patches,
                objectsState,
                trash != null,
                multiCall
        );
        multiCall.execute();
        return Futures.transformAsync(patchesFuture, new AsyncFunction<Boolean, Boolean>() {
            @Override
            public ListenableFuture<Boolean> apply(@NullableDecl Boolean patchesResults) throws Exception {
                if (mailboxCreateFuture != null) {
                    SetMailboxMethodResponse setMailboxResponse = mailboxCreateFuture.get().getMain(SetMailboxMethodResponse.class);
                    SetMailboxException.throwIfFailed(setMailboxResponse);
                }
                return Futures.immediateFuture(patchesResults);
            }
        }, MoreExecutors.directExecutor());
    }

    public ListenableFuture<Boolean> emptyTrash() {
        return Futures.transformAsync(getMailboxes(), new AsyncFunction<Collection<? extends IdentifiableMailboxWithRole>, Boolean>() {
            @Override
            public ListenableFuture<Boolean> apply(@NullableDecl Collection<? extends IdentifiableMailboxWithRole> mailboxes) {
                Preconditions.checkNotNull(mailboxes, "SpecialMailboxes collection must not be null but can be empty");
                final IdentifiableMailboxWithRole trash = MailboxUtil.find(mailboxes, Role.TRASH);
                if (trash == null) {
                    return Futures.immediateFailedFuture(new IllegalStateException("No mailbox with trash role"));
                }
                return emptyTrash(trash);
            }
        }, MoreExecutors.directExecutor());

    }

    public ListenableFuture<Boolean> emptyTrash(@NonNullDecl IdentifiableMailboxWithRole trash) {
        final JmapClient.MultiCall multiCall = jmapClient.newMultiCall();
        final EmailFilterCondition filter = EmailFilterCondition.builder().inMailbox(trash.getId()).build();
        final Call queryCall = multiCall.call(new QueryEmailMethodCall(filter));
        final ListenableFuture<MethodResponses> setFuture = multiCall.call(new SetEmailMethodCall(accountId, null, queryCall.createResultReference(Request.Invocation.ResultReference.Path.IDS))).getMethodResponses();
        multiCall.execute();
        return Futures.transformAsync(setFuture, new AsyncFunction<MethodResponses, Boolean>() {
            @Override
            public ListenableFuture<Boolean> apply(@NullableDecl MethodResponses methodResponses) throws Exception {
                SetEmailMethodResponse setEmailMethodResponse = setFuture.get().getMain(SetEmailMethodResponse.class);
                SetEmailException.throwIfFailed(setEmailMethodResponse);
                final String[] destroyed = setEmailMethodResponse.getDestroyed();
                LOGGER.info("Deleted {} emails", destroyed == null ? 0 : destroyed.length);
                return Futures.immediateFuture(true);
            }
        }, MoreExecutors.directExecutor());
    }

    public ListenableFuture<Status> refresh() {
        return Futures.transformAsync(getObjectsState(), new AsyncFunction<ObjectsState, Status>() {
            @Override
            public ListenableFuture<Status> apply(@NullableDecl ObjectsState objectsState) throws Exception {
                return refresh(objectsState);
            }
        }, MoreExecutors.directExecutor());
    }

    private ListenableFuture<Status> refresh(ObjectsState objectsState) {
        final JmapClient.MultiCall multiCall = jmapClient.newMultiCall();
        List<ListenableFuture<Status>> futuresList = piggyBack(objectsState, multiCall);
        multiCall.execute();
        return transform(futuresList);
    }

    private List<ListenableFuture<Status>> piggyBack(ObjectsState objectsState, JmapClient.MultiCall multiCall) {
        ImmutableList.Builder<ListenableFuture<Status>> futuresListBuilder = new ImmutableList.Builder<>();
        if (objectsState.mailboxState != null) {
            futuresListBuilder.add(updateMailboxes(objectsState.mailboxState, multiCall));
        } else {
            futuresListBuilder.add(loadMailboxes(multiCall));
        }

        //update to emails should happen before update to threads
        //when mua queries threads the corresponding emails should already be in the cache

        if (objectsState.emailState != null) {
            futuresListBuilder.add(updateEmails(objectsState.emailState, multiCall));
        }
        if (objectsState.threadState != null) {
            futuresListBuilder.add(updateThreads(objectsState.threadState, multiCall));
        }
        return futuresListBuilder.build();
    }

    private ListenableFuture<Status> updateThreads(final String state, final JmapClient.MultiCall multiCall) {
        Preconditions.checkNotNull(state, "state can not be null when updating threads");
        final SettableFuture<Status> settableFuture = SettableFuture.create();
        final UpdateUtil.MethodResponsesFuture methodResponsesFuture = UpdateUtil.threads(multiCall, accountId, state);
        methodResponsesFuture.changes.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    final ChangesThreadMethodResponse changesResponse = methodResponsesFuture.changes.get().getMain(ChangesThreadMethodResponse.class);
                    final GetThreadMethodResponse createdResponse = methodResponsesFuture.created.get().getMain(GetThreadMethodResponse.class);
                    final GetThreadMethodResponse updatedResponse = methodResponsesFuture.updated.get().getMain(GetThreadMethodResponse.class);
                    final Update<Thread> update = Update.of(changesResponse, createdResponse, updatedResponse);
                    if (update.hasChanges()) {
                        cache.updateThreads(update);
                    }
                    settableFuture.set(Status.of(update));
                } catch (InterruptedException | ExecutionException | CacheWriteException | CacheConflictException e) {
                    settableFuture.setException(extractException(e));
                }
            }
        }, ioExecutorService);
        return settableFuture;
    }

    private static ListenableFuture<Status> transform(List<ListenableFuture<Status>> list) {
        return Futures.transform(Futures.allAsList(list), new Function<List<Status>, Status>() {
            @NullableDecl
            @Override
            public Status apply(@NullableDecl List<Status> statuses) {
                if (statuses.contains(Status.HAS_MORE)) {
                    return Status.HAS_MORE;
                }
                if (statuses.contains(Status.UPDATED)) {
                    return Status.UPDATED;
                }
                return Status.UNCHANGED;
            }
        }, MoreExecutors.directExecutor());
    }

    public ListenableFuture<Status> query(Filter<Email> filter) {
        return query(EmailQuery.of(filter));
    }

    public ListenableFuture<Status> query(@NonNullDecl final EmailQuery query) {
        final ListenableFuture<QueryStateWrapper> queryStateFuture = ioExecutorService.submit(new Callable<QueryStateWrapper>() {
            @Override
            public QueryStateWrapper call() throws Exception {
                return cache.getQueryState(query.toQueryString());
            }
        });

        return Futures.transformAsync(queryStateFuture, new AsyncFunction<QueryStateWrapper, Status>() {
            @Override
            public ListenableFuture<Status> apply(@NullableDecl QueryStateWrapper queryStateWrapper) {
                Preconditions.checkNotNull(queryStateWrapper, "QueryStateWrapper can not be null");
                if (queryStateWrapper.queryState == null || queryStateWrapper.upTo == null) {
                    return initialQuery(query, queryStateWrapper);
                } else {
                    Preconditions.checkNotNull(queryStateWrapper.objectsState, "ObjectsState can not be null if queryState was not");
                    Preconditions.checkNotNull(queryStateWrapper.objectsState.emailState, "emailState can not be null if queryState was not");
                    Preconditions.checkNotNull(queryStateWrapper.objectsState.threadState, "threadState can not be null if queryState was not");
                    return refreshQuery(query, queryStateWrapper);
                }
            }
        }, MoreExecutors.directExecutor());
    }

    public ListenableFuture<Status> query(@NonNullDecl final EmailQuery query, final String afterEmailId) {
        final ListenableFuture<QueryStateWrapper> queryStateFuture = ioExecutorService.submit(new Callable<QueryStateWrapper>() {
            @Override
            public QueryStateWrapper call() throws Exception {
                return cache.getQueryState(query.toQueryString());
            }
        });
        return Futures.transformAsync(queryStateFuture, new AsyncFunction<QueryStateWrapper, Status>() {
            @Override
            public ListenableFuture<Status> apply(@NullableDecl QueryStateWrapper queryStateWrapper) {
                return query(query, afterEmailId, queryStateWrapper);
            }
        }, MoreExecutors.directExecutor());
    }

    private ListenableFuture<Status> query(@NonNullDecl final EmailQuery query, @NonNullDecl final String afterEmailId, final QueryStateWrapper queryStateWrapper) {
        Preconditions.checkNotNull(query, "Query can not be null");
        Preconditions.checkNotNull(afterEmailId, "afterEmailId can not be null");
        Preconditions.checkNotNull(queryStateWrapper, "QueryStateWrapper can not be null when paging");


        //TODO: this currently means we can’t page in queries that aren’t cacheable (=don’t have a queryState)
        //TODO: we should probably get rid of that check and instead simply don’t do the update call
        //TODO: likewise we probably need to be able to ignore a canNotCalculate Changes error on the update response
        if (queryStateWrapper.queryState == null) {
            throw new InconsistentQueryStateException("QueryStateWrapper needs queryState for paging");
        }
        if (!afterEmailId.equals(queryStateWrapper.upTo)) {
            //in conjunction with lttrs-android this can happen if we have a QueryItemOverwrite for the last item in
            //a query. This will probably fix itself once the update command has run as well as a subsequent updateQuery() call.
            //TODO: is there a point in triggering a queryUpdate from Mua? Probably not as we don’t know if the update command ran yet.
            throw new InconsistentQueryStateException("upToId from QueryState needs to match the supplied afterEmailId");
        }
        final SettableFuture<Status> settableFuture = SettableFuture.create();
        JmapClient.MultiCall multiCall = jmapClient.newMultiCall();
        final ListenableFuture<Status> queryRefreshFuture = refreshQuery(query, queryStateWrapper, multiCall);

        final Call queryCall = multiCall.call(new QueryEmailMethodCall(query, afterEmailId, this.queryPageSize));
        final ListenableFuture<MethodResponses> queryResponsesFuture = queryCall.getMethodResponses();
        final ListenableFuture<MethodResponses> getThreadIdsResponsesFuture = multiCall.call(new GetEmailMethodCall(accountId, queryCall.createResultReference(Request.Invocation.ResultReference.Path.IDS), new String[]{"threadId"})).getMethodResponses();

        queryResponsesFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    //This needs to be the first response that we get in order to properly process a potential
                    //anchorNotFound in the catch block
                    final QueryEmailMethodResponse queryResponse = queryResponsesFuture.get().getMain(QueryEmailMethodResponse.class);
                    final GetEmailMethodResponse getThreadIdsResponse = getThreadIdsResponsesFuture.get().getMain(GetEmailMethodResponse.class);

                    final QueryResult queryResult = QueryResult.of(queryResponse, getThreadIdsResponse);

                    //processing order is:
                    //  1) refresh the existent query (which in our implementation also piggybacks email and thread updates)
                    //  2) store new items

                    //TODO status=has_more should probably throw; but cache will eventually throw anyway
                    //TODO as mentioned above we probably need to ignore canNotCalculate changes errors and the like otherwise we won’t be able to page through queries that aren’t cachable
                    queryRefreshFuture.get();

                    try {
                        cache.addQueryResult(query.toQueryString(), afterEmailId, queryResult);
                    } catch (CorruptCacheException e) {
                        LOGGER.info("Invalidating query result cache after cache corruption", e);
                        cache.invalidateQueryResult(query.toQueryString());
                        throw e;
                    }

                    fetchMissing(query.toQueryString()).addListener(new Runnable() {
                        @Override
                        public void run() {
                            settableFuture.set(queryResult.items.length > 0 ? Status.UPDATED : Status.UNCHANGED);
                        }
                    }, MoreExecutors.directExecutor());
                } catch (ExecutionException e) {
                    final Throwable cause = e.getCause();
                    if (cause instanceof MethodErrorResponseException) {
                        final MethodErrorResponse methodError = ((MethodErrorResponseException) cause).getMethodErrorResponse();
                        if (methodError instanceof AnchorNotFoundMethodErrorResponse) {
                            if (Status.unchanged(queryRefreshFuture)) {
                                LOGGER.info("Invalidating query result cache after receiving AnchorNotFound response");
                                cache.invalidateQueryResult(query.toQueryString());
                            } else {
                                LOGGER.info(
                                        "Holding back on invaliding query result cache despite AnchorNotFound response because query refresh had changes"
                                );
                            }
                        }
                    }
                    settableFuture.setException(extractException(e));
                } catch (Exception e) {
                    settableFuture.setException(extractException(e));
                }
            }
        }, ioExecutorService);
        multiCall.execute();
        return settableFuture;
    }

    private ListenableFuture<Status> refreshQuery(@NonNullDecl final EmailQuery query, @NonNullDecl final QueryStateWrapper queryStateWrapper) {
        final JmapClient.MultiCall multiCall = jmapClient.newMultiCall();
        ListenableFuture<Status> future = refreshQuery(query, queryStateWrapper, multiCall);
        multiCall.execute();
        return future;
    }

    private ListenableFuture<Status> refreshQuery(@NonNullDecl final EmailQuery query, @NonNullDecl final QueryStateWrapper queryStateWrapper, final JmapClient.MultiCall multiCall) {
        Preconditions.checkNotNull(queryStateWrapper.queryState, "QueryState can not be null when attempting to refresh query");
        final SettableFuture<Status> settableFuture = SettableFuture.create();

        final List<ListenableFuture<Status>> piggyBackedFuturesList = piggyBack(queryStateWrapper.objectsState, multiCall);

        final Call queryChangesCall = multiCall.call(new QueryChangesEmailMethodCall(accountId, queryStateWrapper.queryState, query)); //TODO do we want to include upTo?
        final ListenableFuture<MethodResponses> queryChangesResponsesFuture = queryChangesCall.getMethodResponses();
        final ListenableFuture<MethodResponses> getThreadIdResponsesFuture = multiCall.call(new GetEmailMethodCall(accountId, queryChangesCall.createResultReference(Request.Invocation.ResultReference.Path.ADDED_IDS), new String[]{"threadId"})).getMethodResponses();

        queryChangesResponsesFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    QueryChangesEmailMethodResponse queryChangesResponse = queryChangesResponsesFuture.get().getMain(QueryChangesEmailMethodResponse.class);
                    GetEmailMethodResponse getThreadIdsResponse = getThreadIdResponsesFuture.get().getMain(GetEmailMethodResponse.class);
                    List<AddedItem<QueryResultItem>> added = QueryResult.of(queryChangesResponse, getThreadIdsResponse);

                    final QueryUpdate<Email, QueryResultItem> queryUpdate = QueryUpdate.of(queryChangesResponse, added);

                    //processing order is:
                    //  1) update Objects (Email, Threads, and Mailboxes)
                    //  2) store query results; If query cache sees an outdated email state it will fail

                    Status piggybackStatus = transform(piggyBackedFuturesList).get(); //wait for updates before attempting to fetch
                    Status queryUpdateStatus = Status.of(queryUpdate);

                    if (queryUpdate.hasChanges()) {
                        cache.updateQueryResults(query.toQueryString(), queryUpdate, getThreadIdsResponse.getTypedState());
                    }

                    if (piggybackStatus == Status.UNCHANGED && queryUpdateStatus == Status.UNCHANGED) {
                        settableFuture.set(Status.UNCHANGED);
                    } else {
                        final List<ListenableFuture<Status>> list = new ArrayList<>();
                        list.add(Futures.immediateFuture(queryUpdateStatus));
                        //TODO this should be unnecessary. At the time of an refresh we have previously loaded all ids
                        //TODO: however it might be that a previous fetchMissing() has failed. so better safe than sorry
                        list.add(fetchMissing(query.toQueryString()));
                        settableFuture.setFuture(transform(list));
                    }

                } catch (InterruptedException | ExecutionException | CacheWriteException | CacheConflictException e) {
                    settableFuture.setException(extractException(e));
                }
            }
        }, ioExecutorService);

        return settableFuture;
    }

    private ListenableFuture<Status> initialQuery(@NonNullDecl final EmailQuery query, @NonNullDecl final QueryStateWrapper queryStateWrapper) {

        Preconditions.checkState(queryStateWrapper.queryState == null || queryStateWrapper.upTo == null, "QueryState or upTo must be NULL when calling initialQuery");

        final SettableFuture<Status> settableFuture = SettableFuture.create();
        JmapClient.MultiCall multiCall = jmapClient.newMultiCall();

        //these need to be processed *before* the Query call or else the fetchMissing will not honor newly fetched ids
        final List<ListenableFuture<Status>> piggyBackedFuturesList = piggyBack(queryStateWrapper.objectsState, multiCall);

        final Call queryCall = multiCall.call(new QueryEmailMethodCall(query, this.queryPageSize));
        final ListenableFuture<MethodResponses> queryResponsesFuture = queryCall.getMethodResponses();
        final Call threadIdsCall = multiCall.call(new GetEmailMethodCall(accountId, queryCall.createResultReference(Request.Invocation.ResultReference.Path.IDS), new String[]{"threadId"}));
        final ListenableFuture<MethodResponses> getThreadIdsResponsesFuture = threadIdsCall.getMethodResponses();


        final ListenableFuture<MethodResponses> getThreadsResponsesFuture;
        final ListenableFuture<MethodResponses> getEmailResponsesFuture;
        if (queryStateWrapper.objectsState.threadState == null && queryStateWrapper.objectsState.emailState == null) {
            final Call threadCall = multiCall.call(new GetThreadMethodCall(accountId, threadIdsCall.createResultReference(Request.Invocation.ResultReference.Path.LIST_THREAD_IDS)));
            getThreadsResponsesFuture = threadCall.getMethodResponses();
            getEmailResponsesFuture = multiCall.call(new GetEmailMethodCall(accountId, threadCall.createResultReference(Request.Invocation.ResultReference.Path.LIST_EMAIL_IDS), true)).getMethodResponses();
        } else {
            getThreadsResponsesFuture = null;
            getEmailResponsesFuture = null;
        }

        multiCall.execute();
        queryResponsesFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    QueryEmailMethodResponse queryResponse = queryResponsesFuture.get().getMain(QueryEmailMethodResponse.class);
                    GetEmailMethodResponse getThreadIdsResponse = getThreadIdsResponsesFuture.get().getMain(GetEmailMethodResponse.class);

                    QueryResult queryResult = QueryResult.of(queryResponse, getThreadIdsResponse);

                    //processing order is:
                    //  1) update Objects (Email, Threads, and Mailboxes)
                    //  2) if getThread or getEmails calls where made process those results
                    //  3) store query results; If query cache sees an outdated email state it will fail
                    transform(piggyBackedFuturesList).get();

                    if (getThreadsResponsesFuture != null && getEmailResponsesFuture != null) {
                        GetThreadMethodResponse getThreadsResponse = getThreadsResponsesFuture.get().getMain(GetThreadMethodResponse.class);
                        GetEmailMethodResponse getEmailResponse = getEmailResponsesFuture.get().getMain(GetEmailMethodResponse.class);
                        cache.setThreadsAndEmails(getThreadsResponse.getTypedState(), getThreadsResponse.getList(), getEmailResponse.getTypedState(), getEmailResponse.getList());
                    }

                    if (queryResult.position != 0) {
                        throw new IllegalStateException("Server reported position " + queryResult.position + " in response to initial query. We expected 0");
                    }

                    cache.setQueryResult(query.toQueryString(), queryResult);

                    if (getThreadsResponsesFuture != null && getEmailResponsesFuture != null) {
                        settableFuture.set(Status.UPDATED);
                    } else {
                        List<ListenableFuture<Status>> list = new ArrayList<>();
                        list.add(Futures.immediateFuture(Status.UPDATED));
                        list.add(fetchMissing(query.toQueryString()));
                        settableFuture.setFuture(transform(list));
                    }
                } catch (InterruptedException | ExecutionException | CacheWriteException e) {
                    settableFuture.setException(extractException(e));
                }
            }
        }, ioExecutorService);
        return settableFuture;
    }

    private ListenableFuture<Status> fetchMissing(@NonNullDecl final String queryString) {
        Preconditions.checkNotNull(queryString, "QueryString can not be null");
        try {
            return fetchMissing(cache.getMissing(queryString));
        } catch (CacheReadException e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    private ListenableFuture<Status> fetchMissing(final Missing missing) {
        Preconditions.checkNotNull(missing, "Missing can not be null");
        Preconditions.checkNotNull(missing.threadIds, "Missing.ThreadIds can not be null; pass empty list instead");
        if (missing.threadIds.size() == 0) {
            return Futures.immediateFuture(Status.UNCHANGED);
        }
        LOGGER.info("fetching " + missing.threadIds.size() + " missing threads");
        final SettableFuture<Status> settableFuture = SettableFuture.create();
        final JmapClient.MultiCall multiCall = jmapClient.newMultiCall();
        final ListenableFuture<Status> updateThreadsFuture = updateThreads(missing.threadState, multiCall);
        final ListenableFuture<Status> updateEmailsFuture = updateEmails(missing.emailState, multiCall);
        final Call threadsCall = multiCall.call(new GetThreadMethodCall(accountId, missing.threadIds.toArray(new String[0])));
        final ListenableFuture<MethodResponses> getThreadsResponsesFuture = threadsCall.getMethodResponses();
        final ListenableFuture<MethodResponses> getEmailsResponsesFuture = multiCall.call(new GetEmailMethodCall(accountId, threadsCall.createResultReference(Request.Invocation.ResultReference.Path.LIST_EMAIL_IDS), true)).getMethodResponses();
        multiCall.execute();
        getThreadsResponsesFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    Status updateThreadsStatus = updateThreadsFuture.get();
                    if (updateThreadsStatus == Status.HAS_MORE) {
                        //throw
                    }

                    Status updateEmailStatus = updateEmailsFuture.get();
                    if (updateEmailStatus == Status.HAS_MORE) {
                        //throw
                    }

                    GetThreadMethodResponse getThreadMethodResponse = getThreadsResponsesFuture.get().getMain(GetThreadMethodResponse.class);
                    GetEmailMethodResponse getEmailMethodResponse = getEmailsResponsesFuture.get().getMain(GetEmailMethodResponse.class);
                    cache.addThreadsAndEmail(getThreadMethodResponse.getTypedState(), getThreadMethodResponse.getList(), getEmailMethodResponse.getTypedState(), getEmailMethodResponse.getList());

                    settableFuture.set(Status.UPDATED);

                } catch (Exception e) {
                    settableFuture.setException(extractException(e));
                }

            }
        }, ioExecutorService);
        return settableFuture;
    }

    public static class Builder {
        private String username;
        private String password;
        private String accountId;
        private SessionCache sessionCache = new SessionFileCache();
        private Cache cache = new InMemoryCache();
        private Long queryPageSize = null;

        private Builder() {

        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder accountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder queryPageSize(int queryPageSize) {
            return queryPageSize((long) queryPageSize);
        }

        public Builder queryPageSize(Long queryPageSize) {
            this.queryPageSize = queryPageSize;
            return this;
        }

        public Builder sessionCache(SessionCache sessionCache) {
            this.sessionCache = sessionCache;
            return this;
        }

        public Builder cache(Cache cache) {
            this.cache = cache;
            return this;
        }

        public Mua build() {
            Preconditions.checkNotNull(accountId, "accountId is required");

            JmapClient jmapClient = new JmapClient(this.username, this.password);
            jmapClient.setSessionCache(this.sessionCache);
            Mua mua = new Mua(jmapClient, cache, accountId);
            mua.queryPageSize = this.queryPageSize;
            return mua;
        }
    }

}
