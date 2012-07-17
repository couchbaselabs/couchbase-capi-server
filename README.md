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
