package blackjack;

/**
 * Spades, clubs, diamonds, hearts.
 * Ideally, this should only be accessible from Card.java.
 * Find out how to do that.
 *
 */
public enum Suit {
SPADES, CLUBS, DIAMONDS, HEARTS;

/**
 * Gives a deep clone. Yay.
 *
 *
 * @return
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
   }
   //  return Suit.HEARTS;
   System.out.println("Enum error in function Suit.deepClone(Suit)");
   throw new RuntimeException();

}
//yay, cut and pasted code

@Override public String toString() {
   //only capitalize the first letter
   String s = super.toString();
   return s.substring(0, 1) + s.substring(1).toLowerCase();
}

}
