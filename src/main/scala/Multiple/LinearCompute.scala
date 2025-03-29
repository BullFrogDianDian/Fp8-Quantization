package Multiple

import chisel3._
import chisel3.util._
import Config._
import fpga.Pipeline

class LinearCompute(row: Int, col: Int) extends Module {
    val io = IO(new Bundle {
        val pipe = new Pipeline
        val config = Input(new LinearConfigInterface)
        val featuresIn = Input(Vec(row, UInt(8.W)))
        val featuresOut = Output(Vec(col, UInt(8.W))) // 输出为 E4M3 格式的 FP8
        val scale = Input(UInt(16.W))      // 全局缩放因子，Q0.16
        val zeroPoint = Input(UInt(16.W))  // 全局零点，Q0.16
    })
    
    val linear = Reg(new LinearConfig(row, col))
    
    // 配置权重和偏置
    when (io.config.en) {
        when (io.config.weight) {
            linear.weight(io.config.i)(io.config.j) := io.config.value
        } .otherwise {
            linear.bias(io.config.i) := io.config.value
        }
    }
    
    // 线性量化到 E4M3
    def quantizeToE4M3(x: UInt, scale: UInt, zeroPoint: UInt): UInt = {
        val sign = x(31)
        val absX = Mux(sign, (~x + 1.U), x)
        
        // 线性量化：(x - zeroPoint) / scale
        val shiftedX = Mux(sign, zeroPoint - absX, absX - zeroPoint)
        val scaledX = (shiftedX * (1.U << 16)) / scale // Q0.16 格式
        
        // E4M3 范围
        val maxE4M3 = 448.U(32.W) // 7.5 * 2^6 = 448
        val minE4M3 = (-448).S(32.W).asUInt
        
        // 饱和处理
        val clippedX = Mux(scaledX > maxE4M3, maxE4M3, Mux(scaledX < minE4M3, minE4M3, scaledX))
        
        // 转换为 E4M3 表示
        val absClipped = Mux(clippedX(31), (~clippedX + 1.U), clippedX)
        val isZero = absClipped === 0.U
        val leadingZeros = PriorityEncoder(Reverse(absClipped))
        val expRaw = Mux(isZero, 0.U, 31.U - leadingZeros)
        
        val expBias = 7.U(4.W)
        val expMax = 15.U(4.W)
        val expMin = 0.U(4.W)
        
        val shiftAmt = Mux(expRaw > 3.U, expRaw - 3.U, 0.U)
        val mantissaRaw = (absClipped >> shiftAmt)(6, 0)
        val mantissa = Mux(expRaw >= 3.U, mantissaRaw(2, 0), 0.U(3.W))
        
        val expAdjusted = expRaw +& expBias
        val exp = Mux(isZero, 0.U, Mux(expAdjusted > expMax, expMax, Mux(expAdjusted < expMin, expMin, expAdjusted(3, 0))))
        
        val fp8 = Cat(clippedX(31), exp, mantissa)
        Mux(isZero, 0.U(8.W), fp8)
    }
    
    // FP8 (E4M3) 乘法，返回 32 位结果
    def fp8MulE4M3(a: UInt, b: UInt): UInt = {
        // 解码 E4M3
        val a_sign = a(7)
        val a_exp = a(6, 3)
        val a_mant = a(2, 0)
        val b_sign = b(7)
        val b_exp = b(6, 3)
        val b_mant = b(2, 0)
        
        // 检查特殊情况
        val a_zero = a_exp === 0.U
        val b_zero = b_exp === 0.U
        val a_inf = a_exp === 15.U
        val b_inf = b_exp === 15.U
        
        // 添加隐含的'1'到尾数
        val a_mant_full = Cat(1.U(1.W), a_mant)
        val b_mant_full = Cat(1.U(1.W), b_mant)
        
        // 乘以尾数 (4位 * 4位 = 8位结果)
        val mant_product = a_mant_full * b_mant_full
        
        // 确定规范化
        val normalize = mant_product(7)
        val norm_mant = Mux(normalize, mant_product(6, 4), mant_product(5, 3))
        
        // 计算指数
        val exp_sum = a_exp +& b_exp
        val res_exp = exp_sum -& 7.U + normalize
        
        // 结果符号
        val res_sign = a_sign ^ b_sign
        
        // 无穷大结果 (32位表示)
        val infinity_result = (Cat(res_sign, 0.U(31.W)) | (1.U << 30).asUInt) // 符号位 + 最大指数
        
        // 正常情况：构建FP8结果并转换为32位
        val res_fp8 = Cat(res_sign, res_exp(3, 0), norm_mant)
        val mantissa_32 = Cat(1.U(1.W), norm_mant, 0.U(28.W)) // 隐含1 + 尾数，左移到整数部分
        val exp_value = res_exp -& 7.U // 移除偏移
        val shift_amount = exp_value.asSInt
        
        // 根据指数移位，正指数左移，负指数右移
        val shifted_mant = Mux(shift_amount >= 0.S,
                              mantissa_32 << shift_amount.asUInt,
                              mantissa_32 >> (-shift_amount).asUInt)
        
        // 应用符号
        val normal_result = Mux(res_sign === 1.U, (~shifted_mant + 1.U), shifted_mant)
        
        // 根据不同情况返回结果
        Mux(a_zero || b_zero, 
            0.U(32.W), // 零结果
            Mux(a_inf || b_inf,
                infinity_result, // 无穷大结果
                Mux(res_exp >= 15.U,
                    infinity_result, // 溢出到无穷大
                    Mux(res_exp === 0.U || res_exp(4),
                        0.U(32.W), // 下溢到零
                        normal_result // 正常结果
                    )
                )
            )
        )
    }
    
    // 预量化输入和权重到 E4M3
    val featuresInQ8 = VecInit(io.featuresIn.map(x => quantizeToE4M3(Cat(0.U(24.W), x), io.scale, io.zeroPoint)))
    val weightQ8 = Reg(Vec(row, Vec(col, UInt(8.W))))
    for (i <- 0 until row) {
        for (j <- 0 until col) {
            weightQ8(i)(j) := quantizeToE4M3(Cat(0.U(24.W), linear.weight(i)(j)), io.scale, io.zeroPoint)
        }
    }
    
    // 中间结果存储
    val ansAll = Reg(Vec(row, Vec(col, UInt(32.W))))
    
    // 矩阵乘法
    for (i <- 0 until row) {
        for (j <- 0 until col) {
            ansAll(i)(j) := fp8MulE4M3(featuresInQ8(i), weightQ8(i)(j))
        }
    }
    
    // 累加和量化
    val ans = Reg(Vec(col, UInt(8.W)))
    val tempSum = Reg(Vec(col, UInt(32.W)))
    
    // 初始化累加器
    for (j <- 0 until col) {
        tempSum(j) := 0.U
    }
    
    // 全局累加
    for (i <- 0 until row) {
        for (j <- 0 until col) {
            tempSum(j) := tempSum(j) + ansAll(i)(j)
        }
    }
    
    // 应用偏置并量化到 E4M3
    for (j <- 0 until col) {
        val biasExtended = Cat(0.U(24.W), linear.bias(j))
        val sum32 = tempSum(j) + biasExtended
        ans(j) := quantizeToE4M3(sum32, io.scale, io.zeroPoint)
    }
    
    // Hard sigmoid（针对 E4M3 的简化定点运算）
    def hardsigmoid(x: UInt): UInt = {
        val one = 64.U(8.W)
        val half = 32.U(8.W)
        val scaledX = Wire(UInt(8.W))
        scaledX := x >> 2
        val sum = Wire(UInt(8.W))
        sum := scaledX + half
        
        val minVal = Mux(sum < one, sum, one)
        val maxVal = Mux(minVal > 0.U, minVal, 0.U)
        maxVal
    }
    
    for (j <- 0 until col) {
        io.featuresOut(j) := hardsigmoid(ans(j))
    }
    
    io.pipe.passCycle(4) // 固定流水线周期
}