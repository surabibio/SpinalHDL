package spinal.tester.scalatest

import spinal.core._
import spinal.lib.bus.bmb._
import spinal.lib.memory.sdram.sdr.{MT41K128M16JT, MT48LC16M16A2, SdramInterface}
import spinal.lib.memory.sdram.xdr.{BmbPortParameter, CoreParameter, CtrlParameter, CtrlWithPhy, CtrlWithoutPhy, PhyLayout, SdramTiming, SoftConfig, mt48lc16m16a2_model}
import spinal.lib._
import spinal.lib.bus.amba3.apb.Apb3
import spinal.lib.bus.amba3.apb.sim.Apb3Driver
import spinal.lib.bus.bmb.sim.{BmbMemoryMultiPort, BmbMemoryMultiPortTester}
import spinal.lib.eda.bench.Rtl
import spinal.lib.memory.sdram.SdramLayout
import spinal.lib.memory.sdram.sdr.sim.SdramModel
import spinal.lib.memory.sdram.xdr.phy.{RtlPhy, RtlPhyInterface, SdrInferedPhy, XilinxS7Phy}
import spinal.lib.sim.Phase

import scala.util.Random

class SdrXdrCtrlPlusPhy(cp : CtrlParameter, pl : PhyLayout) extends Component{
  val ctrl = new CtrlWithoutPhy(cp, pl)

  val bmb = Vec(cp.ports.map(p => slave(Bmb(p.bmb))))
  val apb = slave(Apb3(12, 32))
  bmb <> ctrl.io.bmb
  apb <> ctrl.io.apb
}

class SdrXdrCtrlPlusRtlPhy(val cp : CtrlParameter,val pl : PhyLayout) extends SdrXdrCtrlPlusPhy(cp, pl){
  val phy = RtlPhy(pl)
  phy.io.ctrl <> ctrl.io.phy

  val phyWrite = master(RtlPhyInterface(phy.pl))
  phyWrite <> phy.io.write

  val beatCounter = out(Reg(UInt(60 bits)) init(0))
  beatCounter := beatCounter + ctrl.io.phy.readEnable.asUInt +ctrl.io.phy.writeEnable.asUInt


  val clockCounter = out(Reg(UInt(60 bits)) init(0))
  clockCounter := clockCounter + 1

  val sel = in UInt(log2Up(pl.phaseCount) bits)
  val CASn = out(ctrl.io.phy.phases.map(_.CASn).read(sel))
  val CKE = out(ctrl.io.phy.phases.map(_.CKE).read(sel))
  val CSn = out(ctrl.io.phy.phases.map(_.CSn).read(sel))
  val RASn = out(ctrl.io.phy.phases.map(_.RASn).read(sel))
  val WEn = out(ctrl.io.phy.phases.map(_.WEn).read(sel))
  val RESETn = out(ctrl.io.phy.phases.map(_.RESETn).read(sel))
  val ODT = out(ctrl.io.phy.phases.map(_.ODT).read(sel))
}

object SpinalSdrTesterHelpers{
  val CSn = 1 << 1
  val RASn = 1 << 2
  val CASn = 1 << 3
  val WEn = 1 << 4

  val PRE = CASn
  val REF = WEn
  val MOD = 0

  val SDRAM_CONFIG = 0x000
  val SDRAM_CTRL_FLAGS = 0x004


  val SDRAM_AUTO_REFRESH = 1
  val SDRAM_NO_ACTIVE = 2

  val SDRAM_SOFT_PUSH = 0x100
  val SDRAM_SOFT_CMD = 0x104
  val SDRAM_SOFT_ADDR = 0x108
  val SDRAM_SOFT_BA = 0x10C
  val SDRAM_SOFT_CLOCKING = 0x110

  val SDRAM_FAW = 0x030
  val SDRAM_ODT = 0x034


  val SDRAM_RESETN = 1
  val SDRAM_CKE = 2


  val SDRAM_CSN = (1 << 1)
  val SDRAM_RASN  = (1 << 2)
  val SDRAM_CASN  = (1 << 3)
  val SDRAM_WEN  = (1 << 4)


  val SDRAM_PRE = (SDRAM_CASN)
  val SDRAM_REF = (SDRAM_WEN)
  val SDRAM_MOD = (0)
  val SDRAM_ZQCL = (SDRAM_RASN | SDRAM_CASN)



  def ctrlParameter(pl : PhyLayout, cp : CoreParameter) = CtrlParameter(
    core = cp,
    ports = Seq(
      //TODO
//      BmbPortParameter(
//        bmb = BmbParameter(
//          addressWidth = pl.sdram.byteAddressWidth,
//          dataWidth = pl.beatWidth,
//          lengthWidth = log2Up(8 * pl.bytePerBurst),
//          sourceWidth = 3,
//          contextWidth = 8
//        ),
//        clockDomain = ClockDomain.current,
//        cmdBufferSize = 64,
//        dataBufferSize = 64 * pl.beatCount,
//        rspBufferSize = 64 * pl.beatCount
//      )
      BmbPortParameter(
        bmb = BmbParameter(
          addressWidth = pl.sdram.byteAddressWidth,
          dataWidth = pl.beatWidth,
          lengthWidth = log2Up(16*pl.bytePerBurst),
          sourceWidth = 3,
          contextWidth = 8
        ),
        clockDomain = ClockDomain.current,
        cmdBufferSize = 16,
        dataBufferSize = 16*pl.beatCount,
        rspBufferSize = 16*pl.beatCount
      ),

      BmbPortParameter(
        bmb = BmbParameter(
          addressWidth = pl.sdram.byteAddressWidth,
          dataWidth = pl.beatWidth,
          lengthWidth = log2Up(32*pl.bytePerBurst),
          sourceWidth = 3,
          contextWidth = 8
        ),
        clockDomain = ClockDomain.current,
        cmdBufferSize = 32,
        dataBufferSize = 32*pl.beatCount,
        rspBufferSize = 32*pl.beatCount
      ),

      BmbPortParameter(
        bmb = BmbParameter(
          addressWidth = pl.sdram.byteAddressWidth,
          dataWidth = pl.beatWidth,
          lengthWidth = log2Up(8*pl.bytePerBurst),
          sourceWidth = 3,
          contextWidth = 8
        ),
        clockDomain = ClockDomain.current,
        cmdBufferSize = 4,
        dataBufferSize = 1*pl.beatCount,
        rspBufferSize = 4*pl.beatCount
      ),

      BmbPortParameter(
        bmb = BmbParameter(
          addressWidth = pl.sdram.byteAddressWidth,
          dataWidth = pl.beatWidth,
          lengthWidth = log2Up(8*pl.bytePerBurst),
          sourceWidth = 3,
          contextWidth = 8
        ),
        clockDomain = ClockDomain.current,
        cmdBufferSize = 1,
        dataBufferSize = 8*pl.beatCount,
        rspBufferSize = 8*pl.beatCount
      )
    )
  )

  def sdrInit(dut : SdrXdrCtrlPlusRtlPhy, timing : SdramTiming, cas : Int, phyClkRatio : Int): Unit ={
    import spinal.core.sim._
    Phase.setup {
      val apb = Apb3Driver(dut.apb, dut.clockDomain)
      //apb.verbose = true

      val soft = SoftConfig(timing, dut.clockDomain.frequency.getValue, dut.ctrl.cpa, phyClkRatio)
      apb.write(0x10, soft.REF)

      apb.write(0x20, (soft.RRD << 24) | (soft.RFC << 16) | (soft.RP << 8)  | (soft.RAS << 0))
      apb.write(0x24,                                                         (soft.RCD << 0))
      apb.write(0x28, (soft.WTP << 24)  | (soft.WTR << 16) | (soft.RTP << 8) |    (cas+2 << 0))

      sleep(100000)

      def command(cmd : Int,  bank : Int, address : Int): Unit ={
        apb.write(0x10C, bank)
        apb.write(0x108, address)
        apb.write(0x104, cmd)
        apb.write(0x100, 0)
        dut.clockDomain.waitSampling(10)
      }

      command(PRE, 0, 0x400)
      command(REF, 0, 0)
      command(REF, 0, 0)
      command(MOD, 0, 0x000 | (cas << 4))
      apb.write(0x04, 1)

      dut.clockDomain.waitSampling(10000)
    }
  }

  def ddr3Init(dut : SdrXdrCtrlPlusRtlPhy, timing : SdramTiming, cl : Int, wr : Int, phyClkRatio : Int): Unit ={
    import spinal.core.sim._
    Phase.setup {
      val apb = Apb3Driver(dut.apb, dut.clockDomain)
      def command(cmd : Int,  bank : Int, address : Int): Unit ={
        apb.write(0x10C, bank)
        apb.write(0x108, address)
        apb.write(0x104, cmd)
        apb.write(0x100, 0)
        dut.clockDomain.waitSampling(10)
      }

      val commandToDataCycle = (cl+phyClkRatio-1)/phyClkRatio;
      val commandPhase = commandToDataCycle*phyClkRatio-cl;
      val sdramConfig = commandPhase | (commandToDataCycle-1 << 16);


      val soft = SoftConfig(timing, dut.clockDomain.frequency.getValue, dut.ctrl.cpa, phyClkRatio)
      apb.write( SDRAM_CONFIG, sdramConfig);
      apb.write( SDRAM_CTRL_FLAGS, SDRAM_NO_ACTIVE);

      apb.write(0x10, soft.REF-1)

      def sat(v : Int) = v.max(0)
      apb.write(0x20, (sat(soft.RRD-2) << 24) | (sat(soft.RFC-2) << 16) | (sat(soft.RP-2) << 8)   | (sat(soft.RAS-2) << 0))
      apb.write(0x24,                                                                               (sat(soft.RCD-2) << 0))
      apb.write(0x28, (sat(soft.WTP-2) << 24)  | (sat(soft.WTR-2) << 16) | (sat(soft.RTP-2) << 8)   | (sat(soft.RTW-2) << 0))


      def io_udelay(us : Int) = {}//sleep(us*1000000)

      apb.write(SDRAM_FAW, soft.FAW-1);

      var ODTend = (1 << (commandPhase + 6)%phyClkRatio)-1
      if(ODTend == 0) ODTend = (1 << phyClkRatio)-1
      val ODT = (commandPhase+6+phyClkRatio-1)/phyClkRatio-1
      apb.write(SDRAM_ODT, (ODT << 0) | (ODTend << 8));

      apb.write(SDRAM_SOFT_CLOCKING, 0);
      io_udelay(200);
      apb.write(SDRAM_SOFT_CLOCKING, SDRAM_RESETN);
      io_udelay(500);
      apb.write(SDRAM_SOFT_CLOCKING, SDRAM_RESETN | SDRAM_CKE);

      val clConfig = (cl - 3) & 0xF;
      val wlConfig = (cl - 5);
      val wrToMr = List(1,2,3,4,-1,5,-1,6,-1,7,-1,0);
      command(SDRAM_MOD, 2, 0x200 | (wlConfig << 3));
      command(SDRAM_MOD, 3, 0);
      command(SDRAM_MOD, 1, 0x44);
      command(SDRAM_MOD, 0, (wrToMr(wr - 5) << 9) | 0x100 | ((clConfig & 1) << 2) | ((clConfig & 0xE) << 3)); //DDL reset
      io_udelay(100);
      command(SDRAM_ZQCL, 0, 0x400);
      io_udelay(100);


      apb.write(SDRAM_CTRL_FLAGS, 0);

      dut.clockDomain.waitSampling(10000)
    }
  }

  def setup(dut : SdrXdrCtrlPlusRtlPhy, noStall : Boolean = false): Unit ={
    import spinal.core.sim._
    def sl = dut.pl.sdram
    val addressTop = 1 << (2 + sl.bankWidth + sl.columnWidth + log2Up(sl.bytePerWord))
    val bytePerBeat = dut.phy.pl.bytePerBeat

    val tester = new BmbMemoryMultiPortTester(
      ports = dut.bmb.map(port =>
        BmbMemoryMultiPort(
          bmb = port,
          cd = dut.clockDomain
        )
      ),
      forkClocks = false
    ){
      override def addressGen(bmb: Bmb): Int = Random.nextInt(addressTop)
      override def transactionCountTarget: Int = 20
    }

    Phase.setup {
      for(beatId <- 0 until addressTop/bytePerBeat){
        var data = BigInt(0)
        for(byteId <- 0 until bytePerBeat){
          data = data | (BigInt(tester.memory.getByte(beatId*bytePerBeat + byteId).toInt & 0xFF) << (byteId*8))
        }
        dut.phyWrite.clk #= false
        dut.phyWrite.cmd.valid #= true
        dut.phyWrite.cmd.address #= beatId
        dut.phyWrite.cmd.data #= data
        sleep(0)
        dut.phyWrite.clk #= true
        sleep(0)
      }
    }

    var beatCount = 0l
    var clockCount = 0l
    Phase.stimulus{
      beatCount = -dut.beatCounter.toLong
      clockCount = -dut.clockCounter.toLong
    }

    Phase.flush{
      beatCount += dut.beatCounter.toLong
      clockCount += dut.clockCounter.toLong
      println(s"${1.0*beatCount/clockCount} beatRate ($beatCount/$clockCount)")
    }
  }
}


object SdramSdrRtlPhyTesterSpinalSim extends App{
  import spinal.core.sim._

  val cl = 5
  val wl = 5
  val bl = 4
  val phyClkRatio = 2
  val sdramPeriod = 3300
  val timing = SdramTiming(
    REF = ( 7800000 ps, 0),
    RAS = (   35000 ps, 0),
    RP  = (   13750 ps, 0),
    RFC = (  160000 ps, 0),
    RRD = (    7500 ps, 4),
    RCD = (   13750 ps, 4),
    RTW = (          (cl+bl+2-wl)*sdramPeriod ps, 0),
    RTP = (  7500   ps, 4),
    WTR = ( (7500  + (wl+bl)*sdramPeriod) ps, wl+bl+4),
    WTP = ( (15000 + (wl+bl)*sdramPeriod) ps, 0),
    FAW = (   40000 ps, 0)
  )


  //  val sl = MT48LC16M16A2.layout
//  val pl = SdrInferedPhy.phyLayout(sl)
  val sl = MT41K128M16JT.layout
  val pl = XilinxS7Phy.phyLayout(sl, phyClkRatio)

  val simConfig = SimConfig
  simConfig.withWave(0)
  simConfig.addSimulatorFlag("-Wno-MULTIDRIVEN")
  simConfig.withConfig(SpinalConfig(defaultClockDomainFrequency = FixedFrequency(1e12/(sdramPeriod*phyClkRatio) Hz)))
  simConfig.compile({
    val cp = SpinalSdrTesterHelpers.ctrlParameter(
      pl = pl,
      cp = CoreParameter(
        portTockenMin = 4,
        portTockenMax = 16,
        timingWidth = 5,
        refWidth = 16,
        writeLatencies = List((wl+phyClkRatio-1)/phyClkRatio-1),
        readLatencies = List((cl+phyClkRatio-1)/phyClkRatio-1)
      )
    )
    new SdrXdrCtrlPlusRtlPhy(cp, pl)
  }).doSimUntilVoid("test", 42) { dut =>
    dut.clockDomain.forkStimulus(sdramPeriod*phyClkRatio)
    SpinalSdrTesterHelpers.setup(dut, noStall = true)
    SpinalSdrTesterHelpers.ddr3Init(
      dut = dut,
      timing = timing,
      cl = cl,
      wr = wl,
      phyClkRatio = phyClkRatio
    )

    fork {
      var counter = 0
      dut.clockDomain.waitSampling()
      stuff()
      def stuff(): Unit = {
        dut.sel #= counter
        counter += 1
        if(counter == phyClkRatio) counter = 0
        delayed(sdramPeriod)(stuff)
      }
    }
  }
}






import spinal.core._
import spinal.lib.eda.bench._

object SdramSdrSyntBench extends App{
  val sl = MT48LC16M16A2.layout.copy(bankWidth = 3)
  val cp = CtrlParameter(
    core = CoreParameter(
      portTockenMin = 4,
      portTockenMax = 8,
      timingWidth = 4,
      refWidth = 16,
      writeLatencies = List(0),
      readLatencies = List(2)
    ),
    ports = Seq(
      BmbPortParameter(
        bmb = BmbParameter(
          addressWidth = sl.byteAddressWidth,
          dataWidth = 16,
          lengthWidth = 3,
          sourceWidth = 0,
          contextWidth = 0
        ),
        clockDomain = ClockDomain.current,
        cmdBufferSize = 8,
        dataBufferSize = 8,
        rspBufferSize = 16
      ),

      BmbPortParameter(
        bmb = BmbParameter(
          addressWidth = sl.byteAddressWidth,
          dataWidth = 16,
          lengthWidth = 4,
          sourceWidth = 0,
          contextWidth = 0
        ),
        clockDomain = ClockDomain.current,
        cmdBufferSize = 8,
        dataBufferSize = 8,
        rspBufferSize = 16
      )/*,

      BmbPortParameter(
        bmb = BmbParameter(
          addressWidth = sl.byteAddressWidth,
          dataWidth = 16,
          lengthWidth = 5,
          sourceWidth = 0,
          contextWidth = 0
        ),
        clockDomain = ClockDomain.current,
        cmdBufferSize = 8,
        rspBufferSize = 2
      )*//*,

      BmbPortParameter(
        bmb = BmbParameter(
          addressWidth = sl.byteAddressWidth,
          dataWidth = 16,
          lengthWidth = 5,
          sourceWidth = 0,
          contextWidth = 0
        ),
        clockDomain = ClockDomain.current,
        cmdBufferSize = 8,
        rspBufferSize = 2
      )*/
    )
  )


  val ports4 = new Rtl {
    override def getName(): String = "Port4"
    override def getRtlPath(): String = "Port4.v"
    SpinalVerilog({
      val c = new CtrlWithoutPhy(cp, SdrInferedPhy.phyLayout(sl)).setDefinitionName(getRtlPath().split("\\.").head)
      c
    })
  }


  val rtls = List(ports4)

  val targets = XilinxStdTargets(
    vivadoArtix7Path = "/media/miaou/HD/linux/Xilinx/Vivado/2018.3/bin"
  ) ++ AlteraStdTargets(
    quartusCycloneIVPath = "/media/miaou/HD/linux/intelFPGA_lite/18.1/quartus/bin",
    quartusCycloneVPath  = "/media/miaou/HD/linux/intelFPGA_lite/18.1/quartus/bin"
  )

  Bench(rtls, targets, "/media/miaou/HD/linux/tmp")




}