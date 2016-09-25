/*
 * Copyright (c) 2016 Lymia Alusyia <lymia@lymiahugs.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package moe.lymia.xmage.macros

import scala.annotation.{StaticAnnotation, compileTimeOnly}
import scala.collection.mutable
import scala.language.experimental.macros
import scala.reflect.macros.whitebox

trait HasCopyConstructorImpl {
  type CopyConstructorCopyType
}

// TODO: Figure out how to get extendCopy to work.
/*@compileTimeOnly("enable macro paradise to expand macro annotations")
class extendCopy extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro GenerateCopyImpl.extendCopy
}*/
@compileTimeOnly("enable macro paradise to expand macro annotations")
class withCopy extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro GenerateCopyImpl.withCopy
}

// TODO: Support companion objects
// TODO: Check for .copy() on objects in automatically generated copy constructor.
object GenerateCopyImpl {
  def extendCopy(c: whitebox.Context)(annottees: c.Expr[Any]*) = impl(c)(true)(annottees : _*)
  def withCopy(c: whitebox.Context)(annottees: c.Expr[Any]*) = impl(c)(false)(annottees : _*)
  def impl(c: whitebox.Context)(extendCopy: Boolean)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._
    val (annottee, expandees) = annottees match {
      case param :: (rest @ (_ :: _)) if param.tree.isInstanceOf[ValDef ] => (Some(param), rest)
      case param :: (rest @ (_ :: _)) if param.tree.isInstanceOf[TypeDef] => (Some(param), rest)
      case _ => (None, annottees)
    }

    if(annottee.nonEmpty) sys.error("@generateCopy can only be used on trait definitions")

    var foundTrait = false
    val outputs = expandees flatMap { expr => expr.tree match {
      case (x @ q"$mods class $nameParam $ctorMods(...$ctorParams) extends ..$parents { $self => ..$body }") =>
        val name               = nameParam.toString
        val objectTerm         = TermName(name)
        val generatedTraitName = TypeName(c.freshName(name+"_VariableContainerf"))
        val generatedCopyName  = TypeName(c.freshName(name+"_CopyConstructor"))
        val copyCParamSym      = TermName(c.freshName("copyFrom"))

        val tCtorParams = ctorParams.asInstanceOf[Seq[Seq[ValDef]]]
        val paramNames = tCtorParams.map(_.map(_.name))

        val extractClass: PartialFunction[Tree, Seq[Tree]] = {
          case q"$_ class $_ $_(...$_) extends ..$_ { $_ => ..$body }" => body
        }
        val typedBody = try {
          c.typecheck(q"""
            $mods class $generatedTraitName $ctorMods(...$ctorParams) extends ..$parents { $self => ..$body }
          """) match {
            case t: ClassDef => extractClass(t)
            case t: Block    => t.children.collectFirst(extractClass).get
          }
        } catch {
          // let the natural error messages that would normally happen occur.
          case _: Throwable => return c.Expr[Any](q"..$expandees")
        }

        val bodyVarDefs = typedBody flatMap {
          case ValDef(modifiers, name, tpt, rhs) =>
            Some(q"$modifiers var $name: $tpt = _")
          case x => None
        }
        val bodyMethodDefs = body flatMap {
          case x: DefDef if x.name != TermName("copy") => Some(x)
          case x => None
        }
        val initDefs = body flatMap {
          case x: ValDef =>
            if(x.rhs != EmptyTree) Some(q"${x.name} = ${x.rhs}") else None
          case x: DefDef => None
          case x => Some(x)
        }
        val copyDefs = body flatMap {
          case x: ValDef => Some(q"${x.name} = $copyCParamSym.${x.name}")
          case x => None
        }

        val extractedTypes = parents map {
          case Apply(id, _) => id
          case x => x
        }
        val filteredParents = parents map {
          case Apply(id, _) =>
            if(extendCopy) q"$id.CopyConstructorCopyType($copyCParamSym)"
            else           q"$id($copyCParamSym)"
          case x => x
        }
        val common = q"""
          override def copy() = new $objectTerm.$generatedCopyName(this)(...$paramNames)
          ..$bodyMethodDefs
        """
        val generated = q"""
          class $nameParam(...$tCtorParams) extends ..$parents with $objectTerm.$generatedTraitName {
            ..$common
            ..$initDefs
          }
          object $objectTerm extends moe.lymia.xmage.macros.HasCopyConstructorImpl {
            trait $generatedTraitName extends ..$extractedTypes {
              ..$bodyVarDefs
            }
            class $generatedCopyName(private var $copyCParamSym: $generatedTraitName)(...$tCtorParams)
              extends ..$filteredParents with $objectTerm.$generatedTraitName {
              ..$common
              ..$copyDefs
              $copyCParamSym = null
            }
            type CopyConstructorCopyType = $generatedCopyName
          }
        """
        println(generated)
        Seq(generated)
      case x => sys.error("Unknown tree: "+x)
    }}

    c.Expr[Any](q"..$outputs")
  }

}