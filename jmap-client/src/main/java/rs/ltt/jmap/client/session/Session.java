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

package rs.ltt.jmap.client.session;

import com.damnhandy.uri.template.UriTemplate;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nonnull;
import okhttp3.HttpUrl;
import rs.ltt.jmap.client.event.CloseAfter;
import rs.ltt.jmap.common.SessionResource;
import rs.ltt.jmap.common.entity.*;

public class Session {

    private final HttpUrl base;

    private final SessionResource sessionResource;

    public Session(@Nonnull HttpUrl base, @Nonnull SessionResource sessionResource) {
        Preconditions.checkNotNull(base, "Base URL for session must not be null");
        Preconditions.checkNotNull(sessionResource, "Session Resource must not be null");
        this.base = base;
        this.sessionResource = sessionResource;
    }

    public HttpUrl getApiUrl() {
        final String apiUrl = sessionResource.getApiUrl();
        final HttpUrl.Builder builder = base.newBuilder(apiUrl);
        Preconditions.checkState(
                builder != null,
                String.format(
                        "Unable to assemble final API Url from base=%s and apiUrl=%s",
                        base, apiUrl));
        return builder.build();
    }

    public HttpUrl getBase() {
        return base;
    }

    public HttpUrl getDownloadUrl(final String accountId, final Downloadable downloadable) {
        return getDownloadUrl(
                accountId,
                downloadable.getBlobId(),
                downloadable.getName(),
                downloadable.getType());
    }

    public HttpUrl getDownloadUrl(String accountId, String blobId, String name, String type) {
        final String downloadUrl = sessionResource.getDownloadUrl();
        Preconditions.checkState(
                downloadUrl != null, "Session Resource did not contain a download Url");
        final UriTemplate uriTemplate =
                UriTemplate.fromTemplate(downloadUrl)
                        .set("accountId", accountId)
                        .set("name", name)
                        .set("blobId", blobId)
                        .set("type", type);
        final HttpUrl.Builder builder = base.newBuilder(uriTemplate.expand());
        Preconditions.checkState(
                builder != null,
                String.format(
                        "Unable to assemble final download Url from base=%s and downloadUrl=%s",
                        base, downloadUrl));
        return builder.build();
    }

    public HttpUrl getEventSourceUrl(
            Collection<Class<? extends AbstractIdentifiableEntity>> types,
            CloseAfter closeAfter,
            Long ping) {
        final String eventSourceUrl = sessionResource.getEventSourceUrl();
        Preconditions.checkState(
                eventSourceUrl != null, "Session Resource did not contain an event source Url");
        final UriTemplate uriTemplate =
                UriTemplate.fromTemplate(eventSourceUrl)
                        .set("closeafter", closeAfter.toString().toLowerCase(Locale.US))
                        .set("ping", ping);
        if (types.size() == 0) {
            uriTemplate.set("types", "*");
        } else {
            uriTemplate.set(
                    "types", types.stream().map(Class::getSimpleName).toArray(String[]::new));
        }
        final HttpUrl.Builder builder = base.newBuilder(uriTemplate.expand());
        Preconditions.checkState(
                builder != null,
                String.format(
                        "Unable to assemble final eventSource Url from base=%s and"
                                + " eventSourceUrl=%s",
                        base, eventSourceUrl));
        return builder.build();
    }

    public HttpUrl getUploadUrl(String accountId) {
        final String uploadUrl = sessionResource.getUploadUrl();
        Preconditions.checkState(
                uploadUrl != null, "Session Resource did not contain an upload Url");
        final UriTemplate uriTemplate =
                UriTemplate.fromTemplate(uploadUrl).set("accountId", accountId);
        final HttpUrl.Builder builder = base.newBuilder(uriTemplate.expand());
        Preconditions.checkState(
                builder != null,
                String.format(
                        "Unable to assemble final upload Url from base=%s and uploadUrl=%s",
                        base, uploadUrl));
        return builder.build();
    }

    public String getState() {
        return sessionResource.getState();
    }

    public String getPrimaryAccount(Class<? extends AccountCapability> clazz) {
        return sessionResource.getPrimaryAccount(clazz);
    }

    public Map<String, Account> getAccounts(final Class<? extends AccountCapability> clazz) {
        return Maps.filterEntries(
                sessionResource.getAccounts(),
                entry -> entry != null && entry.getValue().hasCapability(clazz));
    }

    public <T extends Capability> T getCapability(Class<T> clazz) {
        return sessionResource.getCapability(clazz);
    }

    public <T extends AccountCapability> T getAccountCapability(
            final String accountId, final Class<T> clazz) {
        final Account account = sessionResource.getAccounts().get(accountId);
        return account == null ? null : account.getCapability(clazz);
    }
}
