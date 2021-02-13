// See LICENSE for license details.

package chisel3.tester

import chisel3.Bits
import chisel3.Element
import chisel3.experimental.EnumType
import scala.annotation.implicitNotFound

// the class is copied from chisel3/iotesters
// A typeclass that defines the types we can poke, peek, or expect from
@implicitNotFound("Cannot peek or poke elements of type ${T}")
trait Pokeable[-T]

object Pokeable {
  implicit object BitsPokeable extends Pokeable[Bits]
  implicit object EnumPokeable extends Pokeable[EnumType]

  trait IsRuntimePokeable // A trait that is applied to elements that were proven to be pokeable at runtime (usually in match statements)
  implicit object RuntimePokeable extends Pokeable[IsRuntimePokeable]

  def unapply(elem: Element): Option[Element with IsRuntimePokeable] = elem match {
    case _: Bits | _: EnumType => Some(elem.asInstanceOf[Element with IsRuntimePokeable])
    case _ => None
  }
}
