package fpga
import chisel3._
import chisel3.util._

//class PHV extends Bundle {
//	val phv = UInt(Const.PHV.width.W)
//}

object Utils {
	def pass(in: UInt, out: UInt): UInt = {
		val reg = Reg(UInt(in.getWidth.W))
		reg := in
		out := reg
		reg
	}
	
	def pass(in: Bool, out: Bool): Bool = {
		val reg = Reg(Bool())
		reg := in
		out := reg
		reg
	}
	
	def pass[T <: Bundle](in: T, out: T): T = {
		val reg = Reg(in.cloneType)
		reg := in
		out := reg
		reg
	}
	
	def passCycle(in: UInt, out: UInt, cycle: Int): Unit = {
		val regs = Reg(Vec(cycle, UInt(in.getWidth.W)))
		for (i <- 0 until cycle - 1) {
			regs(i + 1) := regs(i)
		}
		regs(0) := in
		out := regs(cycle - 1)
	}
	
	def passCycle(in: Bool, out: Bool, cycle: Int): Unit = {
		val regs = Reg(Vec(cycle, Bool()))
		for (i <- 0 until cycle - 1) {
			regs(i + 1) := regs(i)
		}
		regs(0) := in
		out := regs(cycle - 1)
	}
}

class Pipeline extends Bundle {
	val validIn = Input(Bool())
	val validOut = Output(Bool())
	val phvIn = Input(UInt(Const.PHV.width.W))
	val phvOut = Output(UInt(Const.PHV.width.W))
	
	def passCycle(cycle: Int): Unit = {
		Utils.passCycle(validIn, validOut, cycle)
		Utils.passCycle(phvIn, phvOut, cycle)
	}
	
	def pass(): Unit = {
		Utils.pass(validIn, validOut)
		Utils.pass(phvIn, phvOut)
	}
	
	def toFeature: Vec[UInt] = {
		val features = Wire(Vec(Const.PHV.featureSize, UInt(8.W)))
		for (i <- Const.PHV.payloadBegin until Const.PHV.payloadBegin + Const.PHV.featureSize) {
			features(i - Const.PHV.payloadBegin) := phvIn((i + 1) * 8 - 1, i * 8)
		}
		features
	}
	
	// ~为子模块，~> 从子模块传递给父模块；~>~子模块之间的传递
	def >~(pipe: Pipeline): Unit = {
		pipe.phvIn := phvIn
		pipe.validIn := validIn
	}
	
	def ~>(pipe: Pipeline): Unit = {
		pipe.phvOut := phvOut
		pipe.validOut := validOut
	}
	
	def ~>~(pipe: Pipeline): Unit = {
		pipe.phvIn := phvOut
		pipe.validIn := validOut
	}
}
