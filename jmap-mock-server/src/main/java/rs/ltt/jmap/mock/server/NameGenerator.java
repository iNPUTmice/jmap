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

import rs.ltt.jmap.common.entity.EmailAddress;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class NameGenerator {
    /**
     * Frequently Occurring Surnames from Census 1990
     * https://www.census.gov/topics/population/genealogy/data/1990_census/1990_census_namefiles.html
     */

    private static final List<String> LAST_NAMES = Arrays.asList(
            "Smith",
            "Johnson",
            "Williams",
            "Jones",
            "Brown",
            "Davis",
            "Miller",
            "Wilson",
            "Moore",
            "Taylor",
            "Anderson",
            "Thomas",
            "Jackson",
            "White",
            "Harris",
            "Martin",
            "Thompson",
            "Garcia",
            "Martinez",
            "Robinson",
            "Clark",
            "Rodriguez",
            "Lewis",
            "Lee",
            "Walker",
            "Hall",
            "Allen",
            "Young",
            "Hernandez",
            "King",
            "Wright"
    );

    private static final List<String> FEMALE_FIRST_NAMES = Arrays.asList(
            "Mary",
            "Patricia",
            "Linda",
            "Barbara",
            "Elizabeth",
            "Jennifer",
            "Maria",
            "Susan",
            "Margaret",
            "Dorothy",
            "Lisa",
            "Nancy",
            "Karen",
            "Betty",
            "Helen",
            "Sandra",
            "Donna",
            "Carol",
            "Ruth",
            "Sharon",
            "Michelle",
            "Laura",
            "Sarah",
            "Kimberly",
            "Deborah",
            "Jessica",
            "Shirley",
            "Cynthia",
            "Angela"
    );

    private static final List<String> MALE_FIRST_NAMES = Arrays.asList(
            "James",
            "John",
            "Robert",
            "Michael",
            "William",
            "David",
            "Richard",
            "Charles",
            "Joseph",
            "Thomas",
            "Christopher",
            "Daniel",
            "Paul",
            "Mark",
            "Donald",
            "George",
            "Kenneth",
            "Steven",
            "Edward",
            "Brian",
            "Ronald",
            "Anthony",
            "Kevin",
            "Jason",
            "Matthew",
            "Gary",
            "Timothy",
            "Jose",
            "Larry"
    );

    public static Name get(final int index) {
        final int firstNameIndex = (index / 2) % Math.min(MALE_FIRST_NAMES.size(), FEMALE_FIRST_NAMES.size());
        final String firstName = (index % 2 == 0 ? FEMALE_FIRST_NAMES : MALE_FIRST_NAMES).get(firstNameIndex);
        final String lastName = LAST_NAMES.get(index % LAST_NAMES.size());
        return new Name(firstName, lastName);
    }

    public static class Name {
        public final String first;
        public final String last;

        public Name(String first, String last) {
            this.first = first;
            this.last = last;
        }

        @Override
        public String toString() {
            return first+" "+last;
        }
    }

    public static EmailAddress getEmailAddress(int index) {
        final NameGenerator.Name name = NameGenerator.get(index);
        return EmailAddress.builder()
                .email(name.first.toLowerCase(Locale.ENGLISH) + "." + name.last.toLowerCase(Locale.ENGLISH) + "@localhost")
                .name(name.first + " " + name.last)
                .build();

    }
}
