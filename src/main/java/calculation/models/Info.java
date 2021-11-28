package calculation.models;

import com.google.gson.JsonObject;

/**
 * Includes a method for turning classes into Json. Mainly used by BigQueryType, but could be used for anything
 */
public abstract class Info {
    public abstract JsonObject toJson();
}
