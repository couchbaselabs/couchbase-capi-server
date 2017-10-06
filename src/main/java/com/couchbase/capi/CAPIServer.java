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

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Enumeration;

import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.security.Credential;

import com.couchbase.capi.servlet.BucketMapServlet;
import com.couchbase.capi.servlet.CAPIServlet;
import com.couchbase.capi.servlet.ClusterMapServlet;
import com.couchbase.capi.servlet.StatsServlet;

public class CAPIServer extends Server {

    private InetAddress publishAddress;
    private InetSocketAddress bindAddress;

    public CAPIServer(CAPIBehavior capiBehavior, CouchbaseBehavior couchbaseBehavior, String username, String password) {
        this(capiBehavior, couchbaseBehavior, 0, username, password);
    }

    public CAPIServer(CAPIBehavior capiBehavior, CouchbaseBehavior couchbaseBehavior, int port, String username, String password) {
        this(capiBehavior, couchbaseBehavior, new InetSocketAddress("0.0.0.0", port), username, password);
    }

    public CAPIServer(CAPIBehavior capiBehavior, CouchbaseBehavior couchbaseBehavior, InetSocketAddress bindAddress, String username, String password) {
        this(capiBehavior, couchbaseBehavior, bindAddress, username, password, 1024);
    }

    public CAPIServer(CAPIBehavior capiBehavior, CouchbaseBehavior couchbaseBehavior, InetSocketAddress bindAddress, String username, String password, int numVbuckets) {
        super(bindAddress);

        ServerConnector connector0 = new ServerConnector(this);
        connector0.setHost(bindAddress.getHostName());
        connector0.setPort(bindAddress.getPort());

//        connector0.setRequestBufferSize(32 * 1024);

        setConnectors(new Connector[]{ connector0 });

        this.bindAddress = bindAddress;

        ServletContextHandler context = new ServletContextHandler(
                ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        context.setSecurityHandler(basicAuth(username, password, "Couchbase Server Admin / REST"));
        setHandler(context);

        context.addServlet(new ServletHolder(new StatsServlet(couchbaseBehavior, capiBehavior)), "/_stats");
        context.addServlet(new ServletHolder(new ClusterMapServlet(couchbaseBehavior)),
                "/pools/*");
        context.addServlet(new ServletHolder(new BucketMapServlet(
                couchbaseBehavior, numVbuckets)), "/pools/default/buckets/*");
        context.addServlet(
                new ServletHolder(new CAPIServlet(capiBehavior)), "/*");

    }

    public int getPort() {
        Connector[] connectors = getConnectors();
        if(connectors.length < 1) {
            throw new IllegalStateException("Cannot get port, there are no connectors");
        }
        Connector connector = connectors[0];
        return ((ServerConnector)connector).getLocalPort();
    }

    /**
     * Returns the first IPv4 address we find
     *
     * @return
     */
    protected InetAddress guessPublishAddress() {
        NetworkInterface ni;
        try {
            ni = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
        } catch (Exception e) {
            return null;
        }

        Enumeration<InetAddress> ia = ni.getInetAddresses();
        while (ia.hasMoreElements()) {
            InetAddress elem = (InetAddress) ia.nextElement();
            if (elem instanceof Inet4Address) {
                return elem;
            }
        }
        return null;
    }

    public URI getCAPIAddress() {
        if(publishAddress == null) {
            publishAddress = guessPublishAddress();
        }
        try {
            return new URI("http://" + publishAddress + ":" + getPort() + "/");
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public InetAddress getPublishAddress() {
        return publishAddress;
    }

    public void setPublishAddress(InetAddress publishAddress) {
        this.publishAddress = publishAddress;
    }

    private static final SecurityHandler basicAuth(String username, String password, String realm) {

        HashLoginService l = new HashLoginService();
        l.putUser(username, Credential.getCredential(password), new String[] {"user"});
        l.setName(realm);

        Constraint constraint = new Constraint();
        constraint.setName(Constraint.__BASIC_AUTH);
        constraint.setRoles(new String[]{"user"});
        constraint.setAuthenticate(true);

        ConstraintMapping cm = new ConstraintMapping();
        cm.setConstraint(constraint);
        cm.setPathSpec("/*");

        ConstraintSecurityHandler csh = new ConstraintSecurityHandler();
        csh.setAuthenticator(new BasicAuthenticator());
        csh.setRealmName(realm);
        csh.addConstraintMapping(cm);
        csh.setLoginService(l);

        return csh;

    }

    public InetSocketAddress getBindAddress() {
        return bindAddress;
    }
}
