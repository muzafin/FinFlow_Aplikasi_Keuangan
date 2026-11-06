package com.logikawan.finflow.core.util

import kotlin.random.Random
import kotlinx.datetime.*

fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()
fun today(): LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
fun newId(): String = "${nowMillis()}-${Random.nextLong().toULong().toString(16)}"
