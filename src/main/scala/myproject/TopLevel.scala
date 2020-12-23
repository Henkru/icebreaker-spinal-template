package myproject

import java.io.{File, PrintWriter}

import spinal.core._
import spinal.lib._
import icebreaker._

class TopLevel extends ICEBreaker(
  externalReset = false,
  powerOnReset = true,
  pllFrequency = Some(25 MHz)) {

  // NoPrefixBundle create module inputs/outputs without the io_ prefix
  // Signal names have to map to pin names in the icebreaker.pcf file
  val io = new NoPrefixBundle {
    val LEDR_N = out Bool
  }

  // ICEBreaker contains three different clock domains
  // 1) intClk = external 12 MHz clock
  // 2) pllClk = clock from the PLL module
  // 3) porClk = Power on reset circuit where clock is fed
  //             from intClk or plkkClk if it is in the use

  // defaultClk =>
  // 1) external 12 MHz crystal if por = false and pll = None
  // 3) pll clock if por = false pll = Some(freq)
  // 2) por clock if por = true
  val area = new ClockingArea(defaultClk) {
    val counter = Reg(UInt(24 bits))
    counter := counter + 1
    io.LEDR_N := counter.msb
  }
}

// Generate Verilog module which can be built with Makefile
object TopLevelVerilog {
  def main(args: Array[String]): Unit = {
    val config = SpinalConfig(
      targetDirectory = "rtl"
    )
    SpinalVerilog(config)({
      val top = new TopLevel

      // Include the clock frequency
      val freq = top.defaultClk.frequency.getValue.toInt / 1000 / 1000
      val f = new PrintWriter(new File(config.targetDirectory + "/freq.txt"))
      f.write(freq.toString)
      f.close()

      top
    })
  }
}
