package org.apache.commons.lang3.text.translate;

public class EscapeUitls {
    /**
     * Mapping to escape <a href="https://secure.wikimedia.org/wikipedia/en/wiki/ISO/IEC_8859-1">ISO-8859-1</a>
     * characters to their named HTML 3.x equivalents.
     *
     * @return the mapping table
     */
    public static String[][] ISO8859_1_ESCAPE() {
        return EntityArrays.ISO8859_1_ESCAPE.clone();
    }

    /**
     * Mapping to escape additional <a href="https://www.w3.org/TR/REC-html40/sgml/entities.html">character entity
     * references</a>. Note that this must be used with {@link #ISO8859_1_ESCAPE()} to get the full list of
     * HTML 4.0 character entities.
     *
     * @return the mapping table
     */
    public static String[][] HTML40_EXTENDED_ESCAPE() {
        return EntityArrays.HTML40_EXTENDED_ESCAPE.clone();
    }

    /**
     * Mapping to escape the basic XML and HTML character entities.
     * <p>
     * Namely: {@code " & < >}
     *
     * @return the mapping table
     */
    public static String[][] BASIC_ESCAPE() {
        return EntityArrays.BASIC_ESCAPE.clone();
    }

    /**
     * Mapping to escape the apostrophe character to its XML character entity.
     *
     * @return the mapping table
     */
    public static String[][] APOS_ESCAPE() {
        return EntityArrays.APOS_ESCAPE.clone();
    }

    /**
     * Mapping to escape the Java control characters.
     * <p>
     * Namely: {@code \b \n \t \f \r}
     *
     * @return the mapping table
     */
    public static String[][] JAVA_CTRL_CHARS_ESCAPE() {
        return EntityArrays.JAVA_CTRL_CHARS_ESCAPE.clone();
    }
}