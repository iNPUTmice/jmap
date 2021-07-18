/*
 * Copyright 2021 Daniel Gultsch
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

package rs.ltt.jmap.client.blob;

public class BlobTransferException extends Exception {

    private final int code;
    private final ProblemDetails problemDetails;

    public BlobTransferException(final int code, final ProblemDetails problemDetails) {
        super(message(code, problemDetails));
        this.code = code;
        this.problemDetails = problemDetails;
    }

    private static String message(final int code, final ProblemDetails problemDetails) {
        if (problemDetails == null) {
            return String.format("HTTP Status code %d", code);
        } else {
            return String.format("HTTP Status code %d. %s", code, problemDetails.getTitle());
        }
    }

    public int getCode() {
        return code;
    }

    public ProblemDetails getProblemDetails() {
        return problemDetails;
    }
}
