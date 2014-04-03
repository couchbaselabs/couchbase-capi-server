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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.couchbase.capi.CAPIBehavior;

/**
 * This servlet implements the Couch API (CAPI)
 *
 * This is not a fully-functional implementation, rather it is a bare-minimum implementation to support
 * receiving a push replication from another CouchDB instance.
 *
 * @author mschoch
 *
 */
@SuppressWarnings("serial")
public class CAPIServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(CAPIServlet.class);
    protected ObjectMapper mapper = new ObjectMapper();

    protected CAPIBehavior capiBehavior;

    public CAPIServlet(CAPIBehavior capiBehavior) {
        this.capiBehavior = capiBehavior;
    }

    /**
     * Takes a look at the structure of the URL requested and dispatch to the right handler method
     */
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String uri = req.getRequestURI();
        String[] splitUri = getUriPieces(uri);

        if((splitUri.length == 1) && splitUri[0].equals("")) {
            handleWelcome(req, resp);
        }
        else if ((splitUri.length == 1 && splitUri[0].startsWith("_"))) {
            handleRootSpecial(req, resp, splitUri[0]);
        }
        else if (splitUri.length == 1) {
            handleDatabase(req, resp, unescapeName(splitUri[0]));
        } else if (splitUri.length == 2) {
            if (splitUri[1].equals("_bulk_docs")) {
                handleBulkDocs(req, resp, unescapeName(splitUri[0]));
            } else if (splitUri[1].equals("_revs_diff")) {
                handleRevsDiff(req, resp, unescapeName(splitUri[0]));
            } else if (splitUri[1].equals("_ensure_full_commit")) {
                handleEnsureFullCommit(req, resp,
                        unescapeName(splitUri[0]));
            } else if (splitUri[1].startsWith("_")) {
                logger.debug("Unsupported special operation {}", splitUri[1]);
            } else {
                // this must be a document id
                handleDocument(req, resp, unescapeName(splitUri[0]),
                        unescapeName(splitUri[1]));
            }
        } else if (splitUri.length == 3) {
            if (splitUri[1].equals("_local")) {
                handleLocalDocument(req, resp,
                        unescapeName(splitUri[0]), "_local/"
                                + unescapeName(splitUri[2]));
            } else {
                // attachment request
                handleAttachment(req, resp, unescapeName(splitUri[0]),
                        splitUri[1], splitUri[2]);
            }
        } else {
            if (splitUri[1].equals("_local")) {
                handleLocalAttachment(req, resp,
                        unescapeName(splitUri[0]), splitUri[2],
                        splitUri[3]);
            } else {
                logger.debug("I don't know how to handle {}", uri);
            }
        }

    }

    /**
     * Handle special operations at the root level /_...
     * @param req
     * @param resp
     * @param special
     * @throws ServletException
     * @throws IOException
     */
    protected void handleRootSpecial(HttpServletRequest req,
            HttpServletResponse resp, String special) throws ServletException,
            IOException {

        if(special.equals("_pre_replicate")) {
            logger.debug("got _pre_replicate: {}", req.getRequestURI());
            handlePreReplicate(req, resp);
            return;
        } else if(special.equals("_commit_for_checkpoint")) {
            logger.debug("got _commit_for_checkpoint: {}", req.getRequestURI());
        } else {
            logger.debug("got unknown special: {}", req.getRequestURI());
        }

        InputStream is = req.getInputStream();
        int requestLength = req.getContentLength();
        byte[] buffer = new byte[requestLength];
        IOUtils.readFully(is, buffer, 0, requestLength);

        logger.trace("root special request body was: '{}'", new String(buffer));

        sendNotFoundResponse(resp);
    }

    protected void handlePreReplicate(HttpServletRequest req,
            HttpServletResponse resp) throws ServletException, IOException {

        // read the request
        InputStream is = req.getInputStream();
        int requestLength = req.getContentLength();
        byte[] buffer = new byte[requestLength];
        IOUtils.readFully(is, buffer, 0, requestLength);

        @SuppressWarnings("unchecked")
        Map<String, Object> parsedValue = (Map<String, Object>) mapper
                .readValue(buffer, Map.class);
        logger.trace("pre replicate parsed value is " + parsedValue);

        int vbucket = (Integer)parsedValue.get("vb");
        String bucket = (String)parsedValue.get("bucket");
        String bucketUUID = (String)parsedValue.get("bucketUUID");
        String vbopaque = (String)parsedValue.get("vbopaque");
        String commitopaque = (String)parsedValue.get("commitopaque");

        String vbucketUUID = capiBehavior.getVBucketUUID("default", bucket, vbucket);

        if((vbopaque != null) && (!vbopaque.equals(vbucketUUID))) {
            logger.debug("returning 400");
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
        if((commitopaque != null) && (!commitopaque.equals(vbucketUUID))) {
            logger.debug("returning 400");
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }

        OutputStream os = resp.getOutputStream();
        resp.setContentType("application/json");
        Map<String, Object> responseMap = new HashMap<String, Object>();
        responseMap.put("vbopaque", vbucketUUID);
        mapper.writeValue(os, responseMap);
    }

    /**
     * Handle GET requests to the root URL
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    protected void handleWelcome(HttpServletRequest req,
            HttpServletResponse resp) throws ServletException,
            IOException {

        if (!req.getMethod().equals("GET")) {
            throw new UnsupportedOperationException(
                    "Only GET operations on / are supported at this time");
        }

        logger.trace("Got " + req.getMethod() + " request for /");
        OutputStream os = resp.getOutputStream();
        resp.setContentType("application/json");
        Map<String, Object> responseMap = capiBehavior.welcome();
        mapper.writeValue(os, responseMap);
    }

    /**
     * Handle GET/HEAD requests to the database URL
     *
     * @param req
     * @param resp
     * @param database
     * @throws ServletException
     * @throws IOException
     */
    protected void handleDatabase(HttpServletRequest req,
            HttpServletResponse resp, String database) throws ServletException,
            IOException {

        if (!(req.getMethod().equals("GET") || req.getMethod().equals("HEAD"))) {
            throw new UnsupportedOperationException(
                    "Only GET/HEAD operations on database are supported at this time");
        }

        logger.trace("Got " + req.getMethod() + " request for " + database);

        OutputStream os = resp.getOutputStream();

        if(capiBehavior.databaseExists(database)) {
            if (req.getMethod().equals("GET")) {
                resp.setContentType("application/json");

                Map<String, Object> responseMap = capiBehavior.getDatabaseDetails(database);
                mapper.writeValue(os, responseMap);
            }
        } else {
            sendNotFoundResponse(resp);
        }

    }

    /**
     * Handle _revs_diff by claiming we don't have any of these revisions
     *
     * @param req
     * @param resp
     * @param database
     * @throws ServletException
     * @throws IOException
     */
    protected void handleRevsDiff(HttpServletRequest req,
            HttpServletResponse resp, String database) throws ServletException,
            IOException {

        if (!req.getMethod().equals("POST")) {
            throw new UnsupportedOperationException("_revs_diff must be POST");
        }

        logger.trace("Got revs diff request for " + database);

        OutputStream os = resp.getOutputStream();
        InputStream is = req.getInputStream();

        int requestLength = req.getContentLength();
        byte[] buffer = new byte[requestLength];
        IOUtils.readFully(is, buffer, 0, requestLength);

        logger.trace("revs diff request body was {}", new String(buffer));

        @SuppressWarnings("unchecked")
        Map<String, Object> parsedValue = (Map<String, Object>) mapper
                .readValue(buffer, Map.class);

        logger.trace("revs diff parsed value is " + parsedValue);

        try {
            Map<String, Object> responseMap = capiBehavior.revsDiff(database, parsedValue);

            if(responseMap != null) {
                mapper.writeValue(os, responseMap);
            } else {
                sendNotFoundResponse(resp);
            }
        } catch (UnavailableException e) {
                sendServiceUnavailableResponse(resp, "too many concurrent requests");
        }
    }

    protected void handleEnsureFullCommit(HttpServletRequest req,
            HttpServletResponse resp, String database) throws ServletException,
            IOException {

        if (!req.getMethod().equals("POST")) {
            throw new UnsupportedOperationException(
                    "_ensure_full_commit must be POST");
        }

        logger.trace("Got ensure full commitf request for " + database);

        resp.setStatus(HttpServletResponse.SC_CREATED);
        resp.setContentType("application/json");

        if(capiBehavior.ensureFullCommit(database)) {

            Map<String, Object> responseMap = new HashMap<String, Object>();
            responseMap.put("ok", true);

            OutputStream os = resp.getOutputStream();
            mapper.writeValue(os, responseMap);
        } else {
            sendNotFoundResponse(resp);
        }
    }

    protected void handleAttachment(HttpServletRequest req,
            HttpServletResponse resp, String databaseName, String documentId,
            String attachmentName) {
        throw new UnsupportedOperationException(
                "Document attachments are not supported at this time");
    }

    protected void handleLocalAttachment(HttpServletRequest req,
            HttpServletResponse resp, String databaseName, String documentId,
            String attachemntName) {
        throw new UnsupportedOperationException(
                "Local Document attachments are not supported at this time");
    }

    protected void handleDocument(HttpServletRequest req,
            HttpServletResponse resp, String databaseName, String documentId)
            throws IOException, ServletException {
        handleDocumentInternal(req, resp, databaseName, documentId, "document");
    }

    protected void handleLocalDocument(HttpServletRequest req,
            HttpServletResponse resp, String databaseName, String documentId)
            throws IOException, ServletException {
        handleDocumentInternal(req, resp, databaseName, documentId, "_local");
    }

    protected void handleDocumentInternal(HttpServletRequest req,
            HttpServletResponse resp, String databaseName, String documentId,
            String documentType) throws IOException, ServletException {

        logger.trace(String.format(
                "Got document request in database %s document %s type %s",
                databaseName, documentId, documentType));

        if (!(req.getMethod().equals("GET") || req.getMethod().equals("HEAD") || req
                .getMethod().equals("PUT"))) {
            throw new UnsupportedOperationException(
                    "Only GET/HEAD/PUT operations on documents are supported at this time");
        }


        if (req.getMethod().equals("GET") || req.getMethod().equals("HEAD")) {

            Map<String, Object> doc = null;
            if (documentType.equals("_local")) {
                doc = capiBehavior.getLocalDocument(databaseName, documentId);
            } else {
                doc = capiBehavior.getDocument(databaseName, documentId);
            }

            if(doc != null) {
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.setContentType("application/json");
                OutputStream os = resp.getOutputStream();
                mapper.writeValue(os, doc);
            } else {
                sendNotFoundResponse(resp);
                return;
            }

        } else if (req.getMethod().equals("PUT")) {

            String rev = null;

            //read the document
            InputStream is = req.getInputStream();

            int requestLength = req.getContentLength();
            byte[] buffer = new byte[requestLength];
            IOUtils.readFully(is, buffer, 0, requestLength);

            @SuppressWarnings("unchecked")
            Map<String, Object> parsedValue = (Map<String, Object>) mapper
                    .readValue(buffer, Map.class);

            if(documentType.equals("_local)")) {
                rev = capiBehavior.storeLocalDocument(databaseName, documentId, parsedValue);
            } else {
                rev = capiBehavior.storeDocument(databaseName, documentId, parsedValue);
            }

            if(rev == null) {
                throw new ServletException("Storing document did not result in valid revision");
            }

            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.setContentType("application/json");
            OutputStream os = resp.getOutputStream();

            Map<String, Object> responseMap = new HashMap<String, Object>();
            responseMap.put("ok", true);
            responseMap.put("id", documentId);
            responseMap.put("rev", rev);
            mapper.writeValue(os, responseMap);
        }

    }

    private void sendNotFoundResponse(HttpServletResponse resp)
            throws IOException, JsonGenerationException, JsonMappingException {
        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        resp.setContentType("application/json");
        OutputStream os = resp.getOutputStream();

        Map<String, Object> responseMap = new HashMap<String, Object>();
        responseMap.put("error", "not_found");
        responseMap.put("reason", "missing");
        mapper.writeValue(os, responseMap);
    }

    private void sendServiceUnavailableResponse(HttpServletResponse resp, String reason)
            throws IOException, JsonGenerationException, JsonMappingException {
        resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        resp.setContentType("application/json");
        OutputStream os = resp.getOutputStream();

        Map<String, Object> responseMap = new HashMap<String, Object>();
        responseMap.put("error", "service_unavailable");
        responseMap.put("reason", reason);
        mapper.writeValue(os, responseMap);
    }

    protected void handleBulkDocs(HttpServletRequest req,
            HttpServletResponse resp, String database) throws ServletException,
            IOException {

        if (!req.getMethod().equals("POST")) {
            throw new UnsupportedOperationException("_bulk_docs must be POST");
        }



        logger.trace("Got bulk docs request for " + database);

        resp.setStatus(HttpServletResponse.SC_CREATED);
        resp.setContentType("application/json");

        OutputStream os = resp.getOutputStream();
        InputStream is = req.getInputStream();

        int requestLength = req.getContentLength();
        byte[] buffer = new byte[requestLength];
        IOUtils.readFully(is, buffer, 0, requestLength);

        @SuppressWarnings("unchecked")
        Map<String, Object> parsedValue = (Map<String, Object>) mapper
                .readValue(buffer, Map.class);

        logger.trace("parsed value is " + parsedValue);

        try {
            List<Object> responseList = capiBehavior.bulkDocs(database, (ArrayList<Map<String, Object>>) parsedValue.get("docs"));
            if(responseList == null) {
                sendNotFoundResponse(resp);
                return;
            }
            mapper.writeValue(os, responseList);
        } catch (UnavailableException e) {
            sendServiceUnavailableResponse(resp, "too many concurrent requests");
        }
    }

    String[] getUriPieces(String uri) {
        // remove initial /
        if (uri.startsWith("/")) {
            uri = uri.substring(1);
        }
        String[] result = uri.split("/");
        return result;
    }

    String unescapeName(String name) throws UnsupportedEncodingException {
        return URLDecoder.decode(name, "UTF-8");
    }

}
