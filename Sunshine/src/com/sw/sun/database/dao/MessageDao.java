
package com.sw.sun.database.dao;

import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

import com.sw.sun.common.android.database.AbstractDaoImpl;
import com.sw.sun.common.logger.MyLog;
import com.sw.sun.database.MessageDbOpenHelper;

public class MessageDao extends AbstractDaoImpl {

    /**
     * 消息的会话ID， 字符串，格式："from_to_timestamp"
     */
    public static final String SESSION_ID = "session_id";

    /**
     * 消息的类型, int
     */
    public static final String BODY_TYPE = "body_type";

    /**
     * 发送的消息的发送时间，由服务器产生
     */
    public static final String SENT_TIME = "sent_time";

    /**
     * 收到消息的收到时间，本地时间
     */
    public static final String RECEIVED_TIME = "received_time";

    /**
     * 是否是收到的消息, 0表示自己发出的, 1表示收到
     */
    public static final String INBOUND_STATUS = "inbound_status";

    /**
     * 发出消息的状态
     */
    public static final String OUTBOUND_STATUS = "outbound_status";

    /**
     * 对于发出的消息，表示接收方 对于收到的消息，表示发送方
     */
    public static final String TARGET = "target";

    /**
     * 消息的seq，由服务器产生
     */
    public static final String M_SEQ = "m_seq";

    /**
     * 消息的session内容，以json格式存储
     */
    public static final String SESSION_CONTENT = "session_content";

    /**
     * 消息的comment内容，以json格式存储
     */
    public static final String COMMENT_CONTENT = "comment_content";

    public static final int STATUS_NOT_INBOUNT = 0; // 自己发出的消息

    public static final int STATUS_IS_INBOUND = 1; // 收到的消息

    public static final int OUTBOUND_STATUS_NOT_SET = 0;

    public static final int OUTBOUND_STATUS_UNSENT = 1;

    public static final int OUTBOUND_STATUS_SENT = 2;

    public static final int BODY_TYPE_COMMENT = 0;

    public static final int BODY_TYPE_SESSION = 1;

    public static final String[] FULL_PROJECTION = {
            BaseColumns._ID, SESSION_ID, BODY_TYPE, SENT_TIME, RECEIVED_TIME, INBOUND_STATUS,
            OUTBOUND_STATUS, TARGET, M_SEQ, SESSION_CONTENT, COMMENT_CONTENT
    };

    public static final int ID_INDEX = 0;

    public static final int SESSION_ID_INDEX = 1;

    public static final int BODY_TYPE_INDEX = 2;

    public static final int SENT_TIME_INDEX = 3;

    public static final int RECEIVED_TIME_INDEX = 4;

    public static final int INBOUND_STATUS_INDEX = 5;

    public static final int OUTBOUND_STATUS_INDEX = 6;

    public static final int TARGET_INDEX = 7;

    public static final int M_SEQ_INDEX = 8;

    public static final int SESSION_CONTENT_INDEX = 9;

    public static final int COMMENT_CONTENT_INDEX = 10;

    private static MessageDao sInstance = new MessageDao();

    private static long sBaseId = 0;

    public static MessageDao getInstance() {
        return sInstance;
    }

    private MessageDao() {
        super();
    }

    @Override
    public String getTableName() {
        return MessageDbOpenHelper.getMessageTableName();
    }

    @Override
    public SQLiteDatabase getWritableDatabase() {
        return MessageDbOpenHelper.getInstance().getWritableDatabase();
    }

    @Override
    public SQLiteDatabase getReadableDatabase() {
        return MessageDbOpenHelper.getInstance().getReadableDatabase();
    }

    // 首选获取当前最大的消息id maxId，如果当前时间大于maxId, 使用当前时间；如果当前时间小于maxId,
    // 使用maxId + 1作为初始值
    private static long initIdGenerator() {
        long id = System.currentTimeMillis();
        final long maxMsgId = MessageDao.getInstance().getMaxId();
        if (maxMsgId > id) {
            id = maxMsgId + 1;
        }
        MyLog.info("MessageDao, the sBaseId is initialized to be " + id);
        return id;
    }

    public static long getNewId() {
        boolean isInit = false;
        long newId, result;
        synchronized (MessageDao.class) {
            newId = sBaseId;
            isInit = sBaseId < 0;
        }
        if (isInit) {
            newId = initIdGenerator();
        }
        synchronized (MessageDao.class) {
            if (sBaseId < 0) {
                sBaseId = newId;
            }
            result = sBaseId++;
        }
        return result;
    }

}
