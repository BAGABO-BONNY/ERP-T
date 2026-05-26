package com.jva.ERP.util;

/**
 * String Utility
 * Provides common string operations
 */
public class StringUtil {

    private StringUtil() {
        // Private constructor to prevent instantiation
    }

    /**
     * Check if string is empty or null
     */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Check if string is not empty
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    /**
     * Convert string to uppercase
     */
    public static String toUpperCase(String str) {
        return isEmpty(str) ? str : str.toUpperCase();
    }

    /**
     * Convert string to lowercase
     */
    public static String toLowerCase(String str) {
        return isEmpty(str) ? str : str.toLowerCase();
    }

    /**
     * Trim string safely
     */
    public static String safeTrim(String str) {
        return isEmpty(str) ? str : str.trim();
    }
}

