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

public class ObjectsState {

    public final String mailboxState;
    public final String threadState;
    public final String emailState;

    public ObjectsState(String mailboxState, String threadState, String emailState) {
        this.mailboxState = mailboxState;
        this.threadState = threadState;
        this.emailState = emailState;
    }

    public static ObjectsState.Builder builder() {
        return new ObjectsState.Builder();
    }

    public static class Builder {

        private String mailboxState;
        private String threadState;
        private String emailState;

        public Builder setMailboxState(String mailboxState) {
            this.mailboxState = mailboxState;
            return this;
        }

        public Builder setThreadState(String threadState) {
            this.threadState = threadState;
            return this;
        }

        public Builder setEmailState(String emailState) {
            this.emailState = emailState;
            return this;
        }

        public ObjectsState build() {
            return new ObjectsState(mailboxState, threadState, emailState);
        }
    }
}
