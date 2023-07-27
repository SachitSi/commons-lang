package org.apache.commons.lang3.text.translate;

public class UnescapeUtils {
    /**
     * Reverse of  for unescaping purposes.
     *
     * @return the mapping table
     */
    public static String[][] ISO8859_1_UNESCAPE() {
        return EntityArrays.ISO8859_1_UNESCAPE.clone();
    }

    /**
     * Reverse of  for unescaping purposes.
     *
     * @return the mapping table
     */
    public static String[][] HTML40_EXTENDED_UNESCAPE() {
        return EntityArrays.HTML40_EXTENDED_UNESCAPE.clone();
    }

    /**
     * Reverse of  for unescaping purposes.
     *
     * @return the mapping table
     */
    public static String[][] BASIC_UNESCAPE() {
        return EntityArrays.BASIC_UNESCAPE.clone();
    }

    /**
     * Reverse of  for unescaping purposes.
     *
     * @return the mapping table
     */
    public static String[][] APOS_UNESCAPE() {
        return EntityArrays.APOS_UNESCAPE.clone();
    }

    /**
     * Reverse of  for unescaping purposes.
     *
     * @return the mapping table
     */
    public static String[][] JAVA_CTRL_CHARS_UNESCAPE() {
        return EntityArrays.JAVA_CTRL_CHARS_UNESCAPE.clone();
    }
}