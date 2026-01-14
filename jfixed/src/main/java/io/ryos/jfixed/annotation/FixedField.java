package io.ryos.jfixed.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation of Field
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface FixedField {
    int offset(); // offset of value corresponding to field
    int length(); // length of value corresponding to field
    String format() default ""; // format pattern
    boolean trim() default true; // trim or not-trim
    char fillChar() default ' '; // char to fill value
}
