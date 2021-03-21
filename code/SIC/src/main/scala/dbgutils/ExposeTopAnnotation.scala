
package dbgutil

/*
 * This file includes annotation and transformation for bringing signals to the top of the module
 * refer to this file for more information:
 * https://github.com/freechipsproject/chisel3/blob/master/src/test/scala/chiselTests/AnnotatingDiamondSpec.scala
 */

import chisel3._
import chisel3.{Module, MultiIOModule}
import chisel3.experimental.{ChiselAnnotation, RunFirrtlTransform, annotate}
import chisel3.util.RegEnable
import firrtl.{CircuitForm, CircuitState, FEMALE, HighForm, LowForm, Namespace, PortKind, Transform, WDefInstance, WRef, WSubField, ir}
import firrtl.annotations.{Annotation, ComponentName, DeletedAnnotation, ModuleName, Named, SingleTargetAnnotation}
//.{DefModule, DefNode, DefRegister, DefWire, Module, NoInfo, Port, Statement}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class SimpleInnerModule extends MultiIOModule {
  val io = IO(new Bundle {
    val a = Input(UInt(32.W))
    val b = Input(UInt(32.W))
    val c = Output(UInt(32.W))
  })
  val debug_x = IO(Output(UInt(33.W)))

  val a_by_2 = io.a << 1.U
  val a_wires = WireInit(VecInit.tabulate(10)(_ => 0.U(10.W)))
  exposeTop(a_by_2)
  exposeTop(io.a)
  exposeTop(a_wires)
  val c = io.a + io.b
  val irrelevantNode = io.a + 2.U
  io.c := c + irrelevantNode
  debug_x := a_by_2
}

class SimpleIntermediateModule extends Module {
  val io = IO(new Bundle {
    val a = Input(UInt(32.W))
    val b = Input(UInt(32.W))
    val c = Output(UInt(32.W))
  })
  val m = Module(new SimpleInnerModule())
  val q = RegEnable(2.U, 0.U, io.a(0))
  exposeTop(q)
  m.io.a := io.a
  m.io.b := io.b
  io.c := m.io.c
}
class SimpleOuterModule extends Module {
  val io = IO(new Bundle {
    val va = Input(Vec(2, UInt(32.W)))
    val vb = Input(Vec(2, UInt(32.W)))
    val vc = Output(Vec(2, UInt(32.W)))
  })
  val m0= Module(new SimpleIntermediateModule())
  val m1 = Module(new SimpleInnerModule())

  m0.io.a := io.va(0)
  m0.io.b := io.vb(0)
  io.vc(0) := m0.io.c(0)

  m1.io.a := io.va(1)
  m1.io.b := io.vb(1)
  io.vc(1) := m1.io.c(1)

}

case class ExposeTopAnnotation(target: Named, value: String) extends SingleTargetAnnotation[Named] {
  def duplicate(n: Named): ExposeTopAnnotation = this.copy(target = n)
}

case class ExposeTopChiselAnnotation(target: InstanceId, value: String)
  extends ChiselAnnotation with RunFirrtlTransform {
  override def toFirrtl: Annotation = {
    ExposeTopAnnotation(target.toNamed, value)
  }
  override def transformClass: Class[ExposeTopTransform] = classOf[ExposeTopTransform]
}

case class DebugInfo() extends ir.Info {
  override def toString: String = " @[" + "augmented debug node" + "]"
  //scalastyle:off method.name
  def ++(that: ir.Info): ir.Info = if (that == ir.NoInfo) this else ir.MultiInfo(Seq(this, that))
}
object DebugInfo {
  def apply(): DebugInfo = new DebugInfo()
}

object exposeTop {
  def apply(component: InstanceId, value: String = "debug"): Unit = {
    val annotation = ExposeTopChiselAnnotation(component, value)
    annotate(annotation)
  }
}

class ExposeTopTransform extends Transform {
  def inputForm: CircuitForm = HighForm
  def outputForm: CircuitForm = HighForm
  def detectWire(namespace: Namespace,
                 statementToAdd: ArrayBuffer[(ir.Statement, String, ir.Type)],
                 modulesToAdd: ArrayBuffer[WDefInstance],
                 wiresToDebug: mutable.HashSet[String],
                 modulesWithDebug: mutable.HashSet[String])(s: ir.Statement) : ir.Statement = {
    s match {
      case ir.DefNode(_, name, exp) => {
        if(wiresToDebug.contains(name)) statementToAdd.append((s, name, exp.tpe))
        s
      }
      case ir.DefRegister(_, name, tpe, _, _, _) => {
        if(wiresToDebug.contains(name)) {
          statementToAdd.append((s, name, tpe))
        }
        s
      }
      case ir.DefWire(_, name, tpe) => {
        if(wiresToDebug.contains(name)) statementToAdd.append((s, name, tpe))
        s
      }
      case WDefInstance(info, name, module, tpe) => {
        if(modulesWithDebug.contains(module)) modulesToAdd.append(WDefInstance(info, name, module, tpe))
        s
      }
      case x => x.mapStmt(detectWire(namespace, statementToAdd, modulesToAdd, wiresToDebug, modulesWithDebug))
    }
  }
  def connect_local_port(m: ir.DefModule,
                         instancesToAdd: ArrayBuffer[(ir.Statement, String, ir.Type)],
                         debugPorts: mutable.HashMap[String, mutable.HashSet[(String, ir.Type)]]
                        ): ir.DefModule = {
    val m_with_ports = m match {
      case ir.Module(info, name, ports, body) => {
        val appended_ports = for { (stmt, name, tpe) <- instancesToAdd } yield {
          val port_to_ret = ir.Port(DebugInfo(), "debug_" + name, ir.Output, tpe)
          val ports = debugPorts.getOrElseUpdate(m.name, mutable.HashSet.empty[(String, ir.Type)])
          ports += Tuple2(name, tpe)
          port_to_ret
        }
        val with_ports = ir.Module(info, name, ports ++ appended_ports, body)
        val connect_blocks: Seq[ir.Statement] = for { (port, (stmt, name, tpe)) <- appended_ports.zip(instancesToAdd)}
          yield {
            ir.Connect(DebugInfo(), WRef(port.name, tpe, PortKind), WRef(name, tpe))
          }
        val original_body: Seq[ir.Statement] = Seq(body)
        val new_body = original_body ++ connect_blocks
        val res = ir.Module(with_ports.info, with_ports.name, with_ports.ports, ir.Block(new_body))
        res
      }
      case x => x
    }
    m_with_ports
  }
  def mapModule(wiresToDebug: mutable.HashMap[String, mutable.HashSet[String]],
                debugPorts: mutable.HashMap[String, mutable.HashSet[(String, ir.Type)]]
               )(m: ir.DefModule): ir.DefModule = {
    // println("Mapping Module: " + m.name)
    // get wires
    var instancesToAdd = new ArrayBuffer[(ir.Statement, String, ir.Type)](1)
    var modulesToAdd = new ArrayBuffer[WDefInstance](1)
    val op = wiresToDebug.get(m.name)
    val modulesWithDebug: mutable.HashSet[String] = mutable.HashSet(wiresToDebug.keySet.toArray:_*)
    modulesWithDebug ++= debugPorts.keySet
    val m_wired = m.mapStmt(detectWire(Namespace(m),
      instancesToAdd, modulesToAdd, op.getOrElse(mutable.HashSet.empty[String]), modulesWithDebug))
    // append ports for these wires
    val m_with_ports = connect_local_port(m, instancesToAdd, debugPorts)

    // println(debugPorts.toString)
    // println(modulesToAdd.toString)

    // now that we added the wire for current module, we then add modules
    val m_connected_instance = m_with_ports match {
      case ir.Module(info, name, ports, body) => {
        val port_list: ArrayBuffer[ir.Port] = ArrayBuffer()
        val connect_blocks: ArrayBuffer[ir.Statement] = ArrayBuffer()
        for { moduleInst <- modulesToAdd } {
          for { f <- debugPorts.get(moduleInst.module) } {
            for { port <- f } yield {
              port_list.append(ir.Port(DebugInfo(), "debug_" + moduleInst.name + "_" + port._1, ir.Output, port._2))
              connect_blocks.append(ir.Connect(DebugInfo(),
                WRef("debug_" + moduleInst.name + "_" + port._1, port._2, PortKind, FEMALE),
                WSubField(WRef(moduleInst.name), "debug_"  + port._1, port._2, firrtl.MALE)))
              val currentDebugPorts = debugPorts.getOrElseUpdate(m.name, mutable.HashSet.empty[(String, ir.Type)])
              currentDebugPorts += Tuple2(moduleInst.name + "_" + port._1, port._2)
            }
            //println("Add " + port_list.toString())
            //println("Add " + connect_blocks.toString())
            //println("")
          }
        }
        ir.Module(info, name, ports ++ port_list, ir.Block(Seq(body) ++ connect_blocks))
      }
      case m => m
    }
    //println(m_connected_instance.serialize)
    //println("----------------------------")
    m_connected_instance
  }

  override def execute(state: CircuitState): CircuitState = {
    val debug_wires = state.annotations.collect {
      case ExposeTopAnnotation(target, value) => (target, value)
    }
    val wiresToDebug = mutable.HashMap.empty[String, mutable.HashSet[String]]
    val debugPorts = mutable.HashMap.empty[String, mutable.HashSet[(String, ir.Type)]]
    for { a <- debug_wires } {
      a match {
        case (ComponentName(wire, ModuleName(mod, _)), _) => {
          val annos = wiresToDebug.getOrElseUpdate(mod, mutable.HashSet.empty[String])
          annos += wire
          Unit
        }
        case _ => Unit
      }
    }
    val circuit = state.circuit.mapModule(mapModule(wiresToDebug, debugPorts))
    state.copy(/*annotations = state.annotations, */circuit = circuit)
  }
}

//object TestApp extends App {
//  chisel3.Driver.execute(args, () => /*new TopOfDiamond())*/ new SimpleOuterModule())
//}
