# parser

This is an embeddable library that generates LR(1) parse tables and runs the parser on provided input.
The input is consumed as a stream (iterator) of tokens.

## step 1: define a vocabulary

The token types, i.e. the vocabulary, are defined in terms of terminals and non-terminals.
Terminals are tokens produced in the input stream whereas non-terminals aggregate them
(and other non-terminals) in the grammar.

Here is an example for a simple grammar describing arithmetic with addition and multiplication.
```kotlin
// Augmenting.
object Start : NonTerminal("@")
object End : Terminal("$")
// Non-terminals.
object Expr : NonTerminal("E")
object TSum : NonTerminal("T")
object TProd : NonTerminal("P")
// Terminals.
object Id : Terminal("id")
object LParen : Terminal("(")
object RParen : Terminal(")")
object Plus : Terminal("+")
object Times : Terminal("*")
```
Each can take a name that will appear in error messages and debugging output.

## step 2: define a grammar

With the token types, above, we can now define the production rules (grammar) for our language.
```kotlin
val grammar = listOf(
    Start.produces(Expr, End),
    Expr.produces(Expr, Plus, TSum),
    Expr.produces(TSum),
    TSum.produces(Id),
    TSum.produces(LParen, Expr, RParen)
)
```
Here is the grammar represented symbolically according to the names provided in the definitions of the token types.
```
@ → E $
E → E + T
E → T
T → id
T → ( E )
```

## step 3: generate the parser

The parser can be generated with or without configuration information attached to the states.
This is selected by the mode.
The default elides the configurations as the tables can be quite numerous.
The only difference is the amount of information reported about the parser state in error messages.
```kotlin
val parser = generateParser(grammer, ParserConfig.Dbg)
```

## step 4: run the parser

The parser consumes input as a stream (iterator) of terminal tokens.

The parser does not produce any direct output.
Instead, it requires a listener which is notified of parser actions.
The reason for this is that consumers will want to build parse trees for their own purposes.
A generic parse tree (as demonstrated in the unit tests) is verbose and unhelpful.

## References

* [Shift-Reduce Parsers](https://www2.lawrence.edu/fast/GREGGJ/CMSC515/Parsing/LR.html).
