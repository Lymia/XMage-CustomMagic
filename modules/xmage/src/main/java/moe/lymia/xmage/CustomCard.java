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

package moe.lymia.xmage;

import mage.cards.CardImpl;
import mage.cards.ExpansionSet;
import mage.constants.CardType;
import mage.constants.Rarity;
import mage.constants.SpellAbilityType;

import java.util.UUID;

public abstract class CustomCard extends CardImpl {
    public CustomCard(UUID ownerId, ExpansionSet set, String cardNumber, String name, Rarity rarity, CardType[] cardTypes, String costs, SpellAbilityType spellAbilityType) {
        super(ownerId, cardNumber, name, rarity, cardTypes, costs, spellAbilityType);
        expansionSetCode = set.getCode();
    }

    public CustomCard(UUID ownerId, ExpansionSet set, String cardNumber, String name, Rarity rarity, CardType[] cardTypes, String costs) {
        this(ownerId, set, cardNumber, name, rarity, cardTypes, costs, SpellAbilityType.BASE);
    }

    public CustomCard(UUID ownerId, ExpansionSet set, String name, Rarity rarity, CardType[] cardTypes, String costs, SpellAbilityType spellAbilityType) {
        this(ownerId, set, name.replaceAll("[^A-Za-z]", ""), name, rarity, cardTypes, costs, spellAbilityType);
    }

    public CustomCard(UUID ownerId, ExpansionSet set, String name, Rarity rarity, CardType[] cardTypes, String costs) {
        this(ownerId, set, name, rarity, cardTypes, costs, SpellAbilityType.BASE);
    }

    public CustomCard(CardImpl card) {
        super(card);
    }
}