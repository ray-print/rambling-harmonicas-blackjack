package blackjack;

/**
 * An individual card. Contains a suit and value.
 *
 */
public class Card {
private final Suit mySuit;
private final CardValue myValue;

public Card(final Suit mySuit, final CardValue myValue) {
   this.mySuit = mySuit;
   this.myValue = myValue;
}

/**
 * Ideally, a copy constructor (deep clone). Untested, so beware. It hinges
 * on enum encapsulation.
 *
 * @param toBeCloned Card to be cloned.
 *
 */
public Card(final Card toBeCloned) {
   this.mySuit = toBeCloned.getSuit();
   this.myValue = toBeCloned.getCardValue();
}

public Suit getSuit() {
   return mySuit;
}

public String suit() {
   return mySuit.name();
}

public CardValue getCardValue() {
   return myValue;
}

public int value() {
   return myValue.value();
}

@Override
public String toString() {
   return this.myValue.toString() + " of " + this.mySuit.toString();
}

}
