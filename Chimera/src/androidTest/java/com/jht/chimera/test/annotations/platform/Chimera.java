package com.jht.chimera.test.annotations.platform;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This test will only run in the Chimera flavor
 */
@SuppressWarnings("WeakerAccess")
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Chimera {
}
