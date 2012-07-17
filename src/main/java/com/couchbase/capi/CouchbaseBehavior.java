package com.couchbase.capi;

import java.util.List;
import java.util.Map;

public interface CouchbaseBehavior {

    List<String> getPools();

    Map<String, Object> getPoolDetails(String pool);

    List<String> getBucketsInPool(String pool);

    List<Object> getNodesServingBucket(String pool, String bucket);
}
