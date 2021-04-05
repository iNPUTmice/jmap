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
                "57839588c47c04e2a010e1cde932f3bebf31c07609235c19a8f95c17445f0818",
                query.asHash()
        );
    }

    @Test
    public void keyword() {
        final Query<Email> query = StandardQueries.keyword(Keyword.FLAGGED, new String[]{"junk", "trash"});
        Assertions.assertEquals(
                "c0f83960f59ea96e99bd2cc2ad54db66ece24522e8ce95bb11f37b769468778e",
                query.asHash()
        );
    }

}
