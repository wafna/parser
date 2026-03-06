package wafna.parser

// Token Types
//// Augmenting.
object Start : NonTerminal("@")
object End : Terminal("$")
//// Non-terminals.
object Expr : NonTerminal("E")
object Expr1 : NonTerminal("E1")
object Expr2 : NonTerminal("E2")
//// Terminals.
object Id : Terminal("id")
object LParen : Terminal("(")
object RParen : Terminal(")")
object Plus : Terminal("+")
object Minus : Terminal("-")
object Times : Terminal("*")
object Divide : Terminal("/")

// Terminal tokens.
val lparen = LParen.token("(")
val rparen = RParen.token(")")
val plus = Plus.token("+")
val minus = Minus.token("-")
val times = Times.token("*")
val divide = Divide.token("/")
val x = Id.token("x")
val y = Id.token("y")
val z = Id.token("z")
val w = Id.token("w")
