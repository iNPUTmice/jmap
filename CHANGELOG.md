### 0.8.6 (2022-01-03)

* Introduce LegacyFileUpload for use in Android
* Request `sentAt` property by default in jmap-mua

### 0.8.5 (2021-12-29)

* Log Websocket traffic to OkHttp logger

### 0.8.4 (2021-12-25)

* Make jmap-mua request some additonal, custom e-mail properties

### 0.8.3 (2021-12-20)

* Give Plugins access to Mua services
* Common interface `BinaryData` for Upload and Downloadable 

### 0.8.2 (2021-12-18)

* Add ability to set MediaType on EmailBodyPartBuilder

### 0.8.1 (2021-12-17)

* Refactor plugin API to make callbacks only accessible from PluginService
* Add method to retrieve installed plugin from Mua

### 0.8.0 (2021-12-15)

* Add plugin interface to jmap-mua

### 0.7.5 (2021-12-14)

* Add `OutputStreamUpload` class to retrieve output stream for upload
* Add utility method to calculate effective date

### 0.7.4 (2021-08-08)

* Fixed bugs in WebSocket implementation

### 0.7.3 (2021-07-27)

* SetPushSubscription and GetPushSubscription doesn’t have an accountId
* Add PushSubscription support to MockMailServer

### 0.7.2 (2021-07-26)

* Common interface for PushVerification and StateChange (PushMessage)

### 0.7.1 (2021-07-22)

* Check for upload and attachment size limits

### 0.7.0 (2021-07-20)

* Add ability to upload and download Blobs

### 0.6.5 (2021-06-27)

* jmap-client: fix EventSourcPushService crashing on missing eventUrl 

### 0.6.4 (2021-06-22)

* jmap-mua-util: Add more utility methods

### 0.6.3 (2021-05-18)

* jmap-mua: Add API to calculate query result total

### 0.6.2 (2021-04-20)

* JmapClient.monitorEvents() now doesn’t require listener

### 0.6.1 (2021-04-19)

* Add support for WebSockets (including push)

### 0.6.0 (2021-04-11)

* Introduce draft API to listen to events


### 0.5.8 (2021-04-05)

* Introduce multi-account support to MockMailServer


### 0.5.7 (2021-03-31)

* Fixed bug with invalid responses
* jmap-mua-util: Include more utility functions

### 0.5.6 (2021-01-03)

* Fix improper ifInState usage in modifyLabels() call

### 0.5.5 (2021-01-01)

* Add modifyLabels(…) method to jmap-mua

### 0.5.4 (2020-12-15)

* Define some common queries in jmap-mua-util
* Change Cache API to use a hashed query string instead of query string

### 0.5.3 (2020-12-09)

* add core namespace to all requests

### 0.5.2 (2020-12-07)

* jmap-mua throw exception if automagic mailbox creation will probably fail

### 0.5.1

* fixed bug when pulling multiple Object changes from Cyrus
* Introduce new `getMailboxByNameAndParent` method to cache interface

### 0.5.0

* added server-side JSON (de)serializer
* added mock-server for better unit tests
* bumped OkHttp version
* run JMAP response processing on OkHttp threads via callbacks 

### 0.4.0 (2020-07-16)

* Internal code refactor and clean-ups
* Library now requires Java 8+

### 0.3.1 (2020-02-24)

* Add MailToUri class to jmap-mua-utils

### 0.3.0 (2020-02-17)

* Use builder pattern (instead of constructors) to create method calls

### 0.2.4 (2020-02-09)

* add support for discovering websocket capability
* add PushSubscription object (including get+set)

### 0.2.3 (2020-01-23)

* Provide easier access to User-Agent and Autocrypt headers in Email entity
* Email update call will only request mutable properties

### 0.2.2 (2020-01-05)

* Fixed FileSessionCache writing to wrong directory  

### 0.2.1 (2019-12-24)

* (temporary?) fix for Cyrus requiring the client to set `:mail` namespace on submit.

### 0.2.0 (2019-12-20)

* Mua.draft(…) and Mua.send(…) now return the id of the created email
* New utility functions in jmap-mua-util to parse email addresses from user input in address fields

### 0.1.6 (2019-12-09)

* Added Email Address Tokenizer to parse address field user input to jmap-mua-utils

### 0.1.5 (2019-12-04)

* Automatically redo queries that can’t calculate changes
* Improved logging (Including HTTP traffic logging)

### 0.1.4 (2019-12-03)

* Move annotation processor to separate sub project

### 0.1.3 (2019-11-30)

* Session object gains ability to get a list of accounts with a given capability

### 0.1.2 (2019-11-28)

* fix for not finding all JMAP Methods when extending the library
* Renamed SessionFileCache to FileSessionCache to match naming pattern
* Created InMemoryFileCache

### 0.1.1 (2019-11-20)

* initial release. Basic email processing with either jmap-client or jmap-mua
  in working condition.
* jmap-common has methods and objects for most of RFC 8620 & RFC 8621
* jmap-common has convenient builders for filter conditions
* jmap-client has support for method calls (including mulitple calls at once),
  processing the responses, and support for references
* jmap-mua has support for reading and processing (changing mailboxes, changing
  keywords, etc) emails.
* jmap-client, jmap-common, jmap-gson and jmap-mua have some unit test
