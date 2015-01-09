
package com.sw.sun.database;

import java.util.ArrayList;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.sw.sun.common.android.GlobalData;
import com.sw.sun.common.android.database.BaseSQLiteOpenHelper;
import com.sw.sun.common.android.database.DBConstants;
import com.sw.sun.common.logger.MyLog;
import com.sw.sun.database.dao.MessageDao;

public class MessageDbOpenHelper extends BaseSQLiteOpenHelper {

    private static final String DATABASE_NAME = "sms.db";

    private static int DATABASE_VERSION = 1;

    private static final String TABLE_MESSAGE = "message";

    private static final String[] COLUMNS_MESSAGE = {
            MessageDao.SESSION_ID, DBConstants.TEXT, MessageDao.BODY_TYPE,
            DBConstants.INTEGER_DEAULT + MessageDao.BODY_TYPE_COMMENT, MessageDao.SENT_TIME,
            DBConstants.ZERO_BASED_LONG, MessageDao.RECEIVED_TIME, DBConstants.ZERO_BASED_LONG,
            MessageDao.INBOUND_STATUS, DBConstants.INTEGER_DEAULT + MessageDao.STATUS_NOT_INBOUNT,
            MessageDao.OUTBOUND_STATUS,
            DBConstants.INTEGER_DEAULT + MessageDao.OUTBOUND_STATUS_NOT_SET, MessageDao.TARGET,
            DBConstants.ZERO_BASED_LONG, MessageDao.M_SEQ, DBConstants.ZERO_BASED_LONG,
            MessageDao.SESSION_CONTENT, DBConstants.TEXT, MessageDao.COMMENT_CONTENT,
            DBConstants.TEXT
    };

    private static MessageDbOpenHelper sInstance = new MessageDbOpenHelper(GlobalData.app());

    private final Object mDatabaseLock = new Object();

    public static String getMessageTableName() {
        return TABLE_MESSAGE;
    }

    public static MessageDbOpenHelper getInstance() {
        return sInstance;
    }

    private MessageDbOpenHelper(final Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public String getDatabaseName() {
        return DATABASE_NAME;
    }

    @Override
    public Object getDatabaseLockObject() {
        return mDatabaseLock;
    }

    @Override
    public ArrayList<String> getAllTablesName() {
        ArrayList<String> tables = new ArrayList<String>();
        tables.add(TABLE_MESSAGE);
        return tables;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        synchronized (mDatabaseLock) {
            try {
                db.beginTransaction();
                safeCreateTable(db, TABLE_MESSAGE, COLUMNS_MESSAGE);
                // 创建索引
                safeExecuteSQL(db, "CREATE INDEX IF NOT EXISTS SINGLE_INDEX_1 ON " + TABLE_MESSAGE
                        + "(" + MessageDao.SESSION_ID + ")");
                safeExecuteSQL(db, "CREATE INDEX IF NOT EXISTS SINGLE_INDEX_2 ON " + TABLE_MESSAGE
                        + "(" + MessageDao.TARGET + ")");
                // safeExecuteSQL(db,
                // "CREATE INDEX IF NOT EXISTS GROUP_INDEX_1 ON "
                // + TABLE_MESSAGE + "(" + MessageDao.MULTI_SENDER_ID
                // + ", " + MessageDao.SENT_TIME + ")");
                db.setTransactionSuccessful();
            } catch (final SQLException e) {
                MyLog.e(e);
            } finally {
                db.endTransaction();
            }
        }

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        synchronized (mDatabaseLock) {
            // TODO
        }
    }

}
