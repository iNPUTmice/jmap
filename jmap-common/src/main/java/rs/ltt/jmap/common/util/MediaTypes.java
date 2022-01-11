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

package rs.ltt.jmap.common.util;

import com.google.common.net.MediaType;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;

public final class MediaTypes {

    public static final MediaType TEXT_PLAIN = MediaType.create("text", "plain");
    public static final MediaType TEXT_HTML = MediaType.create("text", "html");
    public static final MediaType MULTIPART_ANY = MediaType.create("multipart", "*");
    public static final MediaType MULTIPART_REPORT = MediaType.create("multipart", "report");

    private MediaTypes() {}

    public static MediaType of(final String type, final String charsetName) {
        final MediaType mediaType = type == null ? null : MediaType.parse(type);
        final Charset charset = parseCharset(charsetName);
        if (mediaType != null && charset != null) {
            return mediaType.withCharset(charset);
        }
        return mediaType;
    }

    private static Charset parseCharset(final String charset) {
        try {
            return charset == null ? null : Charset.forName(charset);
        } catch (final UnsupportedCharsetException e) {
            return null;
        }
    }
}
