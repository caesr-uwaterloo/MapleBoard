
package coherence.internal

import chisel3.util.log2Ceil

import scala.annotation.StaticAnnotation
import scala.annotation.compileTimeOnly
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.language.experimental.macros
import scala.reflect.macros.blackbox
import scala.reflect.macros.blackbox.Context

object AsAutoEnum {
  def impl(c: blackbox.Context)(annottees: c.Tree*): c.Tree = {
    import c.universe._
    val tr::obj::Nil = annottees
    tr match {
      case x@q"..$mod trait $tname extends $template { ..$stats }" => {
        var stateCollection: mutable.ArrayBuffer[String] = new ArrayBuffer()
        val dec = stats.zipWithIndex.map(f => f._1 match {
          case c@q"val $name: Int" => {
            stateCollection.append(name.toString())
            q"val $name: Int = (super.getGeneratedItemCount + ${f._2})"
          }
          case any => q"$any"
        })
        val stateNameStr = stateCollection.toList
        val stateNameS = stateNameStr.mkString(" ")
        val width = log2Ceil(dec.length)
        val decWithWidth =
          q"""
        ..$dec
        override def getGeneratedItemCount: Int = super.getGeneratedItemCount + ${stateCollection.length}
        override def getWidth: Int = log2Ceil(getGeneratedItemCount);
        override def getStateList: List[String] = super.getStateList ::: $stateNameStr;
        """
        val ret = q"""
        abstract trait $tname extends $template { ..$decWithWidth };
        object ${tname.toTermName} extends ${tname}
        """
        q"..$ret"
      }
      case _ => throw new Exception("WRONG")
    }
  }
}

@compileTimeOnly("enable macro paradise to expand macro annotations")
class AsAutoEnum extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro AsAutoEnum.impl
}

