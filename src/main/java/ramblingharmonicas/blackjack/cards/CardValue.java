package blackjack.cards;

/**
 * Enumerated card values.
 *
 */
public enum CardValue {
ACE(1), TWO(2), THREE(3), FOUR(4),
FIVE(5), SIX(6), SEVEN(7), EIGHT(8), NINE(9), TEN(10), JACK(10),
QUEEN(10), KING(10);
final private int myValue;

private CardValue(final int myValue) {
   this.myValue = myValue;
}

public int value() {
   return myValue;
}

/**
 * Really, Java, this is the best you got?
 * 1 -> ACE
 * 2-> TWO ETC
 *
 * @param value Between 1-11.
 * @return
 */
static public CardValue cardValueFromInt(int value) {
   switch (value) {
      case 1:
      case 11:
         return CardValue.ACE;
      case 2:
         return CardValue.TWO;
      case 3:
         return CardValue.THREE;
      case 4:
         return CardValue.FOUR;
      case 5:
         return CardValue.FIVE;
      case 6:
         return CardValue.SIX;
      case 7:
         return CardValue.SEVEN;
      case 8:
         return CardValue.EIGHT;
      case 9:
         return CardValue.NINE;
      case 10:
         return CardValue.TEN;
      default:
         throw new IllegalArgumentException("Invalid CardValue: " + value);
   }
}

@Override
public String toString() {
   //only capitalize the first letter
   String s = super.toString();
   if ((this.myValue == 1) || (this.myValue == 8)) {
      return "An " + s.substring(0, 1) + s.substring(1).toLowerCase();
   }
   return "A " + s.substring(0, 1) + s.substring(1).toLowerCase();
}
}
