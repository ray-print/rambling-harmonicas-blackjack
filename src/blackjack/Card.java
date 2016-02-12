package blackjack;

/**
 * An individual card. Contains a suit and value.
 *
 */
public class Card {
static String getValueCode(CardValue cv) {
   switch (cv.value()) {
      case 10:
         return "t"; //use aCard.getCardValue().
      case 11:
         return "j";
      case 12:
         return "q";
      case 13:
         return "k";
      case 1:
         return "a";
      default:
         return String.valueOf(cv.value());
   }
}

static String getSuitAbbreviation(Suit aSuit) {
   return "" + Character.toLowerCase(aSuit.toString().charAt(0));
}

/**
 * Returns the filename, without extension, of the given card.
 *
 * @param aCard
 * @return
 *
 */
static public String getFileName(Card aCard) {
   return getSuitAbbreviation(aCard.getSuit()) + getValueCode(aCard.getCardValue());
}

private final Suit mySuit;
private final CardValue myValue;

/**
 * Untested
 *
 *
 * @param mySuit
 * @param myValue
 */
public Card(final Suit mySuit, final CardValue myValue) {
   this.mySuit = mySuit;
   this.myValue = myValue;
}

public Card(final Suit mySuit, final CardValue myValue, final boolean noclone) {
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

public String suit() {
   return mySuit.name();
}

public int value() {
   return myValue.value();
}

public CardValue getCardValue() {
   return myValue;
}

public Suit getSuit() {
   return mySuit;
}

@Override
public String toString() {
   return this.myValue.toString() + " of " + this.mySuit.toString();

}

}
