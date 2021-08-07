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

import rs.ltt.jmap.common.method.MethodResponse;


//TODO do we want to type this for the calling method? This could be useful to delegate this into different methods based on the type
public class MethodResponses {

    private final MethodResponse main;
    private final MethodResponse[] additional;

    public MethodResponses(MethodResponse main) {
        this.main = main;
        this.additional = new MethodResponse[0];
    }

    public MethodResponses(MethodResponse main, MethodResponse[] additional) {
        this.main = main;
        this.additional = additional;
    }

    public MethodResponse getMain() {
        return main;
    }

    public <T extends MethodResponse> T getMain(Class<T> clazz) {
        //TODO check before cast and throw unexpected method error
        return clazz.cast(main);
    }

    public MethodResponse[] getAdditional() {
        return additional;
    }
}
