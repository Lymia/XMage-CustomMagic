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

import java.util.UUID

import mage.ObjectColor
import mage.abilities.{Ability, SpellAbility}
import mage.abilities.common.SpellCastControllerTriggeredAbility
import mage.abilities.costs.mana.ManaCostsImpl
import mage.abilities.effects.common.ExileSpellEffect
import mage.abilities.effects.{ContinuousEffect, Effect, OneShotEffect, ReplacementEffectImpl}
import mage.cards.Card
import mage.constants._
import mage.filter.FilterSpell
import mage.filter.predicate.mageobject.MulticoloredPredicate
import mage.game.Game
import mage.game.events.GameEvent
import moe.lymia.xmage.macros._

// Virtuoso implementation
object VirtuosoFilter extends FilterSpell("multicolored spell") {
  this.add(new MulticoloredPredicate())
}
@withCopy class VirtuosoAbility(effect: Effect, zone: Zone = Zone.BATTLEFIELD,
                                optional: Boolean = false, rememberSource: Boolean = false)
  extends SpellCastControllerTriggeredAbility(zone, effect, VirtuosoFilter, optional, rememberSource) {
  override def getRule: String = "<i>Virtuoso</i> &mdash; "+super.getRule
}

// Resonance implementation
@withCopy class ResonanceAbility(var card: Card, cost: String)
  extends SpellAbility(new ManaCostsImpl(cost), card.getName+" with resonance",
                       Zone.HAND, SpellAbilityType.BASE_ALTERNATE) {
  // copied from awaken
  {
    val spellAbility = card.getSpellAbility
    getCosts  .addAll(spellAbility.getCosts  .copy())
    getEffects.addAll(spellAbility.getEffects.copy())
    getTargets.addAll(spellAbility.getTargets.copy())
    timing = spellAbility.getTiming

    addEffect(ExileSpellEffect.getInstance)
    addEffect(new ResonanceAbilityReplacementEffect(card.getColor(null), spellAbility))

    card = null
  }

  override def getRule: String =
    s"Resonance $cost <i>(If you cast this spell for $cost, exile it as it resolves. As you cast your next instant or "+
     "sorcery spell, put this card into your graveyard and you may add its other abilities and colors to that spell.)</i>"
}
@withCopy class ResonanceAbilityReplacementEffect(colors: ObjectColor, abilities: SpellAbility)
  extends ReplacementEffectImpl(Duration.OneUse, Outcome.Benefit, false) {

  this.staticText = "Resonance replacement effect"

  override def replaceEvent(event: GameEvent, source: Ability, game: Game) = {
    val card = game.getCard(source.getSourceId)
    val spell = game.getCard(event.getSourceId)
    val you = game.getPlayer(source.getControllerId)

    if(game.getState.getZone(card.getId) == Zone.EXILED && spell != null) {
      val owner = game.getPlayer(card.getOwnerId)
      if(owner != null) owner.moveCards(card, Zone.GRAVEYARD, source, game)

      if(you.chooseUse(Outcome.Benefit, "Would you like to add the Resonance effects?", source, game)) {
        val spellColor = spell.getColor(game)
        spellColor.setColor(spellColor.union(colors))
        game.getContinuousEffects.addEffect(new ResonanceAbilitySpliceEffect(event.getTargetId, abilities), source)
      }
    }

    used = true
    false
  }

  override def applies(event: GameEvent, source: Ability, game: Game) =
    event.getPlayerId.equals(source.getControllerId) && {
      val spell = game.getCard(event.getSourceId)
      spell != null && (spell.getCardType.contains(CardType.INSTANT) || spell.getCardType.contains(CardType.SORCERY))
    }

  override def checksEventType(gameEvent: GameEvent, game: Game) =
    gameEvent.getType == GameEvent.EventType.CAST_SPELL

  override def copy(): ContinuousEffect = ???
}
@withCopy class ResonanceAbilitySpliceEffect(targetSpell: UUID, abilities: SpellAbility)
  extends ReplacementEffectImpl(Duration.OneUse, Outcome.Benefit, false) {

  this.staticText = "Resonance splice effect"

  override def replaceEvent(event: GameEvent, source: Ability, game: Game): Boolean = {
    val spell = game.getStack.getSpell(event.getTargetId)

    if(spell != null) {
      val spliced = abilities.copy()
      spliced.setSpellAbilityType(SpellAbilityType.SPLICE)
      spliced.setSourceId(spell.getSourceId)
      spell.addSpellAbility(spliced)
    }

    used = true
    false
  }

  override def applies(gameEvent: GameEvent, source: Ability, game: Game) = {
    gameEvent.getTargetId == targetSpell
  }
  override def checksEventType(gameEvent: GameEvent, game: Game): Boolean = {
    gameEvent.getType == GameEvent.EventType.CAST_SPELL_LATE
  }

  override def copy(): ContinuousEffect = ???
}