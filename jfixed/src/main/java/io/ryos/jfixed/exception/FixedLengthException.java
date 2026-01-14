package io.ryos.jfixed.exception;

public class FixedLengthException extends RuntimeException {
    private final int lineNumber;
    private final String fieldName;
    private final String originalLine;

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

    public int getLineNumber() {
        return lineNumber;
    }

    public String getFieldName() {
        return fieldName;
    }

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
