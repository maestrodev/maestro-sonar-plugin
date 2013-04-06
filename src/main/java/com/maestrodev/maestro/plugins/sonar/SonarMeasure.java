/*
 * Copyright (c) 2013, MaestroDev. All rights reserved.
 */
package com.maestrodev.maestro.plugins.sonar;

/**
 * Represents a single Sonar measurement.
 *
 * @author David Castro <dcastro@maestrodev.com>
 */
public class SonarMeasure {
    String formattedValue;
    String key;
    Object value;

    public SonarMeasure(String key, Object value, String formattedValue) {
        this.key = key;
        this.value = value;
        this.formattedValue = formattedValue;
    }

    public String getFormattedValue() {
        return formattedValue;
    }

    public void setFormattedValue(String formattedValue) {
        this.formattedValue = formattedValue;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "SonarMeasure{" +
            "key='" + key + '\'' +
            ", value=" + value +
            ", formattedValue='" + formattedValue + '\'' +
            '}';
    }
    /**
     * Example Measure
     * {
     * "description": "Density of successful unit tests",
     * "direction": 1,
     * "domain": "Tests",
     * "hidden": false,
     * "key": "test_success_density",
     * "name": "Unit tests success (%)",
     * "qualitative": true,
     * "user_managed": false,
     * "val_type": "PERCENT"
     * }
     */
}
