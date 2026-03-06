package wafna.parser

import java.util.*

fun <T> Iterator<T>.toList(): List<T> = buildList { while (hasNext()) add(next()) }

fun Terminal.token(text: String): TerminalToken =
    TerminalToken(this, text)

