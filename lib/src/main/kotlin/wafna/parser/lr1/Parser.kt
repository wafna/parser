package wafna.parser.lr1

class Config private constructor(val production: Production, val dot: Int, val follows: Set<NonTerminal>) {
    constructor(production: Production, follows: Set<NonTerminal>) : this(production, 0, follows)
    // When it goes off the end we have a reduction.
    val dotted = if (dot < production.rhs.size) production.rhs[dot] else null
    fun bump() = Config(production, dot + 1, follows)
    fun equalsProductionDot(that: Config): Boolean =
        production == that.production && dot == that.dot
    operator fun plus(other: Config): Config {
        require(equalsProductionDot(other)) {
            "Different production configs."
        }
        return Config(production, dot, follows + other.follows)
    }
}

val Config.show: String
    get() = "${production.lhs} → ${
        buildList {
            production.rhs.withIndex().forEach { (i, e) ->
                if (i == dot) add("•")
                add(e.toString())
            }
            if (production.rhs.size <= dot)
                add("•")
        }.joinToString(" ")
    }"

class Parser {
}