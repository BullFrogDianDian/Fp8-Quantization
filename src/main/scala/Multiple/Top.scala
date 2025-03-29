package Multiple

import chisel3._
import fpga.{MAELoss, Pipeline}

class Top extends Module {
    val io = IO(new Bundle {
        val addr = Input(UInt(32.W))
        val data = Input(UInt(32.W))
        val pipe = new Pipeline
        val encoderScale = Input(UInt(16.W))
        val encoderZeroPoint = Input(UInt(16.W))
        val decoderScale = Input(UInt(16.W))
        val decoderZeroPoint = Input(UInt(16.W))
    })
    
    val ctrl = Module(new ControlUnit)
    val encoder = Module(new LinearCompute(64, 32))
    val decoder = Module(new LinearCompute(32, 64))
    val maeLoss = Module(new MAELoss)
    val rebuild = Module(new Rebuild)
    
    ctrl.io.addr := io.addr
    ctrl.io.data := io.data
    ctrl.io.encoderLinearConfig <> encoder.io.config
    ctrl.io.decoderLinearConfig <> decoder.io.config
    
    encoder.io.scale := io.encoderScale
    encoder.io.zeroPoint := io.encoderZeroPoint
    decoder.io.scale := io.decoderScale
    decoder.io.zeroPoint := io.decoderZeroPoint
    
    io.pipe >~ encoder.io.pipe
    encoder.io.featuresIn := io.pipe.toFeature
    encoder.io.pipe ~>~ decoder.io.pipe
    decoder.io.featuresIn := encoder.io.featuresOut
    
    decoder.io.pipe ~>~ maeLoss.io.pipe
    maeLoss.io.featuresIn := decoder.io.featuresOut
    
    maeLoss.io.pipe ~>~ rebuild.io.pipe
    rebuild.io.ans := maeLoss.io.ans
    
    rebuild.io.pipe ~> io.pipe
}