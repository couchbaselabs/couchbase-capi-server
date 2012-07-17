# Couchbase CAPI Server

This project implements an HTTP server capable of responding to Couchbase and CAPI requests.

Couchbase Requests supported:

- /pools (GET)
- /pools/default (GET)
- /pools/default/buckets (GET)
- /pools/default/buckets/{bucket} (GET)

CAPI Requests supported:

- /{database} (HEAD, GET)
- /{database}/{docid} (GET)
- /{database}/_ensure_full_commit (POST)
- /{database}/_revs_diff (POST)
- /{database}/_bulk_docs (POST)

This project does not come with an actual implementation of the behaviors behind these actions.  Instead two interfaces are exposed:

- CouchbaseBehavior
- CAPIBehavior

Users of this library will provide their own implemenations of these interfaces to deliver the desired behavior.


## Building

This project is built using Maven.

    mvn install 
      
## Using

1.  Implement your own CouchbaseBehavior
```java
    public class MyCustomCouchbaseBehavior implements CouchbaseBehavior { ... }
```
2.  Implement your own CAPIBehavior
```java
    public class MyCustomCAPIBehavior implements CAPIBehavior { ... }
```
3.  Create instances of #1 and #2
```java
    CouchbaseBehavior couchbaseBehavior = new MyCustomCouchbaseBehavior();
    CAPIBehavior capiBehavior = new MyCustomCAPIBehavior();
```
4.  Start a CAPIServer
```java
    CAPIServer capiServer = new CAPIServer(capiBehavior, couchbaseBehavior);
    capiServer.start();
```    

By default this will start a server bound to 0.0.0.0 on an ephemeral port.  If you'd like to bind to a different interface or a particular port, there are alternate constructors available.
    
