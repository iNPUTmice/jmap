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
import rs.ltt.jmap.common.Response;
import rs.ltt.jmap.common.method.MethodCall;
import rs.ltt.jmap.common.method.MethodResponse;
import rs.ltt.jmap.common.method.call.core.EchoMethodCall;
import rs.ltt.jmap.common.method.call.email.*;
import rs.ltt.jmap.common.method.call.identity.ChangesIdentityMethodCall;
import rs.ltt.jmap.common.method.call.identity.GetIdentityMethodCall;
import rs.ltt.jmap.common.method.call.identity.SetIdentityMethodCall;
import rs.ltt.jmap.common.method.call.mailbox.*;
import rs.ltt.jmap.common.method.call.snippet.GetSearchSnippetsMethodCall;
import rs.ltt.jmap.common.method.call.submission.*;
import rs.ltt.jmap.common.method.call.thread.ChangesThreadMethodCall;
import rs.ltt.jmap.common.method.call.thread.GetThreadMethodCall;
import rs.ltt.jmap.common.method.call.vacation.GetVacationResponseMethodCall;
import rs.ltt.jmap.common.method.call.vacation.SetVacationResponseMethodCall;
import rs.ltt.jmap.common.method.error.UnknownMethodMethodErrorResponse;
import rs.ltt.jmap.common.method.response.core.EchoMethodResponse;

public class StubMailServer extends JmapDispatcher {

    public StubMailServer() {
        super(0);
    }

    public StubMailServer(int accountIndex) {
        super(accountIndex);
    }

    @Override
    protected MethodResponse[] dispatch(final MethodCall methodCall, final ListMultimap<String, Response.Invocation> previousResponses) {
        /**
         * jmap-core
         */
        if (methodCall instanceof EchoMethodCall) {
            return execute((EchoMethodCall) methodCall, previousResponses);
        }

        /**
         * jmap-mail / Email
         */
        if (methodCall instanceof ChangesEmailMethodCall) {
            return execute((ChangesEmailMethodCall) methodCall, previousResponses);
        }
        if (methodCall instanceof CopyEmailMethodCall) {
            return execute((CopyEmailMethodCall) methodCall, previousResponses);
        }
        if (methodCall instanceof GetEmailMethodCall) {
            return execute((GetEmailMethodCall) methodCall, previousResponses);
        }
        if (methodCall instanceof ImportEmailMethodCall) {
            return execute((ImportEmailMethodCall) methodCall, previousResponses);
        }
        if (methodCall instanceof ParseEmailMethodCall) {
            return execute((ParseEmailMethodCall) methodCall, previousResponses);
        }
        if (methodCall instanceof QueryChangesEmailMethodCall) {
            return execute((QueryChangesEmailMethodCall) methodCall, previousResponses);
        }
        if (methodCall instanceof QueryEmailMethodCall) {
            return execute((QueryEmailMethodCall) methodCall, previousResponses);
        }
        if (methodCall instanceof SetEmailMethodCall) {
            return execute((SetEmailMethodCall) methodCall, previousResponses);
        }

        /**
         * jmap-mail / Identity
         */
        if (methodCall instanceof ChangesIdentityMethodCall) {
            return execute((ChangesIdentityMethodCall) methodCall, previousResponses);
        }
        if (methodCall instanceof GetIdentityMethodCall) {
            return execute((GetIdentityMethodCall) methodCall, previousResponses);
        }
        if (methodCall instanceof SetIdentityMethodCall) {
            return execute((SetIdentityMethodCall) methodCall, previousResponses);
        }

        /**
         * jmap-mail / Mailbox
         */
        if (methodCall instanceof ChangesMailboxMethodCall) {
            return execute((ChangesMailboxMethodCall) methodCall, previousResponses);
        }
        if (methodCall instanceof GetMailboxMethodCall) {
            return execute((GetMailboxMethodCall) methodCall, previousResponses);
        }
        if (methodCall instanceof QueryChangesMailboxMethodCall) {
            return execute((QueryChangesMailboxMethodCall) methodCall, previousResponses);
        }
        if (methodCall instanceof QueryMailboxMethodCall) {
            return execute((QueryMailboxMethodCall) methodCall, previousResponses);
        }
        if (methodCall instanceof SetMailboxMethodCall) {
            return execute((SetMailboxMethodCall) methodCall, previousResponses);
        }

        /**
         * jmap-mail / Snippet
         */
        if (methodCall instanceof GetSearchSnippetsMethodCall) {
            return execute((GetSearchSnippetsMethodCall) methodCall, previousResponses);
        }

        /**
         * jmap-mail / Submission
         */
        if (methodCall instanceof ChangesEmailSubmissionMethodCall) {
            return execute((ChangesEmailSubmissionMethodCall) methodCall, previousResponses);
        }
        if (methodCall instanceof GetEmailSubmissionMethodCall) {
            return execute((GetEmailSubmissionMethodCall) methodCall, previousResponses);
        }
        if (methodCall instanceof QueryChangesEmailSubmissionMethodCall) {
            return execute((QueryChangesEmailSubmissionMethodCall) methodCall, previousResponses);
        }
        if (methodCall instanceof QueryEmailSubmissionMethodCall) {
            return execute((QueryEmailSubmissionMethodCall) methodCall, previousResponses);
        }
        if (methodCall instanceof SetEmailSubmissionMethodCall) {
            return execute((SetEmailSubmissionMethodCall) methodCall, previousResponses);
        }

        /**
         * jmap-mail / Thread
         */
        if (methodCall instanceof ChangesThreadMethodCall) {
            return execute((ChangesThreadMethodCall) methodCall, previousResponses);
        }
        if (methodCall instanceof GetThreadMethodCall) {
            return execute((GetThreadMethodCall) methodCall, previousResponses);
        }

        /**
         * jmap-mail / Vacation
         */
        if (methodCall instanceof GetVacationResponseMethodCall) {
            return execute((GetVacationResponseMethodCall) methodCall, previousResponses);
        }
        if (methodCall instanceof SetVacationResponseMethodCall) {
            return execute((SetVacationResponseMethodCall) methodCall, previousResponses);
        }

        return new MethodResponse[]{new UnknownMethodMethodErrorResponse()};
    }

    protected MethodResponse[] execute(EchoMethodCall methodCall, ListMultimap<String, Response.Invocation> previousResponses) {
        return new MethodResponse[]{EchoMethodResponse.builder().libraryName(methodCall.getLibraryName()).build()};
    }

    protected MethodResponse[] execute(ChangesEmailMethodCall methodCall, ListMultimap<String, Response.Invocation> previousResponses) {
        return new MethodResponse[]{new UnknownMethodMethodErrorResponse()};
    }

    protected MethodResponse[] execute(CopyEmailMethodCall methodCall, ListMultimap<String, Response.Invocation> previousResponses) {
        return new MethodResponse[]{new UnknownMethodMethodErrorResponse()};
    }

    protected MethodResponse[] execute(GetEmailMethodCall methodCall, ListMultimap<String, Response.Invocation> previousResponses) {
        return new MethodResponse[]{new UnknownMethodMethodErrorResponse()};
    }

    protected MethodResponse[] execute(ImportEmailMethodCall methodCall, ListMultimap<String, Response.Invocation> previousResponses) {
        return new MethodResponse[]{new UnknownMethodMethodErrorResponse()};
    }

    protected MethodResponse[] execute(ParseEmailMethodCall methodCall, ListMultimap<String, Response.Invocation> previousResponses) {
        return new MethodResponse[]{new UnknownMethodMethodErrorResponse()};
    }

    protected MethodResponse[] execute(QueryChangesEmailMethodCall methodCall, ListMultimap<String, Response.Invocation> previousResponses) {
        return new MethodResponse[]{new UnknownMethodMethodErrorResponse()};
    }

    protected MethodResponse[] execute(QueryEmailMethodCall methodCall, ListMultimap<String, Response.Invocation> previousResponses) {
        return new MethodResponse[]{new UnknownMethodMethodErrorResponse()};
    }

    protected MethodResponse[] execute(SetEmailMethodCall methodCall, ListMultimap<String, Response.Invocation> previousResponses) {
        return new MethodResponse[]{new UnknownMethodMethodErrorResponse()};
    }

    protected MethodResponse[] execute(ChangesIdentityMethodCall methodCall, ListMultimap<String, Response.Invocation> previousResponses) {
        return new MethodResponse[]{new UnknownMethodMethodErrorResponse()};
    }

    protected MethodResponse[] execute(GetIdentityMethodCall methodCall, ListMultimap<String, Response.Invocation> previousResponses) {
        return new MethodResponse[]{new UnknownMethodMethodErrorResponse()};
    }

    protected MethodResponse[] execute(SetIdentityMethodCall methodCall, ListMultimap<String, Response.Invocation> previousResponses) {
        return new MethodResponse[]{new UnknownMethodMethodErrorResponse()};
    }

    protected MethodResponse[] execute(ChangesMailboxMethodCall methodCall, ListMultimap<String, Response.Invocation> previousResponses) {
        return new MethodResponse[]{new UnknownMethodMethodErrorResponse()};
    }

    protected MethodResponse[] execute(GetMailboxMethodCall methodCall, ListMultimap<String, Response.Invocation> previousResponses) {
        return new MethodResponse[]{new UnknownMethodMethodErrorResponse()};
    }

    protected MethodResponse[] execute(QueryChangesMailboxMethodCall methodCall, ListMultimap<String, Response.Invocation> previousResponses) {
        return new MethodResponse[]{new UnknownMethodMethodErrorResponse()};
    }

    protected MethodResponse[] execute(QueryMailboxMethodCall methodCall, ListMultimap<String, Response.Invocation> previousResponses) {
        return new MethodResponse[]{new UnknownMethodMethodErrorResponse()};
    }

    protected MethodResponse[] execute(SetMailboxMethodCall methodCall, ListMultimap<String, Response.Invocation> previousResponses) {
        return new MethodResponse[]{new UnknownMethodMethodErrorResponse()};
    }

    protected MethodResponse[] execute(GetSearchSnippetsMethodCall methodCall, ListMultimap<String, Response.Invocation> previousResponses) {
        return new MethodResponse[]{new UnknownMethodMethodErrorResponse()};
    }

    protected MethodResponse[] execute(ChangesEmailSubmissionMethodCall methodCall, ListMultimap<String, Response.Invocation> previousResponses) {
        return new MethodResponse[]{new UnknownMethodMethodErrorResponse()};
    }

    protected MethodResponse[] execute(GetEmailSubmissionMethodCall methodCall, ListMultimap<String, Response.Invocation> previousResponses) {
        return new MethodResponse[]{new UnknownMethodMethodErrorResponse()};
    }

    protected MethodResponse[] execute(QueryChangesEmailSubmissionMethodCall methodCall, ListMultimap<String, Response.Invocation> previousResponses) {
        return new MethodResponse[]{new UnknownMethodMethodErrorResponse()};
    }

    protected MethodResponse[] execute(QueryEmailSubmissionMethodCall methodCall, ListMultimap<String, Response.Invocation> previousResponses) {
        return new MethodResponse[]{new UnknownMethodMethodErrorResponse()};
    }

    protected MethodResponse[] execute(SetEmailSubmissionMethodCall methodCall, ListMultimap<String, Response.Invocation> previousResponses) {
        return new MethodResponse[]{new UnknownMethodMethodErrorResponse()};
    }

    protected MethodResponse[] execute(ChangesThreadMethodCall methodCall, ListMultimap<String, Response.Invocation> previousResponses) {
        return new MethodResponse[]{new UnknownMethodMethodErrorResponse()};
    }

    protected MethodResponse[] execute(GetThreadMethodCall methodCall, ListMultimap<String, Response.Invocation> previousResponses) {
        return new MethodResponse[]{new UnknownMethodMethodErrorResponse()};
    }

    protected MethodResponse[] execute(GetVacationResponseMethodCall methodCall, ListMultimap<String, Response.Invocation> previousResponses) {
        return new MethodResponse[]{new UnknownMethodMethodErrorResponse()};
    }

    protected MethodResponse[] execute(SetVacationResponseMethodCall methodCall, ListMultimap<String, Response.Invocation> previousResponses) {
        return new MethodResponse[]{new UnknownMethodMethodErrorResponse()};
    }
}
