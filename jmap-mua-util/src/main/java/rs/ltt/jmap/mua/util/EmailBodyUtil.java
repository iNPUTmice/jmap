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
 */

package rs.ltt.jmap.mua.util;

import com.google.common.base.CharMatcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class EmailBodyUtil {

    public static List<Block> parse(final List<String> textBodies) {
        return textBodies.stream()
                .map(EmailBodyUtil::parse)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    public static List<Block> parse(String body) {
        String[] lines = body.split("\\n");
        ArrayList<Block> blocks = new ArrayList<>();

        Block currentBlock = null;

        for (String currentLine : lines) {
            QuoteIndicator quoteIndicator = QuoteIndicator.quoteDepth(currentLine);
            if (currentBlock == null) {
                currentBlock = new Block(quoteIndicator.depth);
                blocks.add(currentBlock);
            } else if (quoteIndicator.depth != currentBlock.depth) {
                currentBlock = new Block(quoteIndicator.depth);
                blocks.add(currentBlock);
            }

            String withQuoteRemoved;
            if (quoteIndicator.chars > 0) {
                withQuoteRemoved = currentLine.substring(quoteIndicator.chars);
            } else {
                withQuoteRemoved = currentLine;
            }

            currentBlock.append(withQuoteRemoved);

        }

        return blocks;
    }

    public static class Block {

        private static final float X = 0.1f;
        private static final float N = 0.15f;
        private static final float VOLATILITY_THRESHOLD = 12.0f;

        private final int depth;
        private final ArrayList<String> lines = new ArrayList<>();

        Block(int depth) {
            this.depth = depth;
        }

        public void append(final String line) {
            this.lines.add(line);
        }

        @Override
        public String toString() {
            int max = maxLineLength();

            int lastLineLength = max;

            boolean breakNextBlockUnderscore = false;
            boolean breakNextBlockHyphen = false;
            boolean skipNextBreak = false;
            StringBuilder stringBuilder = new StringBuilder();
            for (String line : this.lines) {
                String[] words = line.split("\\s+");
                String firstWord = words.length == 0 ? "" : words[0];
                if (stringBuilder.length() != 0) {

                    boolean listItem = (firstWord.length() <= 3 && (firstWord.endsWith(")") || firstWord.endsWith(":"))) || line.startsWith("* ") || line.startsWith("- ") || (firstWord.matches("\\[[0-9]+]:"));
                    if (skipNextBreak) {
                        //do nothing
                    } else if (breakNextBlockUnderscore || breakNextBlockHyphen) {
                        stringBuilder.append('\n');
                    } else if (line.isEmpty()) {
                        stringBuilder.append('\n');
                    } else if (listItem || line.contains("__")) {
                        stringBuilder.append('\n');
                    } else if (lastLineLength + firstWord.length() < max) {
                        stringBuilder.append('\n');
                    } else {
                        stringBuilder.append(' ');
                    }
                    lastLineLength = line.length();
                    skipNextBreak = false;
                } else if (line.isEmpty() && depth == 0) {
                    stringBuilder.append('\n');
                    skipNextBreak = true;
                }

                skipNextBreak |= line.endsWith(" ");
                final boolean blockBoundaryUnderscore = line.matches("_{2,}");
                final boolean blockBoundaryHypen = line.matches("-{2,}");
                breakNextBlockUnderscore = (breakNextBlockUnderscore && !line.isEmpty() && !blockBoundaryUnderscore) || (blockBoundaryUnderscore && !breakNextBlockUnderscore);
                breakNextBlockHyphen = (breakNextBlockHyphen && !line.isEmpty() && !blockBoundaryHypen) || (blockBoundaryHypen && !breakNextBlockHyphen);
                stringBuilder.append(line);
            }
            return stringBuilder.toString();
        }

        private int maxLineLength() {
            ArrayList<Integer> lineLengths = new ArrayList<>();
            int max = 0;
            for (String line : lines) {
                if (CharMatcher.is(' ').countIn(line) > 1) {
                    max = Math.max(line.length(), max);
                    lineLengths.add(line.length());
                }
            }
            Collections.sort(lineLengths);
            if (lineLengths.size() <= 1) {
                return max;
            }
            //get the top x% of line length
            List<Integer> topXPercent = lineLengths.subList((int) ((1.0 - X) * lineLengths.size()) - 1, lineLengths.size());

            double sum = 0.0;
            int top = lineLengths.get(lineLengths.size() - 1);
            for (int i = 0; i < lineLengths.size() - 1; ++i) {
                int bottom = lineLengths.get(i);
                sum += ((float) top / (float) bottom) - 1.0f;
            }
            double avg = sum / topXPercent.size();

            if (avg >= VOLATILITY_THRESHOLD) {
                return Integer.MAX_VALUE;
            }

            //are those top x% of lines less than n% apart from each other?
            if (topXPercent.get(topXPercent.size() - 1) - topXPercent.get(0) <= N * topXPercent.get(topXPercent.size() - 1)) {
                //return median of those top x%
                return topXPercent.size() % 2 == 1 ? topXPercent.get(topXPercent.size() / 2) : ((topXPercent.get(topXPercent.size() / 2 - 1) + topXPercent.get(topXPercent.size() / 2)) / 2);
            }
            return max;
        }

        public int getDepth() {
            return depth;
        }
    }

    public static class QuoteIndicator {

        private final int chars;
        private final int depth;

        QuoteIndicator(int chars, int depth) {
            this.chars = chars;
            this.depth = depth;
        }

        static QuoteIndicator quoteDepth(String line) {
            int quoteDepth = 0;
            int chars = 0;
            for (int i = 0; i < line.length(); ++i) {
                char c = line.charAt(i);
                if (c == '>') {
                    quoteDepth++;
                } else if (c != ' ') {
                    break;
                }
                ++chars;
            }
            return new QuoteIndicator(chars, quoteDepth);
        }
    }


}
