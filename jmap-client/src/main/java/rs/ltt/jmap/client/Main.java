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

import rs.ltt.jmap.client.session.SessionFileCache;
import rs.ltt.jmap.common.Request;
import rs.ltt.jmap.common.entity.Email;
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

        if (args.length != 2) {
            System.err.println("java -jar jmap-client.jar username password");
            System.exit(1);
            return;
        }

        try (final JmapClient client = new JmapClient(args[0], args[1])) {
            client.setSessionCache(new SessionFileCache());

            final Future<MethodResponses> methodResponsesFuture = client.call(new EchoMethodCall());

            //System.out.println("Echo call result: " + methodResponsesFuture.get().getMain(EchoMethodResponse.class).toString());

            final JmapClient.MultiCall multiCall = client.newMultiCall();

            Filter<Email> emailFilter = FilterOperator.and(
                    FilterOperator.not(EmailFilterCondition.builder().text("match2").build()),
                    EmailFilterCondition.builder().text("wie").build(),
                    EmailFilterCondition.builder().text("test").build()
            );

            Request.Invocation emailQuery = Request.Invocation.create(new QueryEmailMethodCall(emailFilter));
            Request.Invocation emailGet = Request.Invocation.create(new GetEmailMethodCall(emailQuery.createReference(Request.Invocation.ResultReference.Path.IDS)));

            Future<MethodResponses> emailQueryResponseFuture = multiCall.add(emailQuery);
            Future<MethodResponses> emailGetResponseFuture = multiCall.add(emailGet);
            multiCall.execute();
            final QueryEmailMethodResponse emailQueryResponse = emailQueryResponseFuture.get().getMain(QueryEmailMethodResponse.class);
            final GetEmailMethodResponse getEmailMethodResponse = emailGetResponseFuture.get().getMain(GetEmailMethodResponse.class);
            for (Email email : getEmailMethodResponse.getList()) {
                System.out.println(email.getSentAt() + " " + email.getFrom() + " " + email.getSubject());
            }
            System.out.println(emailQueryResponse);
            String currentState = emailQueryResponse.getQueryState();
            while (true) {
                Thread.sleep(5000);
                final JmapClient.MultiCall updateMultiCall = client.newMultiCall();
                Request.Invocation emailQueryChanges = Request.Invocation.create(new QueryChangesEmailMethodCall(currentState, emailFilter));
                Request.Invocation addedEmailGet = Request.Invocation.create(new GetEmailMethodCall(emailQueryChanges.createReference(Request.Invocation.ResultReference.Path.ADDED_IDS)));
                Future<MethodResponses> emailQueryChangeResponseFuture = updateMultiCall.add(emailQueryChanges);
                Future<MethodResponses> emailGetAddedResponseFuture = updateMultiCall.add(addedEmailGet);
                updateMultiCall.execute();
                QueryChangesEmailMethodResponse emailQueryChangesResponse = emailQueryChangeResponseFuture.get().getMain(QueryChangesEmailMethodResponse.class);
                GetEmailMethodResponse emailAddedGetResponse = emailGetAddedResponseFuture.get().getMain(GetEmailMethodResponse.class);
                for (Email email : emailAddedGetResponse.getList()) {
                    System.out.println(email.getSentAt() + " " + email.getFrom() + " " + email.getSubject());
                }
                //System.out.println(emailAddedGetResponse);
                currentState = emailQueryChangesResponse.getNewQueryState();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
