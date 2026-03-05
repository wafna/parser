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

// Each state does its thing.
sealed interface Action
class Accept(val count: Int) : Action
// Attempt to shift the next input.
class Shift(val shifts: Map<TokenType, ParseState>) : Action
// Reduce the stack.
class Reduce(val reductions: Map<Terminal, Reduction>) : Action
// Conflict; peek the next input and reduce on a match else shift.
class Resolve(
    val reductions: Map<Terminal, Reduction>,
    val shifts: Map<TokenType, ParseState>
) : Action

// The state's basis is the set of configs that transitioned to it.
// The state's extension is the closure on the dotted elements from the basis configs.
data class ParseState(val id: Int, val basis: List<Config>, val extension: List<Config>) {
    internal var action: Action? = null
    // Two states are equal if their bases are equal.
    fun basisEquals(other: List<Config>): Boolean =
        basis.size == other.size && basis.all { c -> other.any { it equalsAll c } }

    override fun toString(): String = show
}

class Parser(val parseStates: List<ParseState>, val start: TokenType, val end: TokenType)

// The first production defines the start fragment at the LHS and the end fragment at the end of the RHS.
// These fragments must appear nowhere else.
fun generateParser(grammar: List<Production>): Parser {
    val state0 = grammar.first()
    // The LHS and last element of the RHS of the augmenting production define the start and end symbols.
    // This is the purpose of the augmenting production.
    val (start, end) = state0.lhs to state0.rhs.reversed().first()
    require(state0.rhs.reversed().drop(1).none { it == start || it == end }) {
        "The $start and $end fragments must not appear in the middle of the start production."
    }
    val grammar = grammar.drop(1).apply {
        filter { it.lhs == start || it.lhs == end || it.rhs.any { it == start || it == end } }.also { bad ->
            require(bad.isEmpty()) {
                "The $start and $end fragments must not appear outside of the start production:${bad.joinToString { "\n$it" }}"
            }
        }
    }
    val parseStates = mutableListOf<ParseState>()
    // Compute the closure on the basis configs.
    fun runState(basis: List<Config>): ParseState {
        require(parseStates.none { it.basisEquals(basis) })
        // Calculate the closure of the basis config(s).
        val closure = mutableListOf<Config>()
        // Closes on the last group of configs to be added to the closure.
        tailrec fun extend(configs: List<Config>) {
            // The next group of configs to add to the closure.
            val ext = mutableListOf<Config>()
            configs.forEach { config ->
                grammar.forEach { p  ->
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

        val parseState = ParseState(parseStates.size, basis, closure)
        parseStates.add(parseState)
        val reduces = mutableListOf<Config>()
        val shifts = mutableListOf<Pair<TokenType, Config>>()
        (basis + closure).forEach { config ->
            when (val dotted = config.dotted) {
                null -> reduces.add(config)
                else -> shifts.add(dotted to config)
            }
        }
        fun transitions() = mutableMapOf<TokenType, ParseState>().also { transitions ->
            for ((symbol, configs) in shifts.groupBy { it.first }) {
                require(!transitions.contains(symbol)) { "Shift conflict on $symbol in ${parseState.show}" }
                val newBasis = configs.map { it.second.bump() }
                val target = when (val t = parseStates.find { it.basisEquals(newBasis) }) {
                    null -> runState(newBasis)
                    else -> t
                }
                transitions[symbol] = target
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
    runState(listOf(Config(state0)))
    return Parser(parseStates, start, end)
}
