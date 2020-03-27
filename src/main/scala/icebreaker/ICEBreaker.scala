package icebreaker

import spinal.core._

class NoPrefixBundle extends Bundle {
  setName("")
}

class ICEBreaker(externalReset: Boolean, powerOnReset: Boolean, pllFrequency: Option[HertzNumber] = None) extends Component {
  val clkIo = new NoPrefixBundle {
    val CLK = in Bool
  }

  // Clock domains
  val intClk = ClockDomain.internal(
    name="int",
    frequency = FixedFrequency(12 MHz),
    config = ClockDomainConfig(
      resetKind = if (externalReset) ASYNC else BOOT
    )
  )
  var porClk: Option[ClockDomain] = None
  var pllClk: Option[ClockDomain] = None

  // Config internal clock
  intClk.clock := clkIo.CLK
  if (externalReset) {
    val resetIo = new NoPrefixBundle {
      val RESET = in Bool
    }
    intClk.reset := resetIo.RESET
  }

  // Add PLL clock if required
  if(pllFrequency.nonEmpty) {
    addPLL(intClk, pllFrequency.get)
  }

  // Add POR circuit if required
  if (powerOnReset) addPowerOnReset(pllClk.getOrElse(intClk))
  val defaultClk = porClk.getOrElse(pllClk.getOrElse(intClk))

  private def addPowerOnReset(clk: ClockDomain): Unit = {
    val clkDomain = ClockDomain.internal(
      name = "por",
      frequency = clk.frequency,
      config = ClockDomainConfig(
        resetKind = SYNC
      )
    )
    val area = new ClockingArea(clk) {
      val por = new PowerOnReset
    }
    clkDomain.clock := clk.readClockWire
    clkDomain.reset := area.por.io.porReset || clk.readResetWire
    porClk = Some(clkDomain)
  }

  private def addPLL(clk: ClockDomain, frequency: HertzNumber): Unit = {
    val clkDomain = ClockDomain.internal(
      name = "PLL",
      frequency = FixedFrequency(frequency),
      config = ClockDomainConfig(
        resetKind = SYNC
      )
    )
    val area = new ClockingArea(clk) {
      val pll = new pll
      pll.io.clock_in := clk.readClockWire
    }
    clkDomain.clock := area.pll.io.clock_out
    clkDomain.reset := !area.pll.io.locked || clk.readResetWire
    pllClk = Some(clkDomain)
  }
}
