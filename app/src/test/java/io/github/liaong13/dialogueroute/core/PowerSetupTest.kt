package io.github.liaong13.dialogueroute.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM tests for the setup verdict and the autostart route table. Both are pure
 * logic, so this runs without a phone.
 */
class PowerSetupTest {

    @Test
    fun `battery exemption does not gate the basic prerequisites`() {
        assertTrue(PowerSetup.verdict(true, true, true, true).ready)
        assertFalse(PowerSetup.verdict(false, true, true, true).ready)
        assertTrue(PowerSetup.verdict(false, true, true, true, xposed = true).ready)
        assertFalse(PowerSetup.verdict(true, false, true, true).ready)
        assertFalse(PowerSetup.verdict(true, true, false, true).ready)
        assertTrue(
            "battery exemption is a background recommendation, not a basic prerequisite",
            PowerSetup.verdict(true, true, true, false).ready)
    }

    @Test
    fun `missing lists only what is actually missing, in setup order`() {
        assertEquals(emptyList<String>(), PowerSetup.verdict(true, true, true, true).missing)
        assertEquals(
            listOf("无障碍权限或已验证的 Xposed 采集", "悬浮窗权限", "判断接口密钥"),
            PowerSetup.verdict(false, false, false, false).missing)
        assertEquals(
            listOf("判断接口密钥"),
            PowerSetup.verdict(true, true, false, false).missing)
        assertEquals(
            emptyList<String>(),
            PowerSetup.verdict(true, true, true, false).missing)
    }

    @Test
    fun `battery advice remains visible even when basic setup is ready`() {
        val verdict = PowerSetup.verdict(true, true, true, false)
        assertTrue(verdict.ready)
        assertEquals(emptyList<String>(), verdict.missing)
        assertEquals(listOf("允许忽略系统电池优化"), verdict.recommendations)
        assertEquals(emptyList<String>(), PowerSetup.verdict(true, true, true, true).recommendations)
    }

    @Test
    fun `the MIUI and HyperOS route is tried first`() {
        val first = PowerSetup.AUTOSTART_ROUTES.first()
        assertEquals("com.miui.securitycenter", first.pkg)
        assertTrue(
            "HyperOS is the target of this setup flow",
            first.cls.contains("autostart", ignoreCase = true))
    }

    @Test
    fun `every autostart route is complete`() {
        assertTrue("there has to be somewhere to send the user", PowerSetup.AUTOSTART_ROUTES.isNotEmpty())
        for (route in PowerSetup.AUTOSTART_ROUTES) {
            assertTrue("blank package in " + route.label, route.pkg.isNotBlank())
            assertTrue("blank class in " + route.label, route.cls.isNotBlank())
            assertTrue("blank label", route.label.isNotBlank())
            assertTrue("class must be fully qualified: " + route.cls, route.cls.contains('.'))
        }
    }
}
