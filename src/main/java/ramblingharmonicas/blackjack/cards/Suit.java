package blackjack.cards;

public enum Suit {
SPADES, CLUBS, DIAMONDS, HEARTS;

/** Is this even needed. I think Java enums somehow obviate the need for it.
 */
static Suit deepClone(Suit mySuit) {
   switch (mySuit) {
      case SPADES:
         return Suit.SPADES;
      case HEARTS:
         return Suit.HEARTS;
      case CLUBS:
         return Suit.CLUBS;
      case DIAMONDS:
         return Suit.DIAMONDS;
      default:
         throw new IllegalArgumentException("Invalid suit: " + mySuit);
   }
}

@Override public String toString() {
   //only capitalize the first letter
   String s = super.toString();
   return s.substring(0, 1) + s.substring(1).toLowerCase();
}

}
