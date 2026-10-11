package io.github.marketkernel.uacryptography.example.widgets

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DAY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC)
private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'").withZone(ZoneOffset.UTC)

/** `2026-10-10 17:30:29 UTC`. */
fun formatTime(time: Instant): String = TIME_FORMAT.format(time)

/** `2026-10-10`. */
fun formatDay(time: Instant): String = DAY_FORMAT.format(time)

/** `1,234 bytes`. */
fun formatSize(bytes: Int): String = String.format(Locale.US, "%,d byte%s", bytes, if (bytes == 1) "" else "s")
