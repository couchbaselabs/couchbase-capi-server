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

import junit.framework.TestCase;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CAPITestCase extends TestCase {

    protected static final Logger logger = LoggerFactory.getLogger(CAPITestCase.class);
    protected static final ObjectMapper mapper = new ObjectMapper();
    protected CAPIBehavior capiBehavior;
    protected CouchbaseBehavior couchbaseBehavior;
    protected CAPIServer capiServer;
    protected int port = -1;

    public CAPITestCase() {
        capiBehavior = new CAPIBehaviorTestImpl();
        couchbaseBehavior = new CouchbaseBehaviorTestImpl();
        capiServer = new CAPIServer(capiBehavior, couchbaseBehavior, "Administrator", "password");
    }

    @Override
    protected void setUp() throws Exception {
        capiServer.start();
        port = capiServer.getPort();
        logger.info("CAPIServer started on port {}", port);
    }

    @Override
    protected void tearDown() throws Exception {
        capiServer.stop();
    }

    protected HttpClient getClient() {
        DefaultHttpClient result = new DefaultHttpClient();

        result.getCredentialsProvider().setCredentials(new AuthScope(null, -1, null), new UsernamePasswordCredentials("Administrator", "password"));

        return result;
    }

    protected String localhost(String path) {
        return "http://localhost:" + port + "/" + path;
    }
}
