package com.xenonware.store

import com.xenonware.store.util.Util
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testPrereleaseComparison() {
        // Essentials case: 18.5-beta.1 vs 18.5-beta.2
        assertTrue(Util.isNewerVersion("18.5-beta.1", "18.5-beta.2"))
        assertFalse(Util.isNewerVersion("18.5-beta.2", "18.5-beta.1"))
        assertFalse(Util.isNewerVersion("18.5-beta.1", "18.5-beta.1"))

        // Beta to stable
        assertTrue(Util.isNewerVersion("18.5-beta.1", "18.5"))
        assertFalse(Util.isNewerVersion("18.5", "18.5-beta.1"))

        // Older base to newer beta
        assertTrue(Util.isNewerVersion("18.4", "18.5-beta.1"))
        assertFalse(Util.isNewerVersion("18.5-beta.1", "18.4"))

        // Numeric token comparisons
        assertTrue(Util.isNewerVersion("18.5-beta.9", "18.5-beta.10"))
        assertFalse(Util.isNewerVersion("18.5-beta.10", "18.5-beta.9"))

        // Pre-release stages
        assertTrue(Util.isNewerVersion("18.5-alpha.1", "18.5-beta.1"))
        assertTrue(Util.isNewerVersion("18.5-beta.1", "18.5-rc.1"))
        assertTrue(Util.isNewerVersion("18.5-rc.1", "18.5"))

        // Without dot in suffix
        assertTrue(Util.isNewerVersion("18.5-beta1", "18.5-beta2"))

        // Standard semver
        assertTrue(Util.isNewerVersion("1.0.0", "1.0.1"))
        assertTrue(Util.isNewerVersion("1.0.0", "2.0.0"))
        assertFalse(Util.isNewerVersion("2.0.0", "1.0.0"))

        // 'v' prefix
        assertTrue(Util.isNewerVersion("v18.5-beta.1", "18.5-beta.2"))
        assertTrue(Util.isNewerVersion("18.5-beta.1", "v18.5-beta.2"))

        // Empty checks
        assertTrue(Util.isNewerVersion("", "18.5-beta.1"))
        assertFalse(Util.isNewerVersion("18.5-beta.1", ""))
    }
}