
package com.sw.sun.common.android.database;

import java.util.ArrayList;

import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

import com.sw.sun.common.logger.MyLog;

public abstract class BaseSQLiteOpenHelper extends SQLiteOpenHelper {

    protected BaseSQLiteOpenHelper(Context context, String name, CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    protected BaseSQLiteOpenHelper(Context context, String name, CursorFactory factory,
            int version, DatabaseErrorHandler errorHandler) {
        super(context, name, factory, version, errorHandler);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        MyLog.e("try downgrade database " + getDatabaseName() + " from " + oldVersion + " to "
                + newVersion);
    }

    @Override
    public abstract String getDatabaseName();

    public abstract Object getDatabaseLockObject();

    public abstract ArrayList<String> getAllTablesName();

    public void dropAllTables() {
        synchronized (getDatabaseLockObject()) {
            final SQLiteDatabase db = this.getWritableDatabase();
            try {
                dropAllTables(db);
                this.onCreate(db);
            } finally {
                db.close();
            }
        }
    }

    private void dropAllTables(final SQLiteDatabase db) {
        ArrayList<String> tables = getAllTablesName();
        if (tables != null && !tables.isEmpty()) {
            try {
                db.beginTransaction();
                for (String table : tables) {
                    db.execSQL(DBConstants.DROP_TABLE_SQL + table);
                }
                db.setTransactionSuccessful();
            } catch (final SQLException ex) {
                MyLog.e("couldn't drop table in " + getDatabaseName() + " database", ex);
                throw ex;
            } finally {
                db.endTransaction();
            }
        }
    }

    public static void safeExecuteSQL(final SQLiteDatabase db, String sql) {
        try {
            db.execSQL(sql);
        } catch (final SQLException e) {
            MyLog.e(e);
        }
    }

    public static void safeCreateTable(SQLiteDatabase db, final String tableName,
            final String[] columns) {
        try {
            DBUtils.createTable(db, tableName, columns);
        } catch (final SQLException e) {
            MyLog.e(e);
        }
    }

    public static void safeCreateTable(SQLiteDatabase db, final String tableName,
            final String[] columns, final String[] primeKeyColumns) {
        try {
            DBUtils.createTable(db, tableName, columns, primeKeyColumns);
        } catch (final SQLException e) {
            MyLog.e(e);
        }
    }

}
