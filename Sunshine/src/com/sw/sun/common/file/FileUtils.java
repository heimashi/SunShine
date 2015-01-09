
package com.sw.sun.common.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;

import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import com.sw.sun.common.logger.MyLog;

public class FileUtils {

    private static final HashMap<String, String> sFileTypes = new HashMap<String, String>();

    private static Method sMetSetPermissions;

    static {
        // images
        sFileTypes.put("FFD8FF", "jpg");
        sFileTypes.put("89504E47", "png");
        sFileTypes.put("47494638", "gif");
        sFileTypes.put("474946", "gif"); // added by mk
        sFileTypes.put("424D", "bmp");
        try {
            Class<?> clsFileUtils = Class.forName("android.os.FileUtils");
            sMetSetPermissions = clsFileUtils.getMethod("setPermissions", String.class, int.class,
                    int.class, int.class);
            sMetSetPermissions.setAccessible(true);
        } catch (Exception e) {
            MyLog.e(e);
        }
    }

    public static boolean isGif(String filePath) {
        return "gif".equals(getFileType(filePath));
    }

    public static String getFileType(String filePath) {
        return sFileTypes.get(getFileHeader(filePath));
    }

    // 获取文件头信息
    private static String getFileHeader(String filePath) {
        FileInputStream is = null;
        String value = null;
        try {
            is = new FileInputStream(filePath);
            byte[] b = new byte[3];
            is.read(b, 0, b.length);
            value = bytesToHexString(b);
        } catch (Exception e) {
        } finally {
            if (null != is) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
        }
        return value;
    }

    private static String bytesToHexString(byte[] src) {
        StringBuilder builder = new StringBuilder();
        if (src == null || src.length <= 0) {
            return null;
        }
        String hv;
        for (int i = 0; i < src.length; i++) {
            hv = Integer.toHexString(src[i] & 0xFF).toUpperCase();
            if (hv.length() < 2) {
                builder.append(0);
            }
            builder.append(hv);
        }
        return builder.toString();
    }

    /**
     * 判断对应的是不是一个文件，并且是否存在
     * 
     * @param path
     * @return
     */
    public static boolean isFileExist(String path) {
        if (TextUtils.isEmpty(path)) {
            return false;
        }
        File file = new File(path);
        return file.exists() && file.isFile();
    }

    /**
     * 该文件（包含目录）是否存在
     * 
     * @param path
     * @return
     */
    public static boolean isExist(String path) {
        if (TextUtils.isEmpty(path)) {
            return false;
        }
        File file = new File(path);
        return file.exists();
    }

    /**
     * 截取path中目录路径
     * 
     * @param path
     * @return
     */
    public static String getFolderPath(String path) {
        String folderPath = "";
        if (!TextUtils.isEmpty(path)) {
            int i = path.lastIndexOf('/');
            if (i > 0) {
                folderPath = path.substring(0, i);
            }
        }
        return folderPath;
    }

    /**
     * 截取path中文件名
     * 
     * @param path
     * @return
     */
    public static String getFileName(String path) {
        return TextUtils.isEmpty(path) ? "" : path.substring(path.lastIndexOf('/') + 1);
    }

    public static String getFileNameWithoutExt(String path) {
        String filename = "";

        if (!TextUtils.isEmpty(path)) {
            int indexOfSlash = path.lastIndexOf('/');
            int indexOfDot = path.lastIndexOf('.');
            if (indexOfDot <= indexOfSlash) {
                indexOfDot = path.length();
            }
            filename = path.substring(indexOfSlash + 1, indexOfDot);
        }

        return filename;
    }

    public static String getFileExt(String path) {
        String extension = "";

        if (!TextUtils.isEmpty(path)) {
            int indexOfSlash = path.lastIndexOf('/');
            int indexOfDot = path.lastIndexOf('.');
            if (indexOfDot > indexOfSlash) {
                extension = path.substring(indexOfDot + 1);
            }
        }

        return extension;
    }

    public static String getMimeType(String path) {
        String extension = getFileExt(path).toLowerCase();
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        return mimeType != null ? mimeType : "*/*";
    }

    public static int setPermissions(File path, int mode) {
        try {
            return (Integer) sMetSetPermissions.invoke(null, path.getAbsolutePath(), mode, -1, -1);
        } catch (Exception e) {
            return -1;
        }
    }

    public static int setPermissions(File path, int mode, int uid, int gid) {
        try {
            return (Integer) sMetSetPermissions
                    .invoke(null, path.getAbsolutePath(), mode, uid, gid);
        } catch (Exception e) {
            return -1;
        }
    }

    public static int setPermissions(String path, int mode) {
        try {
            return (Integer) sMetSetPermissions.invoke(null, path, mode, -1, -1);
        } catch (Exception e) {
            return -1;
        }
    }

    public static int setPermissions(String path, int mode, int uid, int gid) {
        try {
            return (Integer) sMetSetPermissions.invoke(null, path, mode, uid, gid);
        } catch (Exception e) {
            return -1;
        }
    }

    public static boolean mkdirs(File file, int mode, int uid, int gid) {
        /* If the terminal directory already exists, answer false */
        if (file.exists()) {
            return false;
        }

        /* try to create a parent directory and then this directory */
        String parentDir = file.getParent();
        if (parentDir != null) {
            mkdirs(new File(parentDir), mode, uid, gid);
        }

        /* If the receiver can be created, answer true */
        if (file.mkdir()) {
            FileUtils.setPermissions(file.getPath(), mode, uid, gid);
            return true;
        }

        return false;
    }

    public static boolean createFile(File file) {
        if (file.exists()) {
            return false;
        }
        /* try to create a parent directory and then this directory */
        String parentDir = file.getParent();
        if (parentDir != null) {
            mkdirs(new File(parentDir), 0777, -1, -1);
        }
        try {
            file.createNewFile();
        } catch (IOException e) {
            // ignore
        }
        return true;
    }

}
