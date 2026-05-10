package com.meghna.audioanalyzer

import com.meghna.audioanalyzer.screens.AudioRoutingPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for AudioRoutingPolicy.
 *
 * Verifies that Android's AudioPolicyManager routing rules are correctly
 * implemented — each stream type routes to the right device based on
 * what is currently connected, following Android's priority chain.
 */
class AudioRoutingPolicyTest {

    // ─── MUSIC Stream Tests ───────────────────────────────────────────────────

    @Test
    fun `music routes to bluetooth A2DP when connected`() {
        val available = setOf("Bluetooth A2DP", "USB Headset", "Built-in Speaker")
        assertTrue(AudioRoutingPolicy.shouldConnect("MUSIC", "Bluetooth A2DP", available))
        assertFalse(AudioRoutingPolicy.shouldConnect("MUSIC", "USB Headset", available))
        assertFalse(AudioRoutingPolicy.shouldConnect("MUSIC", "Built-in Speaker", available))
    }

    @Test
    fun `music routes to USB headset when no bluetooth available`() {
        val available = setOf("USB Headset", "Built-in Speaker")
        assertTrue(AudioRoutingPolicy.shouldConnect("MUSIC", "USB Headset", available))
        assertFalse(AudioRoutingPolicy.shouldConnect("MUSIC", "Built-in Speaker", available))
    }

    @Test
    fun `music routes to wired headphones when no bluetooth or USB`() {
        val available = setOf("Wired Headphones", "Built-in Speaker")
        assertTrue(AudioRoutingPolicy.shouldConnect("MUSIC", "Wired Headphones", available))
        assertFalse(AudioRoutingPolicy.shouldConnect("MUSIC", "Built-in Speaker", available))
    }

    @Test
    fun `music routes to wired headset when no higher priority device available`() {
        val available = setOf("Wired Headset", "Built-in Speaker")
        assertTrue(AudioRoutingPolicy.shouldConnect("MUSIC", "Wired Headset", available))
        assertFalse(AudioRoutingPolicy.shouldConnect("MUSIC", "Built-in Speaker", available))
    }

    @Test
    fun `music routes to speaker when no headset connected`() {
        val available = setOf("Earpiece", "Built-in Speaker")
        assertTrue(AudioRoutingPolicy.shouldConnect("MUSIC", "Built-in Speaker", available))
        assertFalse(AudioRoutingPolicy.shouldConnect("MUSIC", "Earpiece", available))
    }

    @Test
    fun `music never routes to earpiece`() {
        val available = setOf("Earpiece", "Built-in Speaker")
        assertFalse(AudioRoutingPolicy.shouldConnect("MUSIC", "Earpiece", available))
    }

    @Test
    fun `music never routes to telephony`() {
        val available = setOf("Telephony", "Built-in Speaker")
        assertFalse(AudioRoutingPolicy.shouldConnect("MUSIC", "Telephony", available))
    }

    // ─── VOICE_CALL Stream Tests ──────────────────────────────────────────────

    @Test
    fun `voice call routes to bluetooth SCO when connected`() {
        val available = setOf("Bluetooth SCO", "Earpiece", "Built-in Speaker")
        assertTrue(AudioRoutingPolicy.shouldConnect("VOICE_CALL", "Bluetooth SCO", available))
        assertFalse(AudioRoutingPolicy.shouldConnect("VOICE_CALL", "Earpiece", available))
        assertFalse(AudioRoutingPolicy.shouldConnect("VOICE_CALL", "Built-in Speaker", available))
    }

    @Test
    fun `voice call routes to wired headset when no bluetooth SCO`() {
        val available = setOf("Wired Headset", "Earpiece", "Built-in Speaker")
        assertTrue(AudioRoutingPolicy.shouldConnect("VOICE_CALL", "Wired Headset", available))
        assertFalse(AudioRoutingPolicy.shouldConnect("VOICE_CALL", "Earpiece", available))
    }

    @Test
    fun `voice call routes to telephony path when available`() {
        val available = setOf("Telephony", "Earpiece", "Built-in Speaker")
        assertTrue(AudioRoutingPolicy.shouldConnect("VOICE_CALL", "Telephony", available))
        assertFalse(AudioRoutingPolicy.shouldConnect("VOICE_CALL", "Earpiece", available))
    }

    @Test
    fun `voice call routes to earpiece as final fallback`() {
        val available = setOf("Earpiece", "Built-in Speaker")
        assertTrue(AudioRoutingPolicy.shouldConnect("VOICE_CALL", "Earpiece", available))
    }

    @Test
    fun `voice call never routes to built-in speaker`() {
        val available = setOf("Earpiece", "Built-in Speaker", "Telephony")
        assertFalse(AudioRoutingPolicy.shouldConnect("VOICE_CALL", "Built-in Speaker", available))
    }

    @Test
    fun `voice call never routes to bluetooth A2DP - A2DP is music only`() {
        val available = setOf("Bluetooth A2DP", "Earpiece", "Built-in Speaker")
        assertFalse(AudioRoutingPolicy.shouldConnect("VOICE_CALL", "Bluetooth A2DP", available))
    }

    // ─── RING Stream Tests ────────────────────────────────────────────────────

    @Test
    fun `ring always routes to speaker only`() {
        val available = setOf("Earpiece", "Built-in Speaker", "USB Headset")
        assertTrue(AudioRoutingPolicy.shouldConnect("RING", "Built-in Speaker", available))
    }

    @Test
    fun `ring never routes to headset even when connected`() {
        val available = setOf("USB Headset", "Bluetooth A2DP", "Built-in Speaker")
        assertFalse(AudioRoutingPolicy.shouldConnect("RING", "USB Headset", available))
        assertFalse(AudioRoutingPolicy.shouldConnect("RING", "Bluetooth A2DP", available))
    }

    @Test
    fun `ring never routes to earpiece`() {
        val available = setOf("Earpiece", "Built-in Speaker")
        assertFalse(AudioRoutingPolicy.shouldConnect("RING", "Earpiece", available))
    }

    // ─── ALARM Stream Tests ───────────────────────────────────────────────────

    @Test
    fun `alarm always routes to speaker - Android safety policy`() {
        val available = setOf("Bluetooth A2DP", "USB Headset", "Built-in Speaker")
        assertTrue(AudioRoutingPolicy.shouldConnect("ALARM", "Built-in Speaker", available))
        assertFalse(AudioRoutingPolicy.shouldConnect("ALARM", "Bluetooth A2DP", available))
        assertFalse(AudioRoutingPolicy.shouldConnect("ALARM", "USB Headset", available))
    }

    @Test
    fun `alarm never routes to earpiece`() {
        val available = setOf("Earpiece", "Built-in Speaker")
        assertFalse(AudioRoutingPolicy.shouldConnect("ALARM", "Earpiece", available))
    }

    // ─── SYSTEM Stream Tests ──────────────────────────────────────────────────

    @Test
    fun `system sounds always route to speaker`() {
        val available = setOf("Bluetooth A2DP", "Built-in Speaker", "Earpiece")
        assertTrue(AudioRoutingPolicy.shouldConnect("SYSTEM", "Built-in Speaker", available))
        assertFalse(AudioRoutingPolicy.shouldConnect("SYSTEM", "Bluetooth A2DP", available))
        assertFalse(AudioRoutingPolicy.shouldConnect("SYSTEM", "Earpiece", available))
    }

    // ─── NOTIFICATION Stream Tests ────────────────────────────────────────────

    @Test
    fun `notification routes to music target AND speaker when headset connected`() {
        val available = setOf("USB Headset", "Built-in Speaker", "Earpiece")
        assertTrue(AudioRoutingPolicy.shouldConnect("NOTIFICATION", "USB Headset", available))
        assertTrue(AudioRoutingPolicy.shouldConnect("NOTIFICATION", "Built-in Speaker", available))
        assertFalse(AudioRoutingPolicy.shouldConnect("NOTIFICATION", "Earpiece", available))
    }

    @Test
    fun `notification routes to speaker only when no headset`() {
        val available = setOf("Built-in Speaker", "Earpiece")
        assertTrue(AudioRoutingPolicy.shouldConnect("NOTIFICATION", "Built-in Speaker", available))
        assertFalse(AudioRoutingPolicy.shouldConnect("NOTIFICATION", "Earpiece", available))
    }

    @Test
    fun `notification follows bluetooth A2DP when connected`() {
        val available = setOf("Bluetooth A2DP", "Built-in Speaker")
        assertTrue(AudioRoutingPolicy.shouldConnect("NOTIFICATION", "Bluetooth A2DP", available))
        assertTrue(AudioRoutingPolicy.shouldConnect("NOTIFICATION", "Built-in Speaker", available))
    }

    // ─── Priority Chain Tests ─────────────────────────────────────────────────

    @Test
    fun `bluetooth A2DP beats USB headset for music`() {
        val available = setOf("Bluetooth A2DP", "USB Headset", "Wired Headphones", "Built-in Speaker")
        assertTrue(AudioRoutingPolicy.shouldConnect("MUSIC", "Bluetooth A2DP", available))
        assertFalse(AudioRoutingPolicy.shouldConnect("MUSIC", "USB Headset", available))
        assertFalse(AudioRoutingPolicy.shouldConnect("MUSIC", "Wired Headphones", available))
        assertFalse(AudioRoutingPolicy.shouldConnect("MUSIC", "Built-in Speaker", available))
    }

    @Test
    fun `bluetooth SCO beats wired headset for voice call`() {
        val available = setOf("Bluetooth SCO", "Wired Headset", "Telephony", "Earpiece")
        assertTrue(AudioRoutingPolicy.shouldConnect("VOICE_CALL", "Bluetooth SCO", available))
        assertFalse(AudioRoutingPolicy.shouldConnect("VOICE_CALL", "Wired Headset", available))
        assertFalse(AudioRoutingPolicy.shouldConnect("VOICE_CALL", "Telephony", available))
        assertFalse(AudioRoutingPolicy.shouldConnect("VOICE_CALL", "Earpiece", available))
    }

    @Test
    fun `bluetooth A2DP and SCO are different profiles - A2DP does not help voice call`() {
        val available = setOf("Bluetooth A2DP", "Earpiece")
        assertFalse(AudioRoutingPolicy.shouldConnect("VOICE_CALL", "Bluetooth A2DP", available))
        assertTrue(AudioRoutingPolicy.shouldConnect("VOICE_CALL", "Earpiece", available))
    }

@Test
fun `music falls back to speaker when no headset devices available`() {
    val available = emptySet<String>()
    // When no devices detected, speaker is the safe fallback
    assertTrue(AudioRoutingPolicy.shouldConnect("MUSIC", "Built-in Speaker", available))
    assertFalse(AudioRoutingPolicy.shouldConnect("MUSIC", "Earpiece", available))
    assertFalse(AudioRoutingPolicy.shouldConnect("MUSIC", "Telephony", available))
}
@Test
fun `unknown stream type does not route to earpiece or telephony`() {
    val available = setOf("Earpiece", "Built-in Speaker", "Telephony")
    assertFalse(AudioRoutingPolicy.shouldConnect("UNKNOWN", "Earpiece", available))
    assertFalse(AudioRoutingPolicy.shouldConnect("UNKNOWN", "Telephony", available))
    assertTrue(AudioRoutingPolicy.shouldConnect("UNKNOWN", "Built-in Speaker", available))
}

@Test
fun `music routes to bluetooth LE when no A2DP but LE available`() {
    val available = setOf("Bluetooth LE", "USB Headset", "Built-in Speaker")
    assertTrue(AudioRoutingPolicy.shouldConnect("MUSIC", "Bluetooth LE", available))
    assertFalse(AudioRoutingPolicy.shouldConnect("MUSIC", "USB Headset", available))
}

@Test
fun `bluetooth A2DP beats bluetooth LE for music`() {
    val available = setOf("Bluetooth A2DP", "Bluetooth LE", "Built-in Speaker")
    assertTrue(AudioRoutingPolicy.shouldConnect("MUSIC", "Bluetooth A2DP", available))
    assertFalse(AudioRoutingPolicy.shouldConnect("MUSIC", "Bluetooth LE", available))
}

@Test
fun `wired headphones beat wired headset for music`() {
    val available = setOf("Wired Headphones", "Wired Headset", "Built-in Speaker")
    assertTrue(AudioRoutingPolicy.shouldConnect("MUSIC", "Wired Headphones", available))
    assertFalse(AudioRoutingPolicy.shouldConnect("MUSIC", "Wired Headset", available))
}

@Test
fun `voice call prefers wired headset over wired headphones - headset has mic`() {
    val available = setOf("Wired Headphones", "Wired Headset", "Earpiece")
    assertTrue(AudioRoutingPolicy.shouldConnect("VOICE_CALL", "Wired Headset", available))
    assertFalse(AudioRoutingPolicy.shouldConnect("VOICE_CALL", "Wired Headphones", available))
}

@Test
fun `notification does not route to earpiece even when connected`() {
    val available = setOf("Earpiece", "Built-in Speaker")
    assertFalse(AudioRoutingPolicy.shouldConnect("NOTIFICATION", "Earpiece", available))
}

@Test
fun `notification with bluetooth A2DP routes to both A2DP and speaker`() {
    val available = setOf("Bluetooth A2DP", "Earpiece", "Built-in Speaker")
    assertTrue(AudioRoutingPolicy.shouldConnect("NOTIFICATION", "Bluetooth A2DP", available))
    assertTrue(AudioRoutingPolicy.shouldConnect("NOTIFICATION", "Built-in Speaker", available))
    assertFalse(AudioRoutingPolicy.shouldConnect("NOTIFICATION", "Earpiece", available))
}


@Test
fun `USB headset beats USB device for music`() {
    val available = setOf("USB Headset", "USB Device", "Built-in Speaker")
    assertTrue(AudioRoutingPolicy.shouldConnect("MUSIC", "USB Headset", available))
    assertFalse(AudioRoutingPolicy.shouldConnect("MUSIC", "USB Device", available))
}

@Test
fun `internal bus device never receives any stream`() {
    val available = setOf("Bus", "Built-in Speaker")
    assertFalse(AudioRoutingPolicy.shouldConnect("MUSIC", "Bus", available))
    assertFalse(AudioRoutingPolicy.shouldConnect("RING", "Bus", available))
    assertFalse(AudioRoutingPolicy.shouldConnect("VOICE_CALL", "Bus", available))
}
}