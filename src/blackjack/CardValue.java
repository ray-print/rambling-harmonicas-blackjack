package blackjack;

/** Enumerated card values.
 *
 */
public enum CardValue
{
ACE(1), TWO(2), THREE(3), FOUR(4),
FIVE(5), SIX(6), SEVEN(7), EIGHT(8), NINE(9), TEN(10), JACK(10),
QUEEN(10), KING(10);

final private int myValue;

private CardValue(final int myValue)
{
   this.myValue = myValue;
}

/**
 *
 *
 *
 * @return Value of the card. Stored as an int, so it's a deep clone.
 */
public int value()
{
   return myValue;
}
/*final private String cardName;
 private CardValue(final String cardName,final int myValue) {
 this.cardName = cardName;
 this.myValue = myValue;

 } */

/*** UNTESTED.
 * 1 -> ACE
 * 2-> TWO ETC
 *
 * @param value Between 1-11.
 * @return
 */
static public CardValue cardValueFromInt(int value)
{  
   if ((value < 1) || (value > 11))
   { 
      System.err.println("Invalid argument: " + value);
      throw new IllegalArgumentException();  
   }
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
      throw new IllegalArgumentException();
   }
}

//modify this to do its thing to cardName???
@Override
public String toString()
{
   //only capitalize the first letter
   String s = super.toString();
   if ((this.myValue == 1) || (this.myValue == 8))
      return "An " + s.substring(0, 1) + s.substring(1).toLowerCase();
   else
      return "A " + s.substring(0, 1) + s.substring(1).toLowerCase();
}

/* Don't need this because of how enums work. Yay.
static public CardValue deepClone(CardValue toBeCloned)
{
   switch (toBeCloned) {
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
   System.out.println("Error in function CardValue.deepClone()");
   throw new RuntimeException();

}
*/

}
