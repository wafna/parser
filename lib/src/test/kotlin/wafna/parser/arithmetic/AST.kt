package wafna.parser.arithmetic

import wafna.parser.Token

sealed interface AST {
    data class Id(val token: Token) : AST
    data class Plus(val p: AST, val q: AST) : AST
    data class Minus(val p: AST, val q: AST) : AST
    data class Times(val p: AST, val q: AST) : AST
    data class Divide(val p: AST, val q: AST) : AST
}

fun AST.show() = buildString {
    fun show(node: AST, indent: Int) {
        repeat(indent) { append("  ") }
        when (node) {
            is AST.Id -> appendLine(node.token.toString())
            is AST.Plus -> {
                appendLine("+")
                show(node.p, indent + 1)
                show(node.q, indent + 1)
            }

            is AST.Minus -> {
                appendLine("-")
                show(node.p, indent + 1)
                show(node.q, indent + 1)
            }

            is AST.Times -> {
                appendLine("*")
                show(node.p, indent + 1)
                show(node.q, indent + 1)
            }

            is AST.Divide -> {
                appendLine("/")
                show(node.p, indent + 1)
                show(node.q, indent + 1)
            }
        }
    }
    show(this@show, 0)
}