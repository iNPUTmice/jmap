# Java JMAP library

A library to synchronize data between a client and a server using the JSON Meta Application Protocol. The current focus is on acting as a client library to retrieve emails from a server however it is easily extensible to also synchronize contacts and calendars. It might even be extended to act as a server library.

The library is written in Java 7 to provide full compatibility with Android. It uses [GSON](https://github.com/google/gson) for JSON serialization and deserialization and makes heavy use of [Guava](https://github.com/google/guava) including its Futures. 

Entities (Mailbox, Email, EmailSubmission, …) are annotated with Project Lombok’s `@Getter` and `@Builder` to make them immutable.

**This library is work in progress. Not *all* specified methods and entities have been implemented yet. They will be added on a *as needed* basis. If you want to use this library and need a specific method you can add it very easily. Adding a new method is as simple as looking at the spec and creating a POJO that represents the data structure.**

## Modules

The library is divided into separate modules.

### jmap-annotation

Each JMAP method call and response is a POJO annotated with `@JmapMethod`. An annotation processor collects a lists of all available JMAP Methods. This modules holds these annotations and annotation processors.

### jmap-common

A collection of POJOs that represent JMAP requests, responses and the entities exchanged with those. It currently holds POJOs for JMAP Core and JMAP Mail but might be extended to hold JMAP Calender and JMAP Contacts POJOs as well. Alternatively it might be split up into `jmap-common-mail`, `jmap-common-contacts` and so on.

### jmap-common-interface

A small collection of interfaces that are required by both `jmap-common` and the annotation processors in `jmap-annotation`.

### jmap-gson

GSON serializer and deserializer to convert the POJOs from `jmap-common` into JMAP compatible JSON.

### jmap-client

A JMAP client library to make JMAP method calls and process the responses. It handles multiples calls in one request (including back references) and multiple method responses per call. Currently it only supports requests over HTTP but it has been designed with the possibility in mind to eventually support requests over WebSockets.

#### A simple example fetching mailboxes

```java
JmapClient client = new JmapClient("user@example.com", "password");

Future<MethodResponses> future = client.call(new GetMailboxMethodCall());

GetMailboxMethodResponse mailboxMethodResponse = future.get().getMain(GetMailboxMethodResponse.class);

for(Mailbox mailbox : mailboxMethodResponse.getList()) {
    System.out.println(mailbox.getName());
}
```

#### Multiple method calls in the same request

```java
JmapClient client = new JmapClient("user@example.com", "password");

JmapClient.MultiCall multiCall = client.newMultiCall();

//create a query request
Call queryEmailCall = multiCall.call(
        new QueryEmailMethodCall(EmailQuery.unfiltered())
);

//create a get email request with a back reference to the IDs found in the previous request
Call getEmailCall = multiCall.call(
        new GetEmailMethodCall(queryEmailCall.createResultReference(Request.Invocation.ResultReference.Path.IDS))
);

multiCall.execute();

//process responses
QueryEmailMethodResponse emailQueryResponse = queryEmailCall.getMethodResponses().get().getMain(QueryEmailMethodResponse.class);
GetEmailMethodResponse getEmailMethodResponse = getEmailCall.getMethodResponses().get().getMain(GetEmailMethodResponse.class);
for (Email email : getEmailMethodResponse.getList()) {
    System.out.println(email.getSentAt() + " " + email.getFrom() + " " + email.getSubject());
}
```

### jmap-mua

A high level API to act as an email client. It handles everything an email client is supposed to handle minus storage backend and GUI. The storage (caching) backend is accessed via an interface that different email clients on different platforms can implement. It comes with a reference in-memory implementation of that interface. `jmap-mua` only ever *writes* to that storage backend. Accessing data in that storage backend and displaying it in a GUI is up to the specific email client.

### lttrs-cli

A very, very rudimentary implementation of a TUI email client that uses `jmap-mua`. It mostly exists for development purposes and to quickly test new features in `jmap-mua`. Code quality (especially in regards to the TUI) is currently pretty low.

![screenshot of lttrs-cli](https://gultsch.de/files/lttrs-cli.png)

### lttrs-android

Lttrs is a proof of concept Android client currently in development. It makes heavy use of [Android Jetpack](https://developer.android.com/jetpack/) for a more maintainable code base than some of the preexisting Android Email clients. Follow the development at [iNPUTmice/lttrs-android](https://github.com/inputmice/lttrs-android).


![screenshot of Ltt.rs for Android](https://gultsch.de/files/lttrs-android.png)
