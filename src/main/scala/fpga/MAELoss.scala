package fpga
import chisel3._
import chisel3.util._

class MAELoss extends Module {
	val io = IO(new Bundle {
		val featuresIn = Input(Vec(64, UInt(8.W)))
		val pipe = new Pipeline
		val ans  = Output(UInt(16.W))
	})
	
	val mae = Reg(Vec(64, UInt(16.W)))
	val featuresOriginal = io.pipe.toFeature
	for (i <- 0 until 64) {
		when (io.featuresIn(i) > featuresOriginal(i)) {
			mae(i) := io.featuresIn(i) - featuresOriginal(i)
		} .otherwise {
			mae(i) := featuresOriginal(i) - io.featuresIn(i)
		}
	}
	
	def accumulate(data: Vec[UInt], factor: Int) = {
		val width = data.length
		val newFactor = if (width > factor) width / factor else width
		assert(width % newFactor == 0, "Width is not divisible by newFactor")
		
		val ansWire = Wire(Vec(width / newFactor, UInt(16.W)))
		val ansReg = Reg(Vec(width / newFactor, UInt(16.W)))
		
		for (i <- 0 until width / newFactor) {
			ansWire(i) := data.slice(i * newFactor, (i + 1) * newFactor).reduce(_ + _)
		}
		
		ansReg := ansWire
		ansReg
	}
	
	def calAns(data: Vec[UInt]) = {
		var ans = data
		while (ans.length > 1) {
			val newAns = accumulate(ans, 8)
			ans = newAns
		}
		ans
	}
	
	val ans1 = calAns(mae)
	io.ans := ans1(0)
	io.pipe.passCycle(3)
}
