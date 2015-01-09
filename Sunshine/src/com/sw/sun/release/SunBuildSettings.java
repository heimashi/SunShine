
package com.sw.sun.release;

/**
 * 在命令行用ant编译并替换掉其中的token,方便标示不同的渠道；为不同的渠道编译出不同的apk,方便跟踪不同推广方式的效果。
 * 
 * @author kevin
 */
public class SunBuildSettings {

    // 不同的渠道
    public static final String RELEASE_CHANNEL = "DEBUG";

    public static final String DEBUG = "DEBUG";

    public static final String DEFAULT = "DEFAULT";

    public static final String TEST = "TEST";

    public static final String RC = "RC";

    public static final String LOGABLE = "LOGABLE";

    public static final boolean IS_NO_CHANNEL = RELEASE_CHANNEL.contains("2A2FE0D7");

    public static final boolean IS_DEFAULT_CHANNEL = DEFAULT.equalsIgnoreCase(RELEASE_CHANNEL);

    public static final boolean IS_DEBUG_BUILD = DEBUG.equalsIgnoreCase(RELEASE_CHANNEL);

    public static final boolean IS_LOGABLE_BUILD = LOGABLE.equalsIgnoreCase(RELEASE_CHANNEL);

    public static final boolean IS_TEST_BUILD = TEST.equalsIgnoreCase(RELEASE_CHANNEL);

    public static final boolean IS_RC_BUILD = RELEASE_CHANNEL.startsWith(RC);

}
