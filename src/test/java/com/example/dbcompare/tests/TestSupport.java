package com.example.dbcompare.tests;

import java.util.Objects;

public final class TestSupport {
    private TestSupport() {
    }

    public static void assertEquals(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
        }
    }

    public static void assertNull(Object actual, String message) {
        if (actual != null) {
            throw new AssertionError(message + " expected null but was=" + actual);
        }
    }

    public static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
