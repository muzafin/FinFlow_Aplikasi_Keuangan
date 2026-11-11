package com.clonical.kasiva.core.util

import com.clonical.kasiva.core.i18n.text
import com.clonical.kasiva.i18n.*
import kotlin.math.abs

// Perhitungan tetap dalam satuan minor; perkalian yang melampaui Long ditolak.
fun calculateAmount(expression: String, currency: String): Long {
    require(expression.length in 1..100) { MR.strings.dash_err_expression_too_long.text() }
    val tokens=Regex("[0-9]+(?:\\.[0-9]+)?|[+\\-×÷*/]").findAll(expression).map{it.value}.toList()
    require(tokens.joinToString("")==expression.filterNot{it.isWhitespace()} && tokens.size%2==1){MR.strings.dash_err_check_formula.text()}
    fun product(a:Long,b:Long):Long {require(a==0L || abs(b)<=Long.MAX_VALUE/abs(a)){MR.strings.dash_err_calc_too_large.text()};return a*b}
    fun gcd(a:Long,b:Long):Long {var x=abs(a);var y=abs(b);while(y!=0L){val t=x%y;x=y;y=t};return if(x==0L)1 else x}
    fun rounded(num:Long,den:Long):Long {
        require(den!=0L){MR.strings.dash_err_divide_by_zero.text()}
        val quotient=num/den;val remainder=num%den
        return checkedAdd(quotient,if(abs(remainder)>=abs(den)/2+abs(den)%2)if((num<0L) xor (den<0L))-1 else 1 else 0)
    }
    // Pengali/pembagi adalah angka biasa (boleh "1.1" untuk JPY, "1.125" untuk IDR; P1-04). Satu suku dihitung
    // sebagai pecahan num/den dan baru dibulatkan sekali di akhir, sehingga 100000/3*3 tetap 100000 (P1-06).
    fun factor(token:String):Pair<Long,Long> {
        val whole=token.substringBefore('.');val fraction=token.substringAfter('.',"").take(9)
        val digits=(whole+fraction).trimStart('0').ifEmpty{"0"}.toLongOrNull() ?: error(MR.strings.dash_err_calc_too_large.text())
        return digits to (0 until fraction.length).fold(1L){n,_->n*10}
    }
    var sum=0L;var sign=1L;var num=minorAmount(tokens.first(),currency);var den=1L
    fun reduce(){val g=gcd(num,den);num/=g;den/=g}
    for(index in 1 until tokens.size step 2) {
        val next=tokens[index+1]
        when(tokens[index]) {
            "*","×"->{val(n,d)=factor(next);num=product(num,n);den=product(den,d);reduce()}
            "/","÷"->{val(n,d)=factor(next);require(n!=0L){MR.strings.dash_err_divide_by_zero.text()};num=product(num,d);den=product(den,n);reduce()}
            "+","-"->{sum=checkedAdd(sum,product(sign,rounded(num,den)));sign=if(tokens[index]=="-")-1 else 1;num=minorAmount(next,currency);den=1L}
            else->error(MR.strings.dash_err_check_formula.text())
        }
    }
    val term=rounded(num,den)
    val result=checkedAdd(sum,product(sign,term))
    require(result in 0..9_000_000_000_000_000L){MR.strings.dash_err_result_range.text()}
    return result
}
