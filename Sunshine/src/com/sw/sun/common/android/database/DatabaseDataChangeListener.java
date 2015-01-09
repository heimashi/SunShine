
package com.sw.sun.common.android.database;

import java.util.HashSet;

public interface DatabaseDataChangeListener {

    public static final int DATA_CHANGE_ADDED = 1;

    public static final int DATA_CHANGE_UPDATED = 2;

    public static final int DATA_CHANGE_DELETED = 3;

    public void onDatabaseDataChanged(int type, HashSet<String> changedIdSet);

}
