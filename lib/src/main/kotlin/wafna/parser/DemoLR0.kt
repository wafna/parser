package wafna.parser

import java.util.*

// Grammatical element.
// It seems we don't need to distinguish between atomic and composite fragments
// (other than that the leaves are never on the LHS of a production, of course).
sealed class Fragment(val name: String? = null) {
    override fun toString(): String = name ?: this::class.java.simpleName
}

object Start : Fragment("START")
object End : Fragment("END")
object Expr : Fragment("E")
object Term : Fragment("T")
object Number : Fragment("N")
object LParen : Fragment("(")
object RParen : Fragment(")")
object Plus : Fragment("+")

data class Production(val lhs: Fragment, val rhs: List<Fragment>)

operator fun Fragment.invoke(vararg lhs: Fragment): Production =
    Production(this, lhs.asList())

internal val grammar = listOf(
    Start(Expr, End),
    Expr(Expr, Plus, Term),
    Expr(Term),
    Term(Number),
    Term(LParen, Expr, RParen)
)

@Suppress("EqualsOrHashCode")
@ConsistentCopyVisibility
internal data class Config internal constructor(val production: Production, val dot: Int) {
    constructor(production: Production) : this(production, 0)
    // When it goes off the end we have a reduction.
    val dotted = if (dot < production.rhs.size) production.rhs[dot] else null
    fun bump() = copy(dot = dot + 1)
    override fun equals(other: Any?): Boolean =
        (other as? Config)?.let {
            production == other.production && dot == other.dot
        } ?: error("What the hell are you even doing?")

}

internal sealed interface Action
internal class Shift(val shifts: Map<Fragment, State>) : Action
internal class Reduce(val fragment: Fragment, val count: Int) : Action
internal class Accept(val fragment: Fragment, val count: Int) : Action

// The state's basis is the set of configs that transitioned to it.
// The state's extension is the closure on the dotted elements from the basis configs.
internal data class State(val id: Int, val basis: List<Config>, val extension: List<Config>) {
    internal var action: Action? = null

    // Two states are equal if their bases are equal.
    fun basisEquals(other: List<Config>): Boolean =
        basis.size == other.size && basis.all { c -> other.any { it == c } }
}

internal fun runGrammar(grammar: List<Production>): List<State> {
    // The first and no other rule in the grammar must generate the Start.
    val start = grammar.first().apply {
        require(lhs == Start && rhs.last() == End && rhs.reversed().drop(1).none { it == Start || it == End })
    }
    val grammar = grammar.drop(1).apply {
        require(none { it.lhs == Start || it.lhs == End })
        require(all { it.rhs.none { it == Start || it == End } })
    }
    val states = mutableListOf<State>()
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
        val shifts = mutableListOf<Pair<Fragment, Config>>()
        (basis + extension).forEach { config ->
            when (val dotted = config.dotted) {
                null -> reductions.add(config)
                else -> shifts.add(dotted to config)
            }
        }
        // In the LR(0) regime, there must be one reduction XOR a positive number of shifts.
        require(reductions.isEmpty() xor shifts.isEmpty()) { "Shift-reduce conflict in ${state.show}" }
        val action = if (reductions.isNotEmpty()) {
            require(1 == reductions.size) { "Unique reduction required." }
            reductions.first().run {
                if (production.rhs.last() == End)
                    Accept(production.lhs, production.rhs.size - 1)
                else
                    Reduce(production.lhs, production.rhs.size)
            }
        } else if (shifts.isNotEmpty()) {
            val transitions = mutableMapOf<Fragment, State>()
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
    runState(listOf(Config(start)))
    return states
}

internal data class PTNode(val fragment: Fragment, val children: List<PTNode> = emptyList())

internal fun Iterator<Fragment>.nextFragment(): Fragment =
    if (hasNext()) next() else End

private data class ParseState(val state: State, val node: PTNode)

internal fun runInput(parser: List<State>, input: Iterator<Fragment>): PTNode {
    val stack = Stack<ParseState>().apply {
        push(ParseState(parser.first(), PTNode(input.nextFragment())))
    }

    tailrec fun next() {
        val (state, node) = stack.peek()
        when (val action = state.action!!) {
            is Shift -> when (val shift = action.shifts[node.fragment]) {
                null ->
                    error("No transition for ${node.fragment} in ${state.show}")

                else -> when (val shiftAction = shift.action!!) {
                    is Accept -> {
                        stack.pop()
                        return
                    }

                    is Reduce -> {
                        val children = List(shiftAction.count) { stack.pop() }.reversed()
                        val s = children.first().state
                        stack.push(ParseState(s, PTNode(shiftAction.fragment, children.map { it.node })))
                    }

                    is Shift ->
                        stack.push(ParseState(shift, PTNode(input.nextFragment())))
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
    get() = "$lhs -> ${rhs.joinToString(" ")}"

internal val Config.show: String
    get() = "${production.lhs} -> ${
        buildList {
            production.rhs.withIndex().forEach { (i, e) ->
                if (i == dot) add("*")
                add(e.toString())
            }
            if (production.rhs.size <= dot)
                add("*")
        }.joinToString(" ")
    }"

internal val State.show: String
    get() = buildString {
        appendLine("STATE $id")
        basis.forEach { appendLine("- ${it.show}") }
        extension.forEach { appendLine("  ${it.show}") }
        when (val a = action) {
            null -> appendLine("!!! ERROR: NO ACTION !!!")
            is Accept -> appendLine("ACCEPT: ${a.fragment} ${a.count}")
            is Reduce -> appendLine("REDUCE: ${a.fragment} ${a.count}")
            is Shift -> appendLine("SHIFT: ${a.shifts.toList().joinToString(" ") { "(${it.first}, ${it.second.id})" }}")
        }
    }
