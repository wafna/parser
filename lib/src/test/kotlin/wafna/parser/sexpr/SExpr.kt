package wafna.parser.sexpr

sealed interface SExpr {
    class SAtom(val data: ByteArray) : SExpr
    class SList(val exprs: List<SExpr>) : SExpr
}

fun SExpr.show(): String = buildString {
    fun showTree(expr: SExpr, prefix: String, isLast: Boolean, isRoot: Boolean) {
        val marker = if (isRoot) "" else if (isLast) "└── " else "├── "
        val newPrefix = prefix + if (isRoot) "" else if (isLast) "    " else "│   "
        when (expr) {
            is SExpr.SAtom -> appendLine("$prefix$marker${String(expr.data)}")
            is SExpr.SList -> {
                appendLine("$prefix$marker\uD80C\uDFF8")
                val last = expr.exprs.size - 1
                expr.exprs.withIndex().forEach { (nth, e) ->
                    showTree(e, newPrefix, nth == last, false)
                }
            }
        }
    }
    showTree(this@show, "", true, true)
}