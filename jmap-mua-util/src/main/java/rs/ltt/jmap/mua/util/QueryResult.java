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

//TODO probably better belongs into util?
package rs.ltt.jmap.mua.util;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import rs.ltt.jmap.common.entity.AddedItem;
import rs.ltt.jmap.common.entity.Email;
import rs.ltt.jmap.common.entity.TypedState;
import rs.ltt.jmap.common.method.response.email.GetEmailMethodResponse;
import rs.ltt.jmap.common.method.response.email.QueryChangesEmailMethodResponse;
import rs.ltt.jmap.common.method.response.email.QueryEmailMethodResponse;

import java.util.List;

public class QueryResult {

    public final QueryResultItem[] items;
    public final long position;
    public final TypedState<Email> queryState;
    public final TypedState<Email> objectState;

    private QueryResult(@NonNullDecl QueryResultItem[] items, long position, TypedState<Email> queryState, TypedState<Email> objectState) {
        this.items = items;
        this.position = position;
        this.queryState = queryState;
        this.objectState = objectState;
    }


    public static QueryResult of(QueryEmailMethodResponse queryEmailMethodResponse, GetEmailMethodResponse emailMethodResponse) {
        final String[] emailIds = queryEmailMethodResponse.getIds();
        final QueryResultItem[] resultItems = new QueryResultItem[emailIds.length];
        final ImmutableMap<String, String> emailIdToThreadIdMap = map(emailMethodResponse);
        for (int i = 0; i < emailIds.length; ++i) {
            final String emailId = emailIds[i];
            resultItems[i] = QueryResultItem.of(emailId, emailIdToThreadIdMap.get(emailId));
        }
        return new QueryResult(resultItems, queryEmailMethodResponse.getPosition(), queryEmailMethodResponse.getTypedQueryState(), emailMethodResponse.getTypedState());
    }

    private static ImmutableMap<String, String> map(GetEmailMethodResponse emailMethodResponse) {
        ImmutableMap.Builder<String, String> builder = new ImmutableMap.Builder<>();
        for (Email email : emailMethodResponse.getList()) {
            builder.put(email.getId(), email.getThreadId());
        }
        return builder.build();
    }

    public static List<AddedItem<QueryResultItem>> of(QueryChangesEmailMethodResponse queryChangesEmailMethodResponse, GetEmailMethodResponse emailMethodResponse) {
        final List<AddedItem<String>> addedEmailIdItems = queryChangesEmailMethodResponse.getAdded();
        ImmutableList.Builder<AddedItem<QueryResultItem>> builder = new ImmutableList.Builder<>();
        final ImmutableMap<String, String> emailIdToThreadIdMap = map(emailMethodResponse);
        for (AddedItem<String> addedItem : addedEmailIdItems) {
            String emailId = addedItem.getItem();
            builder.add(AddedItem.of(QueryResultItem.of(emailId, emailIdToThreadIdMap.get(emailId)), addedItem.getIndex()));
        }
        return builder.build();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("items", items)
                .add("position", position)
                .add("objectState", objectState.getState())
                .toString();
    }
}
