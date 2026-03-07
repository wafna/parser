package wafna.parser

fun <T> Iterator<T>.toList(): List<T> = buildList { while (hasNext()) add(next()) }

fun Terminal.token(text: String): TerminalToken =
    TerminalToken(this, text)

object DebugStateListener : StateListener {
    var step = 0
    fun showStep() = "[${"%3d".format(step++)}]"
    override fun begin() {
        step = 0
    }

    override fun shift(stack: List<Int>, state: ParseState, input: Token, shift: Int) {
        val header = when (val action = state.action) {
            is Resolve -> if (action.shifts.contains(input.type) && action.reductions.contains(input.type))
                "* CONFLICT  " else "*    SHIFT  "

            else -> "     SHIFT  "
        }
        println(
            "${showStep()}${header}${"$input".padEnd(8)} →  ${"%d".format(shift).padEnd(12)}  [${
                stack.reversed().joinToString(", ")
            }]"
        )
    }

    override fun reduce(
        stack: List<Int>,
        state: ParseState,
        input: Token,
        count: Int,
        tokenType: TokenType
    ) {
        val header = when (state.action) {
            is Resolve -> "*   REDUCE  "
            else -> "    REDUCE  "
        }
        println(
            "${showStep()}$header${"$input".padEnd(8)} →  ${"$tokenType $count".padEnd(12)}  [${
                stack.reversed().joinToString(", ")
            }]"
        )
    }

    override fun accept(stack: List<Int>, state: ParseState) {
        println("${showStep()}    ACCEPT")
    }
}