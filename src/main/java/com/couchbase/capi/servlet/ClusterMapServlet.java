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
 * This servlet is responsible for providing the cluster list and cluster details.
 *
 * Requests like:
 *
 * /pools
 * AND
 * /pools/default
 *
 * @author mschoch
 *
 */
@SuppressWarnings("serial")
public class ClusterMapServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(ClusterMapServlet.class);
    protected ObjectMapper mapper = new ObjectMapper();

    private CouchbaseBehavior couchbaseBehavior;

    public ClusterMapServlet(CouchbaseBehavior couchbaseBehavior) {
        this.couchbaseBehavior = couchbaseBehavior;
    }

    /**
     * Handle get requests for the matching URLs and direct to the right handler method.
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String pool = req.getPathInfo();
        OutputStream os = resp.getOutputStream();

        if (pool == null || pool.equals("/")) {
            executePoolsRequest(os);
        } else {
            // trim off slash
            if (pool.startsWith("/")) {
                pool = pool.substring(1);
            }
            String uuid = req.getParameter("uuid");
            executePoolRequest(resp, os, pool, uuid);
        }
    }

    /**
     * Returns a single pool named "default"
     *
     * @param os
     * @throws IOException
     */
    protected void executePoolsRequest(OutputStream os) throws IOException {
        logger.trace("asked for pools");
        List<Object> pools = new ArrayList<Object>();

        List<String> poolNames = couchbaseBehavior.getPools();

        for (String poolName : poolNames) {
            Map<String, Object> pool = new HashMap<String, Object>();
            pool.put("name", poolName);
            pool.put("uri", "/pools/" + poolName + "?uuid=" + couchbaseBehavior.getPoolUUID(poolName));
            pools.add(pool);
        }

        Map<String, Object> responseMap = new HashMap<String, Object>();
        responseMap.put("pools", pools);
        responseMap.put("uuid", couchbaseBehavior.getPoolUUID("default"));

        mapper.writeValue(os, responseMap);
    }

    /**
     * When asked about the details of a pool, returns a pointer to the bucket list
     *
     * @param os
     * @param pool
     * @throws IOException
     */
    protected void executePoolRequest(HttpServletResponse resp, OutputStream os, String pool, String uuid)
            throws IOException {
        logger.trace("asked for pool {}", pool);

        Map<String, Object> responseMap = couchbaseBehavior.getPoolDetails(pool);
        if(responseMap != null) {
            // if the request contained a UUID, make sure it matches
            if(uuid != null) {
                String poolUUID = couchbaseBehavior.getPoolUUID(pool);
                if(!uuid.equals(poolUUID)) {
                    resp.setStatus(404);
                    os.write("Cluster uuid does not match the requested.".getBytes());
                    os.close();
                } else {
                    mapper.writeValue(os, responseMap);
                }
            } else {
                mapper.writeValue(os, responseMap);
            }
        } else {
            resp.setStatus(404);
        }
    }

}
