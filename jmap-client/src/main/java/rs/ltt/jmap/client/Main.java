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

package rs.ltt.jmap.client;

import okhttp3.HttpUrl;
import rs.ltt.jmap.client.JmapRequest.Call;
import rs.ltt.jmap.client.session.FileSessionCache;
import rs.ltt.jmap.client.session.Session;
import rs.ltt.jmap.common.Request;
import rs.ltt.jmap.common.entity.Email;
import rs.ltt.jmap.common.entity.capability.MailAccountCapability;
import rs.ltt.jmap.common.entity.filter.EmailFilterCondition;
import rs.ltt.jmap.common.entity.filter.Filter;
import rs.ltt.jmap.common.entity.filter.FilterOperator;
import rs.ltt.jmap.common.method.call.core.EchoMethodCall;
import rs.ltt.jmap.common.method.call.email.GetEmailMethodCall;
import rs.ltt.jmap.common.method.call.email.QueryChangesEmailMethodCall;
import rs.ltt.jmap.common.method.call.email.QueryEmailMethodCall;
import rs.ltt.jmap.common.method.response.email.GetEmailMethodResponse;
import rs.ltt.jmap.common.method.response.email.QueryChangesEmailMethodResponse;
import rs.ltt.jmap.common.method.response.email.QueryEmailMethodResponse;

import java.util.concurrent.Future;


public class Main {

    public static void main(String... args) {

        final String username;
        final String password;
        final HttpUrl sessionResource;

        if (args.length ==2 ) {
            sessionResource = null;
            username = args[0];
            password = args[1];
        } else if (args.length == 3) {
            sessionResource = HttpUrl.get(args[0]);
            username = args[1];
            password = args[2];
        } else {
            System.err.println("java -jar jmap-client.jar [url] username password");
            System.exit(1);
            return;
        }

        try (final JmapClient client = new JmapClient(username, password, sessionResource)) {
            client.setSessionCache(new FileSessionCache());

            client.monitorEvents(stateChange -> {
                System.out.println(stateChange.toString());
                return true;
            });

            final Future<MethodResponses> methodResponsesFuture = client.call(EchoMethodCall.builder().build());

            //System.out.println("Echo call result: " + methodResponsesFuture.get().getMain(EchoMethodResponse.class).toString());

            Session session = client.getSession().get();
            String accountId = session.getPrimaryAccount(MailAccountCapability.class);

            final JmapClient.MultiCall multiCall = client.newMultiCall();

            Filter<Email> emailFilter = FilterOperator.and(
                    FilterOperator.not(EmailFilterCondition.builder().text("match2").build()),
                    EmailFilterCondition.builder().text("test").build()
            );

            Call emailQueryCall = multiCall.call(
                    QueryEmailMethodCall.builder()
                            .accountId(accountId)
                            .filter(emailFilter)
                            .build()
            );
            Future<MethodResponses> emailQueryResponseFuture = emailQueryCall.getMethodResponses();

            Future<MethodResponses> emailGetResponseFuture = multiCall.call(
                    GetEmailMethodCall.builder()
                            .accountId(accountId)
                            .idsReference(emailQueryCall.createResultReference(Request.Invocation.ResultReference.Path.IDS))
                            .build()
            ).getMethodResponses();

            multiCall.execute();
            final QueryEmailMethodResponse emailQueryResponse = emailQueryResponseFuture.get().getMain(QueryEmailMethodResponse.class);
            final GetEmailMethodResponse getEmailMethodResponse = emailGetResponseFuture.get().getMain(GetEmailMethodResponse.class);
            for (Email email : getEmailMethodResponse.getList()) {
                System.out.println(email.getSentAt() + " " + email.getFrom() + " " + email.getSubject());
            }
            /*String currentState = emailQueryResponse.getQueryState();
            while (true) {
                Thread.sleep(5000);
                final JmapClient.MultiCall updateMultiCall = client.newMultiCall();

                Call emailQueryChangesCall = updateMultiCall.call(
                        QueryChangesEmailMethodCall.builder()
                                .accountId(accountId)
                                .sinceQueryState(currentState)
                                .filter(emailFilter)
                                .build()
                );
                Future<MethodResponses> emailQueryChangesResponseFuture = emailQueryChangesCall.getMethodResponses();

                Future<MethodResponses> emailGetAddedResponseFuture = updateMultiCall.call(
                        GetEmailMethodCall.builder()
                                .accountId(accountId)
                                .idsReference(emailQueryChangesCall.createResultReference(Request.Invocation.ResultReference.Path.ADDED_IDS))
                                .build()
                ).getMethodResponses();

                updateMultiCall.execute();
                QueryChangesEmailMethodResponse emailQueryChangesResponse = emailQueryChangesResponseFuture.get().getMain(QueryChangesEmailMethodResponse.class);
                GetEmailMethodResponse emailAddedGetResponse = emailGetAddedResponseFuture.get().getMain(GetEmailMethodResponse.class);
                for (Email email : emailAddedGetResponse.getList()) {
                    System.out.println(email.getSentAt() + " " + email.getFrom() + " " + email.getSubject());
                }
                //System.out.println(emailAddedGetResponse);
                currentState = emailQueryChangesResponse.getNewQueryState();
            }*/
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
