
package com.sw.sun.common.android.database;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

import com.sw.sun.common.logger.MyLog;

/**
 * 一个默认的Dao实现，继承此类的子类建议采用单例模式
 * 
 * @author kevin
 */
public abstract class AbstractDaoImpl implements Dao<ContentValuesable> {

    protected final List<DatabaseDataChangeListener> mDataChangeListenerList;

    public abstract String getTableName();

    public abstract SQLiteDatabase getWritableDatabase();

    public abstract SQLiteDatabase getReadableDatabase();

    protected AbstractDaoImpl() {
        mDataChangeListenerList = new ArrayList<DatabaseDataChangeListener>();
    }

    /**
     * 返回删除成功的条数
     */
    @Override
    public int delete(String where, String[] whereArgs) {
        int count = getWritableDatabase().delete(getTableName(), where, whereArgs);
        if (count > 0) {
            notifyDatabaseDataChangeListeners(DatabaseDataChangeListener.DATA_CHANGE_DELETED, null);
        }
        return count;
    }

    /**
     * 默认实现是根据BaseColumns._ID来删除; 返回删除成功的条数
     */
    @Override
    public int delete(ContentValuesable t) {
        int count = 0;
        if (t != null) {
            ContentValues values = t.toContentValues();
            if (values != null && values.containsKey(BaseColumns._ID)) {
                String id = values.getAsString(BaseColumns._ID);
                MyLog.v("try to delete " + getTableName() + ", id= " + id);
                count = getWritableDatabase().delete(getTableName(), BaseColumns._ID + "=?",
                        new String[] {
                            id
                        });
                if (count > 0) {
                    HashSet<String> changedIdSet = new HashSet<String>();
                    changedIdSet.add(id);
                    notifyDatabaseDataChangeListeners(
                            DatabaseDataChangeListener.DATA_CHANGE_DELETED, changedIdSet);
                }
            }
        }
        return count;
    }

    /**
     * 返回新插入内容的_id
     */
    @Override
    public long insert(ContentValues initialValues) {
        ArrayList<Object> resultList = new ArrayList<Object>();
        bulkInsert(new ContentValues[] {
            initialValues
        }, resultList);
        if (resultList.size() == 1) {
            if (resultList.get(0) instanceof ArrayList<?>) {
                ArrayList<Long> rowIdList = (ArrayList<Long>) resultList.get(0);
                return rowIdList.isEmpty() ? 0 : rowIdList.get(0);
            }
        }
        return 0;
    }

    /**
     * 返回新插入内容的_id
     */
    @Override
    public long insert(ContentValuesable t) {
        if (t != null) {
            return insert(t.toContentValues());
        }
        return 0;
    }

    public int bulkInsert(List<? extends ContentValuesable> list) {
        return bulkInsert(list, null);
    }

    public int bulkInsert(List<? extends ContentValuesable> list, List<Object> resultList) {
        int count = 0;
        if (list != null && !list.isEmpty()) {
            final ContentValues[] valuesArray = new ContentValues[list.size()];
            for (int i = 0; i < list.size(); i++) {
                valuesArray[i] = list.get(i).toContentValues();
            }
            count = bulkInsert(valuesArray, null);
        }
        return count;
    }

    /**
     * 批量插入
     * 
     * @param aryValues
     * @param resultList 
     *            只带回一个插入内容的rowIdList，其中可能有插入失败的rowId(值为-1)，rowIdList顺序和aryValues一致
     * @return 插入成功的条数
     */
    @Override
    public int bulkInsert(ContentValues[] aryValues, List<Object> resultList) {
        int count = 0;
        if (aryValues != null && aryValues.length > 0) {
            ArrayList<Long> rowIdList = new ArrayList<Long>();
            HashSet<String> addedIdSet = new HashSet<String>();
            SQLiteDatabase db = getWritableDatabase();
            db.beginTransaction();
            try {
                for (ContentValues values : aryValues) {
                    long rowId = -1;
                    try {
                        rowId = db.insert(getTableName(), null, values);
                    } catch (SQLException e) { // 加一个异常捕获，如果有一条插入失败了，其他的也要能正常插入
                        MyLog.e(e);
                    }
                    if (rowId > 0) {
                        count++;
                        addedIdSet.add(String.valueOf(rowId));
                    }
                    rowIdList.add(rowId);
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
            if (resultList != null) {
                resultList.add(rowIdList);
            }
            if (count > 0) {
                notifyDatabaseDataChangeListeners(DatabaseDataChangeListener.DATA_CHANGE_ADDED,
                        addedIdSet);
            }
        }
        return count;
    }

    @Override
    public Cursor query(String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        return getReadableDatabase().query(getTableName(), projection, selection, selectionArgs,
                null, null, sortOrder);
    }

    /**
     * 返回更新成功的条数
     */
    @Override
    public int update(ContentValues values, String where, String[] whereArgs) {
        int count = 0;
        count = getWritableDatabase().update(getTableName(), values, where, whereArgs);
        if (count > 0) {
            notifyDatabaseDataChangeListeners(DatabaseDataChangeListener.DATA_CHANGE_UPDATED, null);
        }
        return count;
    }

    /**
     * 默认实现是根据BaseColumns._ID来更新; 返回更新成功的条数
     */
    @Override
    public int update(ContentValuesable t) {
        int count = 0;
        if (t != null) {
            ContentValues values = t.toContentValues();
            if (values != null && values.containsKey(BaseColumns._ID)) {
                String id = values.getAsString(BaseColumns._ID);
                MyLog.v("try to update " + getTableName() + ", id= " + id);
                count = getWritableDatabase().update(getTableName(), values,
                        BaseColumns._ID + "=?", new String[] {
                            id
                        });
                if (count > 0) {
                    HashSet<String> changedIdSet = new HashSet<String>();
                    changedIdSet.add(id);
                    notifyDatabaseDataChangeListeners(
                            DatabaseDataChangeListener.DATA_CHANGE_UPDATED, changedIdSet);
                }
            }
        }
        return count;
    }

    public void notifyDatabaseDataChangeListeners(int type, HashSet<String> changedIDSet) {
        synchronized (mDataChangeListenerList) {
            for (DatabaseDataChangeListener listener : mDataChangeListenerList) {
                listener.onDatabaseDataChanged(type, changedIDSet);
            }
        }
    }

    public void addDatabaseDataChangeListener(DatabaseDataChangeListener listener) {
        if (listener != null) {
            synchronized (mDataChangeListenerList) {
                mDataChangeListenerList.add(listener);
            }
        }
    }

    public void removeDatabaseDataChangeListener(DatabaseDataChangeListener listener) {
        if (listener != null) {
            synchronized (mDataChangeListenerList) {
                mDataChangeListenerList.remove(listener);
            }
        }
    }

    public long getMaxId() {
        Cursor cursor = null;
        try {
            cursor = this.query(new String[] {
                BaseColumns._ID
            }, null, null, BaseColumns._ID + " DESC LIMIT 1");
            if ((cursor != null) && cursor.moveToFirst()) {
                return cursor.getLong(0);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return 0;
    }
}
