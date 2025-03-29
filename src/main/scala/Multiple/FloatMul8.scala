package Multiple

import chisel3._
import chisel3.util._

class FloatMul8 extends Module {
    val io = IO(new Bundle {
        val a = Input(UInt(8.W))
        val b = Input(UInt(8.W))
        val c = Output(UInt(8.W))
    })

    // Test 1: 2.5 * 2.0 = 5.0
    // 2.5 = 0_0100_010 (1.25 * 2^1)
    // 2.0 = 0_0011_000 (1.0 * 2^1)
    // 5.0 = 0_0101_010 (1.25 * 2^2)

    val a_sign = io.a(7)
    val a_exp = io.a(6, 3)
    val a_mant = io.a(2, 0)
    
    val b_sign = io.b(7)
    val b_exp = io.b(6, 3)
    val b_mant = io.b(2, 0)
    
    val res_sign = a_sign ^ b_sign
    
    val a_mant_full = Cat(1.U(1.W), a_mant)
    val b_mant_full = Cat(1.U(1.W), b_mant)
    val mant_product = a_mant_full * b_mant_full
    
    val normalize = mant_product(7)
    val norm_mant = Mux(normalize, mant_product(6, 4), mant_product(5, 3))
    
    val exp_sum = a_exp +& b_exp
    val res_exp = exp_sum -& 7.U + normalize
    
    // Check for special cases
    val a_zero = a_exp === 0.U
    val b_zero = b_exp === 0.U
    val a_inf = a_exp === 15.U
    val b_inf = b_exp === 15.U
    
    // Result assembly with special case handling
    val result = Wire(UInt(8.W))
    
    when(a_zero || b_zero) {
        // Zero result
        result := Cat(res_sign, 0.U(7.W))
    }.elsewhen(a_inf || b_inf) {
        // Infinity result
        result := Cat(res_sign, 15.U(4.W), 0.U(3.W))
    }.elsewhen(res_exp >= 15.U) {
        // Overflow to infinity
        result := Cat(res_sign, 15.U(4.W), 0.U(3.W))
    }.elsewhen(res_exp === 0.U || res_exp(4)) {
        // Underflow to zero
        result := Cat(res_sign, 0.U(7.W))
    }.otherwise {
        result := Cat(res_sign, res_exp(3, 0), norm_mant)
    }
    
    io.c := result
} 