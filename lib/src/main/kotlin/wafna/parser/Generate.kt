package wafna.parser

class Config private constructor(val production: Production, initFollow: Set<Terminal>, val dot: Int) {
    constructor(production: Production) : this(production, emptySet(), 0)
    constructor(production: Production, initFollow: Set<Terminal>) : this(production, initFollow, 0)
    // When it goes off the end we have a reduction.
    val dotted = if (dot < production.rhs.size) production.rhs[dot] else null
    val follows = initFollow.toMutableSet()
    fun bump() = Config(production, follows, dot = dot + 1)
    // The terminal after the dot
    val follow: Set<Terminal> =
        if (dot + 1 >= production.rhs.size) emptySet()
        else when (val n = production.rhs[dot + 1]) {
            is Terminal -> setOf(n)
            is NonTerminal -> emptySet()
        }
    // For finding lookaheads to merge.
    infix fun equalsProd(other: Config): Boolean =
        production == other.production && dot == other.dot
    // For finding identical bases.
    infix fun equalsAll(other: Config): Boolean =
        production == other.production && dot == other.dot && follows == other.follows

    override fun toString(): String = show
}

data class Reduction(val to: NonTerminal, val count: Int)

/**
 * Each state does one thing.
 */
sealed interface Action
/** Accept the input. */
class Accept(val count: Int) : Action
/** Shift the next input. */
class Shift(val shifts: Map<TokenType, Int>) : Action
/** Reduce the stack. */
class Reduce(val reductions: Map<Terminal, Reduction>) : Action
/** Resolve a shift-reduce conflict. */
class Resolve(
    val reductions: Map<Terminal, Reduction>,
    val shifts: Map<TokenType, Int>
) : Action

/**
 * This represents the state as we're building the parser.
 * The state's basis is the set of configs that transitioned to it.
 * The state's extension is the closure on the dotted elements from the basis configs.
 */
internal data class ParseConfigState(val id: Int, val basis: List<Config>, val extension: List<Config>) {
    internal var action: Action? = null
    // Two states are equal if their bases are equal.
    fun basisEquals(other: List<Config>): Boolean =
        basis.size == other.size && basis.all { c -> other.any { it equalsAll c } }

    override fun toString(): String = show
}

data class ParseState(val id: Int, val action: Action, val configs: List<Config> = emptyList())

class Parser(val parseStates: List<ParseState>, val start: NonTerminal, val end: Terminal)

enum class ParserConfig { Dbg, Opt }

/**
 * Builds an LR(1) state machine for the input grammar.
 * The first production MUST be the augmenting production.
 * The parser can be generated with (Dbg) or without (Opt) configuration info for debugging or space-saving, respectively.
 */
fun generateParser(grammar: List<Production>, config: ParserConfig = ParserConfig.Opt): Parser {
    val augmenter = grammar.first()
    // The LHS and last element of the RHS of the augmenting production define the start and end tokens.
    val (start, end) = augmenter.lhs to augmenter.rhs.reversed().first()
    require(augmenter.rhs.reversed().drop(1).none { it == start || it == end }) {
        "The $start and $end tokens must not appear in the middle of the start production."
    }
    val grammar = grammar.drop(1).apply {
        filter { it.lhs == start || it.lhs == end || it.rhs.any { it == start || it == end } }.also { bad ->
            require(bad.isEmpty()) {
                "The $start and $end tokens must not appear outside of the start production:${bad.joinToString { "\n$it" }}"
            }
        }
    }
    val parseStates = mutableListOf<ParseConfigState>()
    // Compute the closure on the basis configs.
    fun runState(basis: List<Config>): ParseConfigState {
        require(parseStates.none { it.basisEquals(basis) })
        // Calculate the closure of the basis config(s).
        val closure = mutableListOf<Config>()
        // Closes on the last group of configs to be added to the closure.
        tailrec fun extend(configs: List<Config>) {
            // The next group of configs to add to the closure.
            val ext = mutableListOf<Config>()
            configs.forEach { config ->
                grammar.forEach { p ->
                    // Pick the productions whose LHS matches the dot.
                    if (config.dotted?.terminal == false && p.lhs == config.dotted) {
                        val e = Config(p, config.follow + config.follows)
                        var merged = false
                        // If an existing config in has the same production and dot we
                        // merge the follows sets.
                        for (c in (basis + closure + ext)) {
                            if (c.equalsProd(e)) {
                                merged = true
                                c.follows += e.follows
                                break
                            }
                        }
                        if (!merged)
                            ext.add(e)
                    }
                }
            }
            if (ext.isNotEmpty()) {
                closure.addAll(ext)
                extend(ext)
            }
        }
        extend(basis)

        val parseState = ParseConfigState(parseStates.size, basis, closure)
        parseStates.add(parseState)
        val reduces = mutableListOf<Config>()
        val shifts = mutableListOf<Pair<TokenType, Config>>()
        (basis + closure).forEach { config ->
            when (val dotted = config.dotted) {
                null -> reduces.add(config)
                else -> shifts.add(dotted to config)
            }
        }
        fun transitions() = mutableMapOf<TokenType, Int>().also { transitions ->
            for ((symbol, configs) in shifts.groupBy { it.first }) {
                require(!transitions.contains(symbol)) { "Shift conflict on $symbol in ${parseState.show}" }
                val newBasis = configs.map { it.second.bump() }
                val target = when (val t = parseStates.find { it.basisEquals(newBasis) }) {
                    null -> runState(newBasis)
                    else -> t
                }
                transitions[symbol] = target.id
            }
        }

        fun reductions() = mutableMapOf<Terminal, Reduction>().also { reductions ->
            for (config in reduces) {
                for (f in config.follows) {
                    if (reductions.contains(f)) {
                        error("Reduce conflict on $f in ${parseState.show}")
                    } else {
                        reductions[f] = Reduction(config.production.lhs, config.production.rhs.size)
                    }
                }
            }
        }

        val action = when (shifts.isNotEmpty() to reduces.isNotEmpty()) {
            true to false -> Shift(transitions())
            false to true -> reductions().let {
                if (it.isEmpty()) {
                    // This should only happen if the augmenting production isn't satisfying the requirements
                    // for the start and end elements.
                    require(1 == basis.size && closure.isEmpty()) { "Double plus ungood." }
                    val aug = basis.first()
                    require(aug.production.lhs == start && aug.follows.isEmpty()) { "So, we're not reducing the augmenting production?" }
                    Accept(aug.production.rhs.size)
                } else Reduce(it)
            }

            true to true -> Resolve(reductions(), transitions())
            else -> error("Empty state: ${parseState.show}")
        }
        parseState.action = action
        return parseState
    }
    runState(listOf(Config(augmenter)))
    val states = when (config) {
        ParserConfig.Dbg -> parseStates.map { ParseState(it.id, it.action!!, it.basis + it.extension) }
        ParserConfig.Opt -> parseStates.map { ParseState(it.id, it.action!!) }
    }
    return Parser(states, start, end as Terminal)
}

internal val ParseConfigState.show: String
    get() = buildString {
        appendLine("STATE $id")
        basis.forEach { appendLine("> ${it.show}") }
        extension.forEach { appendLine("  ${it.show}") }
        fun Map<Terminal, Reduction>.show(): String =
            toList().joinToString(", ") { "${it.first} → (${it.second.to}, ${it.second.count})" }

        fun Map<TokenType, Int>.show(): String =
            toList().joinToString(", ") { "${it.first} → ${it.second}" }
        when (val a = action) {
            null -> {} // appendLine("\t<no actions>")
            is Accept -> appendLine("\tACCEPT: ${a.count}")
            is Reduce -> appendLine("\tREDUCE: ${a.reductions.show()}")
            is Shift -> appendLine("\tSHIFT: ${a.shifts.show()}")
            is Resolve -> appendLine("\tRESOLVE:\n\tSHIFT: ${a.shifts.show()}\n\tREDUCE: ${a.reductions.show()}")
        }
    }


