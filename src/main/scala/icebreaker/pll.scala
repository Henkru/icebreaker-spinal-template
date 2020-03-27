package icebreaker

import spinal.core._

private class pll extends BlackBox {
  val io = new Bundle {
    val clock_in = in Bool
    val clock_out = out Bool
    val locked = out Bool
  }
  noIoPrefix()
}

