package Multiple

import chisel3._
import Config._

/**************************************
 *  32位的地址，32位的数据
 *  地址高四位（31,28）保留，（27,16）cs，16位地址或者8位i，8位j
 *  cs：
 *  0x000 保留
 *  0x001 encoder 权重
 *  0x002 encoder 偏移
 *  0x003 decoder 权重
 *  0x004 decoder 偏移
 **************************************/

class ControlUnit extends Module {
	val io = IO(new Bundle {
		val addr = Input(UInt(32.W))
		val data = Input(UInt(32.W))
		val encoderLinearConfig = Output(new LinearConfigInterface)
		val decoderLinearConfig = Output(new LinearConfigInterface)
	})
	
	val cs = io.addr(27, 16)
	
	io.encoderLinearConfig.en := false.B
	io.encoderLinearConfig.weight := DontCare
	io.encoderLinearConfig.i := io.addr(15, 8)
	io.encoderLinearConfig.j := io.addr(7, 0)
	io.encoderLinearConfig.value := io.data(7, 0)
	when (cs === 0x001.U) {
		io.encoderLinearConfig.en := true.B
		io.encoderLinearConfig.weight := true.B
	}
	when (cs === 0x002.U) {
		io.encoderLinearConfig.en := true.B
		io.encoderLinearConfig.weight := false.B
	}
	
	io.decoderLinearConfig.en := false.B
	io.decoderLinearConfig.weight := DontCare
	io.decoderLinearConfig.i := io.addr(15, 8)
	io.decoderLinearConfig.j := io.addr(7, 0)
	io.decoderLinearConfig.value := io.data(7, 0)
	when(cs === 0x003.U) {
		io.decoderLinearConfig.en := true.B
		io.decoderLinearConfig.weight := true.B
	}
	when(cs === 0x004.U) {
		io.decoderLinearConfig.en := true.B
		io.decoderLinearConfig.weight := false.B
	}
}
