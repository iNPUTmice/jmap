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

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import rs.ltt.jmap.common.entity.AbstractIdentifiableEntity;
import rs.ltt.jmap.common.entity.Email;
import rs.ltt.jmap.common.entity.Mailbox;
import rs.ltt.jmap.common.entity.Thread;
import rs.ltt.jmap.common.method.response.mailbox.SetMailboxMethodResponse;

public class Update {

    private final Map<Class<? extends AbstractIdentifiableEntity>, Changes> changes;

    private final String newVersion;

    private Update(
            Map<Class<? extends AbstractIdentifiableEntity>, Changes> changes, String newVersion) {
        this.changes = changes;
        this.newVersion = newVersion;
    }

    public static Update of(
            SetMailboxMethodResponse setMailboxMethodResponse, final String newVersion) {
        return new Update(
                ImmutableMap.of(
                        Mailbox.class,
                        new Changes(
                                nullToEmpty(setMailboxMethodResponse.getUpdated())
                                        .keySet()
                                        .toArray(new String[0]),
                                nullToEmpty(setMailboxMethodResponse.getCreated()).values().stream()
                                        .map(Mailbox::getId)
                                        .toArray(String[]::new))),
                newVersion);
    }

    public static Update merge(final Collection<Update> updates) {
        final ImmutableMultimap.Builder<Class<? extends AbstractIdentifiableEntity>, Changes>
                changesBuilder = ImmutableMultimap.builder();
        String newVersion = null;
        for (final Update update : updates) {
            for (Map.Entry<Class<? extends AbstractIdentifiableEntity>, Changes> entityChanges :
                    update.changes.entrySet()) {
                changesBuilder.put(entityChanges.getKey(), entityChanges.getValue());
            }
            newVersion = update.newVersion;
        }
        ImmutableMap<Class<? extends AbstractIdentifiableEntity>, Collection<Changes>> multiMap =
                changesBuilder.build().asMap();
        Map<Class<? extends AbstractIdentifiableEntity>, Changes> changes =
                Maps.transformValues(multiMap, Changes::merge);

        return new Update(changes, newVersion);
    }

    private static <T extends AbstractIdentifiableEntity> Map<String, T> nullToEmpty(
            Map<String, T> value) {
        return value == null ? Collections.emptyMap() : value;
    }

    public static Update created(Email email, String newVersion) {
        final ImmutableMap.Builder<Class<? extends AbstractIdentifiableEntity>, Changes> builder =
                new ImmutableMap.Builder<>();
        builder.put(Email.class, new Changes(new String[0], new String[] {email.getId()}));
        builder.put(Thread.class, new Changes(new String[0], new String[] {email.getThreadId()}));
        builder.put(
                Mailbox.class,
                new Changes(email.getMailboxIds().keySet().toArray(new String[0]), new String[0]));
        return new Update(builder.build(), newVersion);
    }

    public static Update updated(
            final Collection<Email> emails, final Collection<String> mailboxes, String newVersion) {
        final ImmutableMap.Builder<Class<? extends AbstractIdentifiableEntity>, Changes> builder =
                new ImmutableMap.Builder<>();
        builder.put(
                Email.class,
                new Changes(
                        emails.stream().map(Email::getId).toArray(String[]::new), new String[0]));
        builder.put(Thread.class, new Changes(new String[0], new String[0]));
        builder.put(Mailbox.class, new Changes(mailboxes.toArray(new String[0]), new String[0]));
        return new Update(builder.build(), newVersion);
    }

    public Changes getChangesFor(final Class<? extends AbstractIdentifiableEntity> clazz) {
        return changes.get(clazz);
    }

    public String getNewVersion() {
        return newVersion;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("changes", changes)
                .add("newVersion", newVersion)
                .toString();
    }
}
