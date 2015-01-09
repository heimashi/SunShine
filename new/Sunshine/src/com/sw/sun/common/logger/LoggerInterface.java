
package com.sw.sun.common.logger;

public interface LoggerInterface {

    public void setTag(String tag);

    public void log(String content);

    public void log(String content, Throwable t);
}
