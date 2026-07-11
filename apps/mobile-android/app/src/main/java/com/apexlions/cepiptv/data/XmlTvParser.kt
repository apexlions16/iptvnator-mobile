package com.apexlions.cepiptv.data

import com.apexlions.cepiptv.model.EpgSnapshot
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.helpers.DefaultHandler
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.xml.parsers.SAXParserFactory

object XmlTvParser {
    private data class Programme(
        val channel: String,
        val start: Long,
        val end: Long,
        val title: String,
    )

    fun parse(content: String, nowMillis: Long = System.currentTimeMillis()): Map<String, EpgSnapshot> {
        require(!content.contains("<!DOCTYPE", ignoreCase = true)) {
            "Güvenlik nedeniyle DTD içeren XMLTV dosyaları desteklenmez."
        }
        require(!content.contains("<!ENTITY", ignoreCase = true)) {
            "Güvenlik nedeniyle harici varlık içeren XMLTV dosyaları desteklenmez."
        }

        val programmes = mutableListOf<Programme>()
        val handler = object : DefaultHandler() {
            private var channel = ""
            private var start = 0L
            private var end = 0L
            private var title: String? = null
            private var readingTitle = false
            private val text = StringBuilder()

            override fun startElement(uri: String?, localName: String?, qName: String?, attributes: Attributes) {
                when (elementName(localName, qName)) {
                    "programme" -> {
                        channel = attributes.getValue("channel").orEmpty()
                        start = parseXmlTvDate(attributes.getValue("start")) ?: 0L
                        end = parseXmlTvDate(attributes.getValue("stop")) ?: 0L
                        title = null
                    }
                    "title" -> if (channel.isNotBlank() && title == null) {
                        readingTitle = true
                        text.setLength(0)
                    }
                }
            }

            override fun characters(ch: CharArray, start: Int, length: Int) {
                if (readingTitle) text.append(ch, start, length)
            }

            override fun endElement(uri: String?, localName: String?, qName: String?) {
                when (elementName(localName, qName)) {
                    "title" -> if (readingTitle) {
                        title = text.toString().trim().takeIf(String::isNotBlank)
                        readingTitle = false
                    }
                    "programme" -> {
                        val safeTitle = title
                        if (channel.isNotBlank() && start > 0 && end > start && safeTitle != null) {
                            programmes += Programme(channel, start, end, safeTitle)
                        }
                        channel = ""
                        readingTitle = false
                    }
                }
            }
        }

        val factory = SAXParserFactory.newInstance().apply {
            isNamespaceAware = false
            isValidating = false
        }
        val reader = factory.newSAXParser().xmlReader
        listOf(
            "http://xml.org/sax/features/external-general-entities",
            "http://xml.org/sax/features/external-parameter-entities",
            "http://apache.org/xml/features/nonvalidating/load-external-dtd",
        ).forEach { feature -> runCatching { reader.setFeature(feature, false) } }
        reader.entityResolver = org.xml.sax.EntityResolver { _, _ -> InputSource(StringReader("")) }
        reader.contentHandler = handler
        reader.parse(InputSource(StringReader(content)))

        return programmes
            .groupBy(Programme::channel)
            .mapValues { (_, list) ->
                val ordered = list.sortedBy(Programme::start)
                val current = ordered.lastOrNull { nowMillis in it.start until it.end }
                val next = ordered.firstOrNull { it.start >= (current?.end ?: nowMillis) }
                EpgSnapshot(
                    currentTitle = current?.title,
                    currentStartMillis = current?.start,
                    currentEndMillis = current?.end,
                    nextTitle = next?.title,
                )
            }
    }

    private fun elementName(localName: String?, qName: String?): String =
        localName?.takeIf(String::isNotBlank) ?: qName.orEmpty()

    private fun parseXmlTvDate(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        val normalized = raw.trim().replace(Regex("\\s+"), " ")
        val candidates = buildList {
            add(normalized)
            if (!normalized.contains(' ')) add("$normalized +0000")
        }
        val patterns = listOf("yyyyMMddHHmmss Z", "yyyyMMddHHmm Z")
        for (candidate in candidates) {
            for (pattern in patterns) {
                val parsed: Date? = runCatching {
                    SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }.parse(candidate)
                }.getOrNull()
                if (parsed != null) return parsed.time
            }
        }
        return null
    }
}
