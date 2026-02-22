package wafna.parser.lr1

sealed class NodeType(val name: String? = null) {
    override fun toString(): String = name ?: this::class.java.simpleName
    // Necessary to calculate look-ahead sets.
    abstract val terminal: Boolean
}

open class Terminal(name: String? = null) : NodeType(name) {
    override val terminal: Boolean = true
}

open class NonTerminal(name: String? = null) : NodeType(name) {
    override val terminal: Boolean = false
}

data class Node(val type: NodeType, val text: String? = null)

data class Production(val lhs: NodeType, val rhs: List<NodeType>)

// Syntactical convenience when defining productions.
operator fun NodeType.invoke(vararg lhs: NodeType): Production =
    Production(this, lhs.asList())

val Production.show: String
    get() = "$lhs → ${rhs.joinToString(" ")}"
