/**
 * Copyright (c) 2012 Couchbase, Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package com.couchbase.capi;

import java.util.List;
import java.util.Map;

public interface CouchbaseBehavior {

    List<String> getPools();

    String getPoolUUID(String pool);

    Map<String, Object> getPoolDetails(String pool);

    List<String> getBucketsInPool(String pool);

    String getBucketUUID(String pool, String bucket);

    List<Map<String, Object>> getNodesServingPool(String pool);

    Map<String, Object> getStats();
}
