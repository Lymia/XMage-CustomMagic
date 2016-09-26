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

object GenerateCopyImpl {
  def extendCopy(c: whitebox.Context)(annottees: c.Expr[Any]*) = impl(c)(true)(annottees : _*)
  def withCopy(c: whitebox.Context)(annottees: c.Expr[Any]*) = impl(c)(false)(annottees : _*)
  def impl(c: whitebox.Context)(extendCopy: Boolean)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.mirror._
    import c.universe._

    val trees = annottees.map(_.tree).toList

    val (classDef  : Seq[Tree], rest) = trees.partition(_.isInstanceOf[ClassDef ])
    val (moduleDef : Seq[Tree], left) = rest .partition(_.isInstanceOf[ModuleDef])

    if(classDef .length > 1) { c.error(c.enclosingPosition, "Multiple ClassDefs found" ); return c.Expr(q"()") }
    if(moduleDef.length > 1) { c.error(c.enclosingPosition, "Multiple ModuleDefs found"); return c.Expr(q"()") }
    if(left.nonEmpty || classDef.isEmpty) {
      c.error(c.enclosingPosition, "@withCopy can only be applied to classes." ); return c.Expr(q"()") }

    val q"$mods class $nameParam[..$tparams] $ctorMods (...$ctorParams) extends ..$parents { ..$body }" =
      classDef.head
    val q"$moduleMods object $moduleName extends ..$moduleParents { ..$moduleBody }" =
      if(moduleDef.isEmpty) q"object ${TermName(nameParam.toString)} { }" else moduleDef.head

    val name               = nameParam.toString
    val generatedTraitName = TypeName(c.freshName(name+"_ImplTrait"))
    val generatedCopyName  = TypeName(c.freshName(name+"_CopyConstructor"))
    val copyCParamSym      = TermName(c.freshName("copyFrom"))

    val extractClass: PartialFunction[Tree, (List[List[ValDef]], Seq[Tree])] = {
      case q"$_ class $_ $_(...$params) extends ..$_ { $_ => ..$body }" => (params, body)
    }
    val (typedParams, typedBody) = try {
      c.typecheck(q"""
        $mods class $generatedTraitName $ctorMods(...$ctorParams) extends ..$parents { ..$body }
      """) match {
        case t: ClassDef => extractClass(t)
        case t: Block    => t.children.collectFirst(extractClass).get
      }
    } catch {
      // let the natural error messages that would normally happen occur.
      case _: Throwable => return c.Expr[Any](q"${classDef.head}; ..$moduleDef")
    }

    def hasCopy(t: ValDef) = try {
      val tp = staticClass(t.tpt.toString)
      tp.info.members.exists(x =>
        x.isMethod && x.asMethod.name == TermName("copy") && x.asMethod.paramLists.flatten.isEmpty)
    } catch {
      case e: ScalaReflectionException =>
        c.warning(t.pos, "Could not check type for copy method: "+e.getMessage)
        false
    }
    val traitPrototypeDefs = typedBody flatMap {
      case x: DefDef if x.name != TermName("copy") =>
        val newMods = removeFlags(c)(x.mods, Flag.PRIVATE, Flag.PROTECTED, Flag.FINAL, Flag.SEALED)
        Some(DefDef(Modifiers(newMods.flags | Flag.DEFERRED, newMods.privateWithin, newMods.annotations),
                    x.name, x.tparams, x.vparamss, x.tpt, EmptyTree))
      case ValDef(modifiers, name, tpt, rhs) =>
        Some(q"$modifiers var $name: $tpt = _")
      case x => None
    }
    val traitMemberDefs = body flatMap {
      case x: TypeDef => Some(x)
      case x: ImplDef => Some(x)
      case x => None
    }

    val commonMethodDefs = body flatMap {
      case x: DefDef if x.name != TermName("copy") =>
        val newMods = removeFlags(c)(x.mods, Flag.PRIVATE, Flag.PROTECTED)
        Some(DefDef(newMods, x.name, x.tparams, x.vparamss, x.tpt, x.rhs))
      case x => None
    }

    val initDefs = body flatMap {
      case x: ValDef =>
        if(x.rhs != EmptyTree) Some(q"${x.name} = ${x.rhs}") else None
      case x: DefDef  => None
      case x: TypeDef => None
      case x: ImplDef => None
      case x => Some(x)
    }
    val copyDefs = typedBody flatMap {
      case x: ValDef =>
        val field = q"$copyCParamSym.${x.name}"
        if(hasCopy(x)) Some(q"${x.name} = if($field != null) $field.copy() else null")
        else           Some(q"${x.name} = $field")
      case x => None
    }

    val tCtorParams = ctorParams.asInstanceOf[Seq[Seq[ValDef]]]
    val copyArguments = typedParams.map(_.map(x =>
      if(hasCopy(x)) q"if(${x.name} != null) ${x.name}.copy() else null" else Ident(x.name)
    ))
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
    val newMods = removeFlags(c)(mods, Flag.ABSTRACT)
    val newModuleMods = removeFlags(c)(moduleMods, Flag.PRIVATE, Flag.PROTECTED)
    val common = q"""
      override def copy() = new $moduleName.$generatedCopyName(this)(...$copyArguments)
      ..$commonMethodDefs
    """
    val generated = q"""
      $newMods class $nameParam $ctorMods(...$tCtorParams) extends ..$parents with $moduleName.$generatedTraitName {
        ..$common
        ..$initDefs
      }
      $newModuleMods object $moduleName extends ..$moduleParents with moe.lymia.xmage.macros.HasCopyConstructorImpl {
        ..$moduleBody
        trait $generatedTraitName extends ..$extractedTypes {
          ..$traitPrototypeDefs
          ..$traitMemberDefs
        }
        class $generatedCopyName(private var $copyCParamSym: $generatedTraitName)(...$tCtorParams)
          extends ..$filteredParents with $moduleName.$generatedTraitName {
          ..$common
          ..$copyDefs
          $copyCParamSym = null
        }
        type CopyConstructorCopyType = $generatedCopyName
      }
    """
    println(generated)
    c.Expr(generated)
  }
}