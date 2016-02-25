package ramblingharmonicas.blackjack;
import ramblingharmonicas.blackjack.cards.*;

/**
 * Contains all cards in a dealing shoe. Stores only the value of each card, not the suit.
 *
 * There's some duplicated code here, possibly some unneeded functions,
 * and a lack of clarity in function names. The draw functions need refactoring -- there
 * are too many of them, and some create excess objects or take objects as arguments (and should
 * not do so for performance reasons)
 * All good areas to work on.
 * Also, this class uses a different indexing system than the one used in the
 * dealer recursive function. This is unnecessarily confusing.
 */
class FastShoe implements VagueShoe {
/**
 *
 * The array index is the CARD VALUE. [0] is unused; [1] is Ace, and so on.
 * This style does not reflect the usage in other parts of the program.
 */
private int[] cardValueCache = new int[11];
private int totalCards;

public FastShoe(int numberOfDecks) throws IllegalArgumentException {
   if (numberOfDecks <= 0) {
      throw new IllegalArgumentException("A FastShoe must have a positive "
              + "number of decks in it.");
   }
   totalCards = 52 * numberOfDecks;
   int i;
   for (i = 1; i < cardValueCache.length - 1; i++) {
      cardValueCache[i] = 4 * numberOfDecks;
   }
   cardValueCache[(cardValueCache.length - 1)] = 16 * numberOfDecks;
   cardValueCache[0] = -50000;

}

public FastShoe deepClone() {
   //avoid exposing private data
   return new FastShoe(cardValueCache);
}

/**
 * Constructs FastShoe from a cardValueCache
 * @param cardArray Internal representation of cards
 */
private FastShoe(int[] cardArray) {
   System.arraycopy(cardArray, 0, this.cardValueCache, 0, cardArray.length);
   int sum = 0;
   for (int i = 1; i < cardArray.length; i++) {
      sum += cardArray[i]; //the initial element is a garbage value.
   }
   this.totalCards = sum;
   cardValueCache[0] = -50000;

}

FastShoe(Shoe normalShoe) {
   this.totalCards = normalShoe.numberOfCards();
   this.cardValueCache = normalShoe.getCardValueCache();
   //OK because it returns a deep clone of the cache.
}

@Override
public int numberOfCards() {
   return totalCards;
}

@Override
public String toString() {
   StringBuilder s = new StringBuilder();
   final String ln = System.getProperty("line.separator");
   s.append("This fast shoe contains a total of ")
           .append(this.numberOfCards()).append(" cards: ").append(ln);
   for (int i = 1; i < cardValueCache.length; i++) {
      s.append(cardValueCache[i]).append(" cards of value ").append(i).append(ln);

   }
   return s.toString();

}

/**
 * Uses caching to (hopefully) speed up this process.
 * Returns a negative number if this is impossible -- always test for negative
 * values when using this function.
 *
 * @param thisCard
 * @return A negative number if there are none of those cards left in the shoe.
 * @throws IllegalStateException if there are no cards left in the shoe
 */
@Override
public double probabilityOf(final CardValue thisCard) {
   if (thisCard == null) {
      throw new NullPointerException();
   }
   return probabilityOf(thisCard.value() - 1);

}

/**
 * Only used by the dealer recursive function.
 * cardIndex -- 0 is ace, 1 is two, etc. (Note that this is not the same
 * indexing system as the one internal to FastShoe)
 *
 * @param cardIndex
 * @return A negative number if there are none of those cards left in the shoe
 * or if the shoe is empty
 *
 */
double probabilityOf(final int cardIndex) {
   if (totalCards == 0) {
      return -100000000;
   }
   else if (cardValueCache[(cardIndex + 1)] == 0) {
      return -100000000;
   }
   else {
      return (double) ((cardValueCache[(cardIndex + 1)]) / ((double) totalCards));
   }

}

/**
 * The probability of drawing a card of value goodCardIndex, given that
 * it's impossible to draw a card of value excludedIndex.
 *
 * @param goodCardIndex
 * @param excludedIndex
 * @return
 */
double fastProbOfExcluding(final int goodCardIndex, final int excludedIndex) {
   if (totalCards == 0) {
      return -5000;
   }
   if (goodCardIndex == excludedIndex) {
      throw new IllegalArgumentException("FastShoe.fastProbOfExcluding(int,int) called"
              + " with cards of the same value: " + goodCardIndex);
   }
   if (cardValueCache[(goodCardIndex + 1)] == 0) {
      return -5000;
   }

   return (double) (cardValueCache[(goodCardIndex + 1)])
           / ((double) (totalCards - cardValueCache[(excludedIndex + 1)]));
}

/**
 *
 * @param thisCard The card value that is to be drawn from the shoe.
 * @param excluded The card value that cannot be drawn from the shoe.
 * @return The probability of drawing the specified CardValue from the Shoe,
 * given that all
 * cards with the excluded CardValue cannot be drawn. Returns a negative value
 * if the CardValue is not present in the shoe or if the Shoe is empty.
 * @throws IllegalArgumentException if the two passed CardValues are the same.
 */
double probabilityOfExcluding(final CardValue thisCard, final CardValue excluded) {
   if (totalCards == 0) {
      return -100000;
   }
   if (thisCard.value() == excluded.value()) {
      throw new IllegalArgumentException("Shoe.probabilityOfExcluding(CardValue, CardValue) called"
              + " with cards of the same value: " + thisCard);
   }
   if (cardValueCache[thisCard.value()] == 0) {
      return -100000;
   }

   return (double) (cardValueCache[thisCard.value()]) / ((double) (totalCards - cardValueCache[excluded.value()]));
}

/**
 * Draws a card. The returned suit is always clubs.
 *
 * @param thisCard
 * @return Card.
 * TODO: Rename to drawSpecific / deprecate
 */
@Override
public Card fastDrawSpecific(final CardValue thisCard) {
   if (totalCards == 0) {
      throw new IllegalStateException("FastShoe.fastDrawSpecific(CardValue) called with no "
              + "cards left in the shoe.");
   }

   if (cardValueCache[thisCard.value()] == 0) {
      throw new IllegalStateException("FastShoe.fastdrawSpecific(CardValue) called for a CardValue "
              + "not present in the shoe.");
   }
   (cardValueCache[thisCard.value()])--;
   totalCards--;
   return new Card(Suit.CLUBS, thisCard);

}

/**
 * Faster than fastdrawSpecific?
 *
 * @param thisCard
 */
public void fasterDrawSpecific(final CardValue thisCard) {
   if (totalCards == 0) {
      throw new IllegalStateException("FastShoe.fastDrawSpecific(CardValue) called with no cards left in the shoe.");
   }

   if (cardValueCache[thisCard.value()] == 0) {
      throw new IllegalStateException("FastShoe.fastdrawSpecific(CardValue) called for a CardValue not present"
              + " in the shoe.");
   }
   (cardValueCache[thisCard.value()])--;
   totalCards--;

}

/**
 * This cardIndex refers to the dealer card index, which is 1 lower than the
 * FastShoe card index.
 *
 * @param thisCard
 */
public void fasterDrawSpecific(final int cardIndex) {
   if (totalCards == 0) {
      throw new IllegalStateException("FastShoe.fastDrawSpecific(CardValue) called with no cards "
              + "left in the shoe.");
   }

   if (cardValueCache[(cardIndex + 1)] == 0) {
      throw new IllegalArgumentException("FastShoe.fastdrawSpecific(cardIndex) called for a "
              + "CardValue not present in the shoe: " + cardIndex + ". Shoe:" + this);
   }
   (cardValueCache[cardIndex + 1])--;
   totalCards--;
}

@Override
public void addCard(final Card thisCard) {
   cardValueCache[(thisCard.value())]++;
   totalCards++;
}

public void addCard(final CardValue myCardValue) {
   cardValueCache[myCardValue.value()]++;
   totalCards++;
}

/**
 * The indexing system is different in the dealer array (aces are in the 0 spot).
 *
 * @param cardIndex
 */
public void addCard(final int cardIndex) {
   cardValueCache[(cardIndex + 1)]++;
   totalCards++;
}

/**
 * This function is meant 
 * to help with the approximation value of a dealer hand when the dealer
 * hand limit had been reached. With the dealer probability cache, it is
 * not necessary to have a dealer max hand size.
 *
 * @param value
 * @return 0 if value is 0 or less. Otherwise, the chances of getting this value
 * or less, given the current shoe. Returns 1 if value is 11 or more.
 */
public double probGettingThisOrLess(final int value) {
   int cardsum = 0;
   if (value <= 0) {
      return 0;
   }
   if (value >= (cardValueCache.length - 1)) {
      return 1; 
   }
   for (int i = 1; i <= value; i++) {
      cardsum += cardValueCache[i];
   }
   return ((double) cardsum) / ((double) totalCards);
}

/**
 *
 * @return The probability of drawing these three cards from the shoe in the
 * given order.
 * Returns a negative number if that's impossible.
 */
public double probTheseThreeInOrder(CardValue firstCard, CardValue secondCard,
        CardValue dealerCard) {
   final double probFirstCard = probabilityOf(firstCard);
   if (probFirstCard < 0) {
      return -100000; 
   }
   if (totalCards < 3) {
      return -100000;
   }
   int[] duplicate = new int[cardValueCache.length];
   System.arraycopy(cardValueCache, 0, duplicate, 0, cardValueCache.length);

   duplicate[firstCard.value()]--;
   if (duplicate[secondCard.value()] == 0) {
      return -100000; 
   }
   final double probSecondCard = (double) duplicate[secondCard.value()] / (double) (totalCards - 1);
   duplicate[secondCard.value()]--;
   if (duplicate[dealerCard.value()] == 0) {
      return -100000; 
   }
   final double probDealerCard = (double) duplicate[dealerCard.value()] / (double) (totalCards - 2);

   return probFirstCard * probSecondCard * probDealerCard;

   //For player-card-order-irrelevant, ?multiply the above by 2?
   //There are 2 possible ways to draw the player cards.
}

/**
 * This function is not directly tested but is a wrapper for
 * fastProbabilityOf(int),
 * which is well tested.
 * Speed ideas -- first, inline this to make it faster, second, don't double
 * allocate the double [] if possible.
 *
 * @return
 *
 */
double[] getAllProbs() {
   int i;
   double[] probabilities = new double[10];
   for (i = 0; i < probabilities.length; i++) {
      probabilities[i] = probabilityOf(i);
   }

   return probabilities;
}

/**
 * This should be used in player recursive to get the correct draw probability;
 * it takes into account the deck composition when the dealer is known not
 * to possess blackjack, and otherwise gives the standard draw probability,
 *
 * @param dealerHole Whether or not the dealer has a hole card.
 * @param DCard What the dealer's up card is.
 * @param drawnCard The card you want to draw.
 * @return The probability of that card being drawn, given the fact that if
 * the dealer has a ten or ace up and a hole card, she has NOT drawn,
 * respectively,
 * an ace or a ten, since in that case he'd have already been blackjack and game
 * over.
 */
double playerProbability(boolean dealerHole, Card DCard, CardValue drawnCard) {
   if (!dealerHole) {
      return probabilityOf(drawnCard);
   }
   final int currentDealerUpCard = DCard.value() - 1;
   if ((currentDealerUpCard != Blackjack.TENCARD)
           && (currentDealerUpCard != Blackjack.ACECARD)) {
      return probabilityOf(drawnCard);
   }

   double probability = 0;
   CardValue undrawable;
   if (DCard.getCardValue().value() == CardValue.TEN.value()) {
      undrawable = CardValue.ACE;
   }
   else if (DCard.getCardValue() == CardValue.ACE) {
      undrawable = CardValue.TEN;
   }
   else {
      throw new NullPointerException("Error in FastShoe.playerProbability"); 
   }
   if (cardValueCache[drawnCard.value()] == 0) //Not in shoe.
   {
      return -1000000;
   }
   if (totalCards == 1) //It's the only card in the shoe. You should never get here.
   {
      assert false;
      System.err.println("Error in FastShoe.playerProbability -- only 1 card left in shoe");
      return 1;
   }

   double probOfDrawingThisCard;
   //Going through the different cards the dealer might have drawn.
   for (CardValue drawnDealerCard : CardValue.oneToTen) {
      if (drawnDealerCard == undrawable) ; //The dealer can't draw this card.
      else {
         probOfDrawingThisCard =
                 fastProbOfExcluding(drawnDealerCard.value() - 1, undrawable.value() - 1);
         if (probOfDrawingThisCard > 0) {
            if (drawnDealerCard == drawnCard) {
               probability += probOfDrawingThisCard * (cardValueCache[drawnCard.value()] - 1);
            }
            else {
               probability += probOfDrawingThisCard * (cardValueCache[drawnCard.value()]);
            }
         }
      }
   }

   probability = probability / (totalCards - 1);

   assert ((probability < probabilityOf(drawnCard) * 1.10)
           && (probability > probabilityOf(drawnCard) * 0.90));
   //This stupid pain in the $&% correction should not be too far away from the original probability.
   // Probably not more than 5 % -- tested on one deck dealer hit soft 17
   //But just in case we'll make it to 10 %.

   return probability;
}

/**
 * If a dealer has a hole card, and is drawing his first card, this
 * function will calculate the probability of him drawing each card.
 *
 * @param dealerUpCardIndex Dealer card index (Ace = 0)
 */
double[] getDealerHCP(int dealerUpCardIndex) {
   double[] probabilities = new double[10];
   int i;
   if (dealerUpCardIndex == Blackjack.ACECARD) {
      for (i = 0; i < probabilities.length - 1; i++) {
         probabilities[i] = fastProbOfExcluding(i, Blackjack.TENCARD);
      }
      probabilities[Blackjack.TENCARD] = -5000;
   }
   else if (dealerUpCardIndex == Blackjack.TENCARD) {
      for (i = 1; i < probabilities.length; i++) {
         probabilities[i] = fastProbOfExcluding(i, Blackjack.ACECARD);
      }

      probabilities[Blackjack.ACECARD] = -5000;
   }
   else {
      probabilities = getAllProbs();
   }

//ERROR CHECKING
   if (Blackjack.debug()) {
      double sum = 0;
      for (i = 0; i < probabilities.length; i++) {
         if (probabilities[i] > 0) {
            sum += probabilities[i];
         }
         if ((i == Blackjack.ACECARD) && (dealerUpCardIndex == Blackjack.TENCARD)) {
            assert (probabilities[i] < 0) : "Ace marked as being possible when it shouldn't be.";
         }
         if ((i == Blackjack.TENCARD) && (dealerUpCardIndex == Blackjack.ACECARD)) {
            assert (probabilities[i] < 0) : "Ten marked as being possible when it shouldn't be.";
         }
         if ((cardValueCache[i + 1] <= 0) && (probabilities[i] > 0)) {
            assert false : "Card marked as being possible to draw when it's not: cardindex " + i;
         }
      }
      assert (sum < (1 + Constants.EPSILON)) && (sum > 1 - Constants.EPSILON);
   }


   return probabilities;
}

/**
 * UNTESTED
 * Helper function for Dealer's recursive
 *
 * @param cardsInHand
 * @param dealerHoleCard
 * @param myCards
 * @return
 */
double[] getDealerProbabilities(int cardsInHand, boolean dealerHoleCard,
        int[] myCards) {
   if ((cardsInHand == 1) && (dealerHoleCard == true)
           && ((myCards[Blackjack.ACECARD] == 1) || (myCards[Blackjack.TENCARD] == 1))) {
      Blackjack.holeCardCheck++;
      return getDealerHCP(Utilities.retrieveSingleCard(myCards));
   }
   else {
      return getAllProbs();
   }
}

/**
 * UNTESTED AND SHOULD BE TESTED
 *
 * Doesn't work with more than 62 decks.
 *
 * @return
 */
String myStringKey() {
   StringBuilder s = new StringBuilder();
   for (int i = 1; i < cardValueCache.length; i++) {
      if (cardValueCache[i] == 0) {
         s.append("000");
      }
      else if (cardValueCache[i] < 10) {
         s.append("00");
         s.append(cardValueCache[i]);
      }
      else if (cardValueCache[i] < 100) {
         s.append("0");
         s.append(cardValueCache[i]);
      }
      else {
         s.append(cardValueCache[i]);
      }

   }

   return s.toString();

}

}
