package wafna.parser

import java.util.*

// The state's basis is the set of configs that transitioned to it.
// The state's extension is the closure on the dotted elements from the basis configs.
data class State(val id: Int, val basis: List<Config>, val extension: List<Config>) {
    internal var action: Action? = null

    // Two states are equal if their bases are equal.
    fun basisEquals(other: List<Config>): Boolean =
        basis.size == other.size && basis.all { c -> other.any { it == c } }
}

class Parser(val states: List<State>, val start: FragmentType, val end: FragmentType)

// The first production defines the start fragment at the LHS and the end fragment at the end of the RHS.
// These fragments must appear nowhere else.
fun runGrammar(grammar: List<Production>): Parser {
    val state0 = grammar.first()
    // The LHS and last RHS of the start state define the start and end symbols.
    val (start, end) = state0.lhs to state0.rhs.reversed().first()
    require(state0.rhs.reversed().drop(1).none { it == start || it == end }) {
        "The $start and $end fragments must not appear in the middle of the start production."
    }
    val grammar = grammar.drop(1).apply {
        filter {it.lhs == start || it.lhs == end || it.rhs.any { it == start || it == end }}.also { bad ->
            require(bad.isEmpty()) {
                "The $start and $end fragments must not appear outside of the start production:${bad.joinToString { "\n${it.show}" }}"
            }
        }
    }
    val states = mutableListOf<State>()
    // Compute the closure on the basis configs.
    fun runState(basis: List<Config>): State {
        require(states.none { it.basisEquals(basis) })
        val extension = mutableListOf<Config>()
        tailrec fun extend(configs: List<Config>) {
            val ext = mutableListOf<Config>()
            configs.forEach { config ->
                grammar.forEach { p ->
                    if (p.lhs == config.dotted) {
                        val c = Config(p)
                        if (!extension.contains(c) && !basis.contains(c) && !ext.contains(c))
                            ext.add(c)
                    }
                }
            }
            if (ext.isNotEmpty()) {
                extension.addAll(ext)
                extend(ext)
            }
        }
        extend(basis)
        val state = State(states.size, basis, extension)
        states.add(state)
        val reductions = mutableListOf<Config>()
        val shifts = mutableListOf<Pair<FragmentType, Config>>()
        (basis + extension).forEach { config ->
            when (val dotted = config.dotted) {
                null -> reductions.add(config)
                else -> shifts.add(dotted to config)
            }
        }
        // In the LR(0) regime, there must be one reduction XOR a positive number of shifts.
        require(reductions.isNotEmpty() or shifts.isNotEmpty()) { "Empty state: ${state.show}" }
        require(reductions.isEmpty() xor shifts.isEmpty()) { "Shift-reduce conflict in ${state.show}" }
        val action = if (reductions.isNotEmpty()) {
            require(1 == reductions.size) { "Unique reduction required.${reductions.joinToString { "\n${it.show}" }}" }
            reductions.first().run {
                if (production.rhs.last() == end)
                    Accept(production.lhs, production.rhs.size - 1)
                else
                    Reduce(production.lhs, production.rhs.size)
            }
        } else if (shifts.isNotEmpty()) {
            val transitions = mutableMapOf<FragmentType, State>()
            for ((symbol, configs) in shifts.groupBy { it.first }) {
                val newBasis = configs.map { it.second.bump() }
                val target = when (val t = states.find { it.basisEquals(newBasis) }) {
                    null -> runState(newBasis)
                    else -> t
                }
                transitions[symbol] = target
            }
            Shift(transitions)
        } else error("No shifts or reductions found in ${state.show}")
        state.action = action
        return state
    }
    runState(listOf(Config(state0)))
    return Parser(states, start, end)
}

data class PTNode(val fragment: Fragment, val children: List<PTNode> = emptyList())

private data class ParseState(val state: State, val node: PTNode)

fun runParser(parser: Parser, input: Iterator<Fragment>): PTNode {
    fun nextInput() = PTNode(if (input.hasNext()) input.next() else Fragment(parser.end))
    val stack = Stack<ParseState>().apply {
        val state0 = parser.states.first()
        push(ParseState(state0, nextInput()))
    }

    tailrec fun next() {
        val (state, node) = stack.peek()
        when (val action = state.action!!) {
            is Shift -> when (val shift = action.shifts[node.fragment.type]) {
                null ->
                    error("No transition for ${node.fragment.type} in ${state.show}")

                else -> when (val shiftAction = shift.action!!) {
                    is Accept -> {
                        // Double-checking the requirements when the parser was generated.
                        require(shiftAction.fragmentType == parser.start) {
                            "Must reduce the start production when accepting."
                        }
                        stack.pop().apply { require(node.fragment.type == parser.end) }
                        return
                    }

                    is Reduce -> {
                        val children = List(shiftAction.count) { stack.pop() }.reversed()
                        val parentState = children.first().state
                        stack.push(ParseState(parentState, PTNode(Fragment(shiftAction.fragmentType), children.map { it.node })))
                    }

                    is Shift ->
                        stack.push(ParseState(shift, nextInput()))
                }
            }

            else -> error("Only shifts are handled here: $action")
        }
        next()
    }
    next()
    require(1 == stack.size) {
        "Incomplete stack reduction."
    }
    return stack.pop().node
}

internal val Production.show: String
    get() = "$lhs → ${rhs.joinToString(" ")}"

internal val Config.show: String
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

internal val State.show: String
    get() = buildString {
        appendLine("STATE $id")
        basis.forEach { appendLine("- ${it.show}") }
        extension.forEach { appendLine("  ${it.show}") }
        when (val a = action) {
            null -> appendLine("!!! ERROR: NO ACTION !!!")
            is Accept -> appendLine("ACCEPT: ${a.fragmentType} ${a.count}")
            is Reduce -> appendLine("REDUCE: ${a.fragmentType} ${a.count}")
            is Shift -> appendLine("SHIFT: ${a.shifts.toList().joinToString(", ") { "${it.first} → ${it.second.id}" }}")
        }
    }
