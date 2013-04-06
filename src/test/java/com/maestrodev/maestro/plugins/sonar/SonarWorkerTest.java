/*
 * Copyright (c) 2013, MaestroDev. All rights reserved.
 */
package com.maestrodev.maestro.plugins.sonar;

import com.maestrodev.maestro.plugins.StompConnectionFactory;
import org.fusesource.stomp.client.BlockingConnection;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;

import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author David Castro <dcastro@maestrodev.com>
 */
public class SonarWorkerTest {
    private static final Logger logger = Logger.getLogger(SonarWorkerTest.class.getName());
    HashMap<String, Object> stompConfig = new HashMap<String, Object>();
    StompConnectionFactory stompConnectionFactory;
    BlockingConnection blockingConnection;

    SonarWorker worker;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        // setup the stomp config
        stompConfig.put("host", "localhost");
        stompConfig.put("port", "61613");
        stompConfig.put("queue", "test");

        // setup the mock stomp connection
        stompConnectionFactory = mock(StompConnectionFactory.class);
        blockingConnection = mock(BlockingConnection.class);
        when(stompConnectionFactory.getConnection(Matchers.anyString(),
            Matchers.anyInt())).thenReturn(blockingConnection);

        JSONObject fields = new JSONObject();
        fields.put("url", "http://localhost:9000");
        fields.put("username", "sonar");
        fields.put("password", "sonar");
        fields.put("projectKey", "test");

        JSONObject workitem = new JSONObject();
        workitem.put("fields", fields);

        worker = new SonarWorker();
        worker.setStompConnectionFactory(stompConnectionFactory);
        worker.setStompConfig(stompConfig);
        worker.setWorkitem(workitem);
    }

    /** Integration test that requires a sonar server.  Useful during testing, but not unless you have a Sonar server.
    @SuppressWarnings("unchecked")
    @Test
    public void shouldGetMetrics() throws IOException {
        // this should put key/value pairs for our metrics in the context
        worker.fetchMetricsForProject();

        // ensure the right data got populated into the __context_outputs__
        JSONObject fields = worker.getFields();
        JSONObject context = (JSONObject)fields.get(SonarWorker.CONTEXT_OUTPUTS);
        JSONObject tests = (JSONObject)context.get("tests");
        JSONObject rules = (JSONObject)context.get("rules");

        JSONArray testsList = (JSONArray)context.get("testsList");
        JSONArray rulesList = (JSONArray)context.get("rulesList");
        assertNotNull(testsList);
        assertNotNull(rulesList);

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "fields: "+fields);
            logger.log(Level.FINE, "context: "+context);
            logger.log(Level.FINE, "tests: "+tests);
            logger.log(Level.FINE, "rules: "+rules);
            logger.log(Level.FINE, "testsList: "+testsList);
            logger.log(Level.FINE, "rulesList: "+rulesList);
        }

        // verify all our metrics ended up in our context
        for (String metricName: SonarWorker.TESTS_METRIC_NAMES) {
            Object value = tests.get(metricName);
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "test metric "+(metricName!=null?metricName : "null")+"="+(value!=null?value : "null"));
            }
            assertNotNull(value);
        }
        // verify all our metrics ended up in our context
        for (String metricName: SonarWorker.RULES_METRIC_NAMES) {
            Object value = rules.get(metricName);
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "rule metric "+(metricName!=null?metricName : "null")+"="+(value!=null?value : "null"));
            }
            assertNotNull(value);
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "context: "+context);
        }
    }
    **/

}
