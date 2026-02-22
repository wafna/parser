package wafna.parser.lr0

// Grammatical element.
// Tags elements of the grammar and gives them names for printing.
open class FragmentType(val name: String? = null) {
    override fun toString(): String = name ?: this::class.java.simpleName
}

data class Fragment(val type: FragmentType, val text: String? = null)

data class Production(val lhs: FragmentType, val rhs: List<FragmentType>)

// Syntactical convenience when defining productions.
operator fun FragmentType.invoke(vararg lhs: FragmentType): Production =
    Production(this, lhs.asList())

val Production.show: String
    get() = "$lhs → ${rhs.joinToString(" ")}"

