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

package moe.lymia.xmage

import scala.reflect.macros.whitebox

package object macros {
  def removeFlags(c: whitebox.Context)(m: c.universe.Modifiers, toRemove: c.universe.FlagSet*) = {
    import c.universe._
    var flags = NoFlags
    val allFlags = Seq(
      Flag.TRAIT, Flag.INTERFACE, Flag.MUTABLE, Flag.MACRO, Flag.DEFERRED, Flag.ABSTRACT, Flag.FINAL, Flag.SEALED,
      Flag.IMPLICIT, Flag.LAZY, Flag.OVERRIDE, Flag.PRIVATE, Flag.PROTECTED, Flag.LOCAL, Flag.CASE, Flag.ABSOVERRIDE,
      Flag.BYNAMEPARAM, Flag.PARAM, Flag.COVARIANT, Flag.CONTRAVARIANT, Flag.DEFAULTPARAM, Flag.PRESUPER,
      Flag.DEFAULTINIT, Flag.ENUM, Flag.PARAMACCESSOR, Flag.CASEACCESSOR, Flag.SYNTHETIC, Flag.ARTIFACT, Flag.STABLE
    )
    for(flag <- allFlags if !toRemove.contains(flag) && m.hasFlag(flag)) flags = flags | flag
    Modifiers(flags, m.privateWithin, m.annotations)
  }
}
