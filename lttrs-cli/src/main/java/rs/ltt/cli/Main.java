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

package rs.ltt.cli;

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.ListenableFuture;
import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import com.googlecode.lanterna.terminal.TerminalResizeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rs.ltt.cli.cache.MyInMemoryCache;
import rs.ltt.cli.model.QueryViewItem;
import rs.ltt.jmap.client.api.MethodErrorResponseException;
import rs.ltt.jmap.client.api.UnauthorizedException;
import rs.ltt.jmap.common.entity.*;
import rs.ltt.jmap.common.entity.filter.EmailFilterCondition;
import rs.ltt.jmap.common.entity.query.EmailQuery;
import rs.ltt.jmap.mua.Mua;
import rs.ltt.jmap.mua.SetEmailException;
import rs.ltt.jmap.mua.Status;
import rs.ltt.jmap.mua.util.MailboxUtil;

import java.io.IOException;
import java.lang.Thread;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM dd");

    private static final MyInMemoryCache myInMemoryCache = new MyInMemoryCache();

    private static boolean running = true;

    private static List<QueryViewItem> items;

    private static int cursorPosition = 0;
    private static int offset = 0;
    private static int availableRows = 0;

    private static EmailQuery currentQuery;

    public static void main(String... args) {
        if (args.length != 2) {
            System.err.println("java -jar lttrs-cli.jar username password");
            System.exit(1);
            return;
        }

        final String username = args[0];
        final String password = args[1];
        final Mua mua = Mua.builder().username(username).password(password).cache(myInMemoryCache).queryPageSize(10L).build();

        DefaultTerminalFactory defaultTerminalFactory = new DefaultTerminalFactory();
        try {
            final Terminal terminal = defaultTerminalFactory.createTerminal();
            final TerminalScreen screen = new TerminalScreen(terminal);
            screen.startScreen();
            screen.setCursorPosition(null);
            screen.refresh();

            final Thread refreshThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    final IdentifiableMailboxWithRole inbox;
                    try {
                        loadingMessage(screen, "Loading mailboxes…");
                        mua.refreshMailboxes().get();
                        inbox = MailboxUtil.find(myInMemoryCache.getMailboxes(), Role.INBOX);
                        loadingMessage(screen, "Loading identities…");
                        mua.refreshIdentities().get();
                    } catch (Exception e) {
                        if (e instanceof ExecutionException) {
                            Throwable cause = e.getCause();
                            if (cause instanceof UnauthorizedException) {
                                loadingMessage(screen, "Unauthorized");
                                return;
                            } else if (cause instanceof MethodErrorResponseException) {
                                loadingMessage(screen, ((MethodErrorResponseException) cause).getMethodErrorResponse().getClass().getName());
                                return;
                            }
                        }
                        loadingMessage(screen, e.getMessage());
                        return;
                    }
                    if (inbox == null) {
                        loadingMessage(screen, "Inbox not found");
                        return;
                    }
                    while (running) {
                        try {
                            currentQuery = EmailQuery.of(EmailFilterCondition.builder().inMailbox(inbox.getId()).build(), true);
                            if (items == null) {
                                loadingMessage(screen, "Loading messages from inbox…");
                            }
                            Status status = mua.query(currentQuery).get();
                            if (status != Status.UNCHANGED) {
                                items = myInMemoryCache.getQueryViewItems(currentQuery.toQueryString());
                                redrawCurrentList(screen);
                            }
                            try {
                                Thread.sleep(5000);
                            } catch (InterruptedException e) {
                                //goodbye
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            refreshThread.start();

            terminal.addResizeListener(new TerminalResizeListener() {
                @Override
                public void onResized(Terminal terminal, TerminalSize terminalSize) {
                    try {
                        if (items != null) {
                            int newAvailableRows = terminalSize.getRows();
                            int maxPossibleOffset = Math.max(0, items.size() - newAvailableRows);
                            int minPossibleOffset = cursorPosition;
                            offset = Math.min(minPossibleOffset, maxPossibleOffset);
                            redrawCurrentList(screen);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });

            while (true) {
                KeyStroke keyStroke = screen.readInput();
                if (((keyStroke.getKeyType() == KeyType.Character && keyStroke.getCharacter() == 'q') || keyStroke.getKeyType() == KeyType.EOF)) {
                    exit(mua, screen, refreshThread);
                    break;
                }
                if (keyStroke.getKeyType() == KeyType.ArrowDown) {
                    moveCursorDown(screen, mua);
                }
                if (keyStroke.getKeyType() == KeyType.ArrowUp) {
                    moveCursorUp(screen);
                }
                if (keyStroke.getKeyType() == KeyType.Character && keyStroke.getCharacter() == 'n') {
                    toggleSeen(mua);
                }
                if (keyStroke.getKeyType() == KeyType.Character && keyStroke.getCharacter() == 's') {
                    toggleFlagged(mua);
                }
                if (keyStroke.getKeyType() == KeyType.Character && keyStroke.getCharacter() == 'a') {
                    archive(mua);
                }
                if (keyStroke.getKeyType() == KeyType.Character && keyStroke.getCharacter() == 'd') {
                    delete(mua);
                }
                if (keyStroke.getKeyType() == KeyType.Character && keyStroke.getCharacter() == 'j') {
                    applyLabel(mua, "jmap");
                }
                if (keyStroke.getKeyType() == KeyType.Character && keyStroke.getCharacter() == 'x') {
                    applyLabel(mua, "xmpp");
                }
                if (keyStroke.getKeyType() == KeyType.Character && keyStroke.getCharacter() == 'w') {
                    write(mua, false);
                }
                if (keyStroke.getKeyType() == KeyType.Character && keyStroke.getCharacter() == 'W') {
                    write(mua, true);
                }
                if (keyStroke.getKeyType() == KeyType.Enter) {
                    send(mua);
                }
                if (keyStroke.getKeyType() == KeyType.Character && keyStroke.getCharacter() == 'T') {
                    emptyTrash(mua);
                }

            }
        } catch (IOException e) {
            System.err.println("unable to create terminal " + e.getMessage());
        }

    }

    private static void moveCursorUp(TerminalScreen screen) throws IOException {
        if (cursorPosition > 0) {
            --cursorPosition;
            if (cursorPosition < offset) {
                --offset;
            }
            redrawCurrentList(screen);
        }
    }

    private static void moveCursorDown(TerminalScreen screen, Mua mua) throws IOException {
        if (items != null && items.size() - 1 > cursorPosition) {
            ++cursorPosition;
            if (cursorPosition - offset == availableRows) {
                ++offset;
            }
            redrawCurrentList(screen);
            if (cursorPosition == items.size() -1) {
                QueryViewItem last = Iterables.getLast(items, null);
                try {
                    Status status = mua.query(currentQuery, last.mostRecent.getId()).get();
                    if (status == Status.UPDATED) {
                        items = myInMemoryCache.getQueryViewItems(currentQuery.toQueryString());
                        redrawCurrentList(screen);
                    }
                } catch (Exception e) {
                    e.printStackTrace();;
                }
            }
        }
    }

    private static void exit(Mua mua, TerminalScreen screen, Thread refreshThread) throws IOException {
        screen.stopScreen();
        running = false;
        refreshThread.interrupt();
        mua.shutdown();
    }

    private static void toggleSeen(Mua mua) {
        QueryViewItem item = items.get(cursorPosition);
        if (item.mostRecent.getKeywords().containsKey(Keyword.SEEN)) {
            mua.removeKeyword(myInMemoryCache.getEmails(item.threadId), Keyword.SEEN);
        } else {
            mua.setKeyword(myInMemoryCache.getEmails(item.threadId), Keyword.SEEN);
        }
    }

    private static void toggleFlagged(Mua mua) {
        QueryViewItem item = items.get(cursorPosition);
        if (item.mostRecent.getKeywords().containsKey(Keyword.FLAGGED)) {
            mua.removeKeyword(myInMemoryCache.getEmails(item.threadId), Keyword.FLAGGED);
        } else {
            mua.setKeyword(myInMemoryCache.getEmails(item.threadId), Keyword.FLAGGED);
        }
    }

    private static void send(Mua mua) {
        QueryViewItem item = items.get(cursorPosition);
        if (!item.mostRecent.getKeywords().containsKey(Keyword.DRAFT)) {
            return;
        }
        Identity identity = Iterables.getFirst(myInMemoryCache.getIdentities(), null);
        if (identity != null) {
            try {
                LOGGER.info("submitted email: "+mua.submit(item.mostRecent, identity).get());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            LOGGER.error("no identity found");
        }

    }

    private static void applyLabel(Mua mua, String label) {
        Mailbox labelMailbox = null;
        for(Mailbox mailbox : myInMemoryCache.getMailboxes()) {
            if(label.equals(mailbox.getName()) && mailbox.getRole() == null){
                labelMailbox = mailbox;
            }
        }
        if (labelMailbox == null) {
            try {
                mua.createMailbox(Mailbox.builder().name(label).build()).get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        } else {
            QueryViewItem item = items.get(cursorPosition);
            Collection<Email> emails = myInMemoryCache.getEmails(item.threadId);
            try {
                mua.copyToMailbox(emails, labelMailbox.getId()).get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }


    }

    private static void write(Mua mua, boolean sendImmediately) {
        EmailBodyValue emailBodyValue = EmailBodyValue.builder()
                .value("This is a message from ltt.rs")
                .build();
        String partId = "1";
        EmailBodyPart emailBodyPart = EmailBodyPart.builder()
                .partId(partId)
                .type("text/plain")
                .build();
        Email email = Email.builder()
                .to(EmailAddress.builder()
                        .email("test@ltt.rs")
                        .name("Test Thetest")
                        .build())
                .from(EmailAddress.builder()
                        .email(mua.getJmapClient().getUsername())
                        .build())
                .subject("This is a test")
                .bodyValue(partId, emailBodyValue)
                .textBody(emailBodyPart)
                .mailboxId(MailboxUtil.find(myInMemoryCache.getMailboxes(), Role.INBOX).getId(), true)
                .build();
        ListenableFuture<Boolean> future;
        if (sendImmediately) {
            Identity identity = Iterables.getFirst(myInMemoryCache.getIdentities(), null);
            future = mua.send(email, identity);
        } else {
            future = mua.draft(email);
        }
        try {
            future.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof SetEmailException) {
                LOGGER.error(cause.toString());
            } else {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    private static void delete(Mua mua) {
        QueryViewItem item = items.get(cursorPosition);
        mua.moveToTrash(myInMemoryCache.getEmails(item.threadId));
    }

    private static void archive(Mua mua) {
        QueryViewItem item = items.get(cursorPosition);
        try {
            mua.archive(myInMemoryCache.getEmails(item.threadId)).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    private static void emptyTrash(Mua mua) {
        try {
            mua.emptyTrash().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    private static void loadingMessage(TerminalScreen screen, String message) {
        screen.clear();
        TerminalSize size = screen.getTerminalSize();
        TextGraphics text = screen.newTextGraphics();
        text.putString(0, size.getRows() - 1, message);
        try {
            screen.refresh();
        } catch (IOException e) {
            LOGGER.error("unable to refresh screen after printing loading message", e);
        }
    }

    private static void redrawCurrentList(TerminalScreen screen) throws IOException {
        TerminalSize terminalSize = screen.doResizeIfNecessary();
        if (terminalSize == null) {
            terminalSize = screen.getTerminalSize();
        }
        TextGraphics textGraphics = screen.newTextGraphics();
        int availableWidth = terminalSize.getColumns();
        availableRows = terminalSize.getRows();
        int fromWidth = 20;
        int dateWidth = 7;
        int threadSizeWidth = 8;
        int subjectPreviewWidth = availableWidth - fromWidth - dateWidth - threadSizeWidth;


        int row = 0;
        for (int i = offset; i < offset + availableRows; ++i) {
            if (i >= items.size()) {
                textGraphics.setForegroundColor(TextColor.ANSI.WHITE);
                textGraphics.setBackgroundColor(TextColor.ANSI.BLACK);
                textGraphics.putString(0, row, Strings.repeat(" ", availableWidth));
                ++row;
                continue;
            }
            QueryViewItem item = items.get(i);
            final boolean seen = item.mostRecent.getKeywords().containsKey(Keyword.SEEN);
            final boolean draft = item.mostRecent.getKeywords().containsKey(Keyword.DRAFT);
            final SGR sgr;
            if (draft) {
                sgr = SGR.ITALIC;
            } else if (seen) {
                sgr = null;
            } else {
                sgr = SGR.BOLD;
            }
            final boolean flagged = item.mostRecent.getKeywords().containsKey(Keyword.FLAGGED);
            boolean selected = i == cursorPosition;
            String from = from(item.from, fromWidth);
            String subject = item.mostRecent.getSubject();
            //String preview = item.mostRecent.getPreview().trim();
            String preview = getPreviewFromBodyParts(item.mostRecent.getTextBody(), item.mostRecent.getBodyValues());
            String date = receivedAt(item.mostRecent.getReceivedAt(), dateWidth);
            textGraphics.setForegroundColor(selected ? TextColor.ANSI.BLACK : TextColor.ANSI.WHITE);
            textGraphics.setBackgroundColor(selected ? TextColor.ANSI.CYAN : TextColor.ANSI.BLACK);
            if (flagged) {
                textGraphics.putString(0, row, "\u2605 ");
            } else {
                textGraphics.putString(0, row, "  ");
            }
            if (sgr == null) {
                textGraphics.putString(2, row, from);
            } else {
                textGraphics.putString(2, row, from, sgr);
            }
            if (sgr == null) {
                textGraphics.putString(2 + fromWidth, row, threadSize(item.count));
            } else {
                textGraphics.putString(2 + fromWidth, row, threadSize(item.count), sgr);
            }
            if (subject.length() > subjectPreviewWidth) {
                if (sgr == null) {
                    textGraphics.putString(2 + fromWidth + threadSizeWidth, row, subject.substring(0, subjectPreviewWidth));
                } else {
                    textGraphics.putString(2 + fromWidth + threadSizeWidth, row, subject.substring(0, subjectPreviewWidth), sgr);
                }
            } else {
                if (sgr == null) {
                    textGraphics.putString(2 + fromWidth + threadSizeWidth, row, subject);
                } else {
                    textGraphics.putString(2 + fromWidth + threadSizeWidth, row, subject, sgr);
                }
                int previewWidth = subjectPreviewWidth - subject.length() + 1;
                if (previewWidth > 1) {
                    textGraphics.setForegroundColor(selected ? TextColor.ANSI.BLACK : TextColor.ANSI.CYAN);
                    if (sgr == null) {
                        textGraphics.putString(2 + fromWidth + threadSizeWidth + subject.length(), row, " ");
                        textGraphics.putString(2 + fromWidth + threadSizeWidth + subject.length() + 1, row, preview(preview, previewWidth));
                    } else {
                        textGraphics.putString(2 + fromWidth + threadSizeWidth + subject.length(), row, " ", sgr);
                        textGraphics.putString(2 + fromWidth + threadSizeWidth + subject.length() + 1, row, preview(preview, previewWidth), sgr);
                    }
                }
            }

            textGraphics.setForegroundColor(selected ? TextColor.ANSI.BLACK : TextColor.ANSI.WHITE);

            if (sgr == null) {
                textGraphics.putString(availableWidth - dateWidth, row, date);
            } else {
                textGraphics.putString(availableWidth - dateWidth, row, date, sgr);
            }
            ++row;
        }
        screen.refresh();
    }

    private static String getPreviewFromBodyParts(List<EmailBodyPart> textBodies, Map<String, EmailBodyValue> bodyValues) {
        StringBuilder builder = new StringBuilder();
        for (EmailBodyPart bodyPart : textBodies) {
            EmailBodyValue foo = bodyValues.get(bodyPart.getPartId());
            if (foo != null) {
                String body = foo.getValue().replaceAll("\\s+", " ");
                builder.append(body, 0, Math.min(256, body.length()));
            }
        }
        return builder.toString();
    }


    private static String from(Set<EmailAddress> from, int width) {
        final boolean multiple = from.size() > 1;
        StringBuilder builder = new StringBuilder();
        for (EmailAddress emailAddress : from) {
            if (builder.length() != 0) {
                builder.append(", ");
            }
            if (emailAddress.getName() != null) {
                String name = emailAddress.getName();
                if (multiple) {
                    builder.append(name.split("\\s+")[0]);
                } else {
                    builder.append(name);
                }
            } else if (emailAddress.getEmail() != null) {
                builder.append(emailAddress.getEmail().split("@")[0]);
            }
        }
        while (builder.length() < width) {
            builder.append(' ');
        }
        return builder.toString();
    }

    private static String receivedAt(Date date, int width) {
        StringBuilder builder = new StringBuilder();
        if (isToday(date)) {
            builder.append(TIME_FORMAT.format(date));
        } else {
            builder.append(DATE_FORMAT.format(date));
        }
        while (builder.length() < width) {
            builder.insert(0, ' ');
        }
        return builder.toString();
    }

    private static String threadSize(int threadSize) {
        return threadSize > 1 ? " " +
                '(' +
                Strings.padStart(String.valueOf(Math.min(threadSize, 999)), 3, ' ') +
                ')' +
                "  " : "        ";
    }

    private static String preview(String preview, int width) {
        StringBuilder builder = new StringBuilder();
        builder.append(preview, 0, Math.min(width, preview.length()));
        while (builder.length() < width) {
            builder.append(' ');
        }
        return builder.toString();
    }

    private static boolean isToday(Date date) {
        Calendar today = Calendar.getInstance();
        Calendar specifiedDate = Calendar.getInstance();
        specifiedDate.setTime(date);

        return today.get(Calendar.DAY_OF_MONTH) == specifiedDate.get(Calendar.DAY_OF_MONTH)
                && today.get(Calendar.MONTH) == specifiedDate.get(Calendar.MONTH)
                && today.get(Calendar.YEAR) == specifiedDate.get(Calendar.YEAR);
    }


}