package org.idb.cacao.validator.utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * Utils for JSON files
 *
 * @author Leon Silva
 */
public class JSONUtils {
    /**
     * Tests wether JSON content in string is valid
     *
     * @param test the string with JSON content
     * @return true if JSON is valid, false otherwise
     */
    public static Boolean isJSONValid(String test) {
        try {
            new JSONObject(test);
        } catch (JSONException ex) {
            try {
                new JSONArray(test);
            } catch (JSONException ex1) {
                return false;
            }
        }

        return true;
    }
}
