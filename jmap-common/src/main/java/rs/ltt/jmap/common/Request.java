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

package rs.ltt.jmap.common;

import rs.ltt.jmap.common.method.MethodCall;
import rs.ltt.jmap.common.util.Namespace;

import java.util.*;

public class Request {

    private static final Map<Class<? extends MethodCall>, String> PACKAGE_NAMESPACE_CACHE = new HashMap<>();

    private String[] using;
    private Invocation[] methodCalls;

    private Request(String[] using, Invocation[] methodCalls) {
        this.using = using;
        this.methodCalls = methodCalls;
    }

    private static String getPackageNamespaceFor(Class<? extends MethodCall> clazz) {
        synchronized (PACKAGE_NAMESPACE_CACHE) {
            if (!PACKAGE_NAMESPACE_CACHE.containsKey(clazz)) {
                final String value = Namespace.get(clazz);
                PACKAGE_NAMESPACE_CACHE.put(clazz, Namespace.get(clazz));
                return value;
            }
            return PACKAGE_NAMESPACE_CACHE.get(clazz);
        }
    }

    public static class Invocation {

        private MethodCall methodCall;
        private String id;
        private Invocation() {

        }

        private Invocation(MethodCall methodCall, String id) {
            this.methodCall = methodCall;
            this.id = id;
        }

        public static Invocation create(MethodCall methodCall, String id) {
            return new Invocation(methodCall, id);
        }

        public MethodCall getMethodCall() {
            return methodCall;
        }

        public ResultReference createReference(String path) {
            return new ResultReference(id, methodCall.getClass(), path);
        }

        public String getId() {
            return id;
        }

        //TODO maybe try typeadapter?

        public static class ResultReference {

            private final String id;
            private final Class<? extends MethodCall> clazz;
            private final String path;

            private ResultReference(String id, Class<?extends MethodCall> clazz, String path) {
                this.id = id;
                this.clazz = clazz;
                this.path = path;
            }

            public String getId() {
                return id;
            }

            public String getPath() {
                return path;
            }

            public Class<? extends MethodCall> getClazz() {
                return clazz;
            }

            public static final class Path {
                public static final String IDS = "/ids";
                public static final String ADDED_IDS = "/added/*/id";
                public static final String LIST_IDS = "/list/*/id";
                public static final String LIST_THREAD_IDS = "/list/*/threadId";
                public static final String LIST_EMAIL_IDS = "/list/*/emailIds";
                public static final String UPDATED = "/updated";
                public static final String CREATED = "/created";
                public static final String UPDATED_PROPERTIES = "/updatedProperties";
            }
        }
    }

    public static class Builder {

        private List<Invocation> invocations = new ArrayList<>();
        private Set<String> using = new HashSet<>();

        public Builder() {

        }

        public Builder call(MethodCall call) {
            final int id = invocations.size();
            final Invocation invocation = new Invocation(call, String.valueOf(id));
            return add(invocation);
        }

        public Builder add(Invocation invocation) {
            this.invocations.add(invocation);
            final String namespace = getPackageNamespaceFor(invocation.methodCall.getClass());
            if (namespace != null) {
                this.using.add(namespace);
            }
            return this;
        }

        public Request build() {
            return new Request(using.toArray(new String[0]), invocations.toArray(new Invocation[0]));
        }
    }
}
