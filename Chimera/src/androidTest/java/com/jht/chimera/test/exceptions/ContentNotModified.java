package com.jht.chimera.test.exceptions;

public class ContentNotModified extends Exception {
    private final String message;

    public ContentNotModified(String message) {
        this.message = message;
    }

    public String message() { return message; }
}
