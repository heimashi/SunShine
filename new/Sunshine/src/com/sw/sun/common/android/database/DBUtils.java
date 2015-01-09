
package com.sw.sun.common.android.database;

import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

public abstract class DBUtils {

    /**
     * 会自动加上_id这一列，并把此列设置为主键
     * 
     * @param db
     * @param tableName
     * @param columnsDefinition ，不需要有_id这一列，会自动加上
     */
    public static void createTable(final SQLiteDatabase db, final String tableName,
            final String[] columnsDefinition) {
        StringBuilder queryStr = new StringBuilder();
        queryStr.append("CREATE TABLE ");
        queryStr.append(tableName);
        queryStr.append("(");
        queryStr.append(BaseColumns._ID);
        queryStr.append(" LONG  PRIMARY KEY ,");
        queryStr.append("");
        // Add the columns now, Increase by 2
        for (int i = 0; i < (columnsDefinition.length - 1); i += 2) {
            if (i != 0) {
                queryStr.append(",");
            }
            queryStr.append(columnsDefinition[i] + " " + columnsDefinition[i + 1]);
        }
        queryStr.append(");");
        db.execSQL(queryStr.toString());
    }

    /**
     * 会自动加上_id这一列，并把此列设置为自增，主键需要自己指定
     * 
     * @param db
     * @param tableName
     * @param columnsDefinition
     * @param primaryKeyColumns 主键列，如果该数组为空，会把_id这列设置成主键
     */
    public static void createTable(final SQLiteDatabase db, final String tableName,
            final String[] columnsDefinition, final String[] primaryKeyColumns) {
        StringBuilder queryStr = new StringBuilder();
        queryStr.append("CREATE TABLE ");
        queryStr.append(tableName);
        queryStr.append("(");
        queryStr.append(BaseColumns._ID);
        queryStr.append(" LONG  AUTOINCREMENT ,");
        queryStr.append("");
        // Add the columns now, Increase by 2
        for (int i = 0; i < (columnsDefinition.length - 1); i += 2) {
            if (i != 0) {
                queryStr.append(",");
            }
            queryStr.append(columnsDefinition[i] + " " + columnsDefinition[i + 1]);
        }
        queryStr.append(", PRIMARY KEY (");
        // 设置主键
        if (primaryKeyColumns != null) {
            for (int i = 0; i < primaryKeyColumns.length - 1; i++) {
                if (i != 0) {
                    queryStr.append(",");
                }
                queryStr.append(primaryKeyColumns[i]);
            }
        } else {
            queryStr.append(BaseColumns._ID);
        }
        queryStr.append("));");
        db.execSQL(queryStr.toString());
    }

}
