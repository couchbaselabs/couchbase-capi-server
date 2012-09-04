package com.couchbase.capi;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;

public class TestCAPI extends CAPITestCase {



    public void testDatabaseHead() throws Exception {
        HttpClient client = getClient();

        HttpUriRequest request = new HttpHead(String.format("http://localhost:%d/default", port));
        HttpResponse response = client.execute(request);

        Assert.assertEquals(200, response.getStatusLine().getStatusCode());


    }

    public void testDatabaseHeadDoesNotExist() throws Exception {
        HttpClient client = getClient();

        HttpUriRequest request = new HttpHead(String.format("http://localhost:%d/doesnotexist", port));
        HttpResponse response = client.execute(request);

        Assert.assertEquals(404, response.getStatusLine().getStatusCode());
    }

    public void testDatabaseGet() throws Exception {

        HttpClient client = getClient();

        HttpUriRequest request = new HttpGet(String.format("http://localhost:%d/default", port));
        HttpResponse response = client.execute(request);

        Assert.assertEquals(200, response.getStatusLine().getStatusCode());

        HttpEntity entity = response.getEntity();

        Map<String,Object> details = null;
        if (entity != null) {
            InputStream input = entity.getContent();
            try {
                details = mapper.readValue(input, Map.class);
            } finally {
                input.close();
            }
        }

        Assert.assertEquals("default", details.get("db_name"));
    }

    public void testDatabaseGetDoesNotExist() throws Exception {

        HttpClient client = getClient();

        HttpUriRequest request = new HttpGet(String.format("http://localhost:%d/doesnotexist", port));
        HttpResponse response = client.execute(request);

        Assert.assertEquals(404, response.getStatusLine().getStatusCode());
    }

    public void testEnsureFullCommit() throws Exception {

        HttpClient client = getClient();

        HttpUriRequest request = new HttpPost(String.format("http://localhost:%d/default/_ensure_full_commit", port));
        HttpResponse response = client.execute(request);

        Assert.assertEquals(201, response.getStatusLine().getStatusCode());
    }

    public void testEnsureFullCommitDoesNotExist() throws Exception {

        HttpClient client = getClient();

        HttpUriRequest request = new HttpPost(String.format("http://localhost:%d/doesnotexist/_ensure_full_commit", port));
        HttpResponse response = client.execute(request);

        Assert.assertEquals(404, response.getStatusLine().getStatusCode());
    }

    public void testRevsDiff() throws Exception {

        HttpClient client = getClient();

        HttpPost request = new HttpPost(String.format("http://localhost:%d/default/_revs_diff", port));

        List<String> revs = new ArrayList<String>();
        revs.add("1-abc");
        revs.add("2-def");
        Map<String,Object> revsDiff = new HashMap<String,Object>();
        revsDiff.put("12345", revs);

        request.setEntity(new ByteArrayEntity(mapper.writeValueAsBytes(revsDiff)));
        HttpResponse response = client.execute(request);

        HttpEntity entity = response.getEntity();
        Map<String,Map<String, List<Object>>> details = null;
        if (entity != null) {
            InputStream input = entity.getContent();
            try {
                details = mapper.readValue(input, Map.class);
            } finally {
                input.close();
            }
        }

        Assert.assertTrue(details.containsKey("12345"));
        Assert.assertTrue(details.get("12345").containsKey("missing"));
        Assert.assertEquals(2, details.get("12345").get("missing").size());
        Assert.assertTrue(details.get("12345").get("missing").contains("1-abc"));
        Assert.assertTrue(details.get("12345").get("missing").contains("2-def"));
    }

    public void testRevsDiffDoesNotExist() throws Exception {
        HttpClient client = getClient();

        HttpPost request = new HttpPost(String.format("http://localhost:%d/doesnotexist/_revs_diff", port));

        List<String> revs = new ArrayList<String>();
        revs.add("1-abc");
        revs.add("2-def");
        Map<String,Object> revsDiff = new HashMap<String,Object>();
        revsDiff.put("12345", revs);

        request.setEntity(new ByteArrayEntity(mapper.writeValueAsBytes(revsDiff)));
        HttpResponse response = client.execute(request);

        Assert.assertEquals(404, response.getStatusLine().getStatusCode());
    }

    public void testBulkDocs() throws Exception {

        HttpClient client = getClient();

        HttpPost request = new HttpPost(String.format("http://localhost:%d/default/_bulk_docs", port));

        Map<String, Object> doc = new HashMap<String, Object>();
        doc.put("_id", "abcdef");
        doc.put("_rev", "1-xyz");

        Map<String, Object> doc2 = new HashMap<String, Object>();
        doc2.put("_id", "ghijkl");
        doc2.put("_rev", "1-pdr");

        List<Object> docs = new ArrayList<Object>();
        docs.add(doc);
        docs.add(doc2);

        Map<String, Object> bulkDocs = new HashMap<String, Object>();
        bulkDocs.put("docs", docs);

        request.setEntity(new ByteArrayEntity(mapper.writeValueAsBytes(bulkDocs)));
        HttpResponse response = client.execute(request);

        HttpEntity entity = response.getEntity();
        List<Map<String, Object>> details = null;
        if (entity != null) {
            InputStream input = entity.getContent();
            try {
                details = mapper.readValue(input, List.class);
            } finally {
                input.close();
            }
        }

        Assert.assertEquals(2, details.size());
        Assert.assertEquals("abcdef", details.get(0).get("id"));
        Assert.assertEquals("1-xyz", details.get(0).get("rev"));
        Assert.assertEquals("ghijkl", details.get(1).get("id"));
        Assert.assertEquals("1-pdr", details.get(1).get("rev"));
    }

    public void testBulkDocsDoesNotExist() throws Exception {
        HttpClient client = getClient();

        HttpPost request = new HttpPost(String.format("http://localhost:%d/doesnotexist/_bulk_docs", port));

        Map<String, Object> doc = new HashMap<String, Object>();
        doc.put("_id", "abcdef");
        doc.put("_rev", "1-xyz");

        Map<String, Object> doc2 = new HashMap<String, Object>();
        doc2.put("_id", "ghijkl");
        doc2.put("_rev", "1-pdr");

        List<Object> docs = new ArrayList<Object>();
        docs.add(doc);
        docs.add(doc2);

        Map<String, Object> bulkDocs = new HashMap<String, Object>();
        bulkDocs.put("docs", docs);

        request.setEntity(new ByteArrayEntity(mapper.writeValueAsBytes(bulkDocs)));
        HttpResponse response = client.execute(request);

        Assert.assertEquals(404, response.getStatusLine().getStatusCode());
    }

    public void testGetDocument() throws Exception {
        HttpClient client = getClient();

        HttpUriRequest request = new HttpGet(String.format("http://localhost:%d/default/docid", port));
        HttpResponse response = client.execute(request);

        Assert.assertEquals(200, response.getStatusLine().getStatusCode());

        HttpEntity entity = response.getEntity();

        Map<String,Object> details = null;
        if (entity != null) {
            InputStream input = entity.getContent();
            try {
                details = mapper.readValue(input, Map.class);
            } finally {
                input.close();
            }
        }

        Assert.assertEquals("docid", details.get("_id"));
        Assert.assertEquals("1-abc", details.get("_rev"));
        Assert.assertEquals("test", details.get("value"));
    }

    public void testGetDocumentDatabaseDoesNotExist() throws Exception {
        HttpClient client = getClient();

        HttpUriRequest request = new HttpGet(String.format("http://localhost:%d/doesnotexist/docid", port));
        HttpResponse response = client.execute(request);

        Assert.assertEquals(404, response.getStatusLine().getStatusCode());
    }

    public void testGetDocumentDocumentDoesNotExist() throws Exception {
        HttpClient client = getClient();

        HttpUriRequest request = new HttpGet(String.format("http://localhost:%d/default/doesnotexist", port));
        HttpResponse response = client.execute(request);

        Assert.assertEquals(404, response.getStatusLine().getStatusCode());
    }

    public void testGetLocalDocument() throws Exception {
        HttpClient client = getClient();

        HttpUriRequest request = new HttpGet(String.format("http://localhost:%d/default/_local/docid", port));
        HttpResponse response = client.execute(request);

        Assert.assertEquals(200, response.getStatusLine().getStatusCode());

        HttpEntity entity = response.getEntity();

        Map<String,Object> details = null;
        if (entity != null) {
            InputStream input = entity.getContent();
            try {
                details = mapper.readValue(input, Map.class);
            } finally {
                input.close();
            }
        }

        Assert.assertEquals("_local/docid", details.get("_id"));
        Assert.assertEquals("1-abc", details.get("_rev"));
        Assert.assertEquals("test", details.get("value"));
    }

    public void testGetLocalDocumentDatabaseDoesNotExist() throws Exception {
        HttpClient client = getClient();

        HttpUriRequest request = new HttpGet(String.format("http://localhost:%d/doesnotexist/_local/docid", port));
        HttpResponse response = client.execute(request);

        Assert.assertEquals(404, response.getStatusLine().getStatusCode());
    }

    public void testGetLocalDocumentDocumentDoesNotExist() throws Exception {
        HttpClient client = getClient();

        HttpUriRequest request = new HttpGet(String.format("http://localhost:%d/default/_local/doesnotexist", port));
        HttpResponse response = client.execute(request);

        Assert.assertEquals(404, response.getStatusLine().getStatusCode());
    }

    public void testActualGetCheckpointDocument() throws Exception {
        HttpClient client = getClient();

        HttpUriRequest request = new HttpGet(String.format("http://localhost:%d/default/_local/441-0921e80de6603d60b1d553bb7c253def%%2Fbeer-sample%%2Fbeer-sample", port));
        HttpResponse response = client.execute(request);

        Assert.assertEquals(200, response.getStatusLine().getStatusCode());
    }
}
