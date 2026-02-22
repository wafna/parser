package wafna.parser

// Grammatical element.
// Tags elements of the grammar and gives them names for printing.
open class Fragment(val name: String? = null) {
    override fun toString(): String = name ?: this::class.java.simpleName
}

data class Production(val lhs: Fragment, val rhs: List<Fragment>)

// Syntactical convenience when defining productions.
operator fun Fragment.invoke(vararg lhs: Fragment): Production =
    Production(this, lhs.asList())

// Configurations track the matching of a production in parse states.
@Suppress("EqualsOrHashCode")
@ConsistentCopyVisibility
data class Config private constructor(val production: Production, val dot: Int) {
    constructor(production: Production) : this(production, 0)
    // When it goes off the end we have a reduction.
    val dotted = if (dot < production.rhs.size) production.rhs[dot] else null
    fun bump() = copy(dot = dot + 1)
    override fun equals(other: Any?): Boolean =
        (other as? Config)?.let {
            production == other.production && dot == other.dot
        } ?: error("What the hell are you even doing?")

}

// Each state does exactly one thing.
sealed interface Action
class Shift(val shifts: Map<Fragment, State>) : Action
class Reduce(val fragment: Fragment, val count: Int) : Action
class Accept(val fragment: Fragment, val count: Int) : Action

