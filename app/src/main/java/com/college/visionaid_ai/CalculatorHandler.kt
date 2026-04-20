package com.college.visionaid_ai

import java.io.DataInput

class CalculatorHandler {

    fun calculate(input: String): String {

      return try {

          val expression = input
              .replace("calculate", "")
              .replace("what is ", "")
              .trim()

          val result = eval(expression)

          "Result is $result"
      } catch (e: Exception) {
          "Sorry I can't calculate that"
      }
    }

    private fun  eval(expr: String): Double {
        return object : Any(){
            var pos = -1
            var ch = 0

            fun nextChar() {
                ch = if (++pos < expr.length) expr[pos].code else -1
            }

            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return false
                }
                return false
            }
            fun parse(): Double{
                nextChar()
                val x = parseExpression()
                if (pos < expr.length) throw RuntimeException("Unexpected")
                return x
            }

            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    when {
                        eat('+'.code) -> x += parseTerm()
                        eat('-'.code) -> x -= parseTerm()
                        else -> return x
                    }
                }
            }

            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    when {
                        eat('*'.code) -> x *= parseFactor()
                        eat('/'.code) -> x /= parseFactor()
                        else -> return  x
                     }
                }
            }

            fun parseFactor(): Double {
                if (eat('+'.code)) return  parseFactor()
                if (eat('-'.code)) return  -parseFactor()

                var x: Double
                var startPos = pos

                if (eat('('.code)) {
                    x = parseExpression()
                    eat(')'.code)
                } else {
                    while (ch in '0'.code..'9'.code || ch=='.'.code) nextChar()
                    x = expr.substring(startPos, pos).toDouble()
                }
                return x
            }
        }.parse()
    }

}