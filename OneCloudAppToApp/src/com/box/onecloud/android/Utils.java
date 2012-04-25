package com.box.onecloud.android;

public class Utils {

    /**
     * Return the extension of a file.
     * 
     * @param fileName
     *            File name.
     * @param defaultValue
     *            Default value to return if an extension could not be determined.
     * @return The file extension (without the dot) or defaultValue if it could not be determined.
     */
    public static String getFileExtension(final String fileName, final String defaultValue) {
        if (fileName == null || fileName.length() == 0) {
            return defaultValue;
        }
        if (!fileName.contains(".")) {
            return defaultValue;
        }
        String ext = fileName.substring(fileName.lastIndexOf(".") + 1);
        if (ext.length() == 0) {
            return defaultValue;
        }
        return ext;
    }

}
