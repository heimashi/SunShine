
package com.sw.sun.common.android.database;

import org.json.JSONObject;

public interface JSONable {

    public String toJSONString();

    public JSONObject toJSONObject();

    public boolean parseJSONString(String jsonStr);

}
