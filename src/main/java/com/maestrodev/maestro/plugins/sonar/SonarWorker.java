/*
 * Copyright (c) 2013, MaestroDev. All rights reserved.
 */
package com.maestrodev.maestro.plugins.sonar;

import com.maestrodev.maestro.plugins.MaestroWorker;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is the main worker class of the maestro sonar plugin. It is used to
 * fetch measurements from a Sonar server.
 *
 * @author David Castro <dcastro@maestrodev.com>
 */
public class SonarWorker extends MaestroWorker {

    private static final Logger logger = Logger.getLogger(SonarWorker.class.getName());
    static final String CONTEXT_OUTPUTS = "__context_outputs__";
    private SonarClient client;

    public static String[] TESTS_METRIC_NAMES = new String[]{
        "coverage", "branch_coverage", "line_coverage",
        "test_success_density", "tests", "test_failures", "test_errors", "test_execution_time"
    };
    public static String[] RULES_METRIC_NAMES = new String[]{
        "violations_density", "violations", "blocker_violations", "critical_violations",
        "major_violations", "minor_violations", "info_violations"
    };

    /**
     * Default constructor.
     */
    public SonarWorker() {

    }

    /**
     * Instantiates a new Sonar client from url, username, pass passed in by the work item
     * fields. URL example: http://localhost:9000
     *
     * @return a Sonar instance.
     */
    protected SonarClient getSonarClient() {
        if (client == null) {
            String url = getField("url");
            String username = getField("username");
            String password = getField("password");
    
            client = new SonarClient(url, username, password);
        }
        return client;
    }

    /**
     * Fetches measures for the project key passed in by the work item and processes them all into the context for
     * Maestro to store and use.
     */
    public void fetchMetricsForProject() {
        String projectKey = getField("projectKey");
        String url = validateUrl(getField("url"));
        String username = getField("username");
        String messageSuffix = String.format(" for sonar project '%s' with username '%s' on server '%s'", projectKey, username, url);

        try {
            JSONObject context = getContext();

            logger.log(Level.INFO, "getting sonar client" + messageSuffix);
            SonarClient client = getSonarClient();

            // fetch all the metadata for the measures
            logger.log(Level.INFO, "fetching metrics meta" + messageSuffix);
            Map<String, SonarMeasureMeta> metas = client.getMeasureMeta(projectKey);

            logger.log(Level.INFO, "fetching metrics" + messageSuffix);
            List<SonarMeasure> testsMeasures = client.getMeasures(projectKey, TESTS_METRIC_NAMES);
            List<SonarMeasure> rulesMeasures = client.getMeasures(projectKey, RULES_METRIC_NAMES);

            /**
             * Get all the values and push them into the context.  We want them to be organized like so:
             *
             * context : {
             *   projectKey: "test",
             *   projectLink: "http://localhost:9000/dashboard/index/test",
             *   testsList: [branch_coverage, ...],
             *   tests: {
             *     branch_coverage: {
             *       name: "Branch coverage",
             *       value: 100,
             *       formattedValue: "100%",
             *       val_type: "PERCENT"
             *     }
             *   },
             *   rulesList: [major_violations, ...],
             *   rules: {
             *     major_violations: {
             *       name: "Major violations",
             *       value: 80,
             *       formattedValue: "80",
             *       val_type: "INT"
             *     }
             *   }
             * }
             */
            JSONObject tests = processMeasures(metas, testsMeasures);
            JSONObject rules = processMeasures(metas, rulesMeasures);

            context.put("projectKey", projectKey);
            context.put("projectLink", url + "/dashboard/index/" + projectKey);
            context.put("tests", tests);
            context.put("rules", rules);

            // add the list of tests for ordering
            JSONArray testsList = new JSONArray();
            for (String test : SonarWorker.TESTS_METRIC_NAMES) {
                testsList.add(test);
            }
            context.put("testsList", testsList);

            // add the list of rules for ordering
            JSONArray rulesList = new JSONArray();
            for (String rule : SonarWorker.RULES_METRIC_NAMES) {
                rulesList.add(rule);
            }
            context.put("rulesList", rulesList);

            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, "putting metrics into context_outputs for maestro to pick up " + context + messageSuffix);
            }

            setField(CONTEXT_OUTPUTS, context);
        } catch (Throwable e) {
            logger.log(Level.WARNING, "Error retrieving metrics" + messageSuffix, e);
            String err = e.getMessage() + "\n" + ExceptionUtils.getStackTrace(e);
            setError("Error retrieving metrics" + messageSuffix + ":\n" + err);
        }
    }

    /**
     * Take the metrics data and metadata and adds the proper objects into the context, so that Maestro
     * can save them for display in the UI
     *
     * @param metas    The metrics metadata
     * @param measures The measurements for a Sonar domain we want to process into the context
     * @return An object that represents all metrics for a Sonar domain we want to send back
     */
    private JSONObject processMeasures(Map<String, SonarMeasureMeta> metas, List<SonarMeasure> measures) {
        JSONObject domainObject = new JSONObject();
        for (SonarMeasure m : measures) {
            // the meta data for sonar metrics
            SonarMeasureMeta meta = metas.get(m.getKey());
            JSONObject o = new JSONObject();
            o.put(SonarMeasureMeta.MEASURE_NAME_KEY, meta.get(SonarMeasureMeta.MEASURE_NAME_KEY));
            o.put(SonarMeasureMeta.MEASURE_VALUE_TYPE_KEY, meta.get(SonarMeasureMeta.MEASURE_VALUE_TYPE_KEY));
            o.put("value", m.getValue());
            o.put("formattedValue", m.getFormattedValue());
            domainObject.put(m.getKey(), o);
        }

        return domainObject;
    }

    /**
     * Simple helper to get the context object, which is where we put all of our output data
     *
     * @return The context object
     */
    private JSONObject getContext() {
        JSONObject outputData = (JSONObject) getFields().get(CONTEXT_OUTPUTS);
        if (outputData == null) {
            outputData = new JSONObject();
        }
        return outputData;
    }

    /**
     * Process URL for correct form and fix some common issues for convenience to the end user
     *
     * @param url The URL to process
     * @return The sanitized URL
     */
    private String validateUrl(String url) throws RuntimeException {
        if (url == null) {
            setError("The Sonar URL must not be empty");
            throw new RuntimeException("The Sonar URL must not be empty");
        }

        // let's first strip off any trailing slash on the URL
        if (url.endsWith("/")) {
            url = url.substring(0, url.length()-1);
        }

        return url;
    }
}
