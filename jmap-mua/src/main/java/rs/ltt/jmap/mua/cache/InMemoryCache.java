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

package rs.ltt.jmap.mua.cache;

import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rs.ltt.jmap.common.entity.Thread;
import rs.ltt.jmap.common.entity.*;
import rs.ltt.jmap.mua.cache.exception.*;
import rs.ltt.jmap.mua.util.QueryResult;
import rs.ltt.jmap.mua.util.QueryResultItem;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class InMemoryCache implements Cache {

    private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryCache.class);

    protected final HashMap<String, Mailbox> mailboxes = new HashMap<>();
    protected final HashMap<String, Thread> threads = new HashMap<>();
    protected final HashMap<String, Email> emails = new HashMap<>();
    protected final HashMap<String, Identity> identities = new HashMap<>();
    protected final HashMap<String, InMemoryQueryResult> queryResults = new HashMap<>();
    private String mailboxState = null;
    private String threadState = null;
    private String emailState = null;
    private String identityState = null;

    @Override
    public String getIdentityState() {
        return identityState;
    }

    @Override
    public String getMailboxState() {
        return mailboxState;
    }
    
    @Override
    @Nonnull
    public QueryStateWrapper getQueryState(String query) {
        synchronized (this.queryResults) {
            final String mailboxState = this.mailboxState;
            final String threadState = this.threadState;
            final String emailState = this.emailState;
            final ObjectsState objectsState = new ObjectsState(mailboxState, threadState, emailState);
            final InMemoryQueryResult queryResult = queryResults.get(query);
            if (queryResult == null) {
                return new QueryStateWrapper(null,false,null,objectsState);
            } else {
                final QueryStateWrapper.UpTo upTo;
                if (queryResult.items.size() > 0) {
                    final int lastPosition = queryResult.items.size() - 1;
                    final QueryResultItem lastItem = queryResult.items.get(lastPosition);
                    final String id = lastItem.getEmailId();
                    upTo = new QueryStateWrapper.UpTo(id, lastPosition);
                } else {
                    upTo = null;
                }
                return new QueryStateWrapper(queryResult.queryState, queryResult.canCalculateChanges, upTo, objectsState);
            }
        }
    }

    @Nonnull
    @Override
    public ObjectsState getObjectsState() {
        return new ObjectsState(mailboxState, threadState, emailState);
    }

    @Override
    public void setMailboxes(TypedState<Mailbox> state, Mailbox[] mailboxes) {
        synchronized (this.mailboxes) {
            this.mailboxes.clear();
            for (Mailbox mailbox : mailboxes) {
                this.mailboxes.put(mailbox.getId(), mailbox);
            }
            this.mailboxState = state.getState();
        }

    }

    @Override
    public void updateMailboxes(Update<Mailbox> mailboxUpdate, final String[] updatedProperties) throws CacheWriteException {
        synchronized (this.mailboxes) {
            for (Mailbox mailbox : mailboxUpdate.getCreated()) {
                this.mailboxes.put(mailbox.getId(), mailbox);
            }
            for (Mailbox mailbox : mailboxUpdate.getUpdated()) {
                Mailbox target = mailboxes.get(mailbox.getId());
                if (target == null) {
                    throw new CacheWriteException(String.format("Unable to update Mailbox(%s). Can not find in cache", mailbox.getId()));
                }
                if (updatedProperties != null) {
                    for (String property : updatedProperties) { //can be null
                        try {
                            copyProperty(target, mailbox, property, Mailbox.class);
                        } catch (NoSuchFieldException | IllegalAccessException e) {
                            throw new CacheWriteException(String.format("Unable to update Mailbox(%s). Can not update field %s", mailbox.getId(), property), e);
                        }
                    }
                } else {
                    this.mailboxes.put(mailbox.getId(), mailbox);
                }
            }
            for (String id : mailboxUpdate.getDestroyed()) {
                this.mailboxes.remove(id);
            }
            this.mailboxState = mailboxUpdate.getNewTypedState().getState();
        }
    }

    @Override
    public Collection<Mailbox> getSpecialMailboxes() throws NotSynchronizedException {
        synchronized (this.mailboxes) {
            if (this.mailboxState == null) {
                throw new NotSynchronizedException("Mailboxes have not been synchronized yet. Run refresh() first.");
            }
            return this.mailboxes.values();
        }
    }

    @Override
    public IdentifiableMailboxWithRoleAndName getMailboxByNameAndParent(String name, String parentId) throws NotSynchronizedException {
        synchronized (this.mailboxes) {
            if (this.mailboxState == null) {
                throw new NotSynchronizedException("Mailboxes have not been synchronized yet. Run refresh() first.");
            }
            return this.mailboxes.values().stream()
                    .filter(mailbox -> mailbox.getName().equals(name) && matches(mailbox.getParentId(), parentId))
                    .findFirst()
                    .orElse(null);
        }
    }

    @Override
    public Collection<IdentifiableMailboxWithRoleAndName> getMailboxesByNames(final String[] names) {
        final List<String> filter = Arrays.asList(names);
        return this.mailboxes.values().stream()
                    .filter(mailbox -> filter.contains(mailbox.getName()))
                    .collect(Collectors.toList());
    }

    private static boolean matches(final String a, final String b) {
        return (a == null && b == null || (a != null && a.equals(b)));
    }

    @Override
    public void setThreadsAndEmails(TypedState<Thread> threadState, Thread[] threads, TypedState<Email> emailState, Email[] emails) {
        setThreads(threadState, threads);
        setEmails(emailState, emails);
    }

    @Override
    public void addThreadsAndEmail(TypedState<Thread> threadState, Thread[] threads, TypedState<Email> emailState, Email[] emails) {
        addThreads(threadState, threads);
        addEmails(emailState, emails);
    }

    private void setThreads(final TypedState<Thread> typedState, Thread[] threads) {
        synchronized (this.threads) {
            this.threads.clear();
            for (Thread thread : threads) {
                this.threads.put(thread.getId(), thread);
            }
            this.threadState = typedState.getState();
        }
    }

    private void addThreads(final TypedState<Thread> typedState, final Thread[] threads) throws CacheConflictException {
        synchronized (this.threads) {
            if (typedState.getState() == null || !typedState.getState().equals(this.threadState)) {
                throw new CacheConflictException(String.format("Trying to add threads with an outdated state. Run update first. Cached state=%s. Your state=%s", this.threadState, typedState.getState()));
            }
            for (Thread thread : threads) {
                this.threads.put(thread.getId(), thread);
            }
        }
    }

    @Override
    public void updateThreads(Update<Thread> threadUpdate) throws CacheWriteException {
        synchronized (this.threads) {

            //TODO check state

            for (Thread thread : threadUpdate.getCreated()) {
                if (threads.containsKey(thread.getId())) {
                    throw new CacheWriteException(String.format("Unable to create Thread(%s). Thread already exists", thread.getId()));
                } else {
                    this.threads.put(thread.getId(), thread);
                }
            }
            for (Thread thread : threadUpdate.getUpdated()) {
                if (!this.threads.containsKey(thread.getId())) {
                    throw new CacheWriteException(String.format("Unable to update Thread(%s). Thread doesnt exists", thread.getId()));
                }
                this.threads.put(thread.getId(), thread);
            }
            for (String id : threadUpdate.getDestroyed()) {
                this.threads.remove(id);
            }
            this.threadState = threadUpdate.getNewTypedState().getState();
        }
    }

    private void setEmails(TypedState<Email> typedState, Email[] emails) {
        synchronized (this.emails) {
            this.emails.clear();
            for (Email email : emails) {
                this.emails.put(email.getId(), email);
            }
            this.emailState = typedState.getState();
        }
    }

    private void addEmails(TypedState<Email> typedState, Email[] emails) throws CacheConflictException {
        synchronized (this.emails) {
            if (typedState.getState() == null || !typedState.getState().equals(this.emailState)) {
                throw new CacheConflictException(String.format("Trying to add emails with an outdated state. Run update first. Cached state=%s. Your state=%s", this.emailState, typedState.getState()));
            }
            for (Email email : emails) {
                this.emails.put(email.getId(), email);
            }
        }
    }

    @Override
    public void updateEmails(Update<Email> emailUpdate, String[] updatedProperties) throws CacheWriteException {
        synchronized (this.emails) {

            //TODO check state

            for (Email email : emailUpdate.getCreated()) {
                this.emails.put(email.getId(), email);
            }
            for (Email email : emailUpdate.getUpdated()) {
                Email target = emails.get(email.getId());
                if (target == null) {
                    throw new CacheWriteException(String.format("Unable to update Email(%s). Can not find in cache", email.getId()));
                }
                for (String property : updatedProperties) {
                    try {
                        copyProperty(target, email, property, Email.class);
                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        throw new CacheWriteException(String.format("Unable to update Mailbox(%s). Can not update field %s", email.getId(), property), e);
                    }
                }
            }
            for (String id : emailUpdate.getDestroyed()) {
                this.emails.remove(id);
            }
            this.emailState = emailUpdate.getNewTypedState().getState();
        }
    }

    @Override
    public void setIdentities(final TypedState<Identity> typedState, final Identity[] identities) {
        synchronized (this.identities) {
            this.identities.clear();
            for (Identity identity : identities) {
                this.identities.put(identity.getId(), identity);
            }
            if (typedState.getState() == null) {
                LOGGER.warn("Identity state was null");
            } else {
                this.identityState = typedState.getState();
            }
        }

    }

    @Override
    public void updateIdentities(Update<Identity> identityUpdate) throws CacheWriteException {
        synchronized (this.identities) {
            for (Identity identity : identityUpdate.getCreated()) {
                if (this.identities.containsKey(identity.getId())) {
                    throw new CacheWriteException(String.format("Unable to create Identity(%s). Identity already exists", identity.getId()));
                } else {
                    this.identities.put(identity.getId(), identity);
                }
            }
            for (Identity identity : identityUpdate.getUpdated()) {
                if (!this.identities.containsKey(identity.getId())) {
                    throw new CacheWriteException(String.format("Unable to update Identity(%s). Identity doesnt exists", identity.getId()));
                }
                this.identities.put(identity.getId(), identity);
            }
            for (String id : identityUpdate.getDestroyed()) {
                this.identities.remove(id);
            }
            this.identityState = identityUpdate.getNewTypedState().getState();
        }
    }

    @Override
    public void invalidateIdentities() {
        synchronized (this.identities) {
            this.identities.clear();
            this.identityState = null;
        }
    }

    @Override
    public void setQueryResult(String query, QueryResult queryResult) {
        synchronized (this.queryResults) {
            final String emailState = queryResult.objectState.getState();
            if (emailState == null || !emailState.equals(this.emailState)) {
                throw new CacheConflictException(String.format("Email state must match when updating query results. Cached state=%s. Your state=%s", this.emailState, emailState));
            }
            this.queryResults.put(query, new InMemoryQueryResult(queryResult.queryState.getState(), queryResult.canCalculateChanges, queryResult.items));
        }
    }

    @Override
    public void addQueryResult(String queryString, String afterEmailId, QueryResult queryResult) throws CacheWriteException, CacheConflictException {
        synchronized (this.queryResults) {
            final String emailState = queryResult.objectState.getState();
            final String queryState = queryResult.queryState.getState();

            //TODO simply ignore if already applied

            if (emailState == null || !emailState.equals(this.emailState)) {
                throw new CacheConflictException(String.format("Email state must match when updating query results. Cached state=%s. Your state=%s", this.emailState, emailState));
            }
            final InMemoryQueryResult inMemoryQueryResult = this.queryResults.get(queryString);
            if (inMemoryQueryResult == null) {
                throw new CacheConflictException("QueryResult does not exist in our database");
            }
            if (queryState == null || !queryState.equals(inMemoryQueryResult.queryState)) {
                throw new CacheConflictException("QueryState does not match");
            }
            final int currentItemCount = inMemoryQueryResult.items.size();

            final String currentLastItemId = inMemoryQueryResult.items.get(currentItemCount - 1).getEmailId();

            if (!currentLastItemId.equals(afterEmailId)) {
                throw new CacheConflictException(String.format("Current last email id (%s) doesn't match afterId (%s) from request", currentLastItemId, afterEmailId));
            }

            if (currentItemCount != queryResult.position) {
                throw new CorruptCacheException(String.format("Unexpected QueryPage. Cache has %d items. Page starts at position %d",currentItemCount,queryResult.position));
            }
            inMemoryQueryResult.items.addAll(Arrays.asList(queryResult.items));
        }
    }

    @Override
    public void updateQueryResults(String query, QueryUpdate<Email, QueryResultItem> update, TypedState<Email> emailState) throws CacheWriteException, CacheConflictException {
        synchronized (this.queryResults) {
            final InMemoryQueryResult queryResult = this.queryResults.get(query);
            if (queryResult == null) {
                throw new CacheWriteException("Unable to update query. Can not find cached version");
            }
            if (emailState.getState() == null || !emailState.getState().equals(this.emailState)) {
                throw new CacheConflictException(String.format("Email state must match when updating query results. Cached state=%s. Your state=%s", this.emailState, emailState.getState()));
            }
            if (update.getOldTypedState().getState() == null || !update.getOldTypedState().getState().equals(queryResult.queryState)) {
                throw new CacheConflictException(String.format("OldState (%s) did not match our expectation ", update.getOldTypedState().getState()));
            }
            for (String removed : update.getRemoved()) {
                LOGGER.info("no removing id " + removed);
                queryResult.remove(removed);
            }
            for (AddedItem<QueryResultItem> addedItem : update.getAdded()) {
                //TODO it is probably save to just not add an item that exceeds the range (position > length) but this indicates a broken uper layer
                LOGGER.info("now adding " + addedItem.getItem().getEmailId() + " on index " + addedItem.getIndex());
                queryResult.items.add((int) addedItem.getIndex(), addedItem.getItem());
            }
            queryResult.queryState = update.getNewTypedState().getState();
        }
    }

    @Override
    public void invalidateEmailThreadsAndQueries() {
        synchronized (this.emails) {
            this.emails.clear();
            this.emailState = null;
        }
        synchronized (this.threads) {
            this.threads.clear();
            this.threadState = null;
        }
        synchronized (this.queryResults) {
            this.queryResults.clear();
        }
    }

    @Override
    public void invalidateMailboxes() {
        synchronized (this.mailboxes) {
            this.mailboxes.clear();
            this.mailboxState = null;
        }
    }

    @Override
    public void invalidateQueryResult(final String queryString) {
        synchronized (this.queryResults) {
            this.queryResults.remove(queryString);
        }
    }

    @Override
    public Missing getMissing(final String query) throws CacheReadException {
        final List<String> threadIds = new ArrayList<>();
        synchronized (this.queryResults) {
            final InMemoryQueryResult queryResult = this.queryResults.get(query);
            if (queryResult == null) {
                throw new CacheReadException("Unable to find cached version");
            }
            for (QueryResultItem item : queryResult.items) {
                threadIds.add(item.getThreadId());
            }
        }
        synchronized (this.threads) {
            threadIds.removeIf(this.threads::containsKey);
            return new Missing(this.threadState, this.emailState, threadIds);
        }
    }

    private static <T extends AbstractIdentifiableEntity> void copyProperty(T target, T source, String property, Class<T> clazz) throws NoSuchFieldException, IllegalAccessException {
        Field field = clazz.getDeclaredField(property);
        field.setAccessible(true);
        field.set(target, field.get(source));
    }

    protected static class InMemoryQueryResult {

        private String queryState;
        private final boolean canCalculateChanges;
        private final ArrayList<QueryResultItem> items;

        InMemoryQueryResult(String queryState, boolean canCalculateChanges, QueryResultItem[] items) {
            this.queryState = queryState;
            this.canCalculateChanges = canCalculateChanges;
            this.items = new ArrayList<>(Arrays.asList(items));
        }

        private void remove(String emailId) {
            Iterator<QueryResultItem> iterator = items.iterator();
            while (iterator.hasNext()) {
                if (emailId.equals(iterator.next().getEmailId())) {
                    iterator.remove();
                    break;
                }
            }
        }

        public List<QueryResultItem> getItems() {
            return ImmutableList.copyOf(items);
        }
    }
}
