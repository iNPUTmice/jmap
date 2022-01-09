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

package rs.ltt.jmap.mua.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import rs.ltt.jmap.common.entity.Email;
import rs.ltt.jmap.common.entity.Keyword;
import rs.ltt.jmap.common.entity.query.Query;

public class StandardQueriesTest {

    @Test
    public void mailbox() {
        final Query<Email> query = StandardQueries.mailbox("inbox");
        Assertions.assertEquals(
                "d0833ce845bbc8d7f1df4a7b03ee90a46bb08875eaf2613894d67c42b20e9a59", query.asHash());
    }

    @Test
    public void keyword() {
        final Query<Email> query =
                StandardQueries.keyword(Keyword.FLAGGED, new String[] {"junk", "trash"});
        Assertions.assertEquals(
                "a13cb6f4e6022b9fedbd6a03bfa1c6ab5c7c9dfc644fce7b4e76ec6462cf2a91", query.asHash());
    }
}
