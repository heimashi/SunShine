
package com.sw.sun.common.file;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import android.text.TextUtils;

import com.sw.sun.common.logger.MyLog;

public class IOUtils {

    private static final int BUFFER_SIZE = 1024;

    public static final String SUPPORTED_IMAGE_FORMATS[] = {
            "jpg", "png", "bmp", "gif", "webp"
    };

    /**
     * 压缩一个文件或文件夹下的所有文件成一个gzip
     * 
     * @param out
     * @param f
     * @param base
     * @throws Exception
     */
    public static void zip(final ZipOutputStream out, final File f, String base,
            final FileFilter filter) throws IOException {

        if (base == null) {
            base = "";
        }
        FileInputStream in = null;
        try {
            if (f.isDirectory()) {
                File[] fl;
                if (filter != null) {
                    fl = f.listFiles(filter);
                } else {
                    fl = f.listFiles();
                }
                out.putNextEntry(new ZipEntry(base + java.io.File.separator));
                base = TextUtils.isEmpty(base) ? "" : base + java.io.File.separator;

                for (int i = 0; i < fl.length; i++) {
                    zip(out, fl[i], base + fl[i].getName(), null);
                }

                File[] dirs = f.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File pathname) {
                        return pathname.isDirectory();
                    }
                });
                if (dirs != null) {
                    for (File subFile : dirs) {
                        zip(out, subFile, base + File.separator + subFile.getName(), filter);
                    }
                }
            } else {
                if (!TextUtils.isEmpty(base)) {
                    out.putNextEntry(new ZipEntry(base));
                } else {
                    final Date date = new Date();
                    out.putNextEntry(new ZipEntry(String.valueOf(date.getTime()) + ".txt"));
                }
                in = new FileInputStream(f);

                int bytesRead = -1;
                final byte[] buffer = new byte[BUFFER_SIZE];
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
        } catch (final IOException e) {
            MyLog.e("zipFiction failed with exception:" + e.toString());
        } finally {
            closeQuietly(in);
        }
    }

    public static void zip(final ZipOutputStream out, String fileName, final InputStream inputStream) {
        try {
            if (!TextUtils.isEmpty(fileName)) {
                out.putNextEntry(new ZipEntry(fileName));
            } else {
                final Date date = new Date();
                out.putNextEntry(new ZipEntry(String.valueOf(date.getTime()) + ".txt"));
            }

            int bytesRead = -1;
            final byte[] buffer = new byte[BUFFER_SIZE];
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            MyLog.e("zipFiction failed with exception:" + e.toString());
        }
    }

    public static void closeQuietly(Reader reader) {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    public static void closeQuietly(InputStream is) {
        if (is != null) {
            try {
                is.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    public static void closeQuietly(OutputStream os) {
        if (os != null) {
            try {
                os.flush();
            } catch (IOException e) {
                // ignore
            }
            try {
                os.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    public static void copyFile(final File src, final File dest) throws IOException {
        if (src.getAbsolutePath().equals(dest.getAbsolutePath())) {
            return;
        }

        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(src);
            out = new FileOutputStream(dest);

            final byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) >= 0) {
                out.write(buf, 0, len);
            }
        } finally {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
        }
    }

    public static boolean unZip(final String zipFile, String targetDir) {
        if (TextUtils.isEmpty(targetDir) || TextUtils.isEmpty(zipFile)) {
            return false;
        }

        final int BUFFER = 4096; // 这里缓冲区我们使用4KB，
        if (!targetDir.endsWith("/")) {
            targetDir += "/";
        }
        String strEntry; // 保存每个zip的条目名称
        try {
            BufferedOutputStream dest = null; // 缓冲输出流
            final FileInputStream fis = new FileInputStream(zipFile);
            final ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
            ZipEntry entry; // 每个zip条目的实例
            final byte data[] = new byte[BUFFER];
            while ((entry = zis.getNextEntry()) != null) {
                int count;
                strEntry = entry.getName();
                final File entryFile = new File(targetDir + strEntry);

                // 因为用entryFile是判断不出来是否是Directiory,所以如果以“/”结束，就跳过，
                // 然后在解压文件时判断父文件夹是否存在。如不存在，则新建
                if (strEntry.endsWith("/")) {
                    continue;
                } else {
                    final File parentFolder = new File(entryFile.getParent());
                    if ((parentFolder != null)
                            && (!parentFolder.exists() || !parentFolder.isDirectory())) {
                        parentFolder.mkdirs();
                        hideFromMediaScanner(parentFolder);
                    }

                    final FileOutputStream fos = new FileOutputStream(entryFile);
                    dest = new BufferedOutputStream(fos, BUFFER);
                    while ((count = zis.read(data, 0, BUFFER)) != -1) {
                        dest.write(data, 0, count);
                    }
                    dest.flush();
                    dest.close();
                }
            }
            zis.close();
        } catch (final IOException e) {
            MyLog.e("解压缩失败！！！", e);
            return false;
        }
        return true;
    }

    /**
     * 告知mediascanner不要扫描某个目录。
     * 
     * @param root 不需要扫描的目录
     */
    public static void hideFromMediaScanner(final File root) {
        final File file = new File(root, ".nomedia");
        if (!file.exists() || !file.isFile()) {
            try {
                file.createNewFile();
            } catch (final IOException e) {
                MyLog.e(e);
            }
        }
    }

    public static byte[] getFileSha1Digest(final String fileName) throws NoSuchAlgorithmException,
            IOException {
        final MessageDigest md = MessageDigest.getInstance("SHA1");
        final File file = new File(fileName);
        final FileInputStream inStream = new FileInputStream(file);
        final byte[] buffer = new byte[4096]; // Calculate digest per 1K

        int readCount = 0;
        while ((readCount = inStream.read(buffer)) != -1) {
            md.update(buffer, 0, readCount);
        }
        try {
            inStream.close();
        } catch (IOException e) {
            MyLog.e(e);
        }

        return md.digest();
    }

    public static byte[] getFileMD5Digest(final String fileName) throws NoSuchAlgorithmException,
            IOException {
        final MessageDigest md = MessageDigest.getInstance("MD5");
        final File file = new File(fileName);
        final FileInputStream inStream = new FileInputStream(file);
        final byte[] buffer = new byte[4096]; // Calculate digest per 1K

        int readCount = 0;
        while ((readCount = inStream.read(buffer)) != -1) {
            md.update(buffer, 0, readCount);
        }
        try {
            inStream.close();
        } catch (IOException e) {
            MyLog.e(e);
        }
        return md.digest();
    }

    /**
     * 删除该目录及目录下所有文件夹和文件，如果file是文件，就不删。因删dirs
     * 
     * @param file
     */
    public static void deleteDirs(final File file) {
        MyLog.v("deleteDirs filePath = " + file.getAbsolutePath());
        if (file.isDirectory()) {
            // 删除所有文件，然后删除自己
            final File[] subFiles = file.listFiles();
            if ((subFiles != null) && (subFiles.length > 0)) {
                for (final File subF : subFiles) {
                    if (subF.isFile()) {
                        subF.delete();
                    } else {
                        deleteDirs(subF);
                    }
                }
            }
            file.delete();
        }
    }

    public static String getFileSuffix(String fileName) {
        final int dotPos = fileName.lastIndexOf('.');
        if (dotPos > 0) {
            return fileName.substring(dotPos + 1);
        }
        return "";
    }

    public static boolean isSupportImageSuffix(String suffix) {
        if (TextUtils.isEmpty(suffix)) {
            return false;
        }
        for (String supported : SUPPORTED_IMAGE_FORMATS) {
            if (supported.equalsIgnoreCase(suffix)) {
                return true;
            }
        }
        return false;
    }
}
