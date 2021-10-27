package rs.ltt.jmap.common;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import rs.ltt.jmap.common.entity.Email;
import rs.ltt.jmap.common.entity.UndoStatus;
import rs.ltt.jmap.common.entity.filter.*;
import rs.ltt.jmap.common.entity.query.EmailSubmissionQuery;
import rs.ltt.jmap.common.entity.query.MailboxQuery;

public class QueryStringTest {

    @Test
    public void complexEmailFilterQueryString() {
        Filter<Email> emailFilter1 =
                FilterOperator.and(
                        EmailFilterCondition.builder().text("one").build(),
                        FilterOperator.not(EmailFilterCondition.builder().text("three").build()),
                        EmailFilterCondition.builder().text("two").build());
        Filter<Email> emailFilter2 =
                FilterOperator.and(
                        EmailFilterCondition.builder().text("two").build(),
                        FilterOperator.not(EmailFilterCondition.builder().text("three").build()),
                        EmailFilterCondition.builder().text("one").build());
        Filter<Email> emailFilter3 =
                FilterOperator.and(
                        EmailFilterCondition.builder().text("two").build(),
                        EmailFilterCondition.builder().text("one").build(),
                        FilterOperator.not(EmailFilterCondition.builder().text("three").build()));
        Assertions.assertEquals(emailFilter1.toQueryString(), emailFilter2.toQueryString());
        Assertions.assertEquals(emailFilter1.asHash(), emailFilter2.asHash());
        Assertions.assertEquals(emailFilter2.toQueryString(), emailFilter3.toQueryString());
        Assertions.assertEquals(emailFilter2.asHash(), emailFilter3.asHash());
    }

    @Test
    public void inMailboxOtherThan() {
        Filter<Email> a =
                EmailFilterCondition.builder()
                        .inMailboxOtherThan(new String[] {"spam", "trash"})
                        .build();
        Filter<Email> b =
                EmailFilterCondition.builder()
                        .inMailboxOtherThan(new String[] {"trash", "spam"})
                        .build();
        Assertions.assertEquals(a.toQueryString(), b.toQueryString());
    }

    @Test
    public void inMailboxOtherThanOr() {
        Filter<Email> a =
                FilterOperator.or(
                        EmailFilterCondition.builder()
                                .inMailboxOtherThan(new String[] {"spam", "trash"})
                                .build(),
                        EmailFilterCondition.builder()
                                .inMailboxOtherThan(new String[] {"inbox"})
                                .build());
        Filter<Email> b =
                FilterOperator.or(
                        EmailFilterCondition.builder()
                                .inMailboxOtherThan(new String[] {"inbox"})
                                .build(),
                        EmailFilterCondition.builder()
                                .inMailboxOtherThan(new String[] {"trash", "spam"})
                                .build());
        Assertions.assertEquals(a.toQueryString(), b.toQueryString());
    }

    @Test
    public void complexMailboxFilterQueryString() {
        MailboxQuery a =
                MailboxQuery.of(
                        FilterOperator.or(
                                MailboxFilterCondition.builder().hasAnyRole(true).build(),
                                MailboxFilterCondition.builder().name("Inbox").build(),
                                FilterOperator.and(
                                        MailboxFilterCondition.builder().isSubscribed(true).build(),
                                        MailboxFilterCondition.builder().parentId("1").build())));
        MailboxQuery b =
                MailboxQuery.of(
                        FilterOperator.or(
                                MailboxFilterCondition.builder().name("Inbox").build(),
                                FilterOperator.and(
                                        MailboxFilterCondition.builder().parentId("1").build(),
                                        MailboxFilterCondition.builder()
                                                .isSubscribed(true)
                                                .build()),
                                MailboxFilterCondition.builder().hasAnyRole(true).build()));
        Assertions.assertEquals(a.toQueryString(), b.toQueryString());
    }

    @Test
    public void complexEmailSubmissionQueryString() {
        EmailSubmissionQuery a =
                EmailSubmissionQuery.of(
                        FilterOperator.or(
                                EmailSubmissionFilterCondition.builder()
                                        .undoStatus(UndoStatus.FINAL)
                                        .build(),
                                EmailSubmissionFilterCondition.builder()
                                        .undoStatus(UndoStatus.PENDING)
                                        .build(),
                                FilterOperator.or(
                                        EmailSubmissionFilterCondition.builder()
                                                .emailIds(new String[] {"1", "2", "3"})
                                                .build(),
                                        EmailSubmissionFilterCondition.builder()
                                                .emailIds(new String[] {"4", "5"})
                                                .build())));
        EmailSubmissionQuery b =
                EmailSubmissionQuery.of(
                        FilterOperator.or(
                                FilterOperator.or(
                                        EmailSubmissionFilterCondition.builder()
                                                .emailIds(new String[] {"4", "5"})
                                                .build(),
                                        EmailSubmissionFilterCondition.builder()
                                                .emailIds(new String[] {"1", "2", "3"})
                                                .build()),
                                EmailSubmissionFilterCondition.builder()
                                        .undoStatus(UndoStatus.PENDING)
                                        .build(),
                                EmailSubmissionFilterCondition.builder()
                                        .undoStatus(UndoStatus.FINAL)
                                        .build()));
        Assertions.assertEquals(a.toQueryString(), b.toQueryString());
    }

    @Test
    public void simpleNotMatchEmailSubmissionQuery() {
        EmailSubmissionQuery a =
                EmailSubmissionQuery.of(
                        EmailSubmissionFilterCondition.builder()
                                .undoStatus(UndoStatus.PENDING)
                                .build());
        EmailSubmissionQuery b =
                EmailSubmissionQuery.of(
                        EmailSubmissionFilterCondition.builder()
                                .undoStatus(UndoStatus.CANCELED)
                                .build());
        Assertions.assertNotEquals(a.toQueryString(), b.toQueryString());
    }
}
