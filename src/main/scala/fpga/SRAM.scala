package fpga

import chisel3._
import chisel3.util._

// double-port SRAM
// 1 port only used for R and
// the other port only used for W

class RPort(val addr_width: Int, val data_width: Int) extends Bundle {
	val en = Input(Bool())
	val addr = Input(UInt(addr_width.W))
	val data = Output(UInt(data_width.W))
}

class WPort(val addr_width: Int, val data_width: Int) extends Bundle {
	val en = Input(Bool())
	val addr = Input(UInt(addr_width.W))
	val data = Input(UInt(data_width.W))
}

class SRAM(val addr_width: Int, val data_width: Int) extends Module {
	val io = IO(new Bundle {
		val w = new WPort(addr_width, data_width)
		val r = new RPort(addr_width, data_width)
	})
	
	
	val mem = SyncReadMem(1 << addr_width, UInt(data_width.W))
	io.r.data := DontCare
	
	when(io.w.en) {
		mem.write(io.w.addr, io.w.data)
	}
	
	io.r.data := mem.read(io.r.addr, io.r.en)
}