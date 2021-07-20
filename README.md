# Java JMAP library
![Build Status](https://github.com/inputmice/jmap/actions/workflows/maven.yml/badge.svg)
[![Codacy Badge](https://img.shields.io/codacy/grade/329b10e923d74c9881f9e8cf53616735?logo=codacy)](https://www.codacy.com/manual/iNPUTmice/jmap)
[![codecov](https://img.shields.io/codecov/c/gh/inputmice/jmap/master?logo=codecov&label=code%20coverage&token=W35RI2ASHI)](https://codecov.io/gh/iNPUTmice/jmap)
[![Maven Central](https://img.shields.io/maven-central/v/rs.ltt.jmap/jmap?color=informational)](https://maven-badges.herokuapp.com/maven-central/rs.ltt.jmap/jmap)
[![Ltt.rs conference room](https://inverse.chat/badge.svg?room=lttrs@conference.conversations.im)](https://conversations.im/j/lttrs@conference.conversations.im)
[![Liberapay patrons](https://img.shields.io/liberapay/patrons/inputmice?logo=liberapay&style=flat&color=informational)](https://liberapay.com/iNPUTmice)

A library to synchronize data between a client and a server using the JSON Meta Application Protocol. The current focus is on acting as a client library to retrieve emails from a server however it is easily extensible to also synchronize contacts and calendars. It might even be extended to act as a server library.

The library is written in Java 8 to provide full compatibility with Android. It uses [GSON](https://github.com/google/gson) for JSON serialization and deserialization and makes heavy use of [Guava](https://github.com/google/guava) including its Futures. 

Entities (Mailbox, Email, EmailSubmission, …) and method calls are annotated with Project Lombok’s `@Getter` and `@Builder` to make them immutable.

**This library is work in progress. Not *all* specified methods and entities have been implemented yet. They will be added on a *as needed* basis. If you want to use this library and need a specific method you can add it very easily. Adding a new method is as simple as looking at the spec and creating a POJO that represents the data structure.**

## Modules

The library is divided into separate modules. *Most people will probably want to use the [jmap-client](https://github.com/iNPUTmice/jmap/blob/master/README.md#jmap-client) module.*

### jmap-annotation

Each JMAP method call and response is a POJO annotated with `@JmapMethod`. An annotation processor collects a lists of all available JMAP Methods. This module only holds the annotations. When extending the library you need to include `jmap-annotation-processor` as well.

### jmap-annotation-processor

The annotation processors that compile lists of JMAP methods, JMAP errors and capabilities into resource files. This module is only required when extending the library.

### jmap-common

A collection of POJOs that represent JMAP requests, responses and the entities exchanged with those. It currently holds POJOs for JMAP Core and JMAP Mail but might be extended to hold JMAP Calender and JMAP Contacts POJOs as well. Alternatively it might be split up into `jmap-common-mail`, `jmap-common-contacts` and so on.

### jmap-common-interface

A small collection of interfaces that are required by both `jmap-common` and the annotation processors in `jmap-annotation`.

### jmap-gson

GSON serializer and deserializer to convert the POJOs from `jmap-common` into JMAP compatible JSON.

### jmap-client

A JMAP client library to make JMAP method calls and process the responses. It handles multiples calls in one request (including back references) and multiple method responses per call. Currently it only supports requests over HTTP but it has been designed with the possibility in mind to eventually support requests over WebSockets.

Gradle:
```groovy
implementation 'rs.ltt.jmap:jmap-client:0.7.0'
```
Maven:
```xml
<dependency>
  <groupId>rs.ltt.jmap</groupId>
  <artifactId>jmap-client</artifactId>
  <version>0.7.0</version>
</dependency>
```

#### A simple example fetching mailboxes

```java
JmapClient client = new JmapClient("user@example.com", "password");

Session session = client.getSession().get();
String accountId = session.getPrimaryAccount(MailAccountCapability.class);

Future<MethodResponses> future = client.call(
    GetMailboxMethodCall.builder().accountId(accountId).build()
);

GetMailboxMethodResponse mailboxMethodResponse = future.get().getMain(GetMailboxMethodResponse.class);

for(Mailbox mailbox : mailboxMethodResponse.getList()) {
    System.out.println(mailbox.getName());
}
```

#### Multiple method calls in the same request

```java
JmapClient client = new JmapClient("user@example.com", "password");

Session session = client.getSession().get();
String accountId = session.getPrimaryAccount(MailAccountCapability.class);

JmapClient.MultiCall multiCall = client.newMultiCall();

//create a query request
Call queryEmailCall = multiCall.call(
        QueryEmailMethodCall.builder()
                .accountId(accountId)
                .query(EmailQuery.unfiltered())
                .build()
);

//create a get email request with a back reference to the IDs found in the previous request
Call getEmailCall = multiCall.call(
        GetMailboxMethodCall.builder()
                .accountId(accountId)
                .idsReference(queryEmailCall.createResultReference(Request.Invocation.ResultReference.Path.IDS))
                .build()
);

multiCall.execute();

//process responses
QueryEmailMethodResponse emailQueryResponse = queryEmailCall.getMethodResponses().get().getMain(QueryEmailMethodResponse.class);
GetEmailMethodResponse getEmailMethodResponse = getEmailCall.getMethodResponses().get().getMain(GetEmailMethodResponse.class);
for (Email email : getEmailMethodResponse.getList()) {
    System.out.println(email.getSentAt() + " " + email.getFrom() + " " + email.getSubject());
}
```

#### Creating extensions

Extending the Java JMAP library with new object types and methods is easy. For each JMAP method you need to create a request and a response. They will have to implement `MethodCall` and `MethodResponse` respectively. Alternatively, if you are implementing one of the standard methods from JMAP Core, you can extend for example `GetMethodResponse<T extends AbstractIdentifiableEntity>` and the corresponding response. Additionally the request and the response need to be annotated with `@JmapMethod`. Finally the package in which those new classes reside needs to be annotated with `@JmapNamepace`.

You will have to include `jmap-annotation-processor` as an additional dependency in your project.

A full example that introduces the object type `Placeholder` and a corresponding `Placeholder/get` method can be found in [iNPUTmice/jmap-examples](https://github.com/iNPUTmice/jmap-examples).

One more thing to look out for: **If you are building a fat jar (shaded jar) the resource files, that map JMAP method names to their respective class names, need to be merged.** How this is done depends on your build system but the [pom.xml file in the example project](https://github.com/iNPUTmice/jmap-examples/blob/master/pom.xml#L40-L61) shows how to do this with Maven.

### jmap-mua

A high level API to act as an email client. It handles everything an email client is supposed to handle minus storage backend and GUI. The storage (caching) backend is accessed via an interface that different email clients on different platforms can implement. It comes with a reference in-memory implementation of that interface. `jmap-mua` only ever *writes* to that storage backend. Accessing data in that storage backend and displaying it in a GUI is up to the specific email client.

Gradle:
```groovy
implementation 'rs.ltt.jmap:jmap-mua:0.7.0'
```
Maven:
```xml
<dependency>
  <groupId>rs.ltt.jmap</groupId>
  <artifactId>jmap-mua</artifactId>
  <version>0.7.0</version>
</dependency>
```

#### Users

jmap-mua serves as the backend for:
*  [Ltt.rs for Unix](https://github.com/inputmice/lttrs-cli)
*  [Ltt.rs for Android](https://github.com/inputmice/lttrs-android).
