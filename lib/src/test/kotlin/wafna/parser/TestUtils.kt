package wafna.parser

fun <T> Iterator<T>.toList(): List<T> =
    buildList {
        while (hasNext()) {
            add(next())
        }
    }