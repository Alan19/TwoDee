package util;

import com.google.gson.JsonObject;

public class GsonHelper {
    public static boolean getAsBoolean(JsonObject jsonObject, String field, boolean defaultValue) {
        if (jsonObject.has(field)) {
            return jsonObject.getAsJsonPrimitive(field).getAsBoolean();
        }
        return defaultValue;
    }

    public static String getAsString(JsonObject jsonObject, String field, String defaultValue) {
        if (jsonObject.has(field)) {
            return jsonObject.getAsJsonPrimitive(field).getAsString();
        }
        return defaultValue;
    }
}
