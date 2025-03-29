package Multiple

import chisel3._
import chisel3.util._
import fpga.{Pipeline, Utils}

class Rebuild extends Module {
	val io = IO(new Bundle {
		val pipe = new Pipeline
		val ans = Input(UInt(16.W))
	})
	
	val phv = Cat(io.ans(13, 6), io.pipe.phvIn(1015, 0))
	
	Utils.pass(phv, io.pipe.phvOut)
	Utils.pass(io.pipe.validIn, io.pipe.validOut)
}
