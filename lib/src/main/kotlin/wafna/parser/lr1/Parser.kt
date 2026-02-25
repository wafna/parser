package wafna.parser.lr1

sealed class SyntaxElementType(val name: String? = null) {
    override fun toString(): String = name ?: this::class.java.simpleName
    abstract val terminal: Boolean
}

open class Terminal(name: String? = null) : SyntaxElementType(name) {
    override val terminal: Boolean = true
}

open class NonTerminal(name: String? = null) : SyntaxElementType(name) {
    override val terminal: Boolean = false
}

data class SyntaxElement(val type: SyntaxElementType, val text: String? = null)

data class Production(val lhs: NonTerminal, val rhs: List<SyntaxElementType>) {
    override fun toString(): String = "$lhs → ${rhs.joinToString(" ")}"
}

// Syntactical convenience when defining productions.
operator fun NonTerminal.invoke(vararg rhs: SyntaxElementType): Production =
    Production(this, rhs.asList())

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
}

// Each state does its thing.
sealed interface Action
// Our work is done, here.
class Accept(val nodeType: SyntaxElementType, val count: Int) : Action
// Attempt to shift the next input.
class Shift(val shifts: Map<SyntaxElementType, State>) : Action
// Reduce the stack.
class Reduce(val elementType: SyntaxElementType, val count: Int) : Action
// Conflict; peek the next input and reduce on a match else shift.
class Resolve(
    val reduceTypes: List<Terminal>,
    val reduceTo: SyntaxElementType,
    val count: Int,
    val shifts: Map<SyntaxElementType, State>
) : Action

// The state's basis is the set of configs that transitioned to it.
// The state's extension is the closure on the dotted elements from the basis configs.
data class State(val id: Int, val basis: List<Config>, val extension: List<Config>) {
    internal var action: Action? = null
    // Two states are equal if their bases are equal.
    fun basisEquals(other: List<Config>): Boolean =
        basis.size == other.size && basis.all { c -> other.any { it equalsAll c } }
}

class Parser(val states: List<State>, val start: SyntaxElementType, val end: SyntaxElementType)

// The first production defines the start fragment at the LHS and the end fragment at the end of the RHS.
// These fragments must appear nowhere else.
fun generateParser(grammar: List<Production>): Parser {
    val state0 = grammar.first()
    // The LHS and last RHS of the augmenting production define the start and end symbols.
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
    val states = mutableListOf<State>()
    // Compute the closure on the basis configs.
    fun runState(basis: List<Config>): State {
        require(states.none { it.basisEquals(basis) })
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

        val state = State(states.size, basis, closure)
        states.add(state)
        val reductions = mutableListOf<Config>()
        val shifts = mutableListOf<Pair<SyntaxElementType, Config>>()
        (basis + closure).forEach { config ->
            when (val dotted = config.dotted) {
                null -> reductions.add(config)
                else -> shifts.add(dotted to config)
            }
        }
        require(reductions.isNotEmpty() or shifts.isNotEmpty()) { "Empty state: ${state.show}" }

//        // In the LR(0) regime, there must be one reduction XOR a positive number of shifts.
//        require(reductions.isNotEmpty() or shifts.isNotEmpty()) { "Empty state: ${state.show}" }
//        require(reductions.isEmpty() xor shifts.isEmpty()) { "Shift-reduce conflict in ${state.show}" }
//        val action = if (reductions.isNotEmpty()) {
//            require(1 == reductions.size) { "Unique reduction required.${reductions.joinToString { "\n$it" }}" }
//            reductions.first().run {
//                if (production.rhs.last() == end)
//                    TODO() // Accept(production.lhs, production.rhs.size - 1)
//                else
//                    TODO() // Reduce(production.lhs, production.rhs.size)
//            }
//        } else if (shifts.isNotEmpty()) {
//            val transitions = mutableMapOf<SyntaxElementType, State>()
//            for ((symbol, configs) in shifts.groupBy { it.first }) {
//                val newBasis = configs.map { it.second.bump() }
//                val target = when (val t = states.find { it.basisEquals(newBasis) }) {
//                    null -> runState(newBasis)
//                    else -> t
//                }
//                transitions[symbol] = target
//            }
//            TODO() // Shift(transitions)
//        } else error("No shifts or reductions found in ${state.show}")
//        TODO() // state.action = action
        return state
    }
    runState(listOf(Config(state0)))
    return Parser(states, start, end)
}

data class PTNode(val syntaxElement: SyntaxElement, val children: List<PTNode> = emptyList())

private data class ParseState(val state: State, val node: PTNode)

val Config.show: String
    get() = buildString {
        append(production.lhs.name)
        append(" → ")
        production.rhs.withIndex().forEach { (i, e) ->
            if (i == dot) append(" •")
            append(" $e")
        }
        if (production.rhs.size <= dot)
            append(" •")
        append("  ${follows.size}: ${follows.joinToString(" ") }")
    }
val State.show: String
    get() = buildString {
        appendLine("STATE $id")
        basis.forEach { appendLine("- ${it.show}") }
        extension.forEach { appendLine("  ${it.show}") }
        when (val a = action) {
            null -> appendLine("!!! ERROR: NO ACTION !!!")
            is Accept -> appendLine("ACCEPT: ${a.nodeType} ${a.count}")
            is Reduce -> appendLine("REDUCE: ${a.elementType} ${a.count}")
            is Shift -> appendLine("SHIFT: ${a.shifts.toList().joinToString(", ") { "${it.first} → ${it.second.id}" }}")
            is Resolve -> appendLine(
                "RESOLVE: REDUCE: ${a.reduceTo} ${a.count} [${a.reduceTypes.joinToString(", ")}], SHIFT: ${
                    a.shifts.toList().joinToString(", ") { "${it.first} → ${it.second.id}" }
                }")
        }
    }

