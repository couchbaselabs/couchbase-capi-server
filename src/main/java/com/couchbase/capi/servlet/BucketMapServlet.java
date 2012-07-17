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
package com.couchbase.capi.servlet;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.couchbase.capi.CouchbaseBehavior;

/**
 * This servlet is responsible for providing the bucket list and bucket details.
 *
 * Requests like:
 *
 * /.../buckets
 * AND
 * /.../buckets/default
 *
 * @author mschoch
 *
 */
@SuppressWarnings("serial")
public class BucketMapServlet extends HttpServlet {

    // FIXME make number of buckets configurable
    protected int NUM_VBUCKETS = 1024;

    private static final Logger logger = LoggerFactory.getLogger(BucketMapServlet.class);
    protected ObjectMapper mapper = new ObjectMapper();

    protected CouchbaseBehavior couchbaseBehavior;

    public BucketMapServlet(CouchbaseBehavior couchbaseBehavior) {
        this.couchbaseBehavior = couchbaseBehavior;
    }

    /**
     * Handle get requests for the matching URLs and direct to the right handler method.
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String bucket = req.getPathInfo();
        OutputStream os = resp.getOutputStream();

        if (bucket == null || bucket.equals("/")) {
            executeBucketsRequest(os, "default");
        } else {
            bucket = getDatabaseNameFromPath(removePathSuffix(bucket, "/"));
            executeBucketRequest(req, os, "default", bucket);
        }
    }

    /**
     * Using the connection manager, find the client ids of the active connections
     * and return this as a list of a valid buckets.
     *
     * @param os
     * @throws IOException
     */
    protected void executeBucketsRequest(OutputStream os, String pool) throws IOException {
        logger.trace("asked for bucket list");

        List<Object> buckets = new ArrayList<Object>();

        List<String> bucketNames = couchbaseBehavior.getBucketsInPool(pool);

        for (String bucketName : bucketNames) {

            Map<String, Object> bucket = new HashMap<String, Object>();
            bucket.put("name", bucketName);
            bucket.put("uri", String.format("/pools/default/buckets/%s", bucketName));
            buckets.add(bucket);
        }

        mapper.writeValue(os, buckets);
    }

    /**
     * Return a fake bucket map for the requested bucket.
     *
     * @param req
     * @param os
     * @param bucket
     * @throws IOException
     */
    protected void executeBucketRequest(HttpServletRequest req, final OutputStream os,
            final String pool, final String bucket) throws IOException {

        List<Object> nodes = couchbaseBehavior.getNodesServingBucket(pool, bucket);

        List<Object> serverList = new ArrayList<Object>();
        for (Object node : nodes) {
            Map<String, Object> nodeObj = (Map<String, Object>)node;
            serverList.add(nodeObj.get("hostname"));
        }


        List<Object> vBucketMap = new ArrayList<Object>();
        for(int i=0; i < NUM_VBUCKETS; i++) {
            List<Object> vbucket = new ArrayList<Object>();
            vbucket.add(i%serverList.size());
            vbucket.add(-1);
            vBucketMap.add(vbucket);
        }

        Map<String, Object> vbucketServerMap = new HashMap<String, Object>();
        vbucketServerMap.put("serverList", serverList);
        vbucketServerMap.put("vBucketMap", vBucketMap);

        Map<String, Object> responseMap = new HashMap<String, Object>();
        responseMap.put("nodes", nodes);
        responseMap.put("vBucketServerMap", vbucketServerMap);
        responseMap.put("name", bucket);
        responseMap.put("bucketType", "membase");

        mapper.writeValue(os, responseMap);
    }

    protected String removePathSuffix(String path, String suffix) {
        if (path.endsWith(suffix)) {
            path = path.substring(0, path.length() - suffix.length());
        }
        return path;
    }

    protected String getDatabaseNameFromPath(String path) {
        String database = null;
        if(path.startsWith("/")) {
            database = path.substring(1);
        }
        return database;
    }

}
