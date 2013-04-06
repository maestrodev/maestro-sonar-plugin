/*
 * Copyright (c) 2013, MaestroDev. All rights reserved.
 */
package com.maestrodev.maestro.plugins.sonar;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents Sonar metadata.
 *
 * @author David Castro <dcastro@maestrodev.com>
 */
public class SonarMeasureMeta extends HashMap implements Map {
    public static String MEASURE_DESCRIPTION_KEY = "description";
    public static String MEASURE_DIRECTION_KEY = "direction";
    public static String MEASURE_DOMAIN_KEY = "domain";
    public static String MEASURE_HIDDEN_KEY = "hidden";
    public static String MEASURE_KEY_KEY = "key";
    public static String MEASURE_NAME_KEY = "name";
    public static String MEASURE_QUALITATIVE_KEY = "qualitative";
    public static String MEASURE_USER_MANAGED_KEY = "user_managed";
    public static String MEASURE_VALUE_TYPE_KEY = "val_type";

    Map<String, Object> props = new HashMap<String, Object>();

    /**
     * Example Measure Meta
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
     */
}
