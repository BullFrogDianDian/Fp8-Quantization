package Multiple

import chisel3._


object Config {
	class LinearConfig(row: Int, col: Int) extends Bundle {
		val weight = Vec(row, Vec(col, UInt(8.W)))
		val bias = Vec(col, UInt(8.W))
	}
	
	class LinearConfigInterface extends Bundle {
		val en = Input(Bool())
		val weight = Input(Bool())
		val i = Input(UInt(8.W))
		val j = Input(UInt(8.W))
		val value = Input(UInt(8.W))
	}
}
