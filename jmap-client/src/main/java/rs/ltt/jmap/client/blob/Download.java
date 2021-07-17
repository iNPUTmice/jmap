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

import okhttp3.Call;

import java.io.InputStream;

public final class Download {
    private final Call call;
    private final boolean resumed;
    private final long contentLength;
    private final InputStream inputStream;

    public Download(Call call, boolean resumed, long contentLength, InputStream inputStream) {
        this.call = call;
        this.resumed = resumed;
        this.contentLength = contentLength;
        this.inputStream = inputStream;
    }

    public int progress(final long done) {
        if (indeterminate()) {
            return 0;
        }
        return (int) Math.round((double) Math.min(contentLength, done) / contentLength * 100);
    }

    public boolean indeterminate() {
        return contentLength == 0;
    }

    public Call getCall() {
        return call;
    }

    public boolean isResumed() {
        return resumed;
    }

    public long getContentLength() {
        return contentLength;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public void cancel() {
        this.call.cancel();
    }
}
