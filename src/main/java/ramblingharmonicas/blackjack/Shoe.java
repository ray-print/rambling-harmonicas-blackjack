package ramblingharmonicas.blackjack;
import ramblingharmonicas.blackjack.cards.*;
import java.util.*;
import java.security.SecureRandom;

/**
 * Contains all cards in a dealing shoe.
 *
 *
 * Future development: I can draw a random card; I can reset it to whatever I
 * want.
 */
public class Shoe implements VagueShoe {
private ArrayList<Card> myCards = new ArrayList<Card>();
private int[] cardValueCache = new int[11];
private int totalCards;
/**
 * This random number generator is overkill; change it if it's going too slow.
 * Alternately, create another thread whose sole job is to generate random
 * numbers,
 * then use a synchronized queue. That'd be cool!
 * Appears to fail miserably on an Android.
 */
private SecureRandom generator = new SecureRandom();

@Override
public int numberOfCards() {
   return totalCards;

}

/**
 * Constructs a shoe with the specified number of decks.
 * For example, Shoe(2) would create a new shoe with 2 decks.
 *
 *
 */
public Shoe(int numberOfDecks) {
   initializeAll(numberOfDecks);
}

private void initializeAll(int numberOfDecks) {
   if (numberOfDecks <= 0) {
      throw new IllegalArgumentException();
   }
   totalCards = numberOfDecks * 52;
   int i;
   for (i = 0; i < numberOfDecks; i++) {
      for (Suit theSuit : Suit.values()) {
         for (CardValue cValue : CardValue.values()) {
            myCards.add(new Card(theSuit, cValue));
         }
      }
   }
   for (i = 0; i < cardValueCache.length - 1; i++) {
      cardValueCache[i] = 4 * numberOfDecks;
   }
   cardValueCache[cardValueCache.length - 1] = 16 * numberOfDecks;

}

/**
 * UNTESTED
 *
 *
 * @return a deep clone of the Shoe.
 */
public Shoe deepClone() {

   Card[] myArray = myCards.toArray(new Card[myCards.size()]);
   Shoe myClone = new Shoe(myArray, this.cardValueCache, this.totalCards);

   return myClone;

}

/**
 * Constructs shoe from array of Cards
 * UNTESTED. Used by deepclone.
 * MAY NOT CORRECTLY COPY cardValueCache -- depends on how Java stores arrays.
 *
 * @param cardArray
 */
private Shoe(Card[] cardArray, int[] cardValCache, int totalCards) {//super(cardValCache);
   myCards.addAll(Arrays.asList(cardArray));
   this.totalCards = totalCards;
   System.arraycopy(cardValCache, 0, this.cardValueCache, 0, cardValCache.length);

}

@Override
public String toString() {
   StringBuilder s = new StringBuilder();
   s.append("The shoe contains a/an: ");
   for (int i = 0; i < totalCards; i++) {
      s.append(myCards.get(i).getCardValue()).append(" of ").append(myCards.get(i).getSuit());

   }
   s.append("for a total of ").append(totalCards).append(" cards.");
   return s.toString();
}

@Override
public void printContents() {
   System.out.println(this);

}

/**
 * Uses caching to (hopefully) speed up this process.
 * Tested in FastShoe for 13 cards, values all correct. Untested in Shoe
 *
 * @param thisCard
 * @return The probability of drawing this CardValue. Returns a negative number
 * if no cards
 * with that CardValue are in the shoe.
 */
@Override
public double fastProbabilityOf(final CardValue thisCard) {
   if (totalCards == 0) {
      return -100000;
   }
   else if (cardValueCache[ thisCard.value()] == 0) {
      return -100000;
   }
   return ((double) (cardValueCache[ thisCard.value()]) / ((double) totalCards));
}

/**
 *
 *
 * @param thisCard
 * @return
 */
public double fastProbabilityOf(final Card thisCard) {
   if (totalCards == 0) {
      return -100000;
   }
   else if (cardValueCache[ thisCard.value()] == 0) {
      return -100000;
   }

   //Java requires explicit upcasting.
   return ((double) (cardValueCache[ thisCard.value()]) / ((double) totalCards));
}

/**
 * Pulls from the deck a semi-random card matching this CardValue; tens, jacks,
 * queens and kings are indistinguishable.
 *
 * @return Returns the card drawn.
 * @exception throws an IllegalArgumentException if there are no cards in the
 * shoe or
 * no cards of the specified value in the shoe. Use probabilityOf first to
 * check for these possibilities.
 *
 * Untested.
 * @args CardValue thisCard is the value of card to be pulled
 */
public Card drawSpecific(final CardValue thisCard) throws ShuffleNeededException {
   if (myCards.isEmpty()) {
      throw new ShuffleNeededException("Shoe.drawSpecific(CardValue) called with no cards left in the shoe.");
   }
   //locate all cards that match that value, pick one of them, if one of them exists.
   int i;
   ArrayList<Integer> locations = new ArrayList<Integer>();
   for (i = 0; i < myCards.size(); i++) {
      if (myCards.get(i).value() == thisCard.value()) {
         locations.add(i);
      }
   }

   if (locations.isEmpty()) {
      throw new ShuffleNeededException("Shoe.drawSpecific(CardValue) called for a CardValue not present"
              + " in the shoe.");
   }

   //pick random number between 0 and locations.size() - 1. Use it to select an index
   // in the locations array. Pick that element of the array, and draw that card.
   // int randomIndex = generator.nextInt(locations.size());
   // int cardRemovedLocation = locations.get(randomIndex);
   // System.out.println ("For card value " + thisCard + ", cardRemovedLocation is " + cardRemovedLocation);
   // myCards.remove (cardRemovedLocation);  (this is identical to the below line.)


   Card pulledCard = myCards.get((int) locations.get(generator.nextInt(locations.size())));
   myCards.remove(pulledCard/* (int) locations.get(generator.nextInt(locations.size())) */);
   cardValueCache[(thisCard.value())]--;
   totalCards--;
   return pulledCard;

}

/**
 * UNTESTED.
 *
 *
 * @return A random card from this Shoe, which has been pulled from the Shoe.
 */
public Card drawRandom() { //OK. This exception is way too much of a pain to deal with.
   if (totalCards == 0) {
      ShuffleNeededException sne = new ShuffleNeededException("No cards left in shoe.");
      if (Blackjack.debug()) {
         throw new RuntimeException(sne);
      }
      sne.printStackTrace();
      //OR just reinitialize????
      return null; //Probably will cause a NPE later.
   }
   final int index = (int) (generator.nextDouble() * (double) myCards.size());
   //As long as Java always rounds down when typecasting, I'm good.
   Card pulledCard = myCards.get(index);
   // I assume it doesn't matter that the deck is always in perfect order.
   myCards.remove(index);
   //does myCard still exist?
   cardValueCache[(pulledCard.value())]--;

   totalCards--;
   return pulledCard;
}

/**
 * This function is untested.
 *
 * @param excluded
 * @return
 * @throws ShuffleNeededException
 */
private Card drawAllExcept(CardValue... excluded) throws ShuffleNeededException {
   int errorCondition = 0;
   Card drawnCard;
   boolean flag = true;
   while (true) {
      drawnCard = drawRandom();
      flag = true;
      for (int i = 0; i < excluded.length; i++) {
         if (drawnCard.value() == excluded[i].value()) {
            flag = false;
            break;
         }
      }
      if (flag) {
         return drawnCard;
      }

      addCard(drawnCard);

      errorCondition++;
      if (errorCondition == 1000) {
         throw new ShuffleNeededException("Some error in drawAllExcept."
                 + " Here is my shoe: " + toString());
      }
   }
}

/**
 ** Draws the first card for the player if player is set to true, otherwise
 * draws the dealer card.
 * Auto-shuffles as needed.
 *
 *
 * @param myMode current DrawMode
 * See the DrawMode class for more information on what the DrawModes mean.
 *
 * @param player True if you're drawing the first player card, false if you're
 * drawing the dealer card
 * @return The drawn card.
 *
 * @throws ShuffleNeededException if a valid card is not in the shoe
 *
 * See other function for list of untested, unimplemented, and mothballed.
 */
public Card drawAppropriate(DrawMode myMode, boolean player, Rules theRules) throws ShuffleNeededException {
   if (!player) {
      switch (myMode) {
         case DEALER_2_6:
            return drawBetween(Blackjack.TWOCARD + 1, Blackjack.SIXCARD + 1);

         case DEALER_7_A:
            double sum = 0;
            final double relativeProbOfAce;
            for (int i = Blackjack.SEVENCARD + 1; i <= Blackjack.TENCARD + 1; i++) {
               sum += cardValueCache[i];
            }
            if (sum != 0) {
               relativeProbOfAce = ((double) cardValueCache[Blackjack.ACECARD + 1]) / sum;
            }
            else {
               relativeProbOfAce = 1;
            }
            if (generator.nextDouble() < relativeProbOfAce) {
               return drawBetween(Blackjack.ACECARD + 1, Blackjack.ACECARD + 1);
            }
            else {
               return drawBetween(Blackjack.SEVENCARD + 1, Blackjack.TENCARD + 1);
            }

         case FREE_PLAY:
            return drawRandom();

         default:
            throw new UnsupportedOperationException("This DrawMode not supported for the dealer: " + myMode.toString());
      }
   }

   switch (myMode) {
      case ALL_SPLITS:
      case FREE_PLAY:
      case ALL_SOFT_AND_HARD: //Any card
         return drawRandom();
      case ALL_HARD:
      case HARD_12_16:
         //Any non-ace card
         return drawBetween(Blackjack.TWOCARD + 1, Blackjack.TENCARD + 1);

      case ALL_SOFT:
         if (cardValueCache[Blackjack.ACECARD + 1] == 0) {
            throw new ShuffleNeededException("No aces left -- can't draw a soft hand.");
         }
         return drawBetween(Blackjack.TWOCARD + 1, Blackjack.TENCARD + 1);


      case HARD_UNDER_12: // A card between 2 and 9.
         return drawBetween(Blackjack.TWOCARD + 1, Blackjack.NINECARD + 1);
      case SOFT_UNDER_16:
         return drawBetween(Blackjack.TWOCARD + 1, Blackjack.FIVECARD + 1);
      case SOFT_OVER_16:
         return drawBetween(Blackjack.SIXCARD + 1, Blackjack.TENCARD + 1);
      // MOTHBALLED
         /*



       case HAND_TOTAL_UNDER_9: //pull 2-6.
       return drawBetween(Blackjack.TWOCARD +1, Blackjack.SIXCARD +1);
       */

      default:
         throw new UnsupportedOperationException(myMode.toString());
   }



}

/**
 * Draws a card between the low card value (Ace = 1) and the high card value
 * (10),
 * inclusive; does this by randomly picking cards until the right one is found.
 * MAKE ME PRIVATE AFTER TESTING
 *
 * @param lowCardValue
 * @param highCardValue
 * @return
 * @throws ShuffleNeededException, IllegalArgumentException
 *
 */
private Card drawBetween(final int lowCardValue, final int highCardValue) throws ShuffleNeededException { //Check possibility
   if (lowCardValue > highCardValue) {
      throw new IllegalArgumentException();
   }
   boolean impossibleDraw = true;
   int i;

   for (i = lowCardValue; i <= highCardValue; i++) {
      if (cardValueCache[i] > 0) {
         impossibleDraw = false;
         break;
      }
   }

   if (impossibleDraw) {
      this.printContents();

      throw new ShuffleNeededException("There are no cards between value " + lowCardValue
              + "and " + highCardValue + " left in the shoe.");
   }


   if (lowCardValue == highCardValue) {
      return drawSpecific(CardValue.cardValueFromInt(lowCardValue));

   }

   boolean badPull = true;
   Card pulledCard = null;
   while (badPull) {
      pulledCard = drawRandom();
      if ((pulledCard.value() < lowCardValue) || (pulledCard.value() > highCardValue)) {
         this.addCard(pulledCard);    //Add the card back to the deck
      }
      else {
         badPull = false;
      }
   }
   if (pulledCard == null) {
      throw new NullPointerException();
   }

   return pulledCard;
}

/**
 * Use me sparingly.
 *
 * @param numberOfDecks
 *
 */
public void reshuffle(int numberOfDecks) {
   Utilities.zero(cardValueCache);
   myCards.clear();
   totalCards = 0;
   initializeAll(numberOfDecks);

}

/**
 * 
 * If the GameMode is not set to free play, then shuffle after every hand.
 * ONLY CALL THIS FUNCTION AFTER FIRST CALLING THE OTHER DRAW-APP
 * FUNCTION; pass the drawn Card as an argument here. Otherwise it
 * won't work.
 *
 *
 * @param cardPullCode
 * @param previouslyDrawnCard
 * @return
 * @throws ShuffleNeededException, IllegalArgumentException
 *
 * Untested -- ALL_SOFT_AND_HARD
 * Not implemented -- Soft under 16, soft over 16
 * Mothballed -- 2-6, 7-A, Hand total under 9.
 */
public Card drawAppropriate(DrawMode myMode, Card previouslyDrawnCard) throws ShuffleNeededException {
   Card drawnCard = null;
   int maxValue, minValue;
   boolean flag = false;
   int errorCondition = 0;
   switch (myMode) {
      case ALL_HARD:
         return drawAllExcept(CardValue.ACE, previouslyDrawnCard.getCardValue());

      case HARD_12_16:
         maxValue = 16 - previouslyDrawnCard.value();
         if (maxValue > 10) {
            maxValue = 10;
         }
         minValue = 12 - previouslyDrawnCard.value();
         if ((minValue > 10) || (minValue < 2)) {
            System.out.println("Draw appropriate called with "
                    + "bad card argument or pull code: an ace can't be drawn to a hard hand.");
            assert (false);
            minValue = 10;
         }
         return drawBetween(minValue, maxValue);

      case SOFT_OVER_16:
      case SOFT_UNDER_16:
      case ALL_SOFT:
         if (previouslyDrawnCard.getCardValue() == CardValue.ACE) {
            return drawAllExcept(previouslyDrawnCard.getCardValue());
         }
         return drawSpecific(CardValue.ACE);
      case ALL_SPLITS:
         return drawSpecific(previouslyDrawnCard.getCardValue());
      case FREE_PLAY:
         return drawRandom();

      case HARD_UNDER_12: // First card was between 2 and 9.
         maxValue = 11 - previouslyDrawnCard.value();
         if (maxValue > 10) {
            System.out.println("drawAppropriate(int,Card) called with invalid card.");
            assert (false);
            maxValue = 10;
         }
         return drawBetween(Blackjack.TWOCARD + 1, maxValue);
      case ALL_SOFT_AND_HARD:
         //Anything but splits. UNTESTED
         return drawAllExcept(previouslyDrawnCard.getCardValue());
      /* MOTHBALLED
       case DEALER_2_6: //pull 2-6. Other card irrelevant.
       return drawAppropriate (BETWEEN_2_AND_6);
       case DEALER_7_A: //pull 7-A. Other card irrelevant.
       return drawAppropriate (BETWEEN_7_AND_A);

       case HAND_TOTAL_UNDER_9: //First card was a pull between 2-7.
       maxValue = 8 - previouslyDrawnCard.value();
       if (maxValue <= 1)
       {
       System.out.println("drawAppropriate(int,Card) called with invalid card.");
       assert(false);
       maxValue = 2;
       }
       return drawBetween(Blackjack.TWOCARD +1, maxValue);
       */
      default:
         throw new UnsupportedOperationException(myMode.toString());
   }
}

/**
 * Draws first card in shoe of specified value. About seven times faster than
 * drawSpecific.
 *
 */
@Override
public Card fastDrawSpecific(final CardValue thisCard) {
   if (myCards.isEmpty()) {
      throw new RuntimeException("Shoe.fastDrawSpecific(CardValue) called with no cards left in the shoe.");
   }
   //locate all cards that match that value, pick one of them, if one of them exists.
   int i;
   //      ArrayList<Integer> locations = new ArrayList<Integer>();


   for (i = 0; i < myCards.size(); i++) {
      if (myCards.get(i).value() == thisCard.value()) {
         Card pulledCard = myCards.get(i);
         myCards.remove(pulledCard);
         cardValueCache[(thisCard.value())]--;
         totalCards--;
         return pulledCard;

      }
   }
   throw new RuntimeException("Shoe.fastdrawSpecific(CardValue) called for a CardValue not present"
           + " in the shoe.");

}

/**
 * TODO: Test this
 *
 * @param Card to be added.
 */
@Override
public void addCard(final Card thisCard) {
   myCards.add(new Card(thisCard));
   cardValueCache[(thisCard.value())]++;
   totalCards++;
}

/**
 * UNTESTED. If arraycopy actually does a deep clone then I should be good.
 *
 * @return Contents of the Card Value Cache. Used by FastShoe to make a clone.
 */
public int[] getCardValueCache() {
   int[] copy = new int[(this.cardValueCache.length)];
   System.arraycopy(cardValueCache, 0, copy, 0, cardValueCache.length);
   return copy;
}

}