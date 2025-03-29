package fpga

import chisel3._
import chisel3.util._
import chisel3.stage._

object Main extends App {
	println("Generating the hardware")
//	(new ChiselStage).execute(Array("-X", "sverilog"), Seq(new ChiselGeneratorAnnotation(() => new Test)))
//	(new ChiselStage).execute(Array("-X", "sverilog"), Seq(new ChiselGeneratorAnnotation(() => new Multiple.LinearCompute(64, 32))))
	(new ChiselStage).execute(Array("-X", "sverilog"), Seq(new ChiselGeneratorAnnotation(() => new Multiple.Top)))
}
