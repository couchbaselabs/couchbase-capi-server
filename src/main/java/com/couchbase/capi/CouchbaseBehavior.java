package com.couchbase.capi;

import java.util.List;
import java.util.Map;

public interface CouchbaseBehavior {

    List<String> getPools();

    String getPoolUUID(String pool);

    Map<String, Object> getPoolDetails(String pool);

    List<String> getBucketsInPool(String pool);

    String getBucketUUID(String pool, String bucket);

    List<Object> getNodesServingPool(String pool);
}
