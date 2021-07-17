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

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import okhttp3.HttpUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rs.ltt.jmap.client.JmapClient;
import rs.ltt.jmap.client.blob.Download;
import rs.ltt.jmap.client.session.InMemorySessionCache;
import rs.ltt.jmap.client.session.SessionCache;
import rs.ltt.jmap.common.entity.*;
import rs.ltt.jmap.common.entity.query.EmailQuery;
import rs.ltt.jmap.mua.cache.Cache;
import rs.ltt.jmap.mua.cache.InMemoryCache;
import rs.ltt.jmap.mua.service.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;

public class Mua extends MuaSession {

    private static final Logger LOGGER = LoggerFactory.getLogger(Mua.class);

    private Mua(JmapClient jmapClient, Cache cache, String accountId) {
        super(jmapClient, cache, accountId);
    }

    public static Builder builder() {
        return new Builder();
    }

    public ListenableFuture<Status> refresh() {
        return getService(RefreshService.class).refresh();
    }

    public ListenableFuture<Status> refreshIdentities() {
        return getService(IdentityService.class).refreshIdentities();
    }

    public ListenableFuture<Status> refreshMailboxes() {
        return getService(MailboxService.class).refreshMailboxes();
    }

    public ListenableFuture<Boolean> createMailbox(final Mailbox mailbox) {
        return getService(MailboxService.class).createMailbox(mailbox);
    }

    public ListenableFuture<Status> query(@Nonnull final EmailQuery query) {
        return getService(QueryService.class).query(query, null);
    }

    public ListenableFuture<Status> query(@Nonnull final EmailQuery query, Boolean calculateTotal) {
        return getService(QueryService.class).query(query, calculateTotal);
    }

    public ListenableFuture<Status> query(@Nonnull final EmailQuery query, final String afterEmailId) {
        return getService(QueryService.class).query(query, null, afterEmailId);
    }

    public ListenableFuture<Status> query(@Nonnull final EmailQuery query, Boolean calculateTotal, final String afterEmailId) {
        return getService(QueryService.class).query(query, calculateTotal, afterEmailId);
    }

    /**
     * Stores an email as a draft. This method will take care of adding the draft and seen keyword and moving the email
     * to the draft mailbox.
     *
     * @param email The email that should be saved as a draft
     * @return String The id of the email that has been created
     */
    public ListenableFuture<String> draft(final Email email) {
        return getService(EmailService.class).draft(email);
    }

    /**
     * Stores an email as a draft. This method will take care of adding the draft and seen keyword and moving the email
     * to the draft mailbox.
     *
     * @param email  The email that should be saved as a draft
     * @param drafts A reference to the Drafts mailbox. Can be null and a new Draft mailbox will automatically be created.
     *               Do not pass null if a Drafts mailbox exists on the server as this call will attempt to create one
     *               and fail.
     * @return The id of the email that has been created
     */
    public ListenableFuture<String> draft(final Email email, final IdentifiableMailboxWithRole drafts) {
        return getService(EmailService.class).draft(email, drafts);
    }

    public ListenableFuture<Boolean> submit(final Email email, final Identity identity) {
        return getService(EmailService.class).submit(email, identity);
    }

    /**
     * Submits (sends / EmailSubmission) a previously drafted email. The email will be removed from the Drafts mailbox
     * and put into the Sent mailbox after successful submission. Additionally the draft keyword will be removed.
     *
     * @param emailId  The id of the email that should be submitted
     * @param identity The identity used to submit that email
     * @return
     */
    public ListenableFuture<Boolean> submit(final String emailId, final IdentifiableIdentity identity) {
        return getService(EmailService.class).submit(emailId, identity);
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
    public ListenableFuture<Boolean> submit(final String emailId, final IdentifiableIdentity identity, @Nullable String draftMailboxId, final IdentifiableMailboxWithRole sent) {
        return getService(EmailService.class).submit(emailId, identity, draftMailboxId, sent);
    }

    public ListenableFuture<String> send(final Email email, final IdentifiableIdentity identity) {
        return getService(EmailService.class).send(email, identity);
    }

    public ListenableFuture<Boolean> setKeyword(final Collection<? extends IdentifiableEmailWithKeywords> emails, final String keyword) {
        return getService(EmailService.class).setKeyword(emails, keyword);
    }

    public ListenableFuture<Boolean> discardDraft(final @Nonnull IdentifiableEmailWithKeywords email) {
        return getService(EmailService.class).discardDraft(email);
    }

    public ListenableFuture<Boolean> removeKeyword(final Collection<? extends IdentifiableEmailWithKeywords> emails,
                                                   final String keyword) {
        return getService(EmailService.class).removeKeyword(emails, keyword);
    }

    /**
     * Copies the individual emails in this collection (usually applied to an entire thread) to the mailbox with the
     * role IMPORTANT. If a mailbox with that role doesn’t exist it will be created.
     *
     * @param emails A collection of emails. Usually all messages in a thread
     * @return
     */
    public ListenableFuture<Boolean> copyToImportant(@Nonnull final Collection<? extends IdentifiableEmailWithMailboxIds> emails) {
        return getService(EmailService.class).copyToImportant(emails);
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
    public ListenableFuture<Boolean> copyToMailbox(final Collection<? extends IdentifiableEmailWithMailboxIds> emails,
                                                   final IdentifiableMailboxWithRole mailbox) {
        return getService(EmailService.class).copyToMailbox(emails, mailbox);
    }


    public ListenableFuture<Boolean> modifyLabels(final Collection<? extends IdentifiableEmailWithMailboxIds> emails,
                                                  final Collection<? extends IdentifiableMailboxWithRoleAndName> additions,
                                                  final Collection<? extends IdentifiableMailboxWithRoleAndName> removals) {
        return getService(EmailService.class).modifyLabels(emails, additions, removals);
    }

    /**
     * Removes the emails in this collection from both the Trash and Archive mailbox (if they are in either of those)
     * and puts all emails into the Inbox instead.
     *
     * @param emails A collection of emails; usually all emails in a thread
     * @return
     */
    public ListenableFuture<Boolean> moveToInbox(final Collection<? extends IdentifiableEmailWithMailboxIds> emails) {
        return getService(EmailService.class).moveToInbox(emails);
    }

    /**
     * Moves the individual emails in this collection (usually applied to an entire thread) from the inbox to the archive.
     * Any email that is not in the inbox will be skipped.
     *
     * @param emails A collection of emails. Usually all messages in a thread
     * @return
     */
    public ListenableFuture<Boolean> archive(final Collection<? extends IdentifiableEmailWithMailboxIds> emails) {
        return getService(EmailService.class).archive(emails);
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
    public ListenableFuture<Boolean> removeFromMailbox(final Collection<? extends IdentifiableEmailWithMailboxIds> emails,
                                                       final String mailboxId) {
        return getService(EmailService.class).removeFromMailbox(emails, mailboxId);
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
    public ListenableFuture<Boolean> removeFromMailbox(final Collection<? extends IdentifiableEmailWithMailboxIds> emails,
                                                       @Nonnull final Mailbox mailbox,
                                                       @Nullable final IdentifiableMailboxWithRole archive) {
        return getService(EmailService.class).removeFromMailbox(emails, mailbox, archive);
    }

    /**
     * Moves all emails in this collection (usually applied to an entire thread) to the trash mailbox. The emails will
     * be removed from all other mailboxes. If a certain email in this collection is already only in the trash mailbox
     * this email will not be processed.
     *
     * @param emails A collection of emails. Usually all messages in a thread
     */
    public ListenableFuture<Boolean> moveToTrash(final Collection<? extends IdentifiableEmailWithMailboxIds> emails) {
        return getService(EmailService.class).moveToTrash(emails);
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
    public ListenableFuture<Boolean> moveToTrash(final Collection<? extends IdentifiableEmailWithMailboxIds> emails,
                                                 final IdentifiableMailboxWithRole trash) {
        return getService(EmailService.class).moveToTrash(emails, trash);
    }

    public ListenableFuture<Boolean> emptyTrash() {
        return getService(EmailService.class).emptyTrash();
    }

    public ListenableFuture<Boolean> emptyTrash(@Nonnull IdentifiableMailboxWithRole trash) {
        return getService(EmailService.class).emptyTrash(trash);
    }

    public ListenableFuture<Boolean> setRole(final IdentifiableMailboxWithRole mailbox, final Role role) {
        return getService(MailboxService.class).setRole(mailbox, role);
    }

    /**
     * Retrieves (downloads) binary data based on a Downloadable (blobId, type, name)
     *
     * @param downloadable An EmailBodyPart or another class that implements Downloadable
     * @return A Download Future that contains the InputStream, size and the cancelable HTTP Call
     */
    public ListenableFuture<Download> download(final Downloadable downloadable) {
        return jmapClient.download(getAccountId(), downloadable);
    }

    /**
     * Resumes the download of binary data based on a Downloadable (blobId, type, name) and the current file size
     * @param downloadable  An EmailBodyPart or another class that implements Downloadable
     * @param rangeStart The amount of data (file size) that has previously been downloaded
     * @return A Download Future that contains the InputStream, size and the cancelable HTTP Call and an indication if resume has been successful
     */
    public ListenableFuture<Download> download(final Downloadable downloadable, final long rangeStart) {
        Preconditions.checkArgument(rangeStart >= 0, "rangeStart must not be smaller than 0");
        return jmapClient.download(getAccountId(), downloadable, rangeStart);
    }

    public static class Builder {
        private String username;
        private String password;
        private HttpUrl sessionResource;
        private String accountId;
        private SessionCache sessionCache = new InMemorySessionCache();
        private Cache cache = new InMemoryCache();
        private Long queryPageSize = null;
        private Boolean useWebSocket;

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

        public Builder sessionResource(String sessionResource) {
            return sessionResource(HttpUrl.get(sessionResource));
        }

        public Builder sessionResource(HttpUrl sessionResource) {
            this.sessionResource = sessionResource;
            return this;
        }

        public Builder accountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder useWebSocket(final boolean useWebSocket) {
            this.useWebSocket = useWebSocket;
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

            final JmapClient jmapClient = new JmapClient(this.username, this.password, this.sessionResource);
            jmapClient.setSessionCache(this.sessionCache);
            if (this.useWebSocket != null) {
                jmapClient.setUseWebSocket(this.useWebSocket);
            }
            final Mua mua = new Mua(jmapClient, cache, accountId);
            mua.setQueryPageSize(this.queryPageSize);
            return mua;
        }
    }
}
