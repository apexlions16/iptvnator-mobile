package com.apexlions.cepiptv.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class XmlTvParserTest {
    @Test
    fun `geçerli ve sonraki programı kanal kimliğine göre bulur`() {
        val content = """
            <?xml version="1.0" encoding="UTF-8"?>
            <tv>
                <programme start="20260101120000 +0300" stop="20260101130000 +0300" channel="trt1.tr">
                    <title lang="tr">Öğle Haberleri</title>
                </programme>
                <programme start="20260101130000 +0300" stop="20260101140000 +0300" channel="trt1.tr">
                    <title lang="tr">Spor Saati</title>
                </programme>
            </tv>
        """.trimIndent()

        val now = Instant.parse("2026-01-01T09:30:00Z").toEpochMilli()
        val snapshot = XmlTvParser.parse(content, now)["trt1.tr"]

        assertEquals("Öğle Haberleri", snapshot?.currentTitle)
        assertEquals("Spor Saati", snapshot?.nextTitle)
    }
}
