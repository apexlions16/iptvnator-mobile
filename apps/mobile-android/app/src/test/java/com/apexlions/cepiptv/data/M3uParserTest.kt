package com.apexlions.cepiptv.data

import com.apexlions.cepiptv.model.MediaKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class M3uParserTest {
    @Test
    fun `kanal adı grup logo ve EPG bilgilerini ayrıştırır`() {
        val content = """
            #EXTM3U
            #EXTINF:-1 tvg-id="trt1.tr" tvg-name="TRT 1" tvg-logo="https://ornek.com/trt.png" group-title="Ulusal",TRT Bir
            https://ornek.com/canli/trt1.m3u8
        """.trimIndent()

        val channels = M3uParser.parse(content, sourceKey = "test", userAgent = "Cep-Test/1.0")

        assertEquals(1, channels.size)
        assertEquals("TRT 1", channels.first().name)
        assertEquals("Ulusal", channels.first().group)
        assertEquals("https://ornek.com/trt.png", channels.first().logoUrl)
        assertEquals("https://ornek.com/canli/trt1.m3u8", channels.first().url)
        assertEquals("trt1.tr", channels.first().epgId)
        assertEquals("Cep-Test/1.0", channels.first().userAgent)
    }

    @Test
    fun `radio true olan yayını radyo olarak işaretler`() {
        val content = """
            #EXTM3U
            #EXTINF:-1 radio="true" group-title="Radyolar",TRT Radyo
            https://ornek.com/radyo.mp3
        """.trimIndent()

        assertEquals(MediaKind.RADIO, M3uParser.parse(content).first().kind)
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
