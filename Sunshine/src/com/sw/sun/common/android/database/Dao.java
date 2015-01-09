
package com.sw.sun.common.android.database;

import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;

public interface Dao<T extends ContentValuesable> {

    public int delete(String where, String[] whereArgs);

    public int delete(T t);

    public long insert(ContentValues initialValues);

    public long insert(T t);

    /**
     * 根据子类不同的实现，resultList参数带回不同的数据
     * 
     * @param aryValues
     * @param resultList 此参数用来返回相应的数据， 一般传入一个空的List对象，不需要返回数据可以传null
     * @return
     */
    public int bulkInsert(ContentValues[] aryValues, List<Object> resultList);

    public Cursor query(String[] projection, String selection, String[] selectionArgs,
            String sortOrder);

    public int update(ContentValues values, String where, String[] whereArgs);

    public int update(T t);

}
