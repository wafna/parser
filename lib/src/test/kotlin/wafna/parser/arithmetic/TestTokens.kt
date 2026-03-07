package wafna.parser.arithmetic

import wafna.parser.NonTerminal
import wafna.parser.Terminal
import wafna.parser.token

// Token Types
//// Augmenting.
object Start : NonTerminal("@")
object End : Terminal("<$>")
//// Non-terminals.
object Expr : NonTerminal("E")
object Term : NonTerminal("T")
object Prod : NonTerminal("P")
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
