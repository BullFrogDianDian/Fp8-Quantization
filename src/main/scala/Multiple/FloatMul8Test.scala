package Multiple

import chisel3._
import chisel3.util._
import chisel3.stage.ChiselStage

// A simple example to test the FP8 multiplier
object FloatMul8Test extends App {
  // Helper functions to convert between FP8 and actual values
  println("FP8 Multiplier Test")
  println("===================")
  
  // Generate Verilog for the multiplier
  println("Generating Verilog for FloatMul8...")
  (new ChiselStage).emitVerilog(new FloatMul8())
} 