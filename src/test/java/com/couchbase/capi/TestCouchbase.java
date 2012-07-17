package com.couchbase.capi;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;

public class TestCouchbase extends CAPITestCase {

    public void testPools() throws Exception {
        HttpClient client = new DefaultHttpClient();

        HttpUriRequest request = new HttpGet(String.format("http://localhost:%d/pools", port));

        HttpResponse response = client.execute(request);

        Assert.assertEquals(200, response.getStatusLine().getStatusCode());

        HttpEntity entity = response.getEntity();

        Map<String,List<Map<String,Object>>> details = null;
        if (entity != null) {
            InputStream input = entity.getContent();
            try {
                details = mapper.readValue(input, Map.class);
            } finally {
                input.close();
            }
        }

        Assert.assertTrue(details.containsKey("pools"));
        Assert.assertEquals(1, details.get("pools").size());
        Assert.assertEquals("default", details.get("pools").get(0).get("name"));
        Assert.assertEquals("/pools/default", details.get("pools").get(0).get("uri"));

        client.getConnectionManager().shutdown();
    }

    public void testPool() throws Exception {
        HttpClient client = new DefaultHttpClient();

        HttpUriRequest request = new HttpGet(String.format("http://localhost:%d/pools/default", port));

        HttpResponse response = client.execute(request);

        Assert.assertEquals(200, response.getStatusLine().getStatusCode());

        HttpEntity entity = response.getEntity();

        Map<String,Map<String,Object>> details = null;
        if (entity != null) {
            InputStream input = entity.getContent();
            try {
                details = mapper.readValue(input, Map.class);
            } finally {
                input.close();
            }
        }

        Assert.assertTrue(details.containsKey("buckets"));
        Assert.assertTrue(details.get("buckets").containsKey("uri"));
        Assert.assertEquals("/pools/default/buckets", details.get("buckets").get("uri"));

        client.getConnectionManager().shutdown();
    }

    public void testPoolBuckets() throws Exception {
        HttpClient client = new DefaultHttpClient();

        HttpUriRequest request = new HttpGet(String.format("http://localhost:%d/pools/default/buckets", port));

        HttpResponse response = client.execute(request);

        Assert.assertEquals(200, response.getStatusLine().getStatusCode());

        HttpEntity entity = response.getEntity();

        List<Map<String,Object>> responseObject = null;
        if (entity != null) {
            InputStream input = entity.getContent();
            try {
                responseObject = mapper.readValue(input, List.class);
            } finally {
                input.close();
            }
        }

        Assert.assertEquals(1, responseObject.size());
        Assert.assertEquals("default", responseObject.get(0).get("name"));
        Assert.assertEquals("/pools/default/buckets/default", responseObject.get(0).get("uri"));



        client.getConnectionManager().shutdown();
    }

    public void testPoolBucketDetails() throws Exception {
        HttpClient client = new DefaultHttpClient();

        HttpUriRequest request = new HttpGet(String.format("http://localhost:%d/pools/default/buckets/default", port));

        HttpResponse response = client.execute(request);

        Assert.assertEquals(200, response.getStatusLine().getStatusCode());

        HttpEntity entity = response.getEntity();

        Map<String,Object> bucketDetails = null;
        if (entity != null) {
            InputStream input = entity.getContent();
            try {
                bucketDetails = mapper.readValue(input, Map.class);
            } finally {
                input.close();
            }
        }

        Assert.assertEquals("membase", bucketDetails.get("bucketType"));

        List<Object> nodes = (List<Object>)bucketDetails.get("nodes");
        Assert.assertEquals(2, nodes.size());

        Map<String,Object> serverMap = (Map<String,Object>)bucketDetails.get("vBucketServerMap");
        List<Object> servers = (List<Object>)serverMap.get("serverList");
        Assert.assertEquals(2, servers.size());
        List<Object> vbuckets = (List<Object>)serverMap.get("vBucketMap");
        Assert.assertEquals(1024, vbuckets.size());

        client.getConnectionManager().shutdown();
    }

}
