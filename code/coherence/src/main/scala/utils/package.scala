
import java.lang.reflect.Field

import chisel3._
import chisel3.util._
import chisel3.experimental._

package object utils {

  object EnumMuxLookup {
    def apply[T <: Data] (key: EnumType, default: T, mapping: Seq[(EnumType, T)]): T = {
      var res = default
      for ((k, v) <- mapping.reverse)
        res = Mux(k === key, v, res)
      res
    }
  }

  def printe(e: EnumType): Unit = {
    // generate enum
    val factoryField = e.getClass.getDeclaredFields.head // getField("factory")
    factoryField.setAccessible(true)
    val facObj = factoryField.get(e).asInstanceOf[EnumFactory]
    val methods = facObj.getClass.getMethods// ("nameOfValue")
    val nameOfValue =  methods.filter(_.toString.contains("chisel3.experimental.EnumFactory.nameOfValue(scala.math.BigInt)")).head
    nameOfValue.setAccessible(true)
    var ctx = when(false.B) {}
    // NOTE: this is not the bottle neck of elaboration
    for { ele <- facObj.all } {
      when(e.asUInt === ele.asUInt) {
        // printf(s"${ele.toString}")
        printf(s"${nameOfValue.invoke(facObj, ele.litValue).asInstanceOf[Option[String]].get}")
      }
    }
  }

  def printWidthThreshold = 256

  def printbundle(bundle: Bundle): Unit = {
    printf(s"${bundle.className}(")
    for { (b, idx) <- bundle.elements.zipWithIndex } {
      printf(s"${b._1}=")
      b._2 match {
        case sub: Bundle => printbundle(sub)
        case enum: EnumType => printe(enum)
        case uInt: UInt => {
          if(uInt.getWidth >= printWidthThreshold) {
            printf("(too long)")
          } else {
            printf(p"${Hexadecimal(uInt)}")
          }
        } // print in hexadecimal instead
        case vec: Vec[_] => {
          if(vec.getWidth >= printWidthThreshold) {
            printf("(too long)")
          } else {
            printf(vec.toPrintable)
          }
        }
        case other => printf(other.toPrintable)
      }
      if(idx != bundle.elements.size - 1) {
        printf(", ")
      }
    }
    printf(")")
  }

  // This implicit allows use to convert bundle into big integer in a stable way
  implicit class BundleLitAsBigIntHelper[T <: Bundle](x: T) {
    def flatten: IndexedSeq[_ <: Data] = {
      x match {
        case elt: Bundle => elt.getElements.toIndexedSeq.flatMap(c => {
          c match {
            case cc: Bundle => cc.flatten
            case cc => IndexedSeq(cc)
          }
        })
        case elt => IndexedSeq(elt)
      }
    }

    def toBigIntHelper(in: Seq[_ <: Data]): (Int, BigInt) = {
      if (in.tail.isEmpty) {
        in.head.litOption match {
          case Some(v) => {
            (in.head.asUInt.getWidth, in.head.litValue())
          }
          case None => throw new RuntimeException(s"Some value is not convertible, ${in.head}")
        }
      } else {
        val left = toBigIntHelper(in.slice(0, in.length / 2))
        val right = toBigIntHelper(in.slice(in.length / 2, in.length))
        val res = (right._2 << left._1) + left._2
        (right._1 + left._1, res)
      }
    }

    def toBigInt: BigInt = {
      val mapped = x.flatten.map(a => a match {
        case e: EnumType => e.litValue().asUInt(e.getWidth.W)
        case other => other.asUInt
      })
      toBigIntHelper(mapped)._2
    }
  }

}
