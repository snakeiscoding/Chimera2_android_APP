package com.jht.chimera.test.annotations.runner;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This test will not run on an emulator.  It requires Chimera Hardware
 */
@SuppressWarnings("WeakerAccess")
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface SplashApp {
}
