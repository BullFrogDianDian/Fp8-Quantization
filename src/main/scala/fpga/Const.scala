package fpga
import chisel3._

object Const {
	object PHV {
		val width = 2048
		val payloadBegin = 42
		val featureSize = 64
	}
	
	object Maddness {
		val d = 16
		val logd = 4
		val loglogd = 2
		val width = 8
	}
}
