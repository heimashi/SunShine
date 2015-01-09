
package com.sw.sun.database.pojo;

import java.io.Serializable;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.text.TextUtils;

import com.sw.sun.common.android.database.ContentValuesable;
import com.sw.sun.common.android.database.JSONable;
import com.sw.sun.common.logger.MyLog;
import com.sw.sun.database.dao.MessageDao;

public class Message implements ContentValuesable, Serializable {

    private static final long serialVersionUID = 100000000000001212L;

    protected long mId;

    protected String mSessionId;

    protected int mBodyType;

    protected long mSentTime;

    protected long mReceivedTime;

    protected boolean mIsInbound;

    protected int mOutboundStatus;

    protected long mTarget;

    protected long mMseq;

    protected Content mSessionContent;

    protected Content mCommentContent;

    public Message() {
        this.mId = MessageDao.getNewId();
        this.mIsInbound = false;
        this.mBodyType = MessageDao.BODY_TYPE_COMMENT;
    }

    /**
     * 传入cursor必须是使用的MessageDao.FULL_PROJECTION; 否则构造出的message是错的
     * 
     * @param c
     */
    public Message(Cursor c) {
        this.mId = c.getLong(MessageDao.ID_INDEX);
        this.mSessionId = c.getString(MessageDao.SESSION_ID_INDEX);
        this.mBodyType = c.getInt(MessageDao.BODY_TYPE_INDEX);
        this.mSentTime = c.getLong(MessageDao.SENT_TIME_INDEX);
        this.mReceivedTime = c.getLong(MessageDao.RECEIVED_TIME_INDEX);
        this.mIsInbound = (c.getInt(MessageDao.INBOUND_STATUS_INDEX) == MessageDao.STATUS_IS_INBOUND);
        this.mOutboundStatus = c.getInt(MessageDao.OUTBOUND_STATUS_INDEX);
        this.mTarget = c.getLong(MessageDao.TARGET_INDEX);
        this.mMseq = c.getLong(MessageDao.M_SEQ_INDEX);
        String sessionContent = c.getString(MessageDao.SESSION_CONTENT_INDEX);
        if (!TextUtils.isEmpty(sessionContent)) {
            this.mSessionContent = new Content(sessionContent);
        }
        String commentContent = c.getString(MessageDao.COMMENT_CONTENT_INDEX);
        if (!TextUtils.isEmpty(commentContent)) {
            this.mCommentContent = new Content(commentContent);
        }
    }

    public long getId() {
        return mId;
    }

    public void setId(long id) {
        this.mId = id;
    }

    public String getSessionId() {
        return mSessionId;
    }

    public void setSessionId(String sessionId) {
        this.mSessionId = sessionId;
    }

    public int getBodyType() {
        return mBodyType;
    }

    public void setBodyType(int bodyType) {
        this.mBodyType = bodyType;
    }

    public long getSentTime() {
        return mSentTime;
    }

    public void setSentTime(long sentTime) {
        this.mSentTime = sentTime;
    }

    public long getReceivedTime() {
        return mReceivedTime;
    }

    public void setReceivedTime(long receivedTime) {
        this.mReceivedTime = receivedTime;
    }

    public boolean isInbound() {
        return mIsInbound;
    }

    public void setIsInbound(boolean isInbound) {
        this.mIsInbound = isInbound;
    }

    public int getOutboundStatus() {
        return mOutboundStatus;
    }

    public void setOutboundStatus(int outboundStatus) {
        this.mOutboundStatus = outboundStatus;
    }

    public long getTarget() {
        return mTarget;
    }

    public void setTarget(long target) {
        this.mTarget = target;
    }

    public long getMseq() {
        return mMseq;
    }

    public void setMseq(long mseq) {
        this.mMseq = mseq;
    }

    public Content getSessionContent() {
        return mSessionContent;
    }

    public void setSessionContent(Content sessionContent) {
        this.mSessionContent = sessionContent;
    }

    public Content getCommentContent() {
        return mCommentContent;
    }

    public void setCommentContent(Content commentContent) {
        this.mCommentContent = commentContent;
    }

    @Override
    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();
        values.put(BaseColumns._ID, this.mId);
        values.put(MessageDao.SESSION_ID, this.mSessionId);
        values.put(MessageDao.BODY_TYPE, this.mBodyType);
        values.put(MessageDao.SENT_TIME, this.mSentTime);
        values.put(MessageDao.RECEIVED_TIME, this.mReceivedTime);
        values.put(MessageDao.INBOUND_STATUS, this.mIsInbound ? MessageDao.STATUS_IS_INBOUND
                : MessageDao.STATUS_NOT_INBOUNT);
        values.put(MessageDao.OUTBOUND_STATUS, this.mOutboundStatus);
        values.put(MessageDao.TARGET, this.mTarget);
        values.put(MessageDao.M_SEQ, this.mMseq);
        values.put(MessageDao.SESSION_CONTENT,
                this.mSessionContent != null ? this.mSessionContent.toJSONString() : "");
        values.put(MessageDao.COMMENT_CONTENT,
                this.mCommentContent != null ? this.mCommentContent.toJSONString() : "");
        return values;
    }

    public static class Content implements JSONable, Serializable {

        private static final long serialVersionUID = 6183393765685091758L;

        private static final String MIME_TYPE = "mime_type";

        private static final String TEXT = "text";

        private static final String URL = "url";

        private static final String DURATION = "duration";

        private static final String SIZE = "size";

        public String mimeType;

        public String text;

        public String url;

        public int duration; // 语音或者视频的播放时间

        public int size; // 富媒体文件的大小，单位：字节

        public Content() {
        }

        public Content(String str) {
            parseJSONString(str);
        }

        // text 和 url字段同时为空时，该对象为空
        public boolean isEmpty() {
            return TextUtils.isEmpty(text) && TextUtils.isEmpty(url);
        }

        @Override
        public String toJSONString() {
            JSONObject obj = toJSONObject();
            if (obj != null) {
                return obj.toString();
            } else {
                return "";
            }
        }

        @Override
        public JSONObject toJSONObject() {
            JSONObject obj = new JSONObject();
            try {
                if (!TextUtils.isEmpty(mimeType)) {
                    obj.put(MIME_TYPE, mimeType);
                }
                if (!TextUtils.isEmpty(text)) {
                    obj.put(TEXT, text);
                }
                if (!TextUtils.isEmpty(url)) {
                    obj.put(URL, url);
                }
                obj.put(DURATION, duration);
                obj.put(SIZE, size);
            } catch (JSONException e) {
                MyLog.e(e);
            }
            return obj;
        }

        @Override
        public boolean parseJSONString(String jsonStr) {
            if (TextUtils.isEmpty(jsonStr)) {
                return false;
            }
            JSONObject obj;
            try {
                obj = new JSONObject(jsonStr);
                this.mimeType = obj.optString(MIME_TYPE, "");
                this.text = obj.optString(TEXT, "");
                this.url = obj.optString(URL, "");
                this.duration = obj.optInt(DURATION);
                this.size = obj.optInt(SIZE);
                obj = null;
                return true;
            } catch (JSONException e) {
                MyLog.e(e);
            }
            return false;
        }

    }

}
