package com.apexlions.cepiptv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class M3uParserTest {
    @Test
    fun `kanal adı grup ve logo bilgilerini ayrıştırır`() {
        val content = """
            #EXTM3U
            #EXTINF:-1 tvg-name="TRT 1" tvg-logo="https://ornek.com/trt.png" group-title="Ulusal",TRT Bir
            https://ornek.com/canli/trt1.m3u8
        """.trimIndent()

        val channels = M3uParser.parse(content)

        assertEquals(1, channels.size)
        assertEquals("TRT 1", channels.first().name)
        assertEquals("Ulusal", channels.first().group)
        assertEquals("https://ornek.com/trt.png", channels.first().logoUrl)
        assertEquals("https://ornek.com/canli/trt1.m3u8", channels.first().url)
    }

    @Test
    fun `yinelenen yayın adreslerini tek kanala indirger`() {
        val content = """
            #EXTM3U
            #EXTINF:-1,Birinci
            http://ornek.com/yayin.ts
            #EXTINF:-1,İkinci
            http://ornek.com/yayin.ts
        """.trimIndent()

        assertEquals(1, M3uParser.parse(content).size)
    }

    @Test
    fun `desteklenmeyen satırları yok sayar`() {
        val content = """
            #EXTM3U
            #EXTVLCOPT:http-referrer=https://ornek.com
            yerel-dosya.ts
        """.trimIndent()

        assertTrue(M3uParser.parse(content).isEmpty())
    }
}
