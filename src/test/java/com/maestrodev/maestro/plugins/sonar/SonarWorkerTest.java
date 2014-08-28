/*
 * Copyright (c) 2013, MaestroDev. All rights reserved.
 */
package com.maestrodev.maestro.plugins.sonar;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.fusesource.stomp.client.BlockingConnection;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;

import us.monoid.web.JSONResource;
import us.monoid.web.Resty;

import com.maestrodev.maestro.plugins.StompConnectionFactory;

/**
 * @author David Castro <dcastro@maestrodev.com>
 */
public class SonarWorkerTest {

    private static final Logger logger = Logger.getLogger(SonarWorkerTest.class.getName());

    private HashMap<String, Object> stompConfig = new HashMap<String, Object>();
    private StompConnectionFactory stompConnectionFactory;
    private BlockingConnection blockingConnection;

    private SonarWorker worker;

    private Resty restyMock;
    private String url = "http://nemo.sonarqube.org";

    @Before
    public void setUp() throws Exception {
        // setup the stomp config
        stompConfig.put("host", "localhost");
        stompConfig.put("port", "61613");
        stompConfig.put("queue", "test");

        // setup the mock stomp connection
        stompConnectionFactory = mock(StompConnectionFactory.class);
        blockingConnection = mock(BlockingConnection.class);
        when(stompConnectionFactory.getConnection(Matchers.anyString(), Matchers.anyInt())).thenReturn(blockingConnection);

        JSONObject fields = new JSONObject();
        fields.put("url", url);
        fields.put("username", null);
        fields.put("password", null);
        fields.put("projectKey", "org.apache.commons:commons-lang3");

        JSONObject workitem = new JSONObject();
        workitem.put("fields", fields);

        worker = new SonarWorker();
        worker.setStompConnectionFactory(stompConnectionFactory);
        worker.setStompConfig(stompConfig);
        worker.setWorkitem(workitem);

        restyMock = mock(Resty.class);
        worker.getSonarClient().resty = restyMock;
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldGetMetrics() throws IOException {

        // mock the responses
        when(restyMock.json(url + "/api/metrics?resource=org.apache.commons:commons-lang3")).thenReturn(
                new JSONResourceMock("/metrics.json"));
        when(
                restyMock
                        .json(url
                                + "/api/resources?resource=org.apache.commons:commons-lang3&metrics=coverage,branch_coverage,line_coverage,test_success_density,tests,test_failures,test_errors,test_execution_time"))
                .thenReturn(new JSONResourceMock("/resources1.json"));
        when(
                restyMock
                        .json(url
                                + "/api/resources?resource=org.apache.commons:commons-lang3&metrics=violations_density,violations,blocker_violations,critical_violations,major_violations,minor_violations,info_violations"))
                .thenReturn(new JSONResourceMock("/resources2.json"));

        // this should put key/value pairs for our metrics in the context
        worker.fetchMetricsForProject();

        // ensure the right data got populated into the __context_outputs__
        Map<String, Object> fields = worker.getFields();
        Map<String, Object> context = (Map<String, Object>) fields.get(SonarWorker.CONTEXT_OUTPUTS);
        Map<String, Object> tests = (Map<String, Object>) context.get("tests");
        Map<String, Object> rules = (Map<String, Object>) context.get("rules");

        Object testsList = context.get("testsList");
        Object rulesList = context.get("rulesList");
        assertNotNull(testsList);
        assertNotNull(rulesList);

        logger.info("fields: " + fields);
        logger.info("context: " + context);
        logger.info("tests: " + tests);
        logger.info("rules: " + rules);
        logger.info("testsList: " + testsList);
        logger.info("rulesList: " + rulesList);

        // verify all our metrics ended up in our context
        for (String metricName : SonarWorker.TESTS_METRIC_NAMES) {
            Object value = tests.get(metricName);
            logger.info("test metric " + (metricName != null ? metricName : "null") + "=" + (value != null ? value : "null"));
            assertNotNull(value);
        }
        // verify all our metrics ended up in our context
        for (String metricName : SonarWorker.RULES_METRIC_NAMES) {
            Object value = rules.get(metricName);
            logger.info("rule metric " + (metricName != null ? metricName : "null") + "=" + (value != null ? value : "null"));
            assertNotNull(value);
        }

        logger.info("context: " + context);
    }

    private class JSONResourceMock extends JSONResource {
        public JSONResourceMock(String file) {
            inputStream = SonarWorkerTest.class.getResourceAsStream(file);
        }
    }
}
