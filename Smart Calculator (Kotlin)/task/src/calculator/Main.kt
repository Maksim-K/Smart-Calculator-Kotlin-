package calculator

import java.math.BigInteger

fun main() {
    Calculator.console()
}

object Calculator {
    private var exitCommandReceived = false
    private val variablesMap = emptyMap<String, String>().toMutableMap()

    enum class Commands(val command: String) {
        EXIT("/exit"), HELP("/help"),
    }

    enum class RegexPatterns(val pattern: Regex) {
        COMMAND(Regex("/.*")),
        EXPRESSION(Regex("""([-+()]*\w+[-+()]*[*/^]?\w*)+""")),
        DOUBLED_PLUS(Regex("[+]{2,}+")),
        DOUBLED_MINUS(Regex("-{2}+")),
        MINUS_PLUS(Regex("([+]-)|(-[+])")),
        VALUE(Regex("[+-]*\\d+")),
        VARIABLE(Regex("[a-zA-Z]+")),
        ASSIGNMENT(Regex(".+=.+")),
        IDENTIFIER(Regex("[a-zA-Z\\d]+")),
        ELEMENTS(Regex("[-+*/^]|[a-zA-Z]+|\\d+")),
        PARENTHESES(Regex("""\([\w-+*\\^/]*\)""")),
    }

    private val operatorPriorities = mapOf(
        "+" to 0, "-" to 0,
        "/" to 1, "*" to 1,
        "^" to 2,
    )

    private val operatorActions = mapOf(
        "+" to { b: String, a: String -> a.toBigInteger() + b.toBigInteger() },
        "-" to { b: String, a: String -> a.toBigInteger() - b.toBigInteger() },
        "/" to { b: String, a: String -> a.toBigInteger() / b.toBigInteger() },
        "*" to { b: String, a: String -> a.toBigInteger() * b.toBigInteger() },
        "^" to { b: String, a: String -> a.toBigInteger().power(b.toBigInteger()) }
    )

    private fun Int.power(power: Int) = (1..power).fold(1) { pow, _ -> pow * this }
    private fun BigInteger.power(power: BigInteger): BigInteger {
        var result = BigInteger.ONE
        var count = power
        while (count-- != BigInteger.ZERO) {
            result *= this
        }
        return result
    }

    private fun String.isOperator(): Boolean = operatorPriorities.containsKey(this)

    fun console() {
        while (!exitCommandReceived) {
            val input = readln().trim()
            if (input.isEmpty()) {
                // no action
            } else if (input.isCommand()) {
                runCommand(input)
            } else if (input.isAssignment()) {
                runAssignment(input)
            } else if (input.isIdentifier()) {
                runIdentifier(input)
            } else if (input.isSimpleExpression()) {
                if (Regex("\\(").findAll(input).count() !=
                    Regex("\\)").findAll(input).count()
                ) {
                    println("Invalid expression")
                } else runSimpleExpression(input)
            } else
                println("Invalid expression")
        }
    }

    private fun String.isCommand() =
        RegexPatterns.COMMAND.pattern.matches(this)

    private fun String.isSimpleExpression() =
        RegexPatterns.EXPRESSION.pattern.matches(normalizeExpressionString(this))

    private fun String.isAssignment() =
        RegexPatterns.ASSIGNMENT.pattern.matches(normalizeExpressionString(this))

    private fun String.isIdentifier() =
        RegexPatterns.IDENTIFIER.pattern.matches(normalizeExpressionString(this))

    private fun runSimpleExpression(expression: String) =
        println(calculate(normalizeExpressionString(expression)))

    private fun runIdentifier(identifier: String) {
        if (!RegexPatterns.VARIABLE.pattern.matches(normalizeExpressionString(identifier))) {
            println("Invalid identifier")
            return
        }

        if (variablesMap.containsKey(identifier)) {
            println(variablesMap[identifier])
        } else println("Unknown variable")

    }

    private fun runAssignment(assignment: String) {
        val sides = normalizeExpressionString(assignment).split("=")
        if (sides.size != 2) {
            println("Invalid assignment")
            return
        }
        //left side not variable
        if (!RegexPatterns.VARIABLE.pattern.matches(sides[0])) {
            println("Invalid identifier")
            return
        }
        //check right side
        when {
            RegexPatterns.VARIABLE.pattern.matches(sides[1]) -> {
                if (!variablesMap.containsKey(sides[1])) {
                    println("Unknown variable")
                    return
                }
            }

            !RegexPatterns.VALUE.pattern.matches(sides[1]) -> {
                println("Invalid identifier")
                return
            }
        }

        variablesMap[sides[0]] = variablesMap[sides[1]] ?: sides[1]
    }

    private fun runCommand(command: String) {
        when (command) {

            Commands.HELP.command -> {
                println("The program calculate expressions like these: 4 + 6 - 8, 2 - 3 - 4, and so on")
            }

            Commands.EXIT.command -> {
                println("Bye!")
                exitCommandReceived = true
            }

            else -> {
                println("Unknown command")
            }
        }
    }

    private fun normalizeExpressionString(string: String) = string
        .replace(" ", "")
        .replace(RegexPatterns.DOUBLED_MINUS.pattern, "+")
        .replace(RegexPatterns.DOUBLED_PLUS.pattern, "+")
        .replace(RegexPatterns.MINUS_PLUS.pattern, "-")

    private fun transformExpression(expression: String): String {
        var result = ""
        val stack = Stack()
        RegexPatterns.ELEMENTS.pattern.findAll(expression).forEach {
            val value = it.value
            if (value.isOperator()) {
                if (stack.isEmpty()) {
                    stack.push(value)
                } else {
                    if ((operatorPriorities[value] ?: -1) > (operatorPriorities[stack.peek()] ?: -1)) {
                        stack.push(value)
                    } else {
                        while (!stack.isEmpty()) result += " ${stack.pop()}"
                        stack.push(value)
                    }
                }
            } else {
                result += " $value"
            }
        }
        while (!stack.isEmpty()) result += " ${stack.pop()}"

        return result
    }

    private fun calculateSimpledExpression(expression: String): String {
        val elements = transformExpression(expression)
            .split(" ")
            .map {
                variablesMap[it] ?: it
            }

        val stack = Stack()
        elements.forEach {
            if (!it.isOperator()) {
                if (RegexPatterns.VARIABLE.pattern.matches(it)) return "Unknown variable"
                stack.push(it)
            } else {
                stack.push(
                    operatorActions[it]?.invoke(stack.pop(), stack.pop()).toString()
                )
            }
        }
        return stack.pop()
    }

    private fun calculate(expression: String): String {
        var result = "0+$expression".replace("(", "(0+")
        while (RegexPatterns.PARENTHESES.pattern.findAll(result).count() > 0) {
            val parts = RegexPatterns.PARENTHESES.pattern.findAll(result)
            parts.forEach {
                val value = it.value
                val calculated = calculateSimpledExpression(value.substring(1, value.lastIndex))
                result = result.replace(value, calculated)
            }
        }
        return calculateSimpledExpression(result)
    }
}

class Stack(private val size: Int = 5) {
    private val stackData: Array<String> = Array(size) { "" }
    private var pointer = 0
    private var actualSize = 0

    fun isEmpty() = actualSize == 0

    fun push(value: String) {
        if (pointer == size - 1) pointer = 0
        actualSize++
        stackData[pointer++] = value
    }

    fun pop(): String {
        if (actualSize != 0) {
            actualSize--
            val result = if (pointer == 0) {
                pointer = size - 1
                stackData[0]
            } else stackData[--pointer]
            return result
        }
        return ""
    }

    fun peek(): String = if (pointer == 0) {
        stackData[size]
    } else stackData[pointer - 1]

}
