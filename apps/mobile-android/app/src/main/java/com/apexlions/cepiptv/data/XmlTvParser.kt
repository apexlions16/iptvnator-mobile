package com.apexlions.cepiptv.data

import com.apexlions.cepiptv.model.EpgSnapshot
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object XmlTvParser {
    private data class Programme(
        val channel: String,
        val start: Long,
        val end: Long,
        val title: String,
    )

    fun parse(content: String, nowMillis: Long = System.currentTimeMillis()): Map<String, EpgSnapshot> {
        val parser = XmlPullParserFactory.newInstance().newPullParser().apply {
            setInput(StringReader(content))
        }
        val programmes = mutableListOf<Programme>()
        var event = parser.eventType
        var channel = ""
        var start = 0L
        var end = 0L
        var title: String? = null

        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "programme" -> {
                        channel = parser.getAttributeValue(null, "channel").orEmpty()
                        start = parseXmlTvDate(parser.getAttributeValue(null, "start")) ?: 0L
                        end = parseXmlTvDate(parser.getAttributeValue(null, "stop")) ?: 0L
                        title = null
                    }
                    "title" -> if (channel.isNotBlank()) title = parser.nextText().trim()
                }
                XmlPullParser.END_TAG -> if (parser.name == "programme") {
                    val safeTitle = title?.takeIf(String::isNotBlank)
                    if (channel.isNotBlank() && start > 0 && end > start && safeTitle != null) {
                        programmes += Programme(channel, start, end, safeTitle)
                    }
                    channel = ""
                }
            }
            event = parser.next()
        }

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
