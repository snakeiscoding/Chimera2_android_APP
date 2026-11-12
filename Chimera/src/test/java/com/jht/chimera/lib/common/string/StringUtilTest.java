package com.jht.chimera.lib.common.string;

import org.junit.Assert;
import org.junit.Test;

public class StringUtilTest {
    @Test
    public void testUUID() {
        Assert.assertTrue(StringUtil.isUUID("00000000-0000-0000-0000-000000000000"));
        Assert.assertTrue(StringUtil.isUUID("abcdef12-3456-7890-0000-000000000000"));
        Assert.assertTrue(StringUtil.isUUID("ABCDEF12-3456-7890-0000-000000000000"));
        Assert.assertTrue(StringUtil.isUUID("ABCdef12-3456-7890-0000-000000000000"));
        Assert.assertFalse(StringUtil.isUUID("ABCdef12-3456-7890-ghjk-000000000000"));
        Assert.assertFalse(StringUtil.isUUID("ABCdef12-3456-7890-ghjk-00000000000"));
        Assert.assertFalse(StringUtil.isUUID("0"));
        Assert.assertFalse(StringUtil.isUUID("b"));
    }
}
