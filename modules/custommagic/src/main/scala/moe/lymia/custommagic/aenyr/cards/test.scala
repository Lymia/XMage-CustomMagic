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

package moe.lymia.custommagic.aenyr.cards

import java.util.UUID

import mage.MageInt
import mage.abilities.effects.common.combat.CantBlockTargetEffect
import mage.abilities.effects.common.continuous.{BoostControlledEffect, BoostTargetEffect}
import mage.abilities.keyword.LifelinkAbility
import mage.constants.{CardType, Duration, Rarity}
import mage.target.common.TargetCreaturePermanent
import moe.lymia.custommagic.aenyr.{Aenyr, ResonanceAbility, VirtuosoAbility}
import moe.lymia.xmage.CustomCard
import moe.lymia.xmage.macros._

@withCopy abstract class LifesongFlutist(ownerId: UUID)
  extends CustomCard(ownerId, Aenyr, "Lifesong Flutist", Rarity.COMMON, Array(CardType.CREATURE), "{W}") {

  subtypes("Human", "Bard")
  pt(1, 1)

  // Lifelink
  addAbility(LifelinkAbility.getInstance())

  // Virtuoso — Whenever you cast a multicolored spell, target creature gets +2/+2 until end of turn.
  addAbility({
    val ability = new VirtuosoAbility(new BoostTargetEffect(2, 2, Duration.EndOfTurn))
    ability.addTarget(new TargetCreaturePermanent())
    ability
  })
}

@withCopy abstract class InspiringBallad(ownerId: UUID)
  extends CustomCard(ownerId, Aenyr, "Inspiring Ballad", Rarity.COMMON, Array(CardType.INSTANT), "{1}{W}") {

  // Creatures you control get +1/+1 until end of turn.
  getSpellAbility.addEffect(new BoostControlledEffect(1, 1, Duration.EndOfTurn))

  // Resonance 3W
  addAbility(new ResonanceAbility(this, "{3}{W}"))
}

@withCopy abstract class DeafeningBallad(ownerId: UUID)
  extends CustomCard(ownerId, Aenyr, "Deafening Ballad", Rarity.COMMON, Array(CardType.SORCERY), "{1}{R}") {

  // Up to two target creatures can’t block this turn.
  getSpellAbility.addEffect(new CantBlockTargetEffect(Duration.EndOfTurn))
  getSpellAbility.addTarget(new TargetCreaturePermanent(0, 2))

  // Resonance 3R
  addAbility(new ResonanceAbility(this, "{3}{R}"))
}
