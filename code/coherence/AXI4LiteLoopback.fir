;buildInfoPackage: chisel3, version: 3.3-SNAPSHOT, scalaVersion: 2.12.10, sbtVersion: 1.3.8
circuit AXI4LiteLoopback : 
  module Queue : 
    input clock : Clock
    input reset : Reset
    output io : {flip enq : {flip ready : UInt<1>, valid : UInt<1>, bits : {addr : UInt<64>, prot : UInt<3>}}, deq : {flip ready : UInt<1>, valid : UInt<1>, bits : {addr : UInt<64>, prot : UInt<3>}}, count : UInt<1>}
    
    cmem _T : {addr : UInt<64>, prot : UInt<3>}[1] @[Decoupled.scala 209:24]
    reg _T_1 : UInt<1>, clock with : (reset => (reset, UInt<1>("h00"))) @[Decoupled.scala 212:35]
    node _T_2 = eq(UInt<1>("h00"), UInt<1>("h00")) @[Decoupled.scala 214:41]
    node _T_3 = eq(_T_1, UInt<1>("h00")) @[Decoupled.scala 215:36]
    node _T_4 = and(_T_2, _T_3) @[Decoupled.scala 215:33]
    node _T_5 = and(_T_2, _T_1) @[Decoupled.scala 216:32]
    node _T_6 = and(io.enq.ready, io.enq.valid) @[Decoupled.scala 40:37]
    wire _T_7 : UInt<1>
    _T_7 <= _T_6
    node _T_8 = and(io.deq.ready, io.deq.valid) @[Decoupled.scala 40:37]
    wire _T_9 : UInt<1>
    _T_9 <= _T_8
    when _T_7 : @[Decoupled.scala 220:17]
      infer mport _T_10 = _T[UInt<1>("h00")], clock @[Decoupled.scala 221:8]
      _T_10.prot <= io.enq.bits.prot @[Decoupled.scala 221:24]
      _T_10.addr <= io.enq.bits.addr @[Decoupled.scala 221:24]
      skip @[Decoupled.scala 220:17]
    when _T_9 : @[Decoupled.scala 224:17]
      skip @[Decoupled.scala 224:17]
    node _T_11 = neq(_T_7, _T_9) @[Decoupled.scala 227:16]
    when _T_11 : @[Decoupled.scala 227:28]
      _T_1 <= _T_7 @[Decoupled.scala 228:16]
      skip @[Decoupled.scala 227:28]
    node _T_12 = eq(_T_4, UInt<1>("h00")) @[Decoupled.scala 231:19]
    io.deq.valid <= _T_12 @[Decoupled.scala 231:16]
    node _T_13 = eq(_T_5, UInt<1>("h00")) @[Decoupled.scala 232:19]
    io.enq.ready <= _T_13 @[Decoupled.scala 232:16]
    infer mport _T_14 = _T[UInt<1>("h00")], clock @[Decoupled.scala 233:21]
    io.deq.bits.prot <= _T_14.prot @[Decoupled.scala 233:15]
    io.deq.bits.addr <= _T_14.addr @[Decoupled.scala 233:15]
    node _T_15 = sub(UInt<1>("h00"), UInt<1>("h00")) @[Decoupled.scala 248:40]
    node _T_16 = tail(_T_15, 1) @[Decoupled.scala 248:40]
    node _T_17 = and(_T_1, _T_2) @[Decoupled.scala 250:32]
    node _T_18 = mux(_T_17, UInt<1>("h01"), UInt<1>("h00")) @[Decoupled.scala 250:20]
    node _T_19 = or(_T_18, _T_16) @[Decoupled.scala 250:62]
    io.count <= _T_19 @[Decoupled.scala 250:14]
    
  module Queue_1 : 
    input clock : Clock
    input reset : Reset
    output io : {flip enq : {flip ready : UInt<1>, valid : UInt<1>, bits : {addr : UInt<64>, prot : UInt<3>}}, deq : {flip ready : UInt<1>, valid : UInt<1>, bits : {addr : UInt<64>, prot : UInt<3>}}, count : UInt<1>}
    
    cmem _T : {addr : UInt<64>, prot : UInt<3>}[1] @[Decoupled.scala 209:24]
    reg _T_1 : UInt<1>, clock with : (reset => (reset, UInt<1>("h00"))) @[Decoupled.scala 212:35]
    node _T_2 = eq(UInt<1>("h00"), UInt<1>("h00")) @[Decoupled.scala 214:41]
    node _T_3 = eq(_T_1, UInt<1>("h00")) @[Decoupled.scala 215:36]
    node _T_4 = and(_T_2, _T_3) @[Decoupled.scala 215:33]
    node _T_5 = and(_T_2, _T_1) @[Decoupled.scala 216:32]
    node _T_6 = and(io.enq.ready, io.enq.valid) @[Decoupled.scala 40:37]
    wire _T_7 : UInt<1>
    _T_7 <= _T_6
    node _T_8 = and(io.deq.ready, io.deq.valid) @[Decoupled.scala 40:37]
    wire _T_9 : UInt<1>
    _T_9 <= _T_8
    when _T_7 : @[Decoupled.scala 220:17]
      infer mport _T_10 = _T[UInt<1>("h00")], clock @[Decoupled.scala 221:8]
      _T_10.prot <= io.enq.bits.prot @[Decoupled.scala 221:24]
      _T_10.addr <= io.enq.bits.addr @[Decoupled.scala 221:24]
      skip @[Decoupled.scala 220:17]
    when _T_9 : @[Decoupled.scala 224:17]
      skip @[Decoupled.scala 224:17]
    node _T_11 = neq(_T_7, _T_9) @[Decoupled.scala 227:16]
    when _T_11 : @[Decoupled.scala 227:28]
      _T_1 <= _T_7 @[Decoupled.scala 228:16]
      skip @[Decoupled.scala 227:28]
    node _T_12 = eq(_T_4, UInt<1>("h00")) @[Decoupled.scala 231:19]
    io.deq.valid <= _T_12 @[Decoupled.scala 231:16]
    node _T_13 = eq(_T_5, UInt<1>("h00")) @[Decoupled.scala 232:19]
    io.enq.ready <= _T_13 @[Decoupled.scala 232:16]
    infer mport _T_14 = _T[UInt<1>("h00")], clock @[Decoupled.scala 233:21]
    io.deq.bits.prot <= _T_14.prot @[Decoupled.scala 233:15]
    io.deq.bits.addr <= _T_14.addr @[Decoupled.scala 233:15]
    node _T_15 = sub(UInt<1>("h00"), UInt<1>("h00")) @[Decoupled.scala 248:40]
    node _T_16 = tail(_T_15, 1) @[Decoupled.scala 248:40]
    node _T_17 = and(_T_1, _T_2) @[Decoupled.scala 250:32]
    node _T_18 = mux(_T_17, UInt<1>("h01"), UInt<1>("h00")) @[Decoupled.scala 250:20]
    node _T_19 = or(_T_18, _T_16) @[Decoupled.scala 250:62]
    io.count <= _T_19 @[Decoupled.scala 250:14]
    
  module Queue_2 : 
    input clock : Clock
    input reset : Reset
    output io : {flip enq : {flip ready : UInt<1>, valid : UInt<1>, bits : {data : UInt<64>, rresp : UInt<2>}}, deq : {flip ready : UInt<1>, valid : UInt<1>, bits : {data : UInt<64>, rresp : UInt<2>}}, count : UInt<1>}
    
    cmem _T : {data : UInt<64>, rresp : UInt<2>}[1] @[Decoupled.scala 209:24]
    reg _T_1 : UInt<1>, clock with : (reset => (reset, UInt<1>("h00"))) @[Decoupled.scala 212:35]
    node _T_2 = eq(UInt<1>("h00"), UInt<1>("h00")) @[Decoupled.scala 214:41]
    node _T_3 = eq(_T_1, UInt<1>("h00")) @[Decoupled.scala 215:36]
    node _T_4 = and(_T_2, _T_3) @[Decoupled.scala 215:33]
    node _T_5 = and(_T_2, _T_1) @[Decoupled.scala 216:32]
    node _T_6 = and(io.enq.ready, io.enq.valid) @[Decoupled.scala 40:37]
    wire _T_7 : UInt<1>
    _T_7 <= _T_6
    node _T_8 = and(io.deq.ready, io.deq.valid) @[Decoupled.scala 40:37]
    wire _T_9 : UInt<1>
    _T_9 <= _T_8
    when _T_7 : @[Decoupled.scala 220:17]
      infer mport _T_10 = _T[UInt<1>("h00")], clock @[Decoupled.scala 221:8]
      _T_10.rresp <= io.enq.bits.rresp @[Decoupled.scala 221:24]
      _T_10.data <= io.enq.bits.data @[Decoupled.scala 221:24]
      skip @[Decoupled.scala 220:17]
    when _T_9 : @[Decoupled.scala 224:17]
      skip @[Decoupled.scala 224:17]
    node _T_11 = neq(_T_7, _T_9) @[Decoupled.scala 227:16]
    when _T_11 : @[Decoupled.scala 227:28]
      _T_1 <= _T_7 @[Decoupled.scala 228:16]
      skip @[Decoupled.scala 227:28]
    node _T_12 = eq(_T_4, UInt<1>("h00")) @[Decoupled.scala 231:19]
    io.deq.valid <= _T_12 @[Decoupled.scala 231:16]
    node _T_13 = eq(_T_5, UInt<1>("h00")) @[Decoupled.scala 232:19]
    io.enq.ready <= _T_13 @[Decoupled.scala 232:16]
    infer mport _T_14 = _T[UInt<1>("h00")], clock @[Decoupled.scala 233:21]
    io.deq.bits.rresp <= _T_14.rresp @[Decoupled.scala 233:15]
    io.deq.bits.data <= _T_14.data @[Decoupled.scala 233:15]
    node _T_15 = sub(UInt<1>("h00"), UInt<1>("h00")) @[Decoupled.scala 248:40]
    node _T_16 = tail(_T_15, 1) @[Decoupled.scala 248:40]
    node _T_17 = and(_T_1, _T_2) @[Decoupled.scala 250:32]
    node _T_18 = mux(_T_17, UInt<1>("h01"), UInt<1>("h00")) @[Decoupled.scala 250:20]
    node _T_19 = or(_T_18, _T_16) @[Decoupled.scala 250:62]
    io.count <= _T_19 @[Decoupled.scala 250:14]
    
  module Queue_3 : 
    input clock : Clock
    input reset : Reset
    output io : {flip enq : {flip ready : UInt<1>, valid : UInt<1>, bits : {data : UInt<64>, strb : UInt<8>}}, deq : {flip ready : UInt<1>, valid : UInt<1>, bits : {data : UInt<64>, strb : UInt<8>}}, count : UInt<1>}
    
    cmem _T : {data : UInt<64>, strb : UInt<8>}[1] @[Decoupled.scala 209:24]
    reg _T_1 : UInt<1>, clock with : (reset => (reset, UInt<1>("h00"))) @[Decoupled.scala 212:35]
    node _T_2 = eq(UInt<1>("h00"), UInt<1>("h00")) @[Decoupled.scala 214:41]
    node _T_3 = eq(_T_1, UInt<1>("h00")) @[Decoupled.scala 215:36]
    node _T_4 = and(_T_2, _T_3) @[Decoupled.scala 215:33]
    node _T_5 = and(_T_2, _T_1) @[Decoupled.scala 216:32]
    node _T_6 = and(io.enq.ready, io.enq.valid) @[Decoupled.scala 40:37]
    wire _T_7 : UInt<1>
    _T_7 <= _T_6
    node _T_8 = and(io.deq.ready, io.deq.valid) @[Decoupled.scala 40:37]
    wire _T_9 : UInt<1>
    _T_9 <= _T_8
    when _T_7 : @[Decoupled.scala 220:17]
      infer mport _T_10 = _T[UInt<1>("h00")], clock @[Decoupled.scala 221:8]
      _T_10.strb <= io.enq.bits.strb @[Decoupled.scala 221:24]
      _T_10.data <= io.enq.bits.data @[Decoupled.scala 221:24]
      skip @[Decoupled.scala 220:17]
    when _T_9 : @[Decoupled.scala 224:17]
      skip @[Decoupled.scala 224:17]
    node _T_11 = neq(_T_7, _T_9) @[Decoupled.scala 227:16]
    when _T_11 : @[Decoupled.scala 227:28]
      _T_1 <= _T_7 @[Decoupled.scala 228:16]
      skip @[Decoupled.scala 227:28]
    node _T_12 = eq(_T_4, UInt<1>("h00")) @[Decoupled.scala 231:19]
    io.deq.valid <= _T_12 @[Decoupled.scala 231:16]
    node _T_13 = eq(_T_5, UInt<1>("h00")) @[Decoupled.scala 232:19]
    io.enq.ready <= _T_13 @[Decoupled.scala 232:16]
    infer mport _T_14 = _T[UInt<1>("h00")], clock @[Decoupled.scala 233:21]
    io.deq.bits.strb <= _T_14.strb @[Decoupled.scala 233:15]
    io.deq.bits.data <= _T_14.data @[Decoupled.scala 233:15]
    node _T_15 = sub(UInt<1>("h00"), UInt<1>("h00")) @[Decoupled.scala 248:40]
    node _T_16 = tail(_T_15, 1) @[Decoupled.scala 248:40]
    node _T_17 = and(_T_1, _T_2) @[Decoupled.scala 250:32]
    node _T_18 = mux(_T_17, UInt<1>("h01"), UInt<1>("h00")) @[Decoupled.scala 250:20]
    node _T_19 = or(_T_18, _T_16) @[Decoupled.scala 250:62]
    io.count <= _T_19 @[Decoupled.scala 250:14]
    
  module Queue_4 : 
    input clock : Clock
    input reset : Reset
    output io : {flip enq : {flip ready : UInt<1>, valid : UInt<1>, bits : {bresp : UInt<2>}}, deq : {flip ready : UInt<1>, valid : UInt<1>, bits : {bresp : UInt<2>}}, count : UInt<1>}
    
    cmem _T : {bresp : UInt<2>}[1] @[Decoupled.scala 209:24]
    reg _T_1 : UInt<1>, clock with : (reset => (reset, UInt<1>("h00"))) @[Decoupled.scala 212:35]
    node _T_2 = eq(UInt<1>("h00"), UInt<1>("h00")) @[Decoupled.scala 214:41]
    node _T_3 = eq(_T_1, UInt<1>("h00")) @[Decoupled.scala 215:36]
    node _T_4 = and(_T_2, _T_3) @[Decoupled.scala 215:33]
    node _T_5 = and(_T_2, _T_1) @[Decoupled.scala 216:32]
    node _T_6 = and(io.enq.ready, io.enq.valid) @[Decoupled.scala 40:37]
    wire _T_7 : UInt<1>
    _T_7 <= _T_6
    node _T_8 = and(io.deq.ready, io.deq.valid) @[Decoupled.scala 40:37]
    wire _T_9 : UInt<1>
    _T_9 <= _T_8
    when _T_7 : @[Decoupled.scala 220:17]
      infer mport _T_10 = _T[UInt<1>("h00")], clock @[Decoupled.scala 221:8]
      _T_10.bresp <= io.enq.bits.bresp @[Decoupled.scala 221:24]
      skip @[Decoupled.scala 220:17]
    when _T_9 : @[Decoupled.scala 224:17]
      skip @[Decoupled.scala 224:17]
    node _T_11 = neq(_T_7, _T_9) @[Decoupled.scala 227:16]
    when _T_11 : @[Decoupled.scala 227:28]
      _T_1 <= _T_7 @[Decoupled.scala 228:16]
      skip @[Decoupled.scala 227:28]
    node _T_12 = eq(_T_4, UInt<1>("h00")) @[Decoupled.scala 231:19]
    io.deq.valid <= _T_12 @[Decoupled.scala 231:16]
    node _T_13 = eq(_T_5, UInt<1>("h00")) @[Decoupled.scala 232:19]
    io.enq.ready <= _T_13 @[Decoupled.scala 232:16]
    infer mport _T_14 = _T[UInt<1>("h00")], clock @[Decoupled.scala 233:21]
    io.deq.bits.bresp <= _T_14.bresp @[Decoupled.scala 233:15]
    node _T_15 = sub(UInt<1>("h00"), UInt<1>("h00")) @[Decoupled.scala 248:40]
    node _T_16 = tail(_T_15, 1) @[Decoupled.scala 248:40]
    node _T_17 = and(_T_1, _T_2) @[Decoupled.scala 250:32]
    node _T_18 = mux(_T_17, UInt<1>("h01"), UInt<1>("h00")) @[Decoupled.scala 250:20]
    node _T_19 = or(_T_18, _T_16) @[Decoupled.scala 250:62]
    io.count <= _T_19 @[Decoupled.scala 250:14]
    
  module AXI4LiteLoopback : 
    input clock : Clock
    input reset : UInt<1>
    input s_axi : {awaddr : UInt<64>, awprot : UInt<3>, awvalid : UInt<1>, flip awready : UInt<1>, wdata : UInt<64>, wstrb : UInt<8>, wvalid : UInt<1>, flip wready : UInt<1>, flip bresp : UInt<2>, flip bvalid : UInt<1>, bready : UInt<1>, araddr : UInt<64>, arprot : UInt<3>, arvalid : UInt<1>, flip arready : UInt<1>, flip rdata : UInt<64>, flip rresp : UInt<2>, flip rvalid : UInt<1>, rready : UInt<1>}
    output m_axi : {awaddr : UInt<64>, awprot : UInt<3>, awvalid : UInt<1>, flip awready : UInt<1>, wdata : UInt<64>, wstrb : UInt<8>, wvalid : UInt<1>, flip wready : UInt<1>, flip bresp : UInt<2>, flip bvalid : UInt<1>, bready : UInt<1>, araddr : UInt<64>, arprot : UInt<3>, arvalid : UInt<1>, flip arready : UInt<1>, flip rdata : UInt<64>, flip rresp : UInt<2>, flip rvalid : UInt<1>, rready : UInt<1>}
    
    inst rAddressQ of Queue @[AXI4LiteLoopback.scala 11:25]
    rAddressQ.clock <= clock
    rAddressQ.reset <= reset
    inst wAddressQ of Queue_1 @[AXI4LiteLoopback.scala 15:25]
    wAddressQ.clock <= clock
    wAddressQ.reset <= reset
    inst rDataQ of Queue_2 @[AXI4LiteLoopback.scala 19:22]
    rDataQ.clock <= clock
    rDataQ.reset <= reset
    inst wDataQ of Queue_3 @[AXI4LiteLoopback.scala 23:22]
    wDataQ.clock <= clock
    wDataQ.reset <= reset
    inst bRespQ of Queue_4 @[AXI4LiteLoopback.scala 27:22]
    bRespQ.clock <= clock
    bRespQ.reset <= reset
    rAddressQ.io.enq.valid <= s_axi.arvalid @[AXI4LiteLoopback.scala 31:26]
    rAddressQ.io.enq.bits.addr <= s_axi.araddr @[AXI4LiteLoopback.scala 32:30]
    rAddressQ.io.enq.bits.prot <= s_axi.arprot @[AXI4LiteLoopback.scala 33:30]
    s_axi.arready <= rAddressQ.io.enq.ready @[AXI4LiteLoopback.scala 34:17]
    m_axi.arvalid <= rAddressQ.io.deq.valid @[AXI4LiteLoopback.scala 36:17]
    m_axi.arprot <= rAddressQ.io.deq.bits.prot @[AXI4LiteLoopback.scala 37:16]
    m_axi.araddr <= rAddressQ.io.deq.bits.addr @[AXI4LiteLoopback.scala 38:16]
    rAddressQ.io.deq.ready <= m_axi.arready @[AXI4LiteLoopback.scala 39:26]
    rDataQ.io.enq.valid <= m_axi.rvalid @[AXI4LiteLoopback.scala 42:23]
    rDataQ.io.enq.bits.data <= m_axi.rdata @[AXI4LiteLoopback.scala 43:27]
    rDataQ.io.enq.bits.rresp <= m_axi.rresp @[AXI4LiteLoopback.scala 44:28]
    m_axi.rready <= rDataQ.io.enq.ready @[AXI4LiteLoopback.scala 45:16]
    s_axi.rdata <= rDataQ.io.deq.bits.data @[AXI4LiteLoopback.scala 47:15]
    s_axi.rresp <= rDataQ.io.deq.bits.rresp @[AXI4LiteLoopback.scala 48:15]
    s_axi.rvalid <= rDataQ.io.deq.valid @[AXI4LiteLoopback.scala 49:16]
    rDataQ.io.deq.ready <= s_axi.rready @[AXI4LiteLoopback.scala 50:23]
    wAddressQ.io.enq.valid <= s_axi.awvalid @[AXI4LiteLoopback.scala 53:26]
    wAddressQ.io.enq.bits.addr <= s_axi.awaddr @[AXI4LiteLoopback.scala 54:30]
    wAddressQ.io.enq.bits.prot <= s_axi.awprot @[AXI4LiteLoopback.scala 55:30]
    s_axi.awready <= wAddressQ.io.enq.ready @[AXI4LiteLoopback.scala 56:17]
    m_axi.awvalid <= wAddressQ.io.deq.valid @[AXI4LiteLoopback.scala 58:17]
    m_axi.awprot <= wAddressQ.io.deq.bits.prot @[AXI4LiteLoopback.scala 59:16]
    m_axi.awaddr <= wAddressQ.io.deq.bits.addr @[AXI4LiteLoopback.scala 60:16]
    wAddressQ.io.deq.ready <= m_axi.awready @[AXI4LiteLoopback.scala 61:26]
    wDataQ.io.enq.valid <= s_axi.wvalid @[AXI4LiteLoopback.scala 64:23]
    node _T = add(s_axi.wdata, UInt<5>("h010")) @[AXI4LiteLoopback.scala 65:42]
    node _T_1 = tail(_T, 1) @[AXI4LiteLoopback.scala 65:42]
    wDataQ.io.enq.bits.data <= _T_1 @[AXI4LiteLoopback.scala 65:27]
    wDataQ.io.enq.bits.strb <= s_axi.wstrb @[AXI4LiteLoopback.scala 66:27]
    s_axi.wready <= wDataQ.io.enq.bits.data @[AXI4LiteLoopback.scala 67:16]
    wDataQ.io.deq.ready <= m_axi.wready @[AXI4LiteLoopback.scala 69:23]
    m_axi.wstrb <= wDataQ.io.deq.bits.strb @[AXI4LiteLoopback.scala 70:15]
    m_axi.wdata <= wDataQ.io.deq.bits.data @[AXI4LiteLoopback.scala 71:15]
    m_axi.wvalid <= wDataQ.io.deq.valid @[AXI4LiteLoopback.scala 72:16]
    bRespQ.io.enq.valid <= m_axi.bvalid @[AXI4LiteLoopback.scala 75:23]
    bRespQ.io.enq.bits.bresp <= m_axi.bresp @[AXI4LiteLoopback.scala 76:28]
    m_axi.bready <= bRespQ.io.enq.ready @[AXI4LiteLoopback.scala 77:16]
    s_axi.bresp <= bRespQ.io.deq.bits.bresp @[AXI4LiteLoopback.scala 79:15]
    s_axi.bvalid <= bRespQ.io.deq.valid @[AXI4LiteLoopback.scala 80:16]
    bRespQ.io.deq.ready <= s_axi.bready @[AXI4LiteLoopback.scala 81:23]
    
