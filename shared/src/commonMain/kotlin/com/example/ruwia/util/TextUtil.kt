package com.example.ruwia.util

import androidx.compose.ui.text.input.KeyboardType

/** Uppercases the first letter of every word (i.e. after start-of-text or any
 *  whitespace). The rest of each word is preserved exactly as typed. */
private val WORD_FIRST_LETTER = Regex("(?<=^|\\s)[a-z]")

fun capitalizeWords(text: String): String =
    WORD_FIRST_LETTER.replace(text) { it.value.uppercase() }

/** True when a [KeyboardType] represents free text (not numbers / passwords /
 *  emails / phones), i.e. fields that benefit from auto-capitalization. */
fun KeyboardType.isTextField(): Boolean =
    this == KeyboardType.Text || this == KeyboardType.Ascii