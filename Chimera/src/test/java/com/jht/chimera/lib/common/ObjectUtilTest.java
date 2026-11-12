package com.jht.chimera.lib.common;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class ObjectUtilTest {
    @Test
    public void testHashString() {
        String value = "foo";
        assertEquals(value.hashCode(), ObjectUtil.hashCode(value));
    }

    @Test
    public void testHashNull() {
        assertEquals(0, ObjectUtil.hashCode(null));
    }

    @Test
    public void testHashVarargs() {
        Object object1 = "foo";
        Object object2 = new Object();
        Object object3 = null;
        Object object4 = 5000000000L;
        List<Object> list = Arrays.asList(object1, object2, object3, object4);
        assertEquals(list.hashCode(), ObjectUtil.hash(object1, object2, object3, object4));
    }
}
