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

import com.google.common.collect.ListMultimap;
import rs.ltt.jmap.common.Request;
import rs.ltt.jmap.common.Response;
import rs.ltt.jmap.common.entity.AddedItem;
import rs.ltt.jmap.common.entity.Email;
import rs.ltt.jmap.common.entity.Thread;
import rs.ltt.jmap.common.method.MethodResponse;
import rs.ltt.jmap.common.method.response.email.GetEmailMethodResponse;
import rs.ltt.jmap.common.method.response.standard.ChangesMethodResponse;
import rs.ltt.jmap.common.method.response.standard.QueryChangesMethodResponse;
import rs.ltt.jmap.common.method.response.standard.QueryMethodResponse;
import rs.ltt.jmap.common.method.response.thread.GetThreadMethodResponse;
import rs.ltt.jmap.common.util.Mapper;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ResultReferenceResolver {

    public static String[] resolve(
            final Request.Invocation.ResultReference resultReference,
            final ListMultimap<String, Response.Invocation> previousResponses
    ) {
        final MethodResponse methodResponse = find(resultReference, previousResponses);
        final String path = resultReference.getPath();
        switch (resultReference.getPath()) {
            case Request.Invocation.ResultReference.Path.IDS:
                if (methodResponse instanceof QueryMethodResponse) {
                    return ((QueryMethodResponse<?>) methodResponse).getIds();
                }
                break;
            case Request.Invocation.ResultReference.Path.LIST_THREAD_IDS:
                if (methodResponse instanceof GetEmailMethodResponse) {
                    return Arrays.stream(((GetEmailMethodResponse) methodResponse).getList()).map(Email::getThreadId).toArray(String[]::new);
                }
                break;
            case Request.Invocation.ResultReference.Path.LIST_EMAIL_IDS:
                if (methodResponse instanceof GetThreadMethodResponse) {
                    return Arrays.stream(((GetThreadMethodResponse) methodResponse).getList()).map(Thread::getEmailIds).flatMap(Collection::stream).toArray(String[]::new);
                }
                break;
            case Request.Invocation.ResultReference.Path.CREATED:
                if (methodResponse instanceof ChangesMethodResponse) {
                    return nullToEmpty(((ChangesMethodResponse<?>) methodResponse).getCreated());
                }
                break;
            case Request.Invocation.ResultReference.Path.UPDATED:
                if (methodResponse instanceof ChangesMethodResponse) {
                    return nullToEmpty(((ChangesMethodResponse<?>) methodResponse).getUpdated());
                }
                break;
            case Request.Invocation.ResultReference.Path.ADDED_IDS:
                if (methodResponse instanceof QueryChangesMethodResponse) {
                    return ((QueryChangesMethodResponse<?>) methodResponse).getAdded().stream().map(AddedItem::getItem).toArray(String[]::new);
                }
                break;
            default:
        }
        throw new IllegalArgumentException(String.format("Unable to resolve path %s for class %s", path, methodResponse.getClass().getName()));
    }

    private static MethodResponse find(
            final Request.Invocation.ResultReference resultReference,
            final ListMultimap<String, Response.Invocation> previousResponses
    ) {
        final String id = resultReference.getId();
        final List<Response.Invocation> invocations = previousResponses.get(id);
        if (invocations == null) {
            throw new IllegalArgumentException("Unable to find any method response with id "+id);
        }
        final String methodCallName = Mapper.METHOD_CALLS.inverse().get(resultReference.getClazz());
        for(final Response.Invocation invocation : invocations) {
            final String responseCallName = Mapper.METHOD_RESPONSES.inverse().get(invocation.getMethodResponse().getClass());
            if (methodCallName.equals(responseCallName)) {
                return invocation.getMethodResponse();
            }
        }
        throw new IllegalArgumentException("Unable to find matching response for "+methodCallName);
    }

    private static String[] nullToEmpty(final String[] value) {
        return value == null ? new String[0] : value;
    }

}
