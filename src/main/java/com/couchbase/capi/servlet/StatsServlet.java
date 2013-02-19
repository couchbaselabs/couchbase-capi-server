package com.couchbase.capi.servlet;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.map.ObjectMapper;

import com.couchbase.capi.CAPIBehavior;
import com.couchbase.capi.CouchbaseBehavior;

@SuppressWarnings("serial")
public class StatsServlet extends HttpServlet {

    protected ObjectMapper mapper = new ObjectMapper();
    protected CouchbaseBehavior couchbaseBehavior;
    protected CAPIBehavior capiBehavior;

    public StatsServlet(CouchbaseBehavior couchbaseBehavior, CAPIBehavior capiBehavior) {
        this.couchbaseBehavior = couchbaseBehavior;
        this.capiBehavior = capiBehavior;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        Map<String,Object> couchbaseStats = couchbaseBehavior.getStats();
        Map<String, Object> capiStats = capiBehavior.getStats();

        Map<String, Object> resultMap = new HashMap<String,Object>();
        resultMap.put("couchbase", couchbaseStats);
        resultMap.put("capi", capiStats);

        OutputStream os = resp.getOutputStream();
        mapper.writeValue(os, resultMap);
    }

}
