package blackjack;

/**
 *
 * This is used by Shoe.drawAppropriate to decide which cards
 * should be dealt to the player; it is stored in Strategy.
 * SHOE.drawAppropriate functions must be updated when these are changed.
 *
 * This is used to allow different player practice modes.
 *
 * FREE_PLAY can be used by either player or dealer.
 *
 * These options are to be used only by the dealer:
 * DEALER_2_6
 * DEALER_7_A
 *
 * All other options are to be used only for the player.
 *
 *
 * Not yet tested:
 * ALL_SOFT_AND_HARD(8);
 * SOFT_UNDER_16(5),
 * SOFT_OVER_16(6),
 * DEALER_2_6
 * DEALER_7_A
 *
 */
public enum DrawMode {
FREE_PLAY(0), HARD_UNDER_12(1), HARD_12_16(2), ALL_HARD(3), ALL_SOFT(4),
SOFT_UNDER_16(5), SOFT_OVER_16(6), ALL_SPLITS(7), ALL_SOFT_AND_HARD(8),
DEALER_2_6(9), DEALER_7_A(10);
//private int currentMode;
private int myValue;

private DrawMode(final int myValue) {
   this.myValue = myValue;
}

public int value() {
   return myValue;
}

/**
 * Converts a value into a DM, inefficiently.
 *
 * @param val
 * @return
 */
static public DrawMode getDM(final int val) {
   for (DrawMode d : values()) {
      if (d.value() == val) {
         return d;
      }
   }
   return null;
}

}
