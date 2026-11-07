package com.clonical.kasiva.core.util

import com.clonical.kasiva.core.i18n.text
import com.clonical.kasiva.i18n.*
import kotlin.math.abs

// Perhitungan tetap dalam satuan minor; perkalian yang melampaui Long ditolak.
fun calculateAmount(expression: String, currency: String): Long {
    require(expression.length in 1..100) { MR.strings.dash_err_expression_too_long.text() }
    val tokens=Regex("[0-9]+(?:\\.[0-9]+)?|[+\\-×÷*/]").findAll(expression).map{it.value}.toList()
    require(tokens.joinToString("")==expression.filterNot{it.isWhitespace()} && tokens.size%2==1){MR.strings.dash_err_check_formula.text()}
    val scale=(0 until currencyDigits(currency)).fold(1L){n,_->n*10}
    fun product(a:Long,b:Long):Long {require(a==0L || abs(b)<=Long.MAX_VALUE/abs(a)){MR.strings.dash_err_calc_too_large.text()};return a*b}
    fun divided(a:Long,b:Long):Long {
        require(b!=0L){MR.strings.dash_err_divide_by_zero.text()}
        val quotient=a/b;val remainder=a%b
        return checkedAdd(quotient,if(abs(remainder)>=abs(b)/2+abs(b)%2)if((a<0L) xor (b<0L))-1 else 1 else 0)
    }
    var sum=0L;var sign=1L;var term=minorAmount(tokens.first(),currency)
    for(index in 1 until tokens.size step 2) {
        val value=minorAmount(tokens[index+1],currency)
        when(tokens[index]) {
            "*","×"->term=divided(product(term,value),scale)
            "/","÷"->term=divided(product(term,scale),value)
            "+","-"->{sum=checkedAdd(sum,product(sign,term));sign=if(tokens[index]=="-")-1 else 1;term=value}
            else->error(MR.strings.dash_err_check_formula.text())
        }
    }
    val result=checkedAdd(sum,product(sign,term))
    require(result in 0..9_000_000_000_000_000L){MR.strings.dash_err_result_range.text()}
    return result
}
