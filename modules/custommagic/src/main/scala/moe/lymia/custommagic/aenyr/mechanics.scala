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

package moe.lymia.custommagic.aenyr

import mage.abilities.common.SpellCastControllerTriggeredAbility
import mage.abilities.effects.Effect
import mage.constants.Zone
import mage.filter.FilterSpell
import mage.filter.predicate.mageobject.MulticoloredPredicate
import moe.lymia.xmage.macros._

object VirtuosoFilter extends FilterSpell("multicolored spell") {
  this.add(new MulticoloredPredicate())
}
@withCopy class VirtuosoAbility(effect: Effect, zone: Zone = Zone.BATTLEFIELD,
                                optional: Boolean = false, rememberSource: Boolean = false)
  extends SpellCastControllerTriggeredAbility(zone, effect, VirtuosoFilter, optional, rememberSource) {
  override def getRule: String = "<i>Virtuoso</i> &mdash; "+super.getRule
}