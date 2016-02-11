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
   //This is safe, right?
   //this.mySuit = Suit.deepClone(mySuit);
   //this.myValue = CardValue.deepClone(myValue);

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

/**
 * Gives a deep clone. Yay.
 *
 *
 * @return
 */
public Suit getSuit() { //public enum Suit { SPADES, CLUBS, DIAMONDS, HEARTS ;
   return mySuit;
}

@Override
public String toString() {
   return this.myValue.toString() + " of " + this.mySuit.toString();

}

/**
 * Gives a deep clone.
 * There's got to be some more efficient way of doing this!
 *
 * @return
 */
/*
 public CardValue getCardValue()
 {
 switch (myValue) {
 case ACE:
 return CardValue.ACE;
 case TWO:
 return CardValue.TWO;
 case THREE:
 return CardValue.THREE;
 case FOUR:
 return CardValue.FOUR;
 case FIVE:
 return CardValue.FIVE;
 case SIX:
 return CardValue.SIX;
 case SEVEN:
 return CardValue.SEVEN;
 case EIGHT:
 return CardValue.EIGHT;
 case NINE:
 return CardValue.NINE;
 case TEN:
 return CardValue.TEN;
 case JACK:
 return CardValue.JACK;
 case QUEEN:
 return CardValue.QUEEN;
 case KING:
 return CardValue.KING;
 }
 System.out.println("Error in function Card.cardVal()");
 throw new RuntimeException();

 }
 */
/*
 * /** Gives a deep clone. Yay.
 *
 *
 * @return
 */
/*
 public Suit getSuit()
 { //public enum Suit { SPADES, CLUBS, DIAMONDS, HEARTS ;
 switch (mySuit) {
 case SPADES:
 return Suit.SPADES;
 case HEARTS:
 return Suit.HEARTS;
 case CLUBS:
 return Suit.CLUBS;
 case DIAMONDS:
 return Suit.DIAMONDS;
 }
 //  return Suit.HEARTS;
 System.out.println("Enum error in function Card.classSuit()");
 throw new RuntimeException();
 }

 */
}
