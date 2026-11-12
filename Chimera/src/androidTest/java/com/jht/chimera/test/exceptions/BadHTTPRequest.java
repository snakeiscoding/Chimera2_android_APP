package com.jht.chimera.test.exceptions;

public class BadHTTPRequest extends Exception {
    private final String message;

    public BadHTTPRequest(String message) {
        this.message = message;
    }

    public String message() { return message; }
}
