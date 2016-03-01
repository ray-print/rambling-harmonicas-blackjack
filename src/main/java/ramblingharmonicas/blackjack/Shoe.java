package ramblingharmonicas.blackjack;
import ramblingharmonicas.blackjack.cards.*;
import java.util.*;
import java.security.SecureRandom;

/**
 * Contains all cards in a dealing shoe.
 * 
 */
public class Shoe implements VagueShoe {
private ArrayList<Card> myCards = new ArrayList<Card>();
private int[] cardValueCache = new int[11];
private int totalCards;
/**
 * This random number generator is overkill; change it if it's going too slow.
 */
private SecureRandom generator = new SecureRandom();

@Override
public int numberOfCards() {
   return totalCards;

}

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

//TODO: Test this function
public Shoe deepClone() {
   Card[] myArray = myCards.toArray(new Card[myCards.size()]);
   Shoe myClone = new Shoe(myArray, this.cardValueCache, this.totalCards);
   return myClone;
}

/**
 * Constructs shoe from array of Cards
 */
private Shoe(Card[] cardArray, int[] cardValCache, int totalCards) {
   myCards.addAll(Arrays.asList(cardArray));
   this.totalCards = totalCards;
   System.arraycopy(cardValCache, 0, this.cardValueCache, 0, cardValCache.length);
}

@Override
public String toString() {
   StringBuilder s = new StringBuilder();
   s.append("The shoe contains a/an: ");
   for (int i = 0; i < totalCards; i++) {
      s.append(myCards.get(i).getCardValue()).append(" of ")
              .append(myCards.get(i).getSuit()).append("; ");
   }
   s.append("for a total of ").append(totalCards).append(" cards.");
   return s.toString();
}

/**
 * TODO: Use FastShoe throughout for calculation, then deprecate this.
 * Uses caching to (hopefully) speed up this process.
 * Tested in FastShoe for 13 cards, values all correct. Untested in Shoe
 *
 * @param thisCard
 * @return The probability of drawing this CardValue. Returns a negative number
 * if no cards with that CardValue are in the shoe.
 */
@Override
public double probabilityOf(final CardValue thisCard) {
   if (totalCards == 0) {
      return -100000;
   }
   else if (cardValueCache[ thisCard.value()] == 0) {
      return -100000;
   }
   return ((double) (cardValueCache[ thisCard.value()]) / ((double) totalCards));
}

/**
 * Pulls from the deck a semi-random card matching this CardValue; tens, jacks,
 * queens and kings are indistinguishable.
 *
 * @return Returns the card drawn.
 * @exception ShuffleNeededException if there are no cards in the
 * shoe or no cards of the specified value in the shoe. To avoid this, you can
 * use probabilityOf first to check that the card exists, and shuffle the deck.
 *
 * Untested.
 * @args CardValue thisCard is the value of card to be pulled
 */
public Card drawSpecific(final CardValue thisCard) throws ShuffleNeededException {
   if (myCards.isEmpty()) {
      throw new ShuffleNeededException("Shoe.drawSpecific(CardValue) called "
              + "with no cards left in the shoe.");
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
      throw new ShuffleNeededException("Shoe.drawSpecific(CardValue) called "
              + "for a CardValue not present in the shoe.");
   }

   //pick random number between 0 and locations.size() - 1. Use it to select an 
   // index
   // in the locations array. Pick that element of the array, and draw that card.

   Card pulledCard = myCards.get((int) 
           locations.get(generator.nextInt(locations.size())));
   myCards.remove(pulledCard);
   cardValueCache[(thisCard.value())]--;
   totalCards--;
   return pulledCard;
}

/** TODO: Add test
 * @return A random card from this Shoe, which has been pulled from the Shoe.
 */
public Card drawRandom() throws ShuffleNeededException{
   if (totalCards == 0) {
      throw new ShuffleNeededException("No cards left in shoe.");
   }
   final int index = (int) (generator.nextDouble() * (double) myCards.size());
   //As long as Java always rounds down when typecasting, I'm good.
   Card pulledCard = myCards.get(index);
   myCards.remove(index);
   cardValueCache[(pulledCard.value())]--;

   totalCards--;
   return pulledCard;
}

/**
 * TODO: Add test
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
 * Draws the first card for the player if player is set to true, otherwise
 * draws the dealer card.
 * 
 * Auto-shuffles as needed.
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
 * TODO: Add tests for:
 * ALL_SOFT_AND_HARD(8);
 * SOFT_UNDER_16(5),
 * SOFT_OVER_16(6),
 * DEALER_2_6
 * DEALER_7_A
 */
public Card drawAppropriate(DrawMode myMode, boolean player, Rules theRules) 
        throws ShuffleNeededException {
   if (!player) {
      switch (myMode) {
         case DEALER_2_6:
            return drawBetween(Constants.TWOCARD + 1, Constants.SIXCARD + 1);
         case DEALER_7_A:
            double sum = 0;
            final double relativeProbOfAce;
            for (int i = Constants.SEVENCARD + 1; i <= Constants.TENCARD + 1; i++) {
               sum += cardValueCache[i];
            }
            if (sum != 0) {
               relativeProbOfAce = ((double)
                       cardValueCache[Constants.ACECARD + 1]) / sum;
            }
            else {
               relativeProbOfAce = 1;
            }
            if (generator.nextDouble() < relativeProbOfAce) {
               return drawBetween(Constants.ACECARD + 1, Constants.ACECARD + 1);
            }
            else {
               return drawBetween(Constants.SEVENCARD + 1, Constants.TENCARD + 1);
            }

         case FREE_PLAY:
            return drawRandom();

         default:
            throw new UnsupportedOperationException("This DrawMode not "
                    + "supported for the dealer: " + myMode.toString());
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
         return drawBetween(Constants.TWOCARD + 1, Constants.TENCARD + 1);
      case ALL_SOFT:
         if (cardValueCache[Constants.ACECARD + 1] == 0) {
            throw new ShuffleNeededException("No aces left -- "
                    + "can't draw a soft hand.");
         }
         return drawBetween(Constants.TWOCARD + 1, Constants.TENCARD + 1);
      case HARD_UNDER_12: // A card between 2 and 9.
         return drawBetween(Constants.TWOCARD + 1, Constants.NINECARD + 1);
      case SOFT_UNDER_16:
         return drawBetween(Constants.TWOCARD + 1, Constants.FIVECARD + 1);
      case SOFT_OVER_16:
         return drawBetween(Constants.SIXCARD + 1, Constants.TENCARD + 1);
      default:
         throw new UnsupportedOperationException(myMode.toString());
   }
}

/**
 * Draws a card between the low card value (Ace = 1) and the high card value
 * (10),
 * inclusive; does this by randomly picking cards until the right one is found.
 *
 * @return
 * @throws ShuffleNeededException, IllegalArgumentException
 *
 */
private Card drawBetween(final int lowCardValue, final int highCardValue) 
        throws ShuffleNeededException {
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
      System.out.println(this);
      throw new ShuffleNeededException
              ("There are no cards between value " + lowCardValue
              + "and " + highCardValue + " left in the shoe.");
   }


   if (lowCardValue == highCardValue) {
      return drawSpecific(CardValue.cardValueFromInt(lowCardValue));
   }

   boolean badPull = true;
   Card pulledCard = null;
   while (badPull) {
      pulledCard = drawRandom();
      if ((pulledCard.value() < lowCardValue) ||
              (pulledCard.value() > highCardValue)) {
         this.addCard(pulledCard);    //Add the card back to the deck
      }
      else {
         badPull = false;
      }
   }
   return pulledCard;
}

public void shuffle(int numberOfDecks) {
   Utilities.zero(cardValueCache);
   myCards.clear();
   totalCards = 0;
   initializeAll(numberOfDecks);
}

/**
 * 
 * If the GameMode is not set to free play, then shuffle after every hand.
 * This should only be called after drawing the first player card with 
 * Shoe.drawAppropriate; pass the drawn Card as an argument here.
 * 
 * @param cardPullCode
 * @param previouslyDrawnCard
 * @return
 * @throws ShuffleNeededException, IllegalArgumentException
 *
 * Untested -- ALL_SOFT_AND_HARD
 * Not implemented -- Soft under 16, soft over 16
 */
public Card drawSecondPlayerCard(DrawMode myMode, Card previouslyDrawnCard) 
        throws ShuffleNeededException {
   int maxValue, minValue;
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
            throw new IllegalArgumentException("Draw appropriate called with "
            + "bad card argument or pull code: an ace can't be drawn to a "
                    + "hard hand.");
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
            throw new IllegalArgumentException("drawAppropriate(int,Card) "
                    + "called with invalid card: " + previouslyDrawnCard);
         }
         return drawBetween(Constants.TWOCARD + 1, maxValue);
      case ALL_SOFT_AND_HARD:
         return drawAllExcept(previouslyDrawnCard.getCardValue());
      default:
         throw new UnsupportedOperationException(myMode.toString());
   }
}

/**
 * Draws first card in shoe of specified value. About seven times faster than
 * drawSpecific.
 */
@Override
public Card fastDrawSpecific(final CardValue thisCard) {
   if (myCards.isEmpty()) {
      throw new ShuffleNeededException("Shoe.fastDrawSpecific(CardValue) "
              + "called with no cards left in the shoe.");
   }
   //locate all cards that match that value, pick one of them, if one exists.
   int i;
   for (i = 0; i < myCards.size(); i++) {
      if (myCards.get(i).value() == thisCard.value()) {
         Card pulledCard = myCards.get(i);
         myCards.remove(pulledCard);
         cardValueCache[(thisCard.value())]--;
         totalCards--;
         return pulledCard;

      }
   }
   throw new ShuffleNeededException("Shoe.fastdrawSpecific(CardValue) called "
           + "for a CardValue not present in the shoe.");
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
 * UNTESTED.
 *
 * @return Contents of the Card Value Cache. Used by FastShoe to make a clone.
 */
public int[] getCardValueCache() {
   int[] copy = new int[(this.cardValueCache.length)];
   System.arraycopy(cardValueCache, 0, copy, 0, cardValueCache.length);
   return copy;
}

}