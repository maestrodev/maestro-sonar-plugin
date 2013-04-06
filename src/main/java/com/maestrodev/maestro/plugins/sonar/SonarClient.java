/*
 * Copyright (c) 2013, MaestroDev. All rights reserved.
 */
package com.maestrodev.maestro.plugins.sonar;

import org.apache.commons.codec.binary.Base64;
import us.monoid.json.JSONArray;
import us.monoid.json.JSONObject;
import us.monoid.web.JSONResource;
import us.monoid.web.Resty;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Client used to connect to a Sonar server and retrieve metrics and metadata.
 *
 * @author David Castro <dcastro@maestrodev.com>
 */
public class SonarClient {
    private static final Logger logger = Logger.getLogger(SonarClient.class.getName());
    private static String JSON_MEASUREMENTS = "msr";
    private static String JSON_MEASUREMENT_KEY = "key";
    private static String JSON_MEASUREMENT_VALUE = "val";
    private static String JSON_MEASUREMENT_FORMAT_VALUE = "frmt_val";

    private static String JSON_MEASUREMENT_META_KEY = "key";

    private String baseUrl;
    private String username;
    private String password;
    private Resty resty;

    /**
     * Create a new SonarClient to use for connecting to Sonar
     *
     * @param baseUrl The base URL for the target Sonar server (e.g. http://localhost:9000)
     */
    public SonarClient(String baseUrl) {
        this.baseUrl = baseUrl;
        resty = new Resty();
    }

    /**
     * Create a new SonarClient to use for connecting to Sonar with authentication.  Use the alternate constructor
     * if you are connecting un-authenticated and your Sonar server supports it.
     *
     * @param baseUrl  The base URL for the target Sonar server (e.g. http://localhost:9000)
     * @param username The Sonar username
     * @param password The Sonar password
     */
    public SonarClient(String baseUrl, String username, String password) {
        this.baseUrl = baseUrl;
        this.username = username;
        this.password = password;
        resty = new Resty();

        try {
            String encodedString = Base64.encodeBase64((username + ":" + password).getBytes("UTF-8")).toString();
            resty.withHeader("Authorization", "Basic " + encodedString);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Problem setting the authorization header for the Sonar client");
        }
    }

    /**
     * Get the measurement metadata for the specified project key
     *
     * @param projectKey The Sonar project key or id
     * @return A map of metric names to metadata for the metric
     * @throws Exception Whenever things hit the fan (failure to connect to Sonar, auth issues, processing badness, etc.)
     */
    public Map<String, SonarMeasureMeta> getMeasureMeta(String projectKey) throws Exception {
        String messageSuffix = String.format(" for sonar project '%s' with username '%s' on server '%s'", projectKey, username, baseUrl);

        try {
            String url = baseUrl + "/api/metrics?resource=" + projectKey;
            logger.log(Level.INFO, "requesting metrics meta from url " + url + messageSuffix);

            JSONResource resource = resty.json(url);
            JSONArray metas = resource.array();

            // create measure objects for all the returned json data
            HashMap<String, SonarMeasureMeta> measureMetas = new HashMap<String, SonarMeasureMeta>();
            for (int i = 0; i < metas.length(); i++) {
                JSONObject tmp = metas.getJSONObject(i);
                logger.log(Level.FINE, "metadata is " + tmp + messageSuffix);

                // fetch all the key/value pairs out of the response and shove them into our object
                SonarMeasureMeta meta = new SonarMeasureMeta();
                Iterator<String> j = tmp.keys();
                while (j.hasNext()) {
                    String key = j.next();
                    Object value = tmp.get(key);
                    meta.put(key, value);
                }

                // add it by key into our map of metas
                measureMetas.put(tmp.getString(JSON_MEASUREMENT_META_KEY), meta);
            }

            return measureMetas;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "There was an error retrieving measurement metas from sonar", e);
            throw new Exception("There was an error retrieving measurement metas from sonar", e);
        }
    }

    /**
     * Get measures for the specified project and keys
     *
     * @param projectKey  the name of the project or the project ID
     * @param measureKeys the key names for the measurements to request for the project
     * @return
     * @throws Exception
     * @throws IOException
     */
    public List<SonarMeasure> getMeasures(String projectKey, String... measureKeys) throws Exception {
        String messageSuffix = String.format(" for sonar project '%s' with username '%s' on server '%s'", projectKey, username, baseUrl);

        String s = "";
        for (int i = 0; i < measureKeys.length; i++) {
            s += measureKeys[i];
            if (i + 1 < measureKeys.length) {
                s += ",";
            }
        }

        try {
            String url = baseUrl + "/api/resources?resource=" + projectKey + "&metrics=" + s;
            logger.log(Level.INFO, "requesting metrics from url " + url + messageSuffix);

            JSONResource resource = resty.json(url);
            JSONArray metrics = resource.array();
            JSONObject o = metrics.getJSONObject(0);
            JSONArray msr = o.getJSONArray(JSON_MEASUREMENTS);

            // create measure objects for all the returned json data
            ArrayList<SonarMeasure> measures = new ArrayList();
            for (int i = 0; i < msr.length(); i++) {
                JSONObject tmp = msr.getJSONObject(i);
                SonarMeasure measure = new SonarMeasure(tmp.getString(JSON_MEASUREMENT_KEY), tmp.get(JSON_MEASUREMENT_VALUE), tmp.getString(JSON_MEASUREMENT_FORMAT_VALUE));
                measures.add(measure);
            }

            return measures;
        } catch (Exception e) {
            throw new Exception("There was an error retrieving measurements from sonar", e);
        }
    }

    /** Example Data
     Metrics Meta
     [
     {
     "description": "Lines",
     "direction": -1,
     "domain": "Size",
     "hidden": false,
     "key": "lines",
     "name": "Lines",
     "qualitative": false,
     "user_managed": false,
     "val_type": "INT"
     },
     ...
     ]

     Metrics
     [
     {
     "date":"2013-04-01T18:31:54-0700",
     "description":"Some Description",
     "id":1,
     "key":"com.acme:acme",
     "lang":"java",
     "lname":"acme",
     "msr":[
     {
     "frmt_val":"2",
     "key":"tests",
     "val":2.0
     },
     {
     "frmt_val":"997 ms",
     "key":"test_execution_time",
     "val":997.0
     },
     {
     "frmt_val":"0",
     "key":"test_errors",
     "val":0.0
     },
     {
     "frmt_val":"1",
     "key":"test_failures",
     "val":1.0
     },
     {
     "frmt_val":"50.0%",
     "key":"test_success_density",
     "val":50.0
     },
     {
     "frmt_val":"10.1%",
     "key":"coverage",
     "val":10.1
     },
     {
     "frmt_val":"82.6%",
     "key":"violations_density",
     "val":82.6
     },
     {
     "frmt_val":"120",
     "key":"violations",
     "val":120.0
     }
     ],
     "name":"acme",
     "qualifier":"TRK",
     "scope":"PRJ",
     "version":"0.1.3-SNAPSHOT"
     }
     ]
     **/

}
