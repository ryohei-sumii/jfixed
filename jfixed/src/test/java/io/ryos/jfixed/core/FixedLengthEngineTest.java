package io.ryos.jfixed.core;

import io.ryos.jfixed.annotation.FixedField;
import io.ryos.jfixed.converter.ConverterRegistry;
import io.ryos.jfixed.exception.FixedLengthException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class FixedLengthEngineTest {

    private FixedLengthEngine engine;

    @BeforeEach
    void setUp() {
        engine = new FixedLengthEngine(StandardCharsets.UTF_8, new ConverterRegistry());
    }

    // POJO Tests
    @Test
    void testProcessPojoBasic() throws Exception {
        String line = "John      Doe       25 ";
        Person person = engine.process(line, Person.class, 1);

        assertEquals("John", person.name);
        assertEquals("Doe", person.surname);
        assertEquals(25, person.age);
    }

    @Test
    void testProcessPojoWithDifferentTypes() throws Exception {
        String line = "John      Doe       25 1234567890123 123.4";
        PersonWithMoreFields person = engine.process(line, PersonWithMoreFields.class, 1);

        assertEquals("John", person.name);
        assertEquals("Doe", person.surname);
        assertEquals(25, person.age);
        assertEquals(1234567890123L, person.id);
        assertEquals(123.4, person.salary, 0.01);
    }

    @Test
    void testProcessPojoWithTrim() throws Exception {
        String line = "  John    Doe       25 ";
        Person person = engine.process(line, Person.class, 1);

        assertEquals("John", person.name);
        assertEquals("Doe", person.surname);
        assertEquals(25, person.age);
    }

    @Test
    void testProcessPojoWithoutTrim() throws Exception {
        String line = "  John    Doe       25 ";
        PersonWithoutTrim person = engine.process(line, PersonWithoutTrim.class, 1);

        assertEquals("  John    ", person.name);
        assertEquals("Doe       ", person.surname);
        assertEquals(25, person.age);
    }

    @Test
    void testProcessPojoWithDefaultValue() throws Exception {
        String line = "John      Doe       25 ";
        PersonWithDefault person = engine.process(line, PersonWithDefault.class, 1);

        assertEquals("John", person.name);
        assertEquals("Doe", person.surname);
        assertEquals(25, person.age);
        assertEquals(0, person.defaultInt);
        assertNull(person.defaultString);
    }

    @Test
    void testProcessPojoWithLocalDate() throws Exception {
        String line = "John      Doe       25 20231225";
        PersonWithDate person = engine.process(line, PersonWithDate.class, 1);

        assertEquals("John", person.name);
        assertEquals(LocalDate.of(2023, 12, 25), person.birthDate);
    }

    @Test
    void testProcessPojoWithLocalDateCustomFormat() throws Exception {
        String line = "John      Doe       25 2023-12-25";
        PersonWithDateCustomFormat person = engine.process(line, PersonWithDateCustomFormat.class, 1);

        assertEquals("John", person.name);
        assertEquals(LocalDate.of(2023, 12, 25), person.birthDate);
    }

    @Test
    void testProcessPojoWithLocalDateTime() throws Exception {
        String line = "John      Doe       25 20231225143000";
        PersonWithDateTime person = engine.process(line, PersonWithDateTime.class, 1);

        assertEquals("John", person.name);
        assertEquals(LocalDateTime.of(2023, 12, 25, 14, 30, 0), person.createdAt);
    }

    @Test
    void testProcessPojoWithBoolean() throws Exception {
        String line = "John      Doe       25 1";
        PersonWithBoolean person = engine.process(line, PersonWithBoolean.class, 1);

        assertEquals("John", person.name);
        assertTrue(person.isActive);
    }

    @Test
    void testProcessPojoWithBigDecimal() throws Exception {
        String line = "John      Doe       25 123.456";
        PersonWithBigDecimal person = engine.process(line, PersonWithBigDecimal.class, 1);

        assertEquals("John", person.name);
        assertEquals(123.456, person.amount.doubleValue(), 0.001);
    }

    @Test
    void testProcessPojoNullCharset() {
        assertThrows(IllegalArgumentException.class, () -> new FixedLengthEngine(null, new ConverterRegistry()));
    }

    @Test
    void testProcessPojoNullConverterRegistry() {
        assertThrows(IllegalArgumentException.class, () -> new FixedLengthEngine(StandardCharsets.UTF_8, null));
    }

    @Test
    void testProcessPojoNullLine() {
        assertThrows(IllegalArgumentException.class, () -> engine.process(null, Person.class, 1));
    }

    @Test
    void testProcessPojoNullClazz() {
        assertThrows(IllegalArgumentException.class, () -> engine.process("test", null, 1));
    }

    @Test
    void testProcessPojoWithException() {
        String line = "John    Doe     abc"; // invalid integer
        assertThrows(FixedLengthException.class, () -> engine.process(line, Person.class, 1));
    }

    @Test
    void testProcessPojoWithIndexOutOfBounds() {
        String line = "John"; // too short
        assertThrows(FixedLengthException.class, () -> engine.process(line, Person.class, 1));
    }

    // Record Tests
    @Test
    void testProcessRecordBasic() throws Exception {
        String line = "John      Doe       25 ";
        PersonRecord person = engine.process(line, PersonRecord.class, 1);

        assertEquals("John", person.name());
        assertEquals("Doe", person.surname());
        assertEquals(25, person.age());
    }

    @Test
    void testProcessRecordWithDifferentTypes() throws Exception {
        String line = "John      Doe       25 1234567890123";
        PersonRecordWithMoreFields person = engine.process(line, PersonRecordWithMoreFields.class, 1);

        assertEquals("John", person.name());
        assertEquals("Doe", person.surname());
        assertEquals(25, person.age());
        assertEquals(1234567890123L, person.id());
    }

    @Test
    void testProcessRecordWithDefaultValue() throws Exception {
        String line = "John      Doe       25 ";
        PersonRecordWithDefault person = engine.process(line, PersonRecordWithDefault.class, 1);

        assertEquals("John", person.name());
        assertEquals("Doe", person.surname());
        assertEquals(25, person.age());
        assertEquals(0, person.defaultInt());
        assertNull(person.defaultString());
    }

    @Test
    void testProcessRecordWithLocalDate() throws Exception {
        String line = "John      Doe       25 20231225";
        PersonRecordWithDate person = engine.process(line, PersonRecordWithDate.class, 1);

        assertEquals("John", person.name());
        assertEquals(LocalDate.of(2023, 12, 25), person.birthDate());
    }

    @Test
    void testProcessRecordWithPrimitiveTypes() throws Exception {
        // name(0-10), gap(10-20), age(20-23), id(23-36), salary(36-42), height(42-48), isActive(48-52)
        String line = "John                25 1234567890123 123.4 456.7true";
        PersonRecordWithPrimitives person = engine.process(line, PersonRecordWithPrimitives.class, 1);

        assertEquals("John", person.name());
        assertEquals(25, person.age());
        assertEquals(1234567890123L, person.id());
        assertEquals(123.4, person.salary(), 0.01);
        assertEquals(456.7f, person.height(), 0.01f);
        assertTrue(person.isActive());
    }

    @Test
    void testProcessRecordWithException() {
        String line = "John    Doe     abc"; // invalid integer
        assertThrows(FixedLengthException.class, () -> engine.process(line, PersonRecord.class, 1));
    }

    @Test
    void testProcessRecordWithIndexOutOfBounds() {
        String line = "John"; // too short
        assertThrows(FixedLengthException.class, () -> engine.process(line, PersonRecord.class, 1));
    }

    @Test
    void testProcessRecordNullLine() {
        assertThrows(IllegalArgumentException.class, () -> engine.process(null, PersonRecord.class, 1));
    }

    @Test
    void testProcessRecordNullClazz() {
        assertThrows(IllegalArgumentException.class, () -> engine.process("test", null, 1));
    }

    @Test
    void testProcessRecordWithAllPrimitiveDefaults() throws Exception {
        String line = "John      Doe       25 ";
        PersonRecordWithAllPrimitives person = engine.process(line, PersonRecordWithAllPrimitives.class, 1);

        assertEquals("John", person.name());
        assertEquals(25, person.age());
        assertEquals(0, person.defaultInt());
        assertEquals(0L, person.defaultLong());
        assertEquals(0.0, person.defaultDouble());
        assertEquals(0.0f, person.defaultFloat());
        assertEquals((short) 0, person.defaultShort());
        assertEquals((byte) 0, person.defaultByte());
        assertEquals('\u0000', person.defaultChar());
        assertFalse(person.defaultBoolean());
    }

    @Test
    void testProcessPojoWithoutNoArgsConstructor() throws Exception {
        // POJO without no-args constructor should fall back to record-style processing
        String line = "John      Doe       25 ";
        PersonWithoutNoArgsConstructor person = engine.process(line, PersonWithoutNoArgsConstructor.class, 1);

        assertEquals("John", person.name);
        assertEquals("Doe", person.surname);
        assertEquals(25, person.age);
    }

    @Test
    void testProcessWithExceptionWrapping() {
        // Test that non-FixedLengthException exceptions are wrapped
        String line = "John      Doe       ";
        // This should cause an exception during processing
        assertThrows(FixedLengthException.class, () -> engine.process(line, Person.class, 1));
    }

    @Test
    void testExtractAndConvertWithException() {
        // Test extractAndConvert exception handling
        String line = "John";
        assertThrows(FixedLengthException.class, () -> engine.process(line, Person.class, 1));
    }


    // Test classes
    static class Person {
        @FixedField(offset = 0, length = 10)
        String name;

        @FixedField(offset = 10, length = 10)
        String surname;

        @FixedField(offset = 20, length = 3)
        int age;
    }

    static class PersonWithoutTrim {
        @FixedField(offset = 0, length = 10, trim = false)
        String name;

        @FixedField(offset = 10, length = 10, trim = false)
        String surname;

        @FixedField(offset = 20, length = 3)
        int age;
    }

    static class PersonWithDefault {
        @FixedField(offset = 0, length = 10)
        String name;

        @FixedField(offset = 10, length = 10)
        String surname;

        @FixedField(offset = 20, length = 3)
        int age;

        int defaultInt;
        String defaultString;
    }

    static class PersonWithMoreFields {
        @FixedField(offset = 0, length = 10)
        String name;

        @FixedField(offset = 10, length = 10)
        String surname;

        @FixedField(offset = 20, length = 3)
        int age;

        @FixedField(offset = 23, length = 13)
        long id;

        @FixedField(offset = 36, length = 6)
        double salary;
    }

    static class PersonWithDate {
        @FixedField(offset = 0, length = 10)
        String name;

        @FixedField(offset = 20, length = 3)
        int age;

        @FixedField(offset = 23, length = 8)
        LocalDate birthDate;
    }

    static class PersonWithDateCustomFormat {
        @FixedField(offset = 0, length = 10)
        String name;

        @FixedField(offset = 20, length = 3)
        int age;

        @FixedField(offset = 23, length = 10, format = "yyyy-MM-dd")
        LocalDate birthDate;
    }

    static class PersonWithDateTime {
        @FixedField(offset = 0, length = 10)
        String name;

        @FixedField(offset = 20, length = 3)
        int age;

        @FixedField(offset = 23, length = 14)
        LocalDateTime createdAt;
    }

    static class PersonWithBoolean {
        @FixedField(offset = 0, length = 10)
        String name;

        @FixedField(offset = 20, length = 3)
        int age;

        @FixedField(offset = 23, length = 1)
        boolean isActive;
    }

    static class PersonWithBigDecimal {
        @FixedField(offset = 0, length = 10)
        String name;

        @FixedField(offset = 20, length = 3)
        int age;

        @FixedField(offset = 23, length = 7)
        java.math.BigDecimal amount;
    }

    record PersonRecord(
            @FixedField(offset = 0, length = 10) String name,
            @FixedField(offset = 10, length = 10) String surname,
            @FixedField(offset = 20, length = 3) int age
    ) {}

    record PersonRecordWithMoreFields(
            @FixedField(offset = 0, length = 10) String name,
            @FixedField(offset = 10, length = 10) String surname,
            @FixedField(offset = 20, length = 3) int age,
            @FixedField(offset = 23, length = 13) long id
    ) {}

    record PersonRecordWithDefault(
            @FixedField(offset = 0, length = 10) String name,
            @FixedField(offset = 10, length = 10) String surname,
            @FixedField(offset = 20, length = 3) int age,
            int defaultInt,
            String defaultString
    ) {}

    record PersonRecordWithDate(
            @FixedField(offset = 0, length = 10) String name,
            @FixedField(offset = 20, length = 3) int age,
            @FixedField(offset = 23, length = 8) LocalDate birthDate
    ) {}

    record PersonRecordWithPrimitives(
            @FixedField(offset = 0, length = 10) String name,
            @FixedField(offset = 20, length = 3) int age,
            @FixedField(offset = 23, length = 13) long id,
            @FixedField(offset = 36, length = 6) double salary,
            @FixedField(offset = 42, length = 6) float height,
            @FixedField(offset = 48, length = 4) boolean isActive
    ) {}

    record PersonRecordWithAllPrimitives(
            @FixedField(offset = 0, length = 10) String name,
            @FixedField(offset = 20, length = 3) int age,
            int defaultInt,
            long defaultLong,
            double defaultDouble,
            float defaultFloat,
            short defaultShort,
            byte defaultByte,
            char defaultChar,
            boolean defaultBoolean
    ) {}

    static class PersonWithoutNoArgsConstructor {
        @FixedField(offset = 0, length = 10)
        String name;

        @FixedField(offset = 10, length = 10)
        String surname;

        @FixedField(offset = 20, length = 3)
        int age;

        // Constructor with parameters - no no-args constructor
        PersonWithoutNoArgsConstructor(String name, String surname, int age) {
            this.name = name;
            this.surname = surname;
            this.age = age;
        }
    }
}
