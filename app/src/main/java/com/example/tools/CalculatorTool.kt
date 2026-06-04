package com.example.tools

import kotlin.math.*

class CalculatorTool : BaseTool() {
    override val name = "calculator"
    override val description = "محاسبات ریاضی. جمع، تفریق، ضرب، تقسیم، توان، جذر، درصد."
    override val inputSchema: Map<String, Map<String, Any>> = mapOf(
        "expression" to mapOf(
            "type" to "string",
            "description" to "عبارت ریاضی مثل \"2+2\"، \"sqrt(144)\"، \"15% of 200\"، \"2^10\""
        )
    )
    override val requiredFields = listOf("expression")

    override suspend fun execute(input: Map<String, Any>): String {
        val expr = (input["expression"] as? String)?.trim() ?: return "عبارت نامعتبر"
        return runCatching {
            val result = evaluate(expr)
            val formatted = if (result == result.toLong().toDouble()) result.toLong().toString()
            else "%.4f".format(result).trimEnd('0').trimEnd('.')
            "نتیجه: $formatted"
        }.getOrElse { "خطا در محاسبه: ${it.message}" }
    }

    private fun evaluate(expr: String): Double {
        // Percentage pattern: "15% of 200"
        val pctOf = Regex("""(\d+(?:\.\d+)?)\s*%\s*of\s*(\d+(?:\.\d+)?)""", RegexOption.IGNORE_CASE)
            .find(expr)
        if (pctOf != null) {
            return pctOf.groupValues[1].toDouble() / 100.0 * pctOf.groupValues[2].toDouble()
        }

        // sqrt(x)
        val sqrtMatch = Regex("""sqrt\(([^)]+)\)""").find(expr)
        if (sqrtMatch != null) return sqrt(evaluate(sqrtMatch.groupValues[1]))

        // pow or ^ notation
        val powMatch = Regex("""pow\(([^,]+),\s*([^)]+)\)""").find(expr)
        if (powMatch != null) return evaluate(powMatch.groupValues[1])
            .pow(evaluate(powMatch.groupValues[2]))

        // Simple parser respecting operator precedence
        return ExprParser(expr).parse()
    }

    // Recursive descent parser for +, -, *, /, ^
    private class ExprParser(private val input: String) {
        private var pos = 0
        private val expr = input.replace("\\s".toRegex(), "")

        fun parse(): Double = parseAddSub().also {
            if (pos < expr.length) throw IllegalArgumentException("Unexpected: ${expr[pos]}")
        }

        private fun parseAddSub(): Double {
            var result = parseMulDiv()
            while (pos < expr.length && (expr[pos] == '+' || expr[pos] == '-')) {
                val op = expr[pos++]
                val right = parseMulDiv()
                result = if (op == '+') result + right else result - right
            }
            return result
        }

        private fun parseMulDiv(): Double {
            var result = parsePow()
            while (pos < expr.length && (expr[pos] == '*' || expr[pos] == '/')) {
                val op = expr[pos++]
                val right = parsePow()
                result = if (op == '*') result * right else result / right
            }
            return result
        }

        private fun parsePow(): Double {
            val base = parseUnary()
            return if (pos < expr.length && expr[pos] == '^') {
                pos++
                base.pow(parsePow())
            } else base
        }

        private fun parseUnary(): Double {
            if (pos < expr.length && expr[pos] == '-') { pos++; return -parsePrimary() }
            if (pos < expr.length && expr[pos] == '+') { pos++ }
            return parsePrimary()
        }

        private fun parsePrimary(): Double {
            if (pos < expr.length && expr[pos] == '(') {
                pos++
                val result = parseAddSub()
                if (pos < expr.length && expr[pos] == ')') pos++
                return result
            }
            val start = pos
            while (pos < expr.length && (expr[pos].isDigit() || expr[pos] == '.')) pos++
            return expr.substring(start, pos).toDouble()
        }
    }
}
