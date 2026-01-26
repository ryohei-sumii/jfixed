package io.ryos.jfixed.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a field as a fixed-length field in a fixed-length data structure.
 * 
 * <p>This annotation defines the position and length of a field within a fixed-length line.
 * The field will be extracted from the specified offset and converted to the field's type.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * class Person {
 *     @FixedField(offset = 0, length = 10)
 *     String name;
 *     
 *     @FixedField(offset = 10, length = 10)
 *     String surname;
 *     
 *     @FixedField(offset = 20, length = 3)
 *     int age;
 * }
 * }</pre>
 * 
 * <p>For date/time fields, you can specify a custom format:</p>
 * <pre>{@code
 * @FixedField(offset = 0, length = 10, format = "yyyy-MM-dd")
 * LocalDate birthDate;
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface FixedField {
    /**
     * The starting position (offset) of the field in the fixed-length line.
     * Position is 0-based.
     * 
     * @return the offset position
     */
    int offset();
    
    /**
     * The length of the field in characters.
     * 
     * @return the field length
     */
    int length();
    
    /**
     * Optional format pattern for date/time fields.
     * For LocalDate, default format is "yyyyMMdd".
     * For LocalDateTime, default format is "yyyyMMddHHmmss".
     * 
     * @return the format pattern, or empty string if not specified
     */
    String format() default "";
    
    /**
     * Whether to trim whitespace from the extracted value.
     * Default is true.
     * 
     * @return true if trimming is enabled, false otherwise
     */
    boolean trim() default true;
    
    /**
     * The character used for padding/filling the field.
     * Default is space character.
     * 
     * @return the fill character
     */
    char fillChar() default ' ';
}
