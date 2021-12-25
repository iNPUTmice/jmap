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

package rs.ltt.jmap.mua.util;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import rs.ltt.jmap.common.entity.EmailAddress;

public class EmailAddressTokenizer {

    public static List<EmailAddressToken> tokenize(final CharSequence cs) {
        return tokenize(cs, false);
    }

    public static List<EmailAddressToken> tokenize(
            final CharSequence cs, final boolean requireExplicitDelimiter) {
        final ImmutableList.Builder<EmailAddressToken> tokenBuilder = new ImmutableList.Builder<>();
        final TokenReader tokenReader = new TokenReader(cs);
        ArrayList<Token> current = new ArrayList<>();
        while (tokenReader.hasMoreToken()) {
            final Token token = tokenReader.read();
            current.add(token);
            if (token.tokenType == TokenType.DELIMITER
                    || (!requireExplicitDelimiter && token.tokenType == TokenType.END)) {
                if (moreThanJustWhiteSpaces(current)) {
                    tokenBuilder.add(combine(cs, current));
                }
                current.clear();
            }
        }
        return tokenBuilder.build();
    }

    private static boolean moreThanJustWhiteSpaces(List<Token> tokens) {
        for (Token token : tokens) {
            if (!isWhitespaceOrDelimiter(token)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isWhitespaceOrDelimiter(Token token) {
        return token.tokenType == TokenType.WHITESPACE
                || token.tokenType == TokenType.DELIMITER
                || token.tokenType == TokenType.END;
    }

    private static EmailAddressToken combine(
            final CharSequence charSequence, List<Token> tokenList) {
        final ArrayList<Token> labelTokens = new ArrayList<>();
        final ArrayList<Token> addressTokens = new ArrayList<>();
        boolean inAddress = false;
        for (Token token : tokenList) {
            if (token.tokenType == TokenType.DELIMITER || token.tokenType == TokenType.END) {
                continue;
            }
            if (inAddress) {
                if (token.tokenType == TokenType.ADDRESS_END) {
                    inAddress = false;
                } else {
                    addressTokens.add(token);
                }
            } else {
                if (token.tokenType == TokenType.ADDRESS_BEGIN) {
                    inAddress = true;
                } else {
                    labelTokens.add(token);
                }
            }
        }
        String label;
        if (labelTokens.size() > 0) {
            final Token startToken = findFirstNonWhiteSpace(labelTokens, addressTokens.size() > 0);
            final Token endToken = findLastNonWhiteSpace(labelTokens, addressTokens.size() > 0);
            if (startToken != null && endToken != null) {
                final int start = startToken.start;
                final int end = endToken.end;
                label = charSequence.subSequence(start, end + 1).toString();
            } else {
                label = null;
            }
        } else {
            label = null;
        }
        final String emailAddress;
        if (addressTokens.size() > 0) {
            final int start = findFirstNonWhiteSpace(addressTokens, false).start;
            final int end = findLastNonWhiteSpace(addressTokens, false).end;
            emailAddress = charSequence.subSequence(start, end + 1).toString();
        } else {
            emailAddress = label;
            label = null;
        }
        return new EmailAddressToken(
                tokenList.get(0).start,
                tokenList.get(tokenList.size() - 1).end,
                EmailAddress.builder().email(emailAddress).name(label).build());
    }

    private static Token findFirstNonWhiteSpace(List<Token> tokens, boolean removeQuote) {
        for (int i = 0; i < tokens.size(); ++i) {
            final Token token = tokens.get(i);
            if (token.tokenType != TokenType.WHITESPACE
                    && (!removeQuote || token.tokenType != TokenType.QUOTE_BEGIN)) {
                return token;
            }
        }
        return null;
    }

    private static Token findLastNonWhiteSpace(List<Token> tokens, boolean removeQuote) {
        for (int i = tokens.size() - 1; i >= 0; --i) {
            final Token token = tokens.get(i);
            if (token.tokenType != TokenType.WHITESPACE
                    && (!removeQuote || token.tokenType != TokenType.QUOTE_END)) {
                return token;
            }
        }
        return null;
    }

    private enum TokenType {
        TEXT,
        ADDRESS_BEGIN,
        ADDRESS_END,
        QUOTE_BEGIN,
        QUOTE_END,
        WHITESPACE,
        DELIMITER,
        END
    }

    private static class TokenReader {
        private final ArrayDeque<Token> queue = new ArrayDeque<>();

        private TokenReader(final CharSequence cs) {
            final int length = cs.length();
            int start = 0;
            boolean inQuote = false;
            for (int i = 0; i < length; ++i) {
                final char c = cs.charAt(i);
                if (inQuote) {
                    if (isQuotationSymbol(c)) {
                        if (start < i) {
                            queue.add(new Token(TokenType.TEXT, start, i - 1));
                        }
                        queue.add(new Token(TokenType.QUOTE_END, i, i));
                        inQuote = false;
                        start = i + 1;
                    }
                } else {
                    if (isWhitespace(c)) {
                        if (start < i) {
                            queue.add(new Token(TokenType.TEXT, start, i - 1));
                        }
                        queue.add(new Token(TokenType.WHITESPACE, i, i));
                        start = i + 1;
                    } else if (isDelimiter(c)) {
                        if (start < i) {
                            queue.add(new Token(TokenType.TEXT, start, i - 1));
                        }
                        queue.add(new Token(TokenType.DELIMITER, i, i));
                        start = i + 1;
                    } else if (isQuotationSymbol(c)) {
                        if (start < i) {
                            queue.add(new Token(TokenType.TEXT, start, i - 1));
                        }
                        queue.add(new Token(TokenType.QUOTE_BEGIN, i, i));
                        inQuote = true;
                        start = i + 1;
                    } else if (isAddressBegin(c)) {
                        if (start < i) {
                            queue.add(new Token(TokenType.TEXT, start, i - 1));
                        }
                        queue.add(new Token(TokenType.ADDRESS_BEGIN, i, i));
                        start = i + 1;
                    } else if (isAddressEnd(c)) {
                        if (start < i) {
                            queue.add(new Token(TokenType.TEXT, start, i - 1));
                        }
                        queue.add(new Token(TokenType.ADDRESS_END, i, i));
                        start = i + 1;
                    }
                }
            }
            if (start < length) {
                queue.add(new Token(TokenType.TEXT, start, length - 1));
            }
            queue.add(new Token(TokenType.END, length - 1, length - 1));
        }

        private boolean isDelimiter(final char c) {
            return c == ';' || c == ',';
        }

        private boolean isWhitespace(final char c) {
            return Character.isWhitespace(c);
        }

        private boolean isQuotationSymbol(final char c) {
            return c == '"';
        }

        private boolean isAddressBegin(final char c) {
            return c == '<';
        }

        private boolean isAddressEnd(final char c) {
            return c == '>';
        }

        public boolean hasMoreToken() {
            return !queue.isEmpty();
        }

        public Token read() {
            return queue.poll();
        }
    }

    private static class Token {
        public final TokenType tokenType;
        public final int start;
        public final int end;

        private Token(TokenType tokenType, int start, int end) {
            this.tokenType = tokenType;
            this.start = start;
            this.end = end;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("tokenType", tokenType)
                    .add("start", start)
                    .add("end", end)
                    .toString();
        }
    }
}
