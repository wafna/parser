package wafna.parser.arithmetic

import wafna.parser.Token

sealed interface AST {
    data class Id(val token: Token) : AST
    abstract class BinOp : AST {
        abstract val p: AST
        abstract val q: AST
    }

    data class Plus(override val p: AST, override val q: AST) : BinOp()
    data class Minus(override val p: AST, override val q: AST) : BinOp()
    data class Times(override val p: AST, override val q: AST) : BinOp()
    data class Divide(override val p: AST, override val q: AST) : BinOp()
}

@Suppress("BooleanLiteralArgument")
fun AST.show() = buildString {
    fun showTree(node: AST, prefix: String, isLast: Boolean, isRoot: Boolean) {
        val marker = if (isRoot) "" else if (isLast) "└── " else "├── "
        fun showNode(name: String) {
            appendLine("$prefix$marker$name")
        }

        val newPrefix = prefix + if (isRoot) "" else if (isLast) "    " else "│   "
        fun showBinOp(name: String, node: AST.BinOp) {
            showNode(name)
            showTree(node.p, newPrefix, false, false)
            showTree(node.q, newPrefix, true, false)
        }
        when (node) {
            is AST.Id -> showNode(node.token.toString())
            is AST.Plus -> showBinOp("+", node)
            is AST.Minus -> showBinOp("-", node)
            is AST.Times -> showBinOp("*", node)
            is AST.Divide -> showBinOp("/", node)
            else -> error(node.javaClass.canonicalName)
        }
    }
    showTree(this@show, "", true, true)
}