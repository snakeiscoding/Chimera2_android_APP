package com.jht.chimera.test.exceptions;

public class AuthorizationFailure extends Exception {
    private final String message;

    public AuthorizationFailure(String message) {
        this.message = message;
    }

    public String message() { return message; }
}
