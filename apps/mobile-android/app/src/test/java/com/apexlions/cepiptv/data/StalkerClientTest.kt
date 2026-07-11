package com.apexlions.cepiptv.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StalkerClientTest {
    @Test
    fun `standart MAC adresini kabul eder`() {
        assertTrue(StalkerClient.isValidMac("00:1A:79:12:34:56"))
        assertTrue(StalkerClient.isValidMac("aa:bb:cc:dd:ee:ff"))
    }

    @Test
    fun `eksik veya geçersiz MAC adresini reddeder`() {
        assertFalse(StalkerClient.isValidMac("00:1A:79:12:34"))
        assertFalse(StalkerClient.isValidMac("00-1A-79-12-34-56"))
        assertFalse(StalkerClient.isValidMac("GG:1A:79:12:34:56"))
    }
}
