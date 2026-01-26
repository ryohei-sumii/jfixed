package io.ryos.jfixed.exception;

/**
 * Exception thrown when parsing fixed-length data fails.
 * 
 * <p>This exception is thrown in various scenarios:</p>
 * <ul>
 *   <li>The input line is too short for the field definitions</li>
 *   <li>Type conversion fails (e.g., invalid number format)</li>
 *   <li>Required fields are missing</li>
 *   <li>Invalid data format</li>
 *   <li>Structure validation fails (e.g., multiple headers, missing sections)</li>
 * </ul>
 * 
 * <p>The exception includes detailed information about the error location:</p>
 * <ul>
 *   <li>Line number where the error occurred</li>
 *   <li>Field name (if applicable)</li>
 *   <li>Original line content</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * try {
 *     Person person = engine.process(line, Person.class, 1);
 * } catch (FixedLengthException e) {
 *     System.err.println("Error at line " + e.getLineNumber() + 
 *                        ", field: " + e.getFieldName() + 
 *                        ": " + e.getMessage());
 * }
 * }</pre>
 */
public class FixedLengthException extends RuntimeException {
    private final int lineNumber;
    private final String fieldName;
    private final String originalLine;

    /**
     * Creates a new FixedLengthException.
     * 
     * @param message the error message
     * @param lineNumber the line number where the error occurred (1-based)
     * @param fieldName the name of the field where the error occurred, or null if not applicable
     * @param originalLine the original line content where the error occurred
     * @param cause the cause of this exception, or null if not applicable
     */
    public FixedLengthException(String message,
                                int lineNumber,
                                String fieldName,
                                String originalLine,
                                Throwable cause) {
        super(message, cause);
        this.lineNumber = lineNumber;
        this.fieldName = fieldName;
        this.originalLine = originalLine;
    }

    /**
     * Returns the line number where the error occurred.
     * Line numbers are 1-based.
     * 
     * @return the line number
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Returns the name of the field where the error occurred.
     * 
     * @return the field name, or null if not applicable
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * Returns the original line content where the error occurred.
     * 
     * @return the original line content
     */
    public String getOriginalLine() {
        return originalLine;
    }

    @Override
    public String toString() {
        return String.format(
                "FixedLengthException{message='%s', lineNumber=%d, fieldName='%s', originalLine='%s'}",
                getMessage(), lineNumber, fieldName, originalLine
        );
    }
}
