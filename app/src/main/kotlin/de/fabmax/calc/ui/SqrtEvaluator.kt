package de.fabmax.calc.ui

import com.fathzer.soft.javaluator.Constant
import com.fathzer.soft.javaluator.DoubleEvaluator
import com.fathzer.soft.javaluator.Operator
import com.fathzer.soft.javaluator.Parameters

/**
 * A javaluator DoubleEvaluator which supports square roots. Square root is implemented as an
 * operator with the unicode square root sign. Operator was chosen because in contrast to functions
 * operators also work without parenthesis.
 */
class SqrtEnabledEvaluator private constructor(val params: Parameters) : DoubleEvaluator(params) {
    companion object {
        val SQRT = Operator("\u221A", 1, Operator.Associativity.RIGHT, 5)
        val PI = Constant("\u03C0")

        private var inst: SqrtEnabledEvaluator? = null

        fun getInstance(): SqrtEnabledEvaluator {
            if (inst == null) {
                val params = DoubleEvaluator.getDefaultParameters()
                params.add(SQRT)
                params.add(PI)
                inst = SqrtEnabledEvaluator(params)
            }
            return inst!!
        }
    }

    override fun evaluate(operator: Operator?, operands: MutableIterator<Double>?, evaluationContext: Any?): Double? {
        if (operator == SQRT) {
            return Math.sqrt(operands!!.next())
        } else {
            return super.evaluate(operator, operands, evaluationContext)
        }
    }

    override fun evaluate(constant: Constant, evaluationContext: Any?): Double? {
        if (PI == constant) {
            return Math.PI
        } else {
            return super.evaluate(constant, evaluationContext)
        }
    }
}