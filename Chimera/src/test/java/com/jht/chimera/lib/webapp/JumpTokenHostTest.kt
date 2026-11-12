package com.jht.chimera.lib.webapp

import android.app.Application
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// Robolectric because JumpTokenHost parses with android.net.Uri
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class JumpTokenHostTest {

    private fun allows(url: String?) = JumpTokenHost.allows(url)

    @Test
    fun `allows https jfit co and its subdomains`() {
        assertTrue(allows("https://jfit.co"))
        assertTrue(allows("https://jfit.co/path"))
        assertTrue(allows("https://dev-matrixworkouts.jfit.co/survey/after-exercise?exercise_id=manual%3A1"))
        assertTrue(allows("https://a.b.jfit.co:443/x"))
    }

    @Test
    fun `allows https matrixfitness com and its subdomains`() {
        assertTrue(allows("https://matrixfitness.com"))
        assertTrue(allows("https://workouts.matrixfitness.com/survey/after-exercise?exercise_id=manual%3A1"))
        assertTrue(allows("https://a.b.matrixfitness.com:443/x"))
        assertTrue(allows("HTTPS://Workouts.MatrixFitness.COM/survey"))
    }

    @Test
    fun `rejects plain http matrixfitness com`() {
        assertFalse(allows("http://matrixfitness.com"))
        assertFalse(allows("http://workouts.matrixfitness.com/survey"))
    }

    @Test
    fun `rejects matrixfitness com lookalikes`() {
        assertFalse(allows("https://matrixfitness.com.evil.com"))
        assertFalse(allows("https://evilmatrixfitness.com"))
        assertFalse(allows("https://evil-matrixfitness.com"))
        assertFalse(allows("https://matrixfitness.co"))
        assertFalse(allows("https://matrixfitness.com@evil.com/"))
        assertFalse(allows("https://evil.com\\@workouts.matrixfitness.com/"))
        assertFalse(allows("https://evil.com/workouts.matrixfitness.com"))
    }

    @Test
    fun `rejects other hosts`() {
        assertFalse(allows("https://example.com/survey"))
        assertFalse(allows("https://matrixfitness.example.com"))
    }

    @Test
    fun `allows characters java net URI rejects`() {
        assertTrue(allows("https://weather.jfit.co/?a=b|c"))
        assertTrue(allows("https://matrixworkouts.jfit.co/console?f={x}"))
        assertTrue(allows("https://weather.jfit.co/?pct=100%"))
        assertTrue(allows("https://weather.jfit.co/?x=a^b"))
        assertTrue(allows("https://my_app.jfit.co/"))
    }

    @Test
    fun `host match ignores case`() {
        assertTrue(allows("HTTPS://Dev-MatrixWorkouts.JFIT.CO/survey"))
    }

    @Test
    fun `rejects plain http`() {
        assertFalse(allows("http://jfit.co"))
        assertFalse(allows("http://dev-matrixworkouts.jfit.co/survey"))
    }

    @Test
    fun `rejects lookalike hosts`() {
        assertFalse(allows("https://jfit.co.evil.com"))
        assertFalse(allows("https://evil-jfit.co"))
        assertFalse(allows("https://evil.com/jfit.co"))
        assertFalse(allows("https://evil.com/?ref=jfit"))
        assertFalse(allows("https://jfit.com"))
        assertFalse(allows("https://myjfit.co"))
    }

    @Test
    fun `rejects userinfo that only looks like jfit`() {
        assertFalse(allows("https://jfit.co@evil.com/"))
        assertFalse(allows("https://user:pass@evil.com/x.jfit.co"))
    }

    @Test
    fun `rejects hosts a browser would read differently`() {
        // browsers treat a backslash as a slash, so this page is on evil.com
        assertFalse(allows("https://evil.com\\@jfit.co/"))
        assertFalse(allows("https://evil.com%2F.jfit.co/"))
        assertFalse(allows("https://evil.com#@jfit.co"))
        assertFalse(allows("https://evil.com?@jfit.co"))
    }

    @Test
    fun `shim runs on any http page mentioning jfit`() {
        assertTrue(JumpTokenHost.allowsShim("https://jfit.co"))
        assertTrue(JumpTokenHost.allowsShim("http://dev-matrixworkouts.jfit.co/survey"))
        assertTrue(JumpTokenHost.allowsShim("https://evil.com/?ref=jfit"))
    }

    @Test
    fun `shim skips pages without http or jfit`() {
        assertFalse(JumpTokenHost.allowsShim(null))
        assertFalse(JumpTokenHost.allowsShim("https://example.com"))
        assertFalse(JumpTokenHost.allowsShim("file:///sdcard/jfit.co.html"))
    }

    @Test
    fun `rejects null blank and unparseable urls`() {
        assertFalse(allows(null))
        assertFalse(allows(""))
        assertFalse(allows("not a url"))
        assertFalse(allows("about:blank"))
        assertFalse(allows("file:///sdcard/jfit.co.html"))
    }
}
