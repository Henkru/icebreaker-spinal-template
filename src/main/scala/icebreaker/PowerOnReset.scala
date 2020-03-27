package icebreaker

import spinal.core._
import spinal.core.sim._

class PowerOnReset(count: Int = 3) extends Component {
  val io = new Bundle {
    val porReset = out Bool
  }
  val resetReg = Reg(Bool) init(True)
  val counter = Reg(UInt(log2Up(count) bits)) init(0)

  counter := counter + 1
  when(counter === count) {
    resetReg := False
  }
  io.porReset := resetReg
}

object PowerOnResetSimulation {
  def main(args: Array[String]) {
    val freq = FixedFrequency(1 MHz)
    val period = (1.0/freq.value.toInt * 1000 * 1000 * 1000).toInt
    val spinalConfig = SpinalConfig(defaultClockDomainFrequency = freq)

    SimConfig
      .withConfig(spinalConfig)
      .withWave
      .allOptimisation
      //.workspacePath("~/tmp")
      .compile(new Component {
        val por = new PowerOnReset
        val porClkDomain = new ClockDomain(ClockDomain.current.clock, por.io.porReset)

        val area = new ClockingArea(porClkDomain) {
          val foobar = Reg(UInt(8 bits)) init(0)
          foobar := foobar + 1
        }
      })
      .doSim{ dut =>
        dut.clockDomain.forkStimulus(period = period)
        for(idx <- 0 to 100){
          //dut.io.tickClk #= (idx % 2) == 0
          dut.clockDomain.waitRisingEdge()
        }
      }
  }
}
