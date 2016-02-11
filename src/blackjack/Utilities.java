package blackjack;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

public class Utilities {
public static String stackTraceToString(Throwable t) {
   StringWriter sw = new StringWriter();
   PrintWriter pw = new PrintWriter(sw);
   t.printStackTrace(pw);
   return sw.toString();
}

/**
 * Tested thrice; worked fine.
 * Not using this function, because it duplicates handTotal and I need to save
 * time.
 *
 * There's a function in State that does the same as this function. This one or
 * the
 * State one should be destroyed because code bloat is bad.
 *
 * @param myCards
 * @return
 */
public static boolean isBust(final ArrayList<Card> myCards) {
   int sum = 0;
   for (int j = 0; j < myCards.size(); j++) {
      sum += myCards.get(j).value();
   }
   if (sum > 21) {
      return true;
   }
   else {
      return false;
   }


}

/**
 * True if the hand is soft. Internal function used by fastDealerRecursive
 */
static boolean isSoft(final int[] myCards, final int handTotal) {
   if (myCards[0] == 0) {
      return false;
   }
   if (lowHandTotal(myCards) == handTotal) {
      return false;
   }
   else {
      return true;
   }

}

private static int lowHandTotal(int[] myCards) {
   int sum = 0;
   for (int i = 0; i < myCards.length; i++) {
      sum += myCards[i] * (i + 1);
   }

   return sum;
}

/*  Tested before altering. Seems to work fine.
 * Overloaded for theoretical performance improvement
 *
 *
 */
static int handTotal(final int[] myCards /*Boolean isSoft*/) //trick doesn't work
{
   int sum = myCards[0] * 11;

   if (myCards.length != 10) {
      throw new RuntimeException("Dealer.handValue error: Array of wrong length.");
   }
   for (int i = 1; i < myCards.length; i++) {
      sum += myCards[i] * (i + 1);

   }
   if (sum <= 21) {
      //if (myCards[0] > 0) isSoft = true; else isSoft = false;
      return sum;
   }
   else {
      int numOfAces = myCards[0];
      while ((sum > 21) && (numOfAces > 0)) {
         sum -= 10;
         numOfAces--;
      }
      //if (numOfAces > 0) isSoft = true; else isSoft =false; 
      // if (sum > 21) System.out.println ("I'm bust! don't call Dealer.handTotal in that case?");
      return sum;
   }

}

/**
 * Tested, seems to work fine.
 *
 *
 *
 */
static int handSize(final int[] myCards) {
   int handSize = 0;
   for (int i = 0; i < myCards.length; i++) {
      handSize += myCards[i];
   }
   return handSize;


}

/**
 * Tested.
 *
 * @param myCards An array of cards.
 * @param thisValue A given card value.
 * @return True if myCards contains a card of thisValue; false otherwise. Tens
 * are all the same.
 */
public static boolean contains(final List<Card> myCards, CardValue thisValue) {
   for (int i = 0; i < myCards.size(); i++) {
      if (myCards.get(i).getCardValue().value() == thisValue.value()) {
         return true;
      }
   }
   return false;
}

/**
 * Tested multiple times, seems to work fine.
 *
 * Gives most advantageous hand total.
 *
 * @param myCards
 * @return Most favorable total of the cards.
 */
public static int handTotal(final List<Card> myCards) {
   int sum = 0;
   int numOfAces = 0;
   for (int i = 0; i < myCards.size(); i++) {
      sum += myCards.get(i).value();
      if (myCards.get(i).value() == 1) {
         numOfAces++;
         sum += 10;
      }
   }
   if (sum <= 21) {
      return sum;
   }
   else {
      while ((sum > 21) && (numOfAces > 0)) {
         sum -= 10;
         numOfAces--;
      }
      // if (sum > 21) System.out.println ("I'm bust! don't call Dealer.handTotal in that case?");
      return sum;
   }

}

/* Tested, works fine.
 */
public static boolean isSoft(List<Card> myCards) {
   int sum = 0;
   for (int j = 0; j < myCards.size(); j++) {
      sum += myCards.get(j).value(); // minimum possible hand value
   }
   if (sum != handTotal(myCards)) {
      return true;
   }
   else {
      return false;
   }



}

/**
 * Untested.
 * Takes an ArrayList of cards and turns it into a form that can be used by
 * Dealer's Recursive.
 *
 * @param myCards
 * @param array
 * @return
 *
 */
static int[] convertCardArraytoArray(ArrayList<Card> myCards, int[] array) {
   if (array == null) {
      array = new int[10];
   }
   else {
      zero(array);
   }
   assert (array.length == 10) : "The starting array must be 10 elements long in order to be used.";
   for (int i = 0; i < array.length; i++) {
      array[i] = 0;
   }
   for (int j = 0; j < myCards.size(); j++) {
      array[ (myCards.get(j).value() - 1)]++;
   }
   return array;
}

/**
 * ACE = 0, TWO = 1, etc.
 *
 *
 * @param myCards
 * @return
 */
static int retrieveSingleCard(int[] myCards) {
   if (handSize(myCards) != 1) {
      throw new IllegalArgumentException("Function retrieve Single card"
              + "called with multiple cards in hand.");
   }
   else {
      for (int i = 0; i < myCards.length; i++) {
         if (myCards[i] == 1) {
            return i;
         }
      }
      throw new IllegalStateException();
      //I should never get to the above line.
   }

}

static float[] doublesToFloat(double[] array) {
   float[] inFloatForm = new float[array.length];
   for (int i = 0; i < array.length; i++) {
      inFloatForm[i] = (float) array[i];
   }
   return inFloatForm;
}

/**
 * UNTESTED
 *
 * @param someStates
 * @return
 */
static double averageBestEV(ArrayList<State> someStates, double[] probThisState,
        double sumOfProbs) {
   double total = 0;
   for (int i = 0; i < someStates.size(); i++) {
      total += someStates.get(i).getExpectedValue() * probThisState[i] / sumOfProbs;
   }
   return total;
}

/**
 * Does NOT alter hardAnswers; requests a deep clone
 * from findStartHand.
 * Should be moved to Strategy class?
 *
 * @param splitEV The EV of splitting
 * @param PCard Player's card
 * @param DCard Dealer's card
 * @param hardAnswers Solved states for hard player hands
 * @param softAnswers Solved states for soft player hands
 * @return The Answer for this splitting hand -- what is the best
 * thing to do?
 */
static Answer splitEVtoAnswer(final double splitEV, CardValue PCard,
        CardValue DCard, ArrayList<ArrayList<State>> hardAnswers,
        ArrayList<ArrayList<State>> softAnswers) {
   State baseState;
   final boolean deepClone = true;
   if (PCard == CardValue.ACE) {
      baseState = findStartHand(softAnswers, PCard, PCard, DCard, deepClone);
   }
   else {
      baseState = findStartHand(hardAnswers, PCard, PCard, DCard, deepClone);
   }
   final double originalBestEV = baseState.getExpectedValue();
   final double originalSecondBestEV = baseState.getSecondBestEV();
   if (splitEV > originalBestEV) {
      baseState.setSecondBestAction(baseState.getPreferredAction());
      baseState.setSecondBestEV(originalBestEV);
      baseState.setPreferredAction(Action.SPLIT);
      baseState.overWriteEV(splitEV);
   }
   else if (splitEV > originalSecondBestEV) {
      baseState.setSecondBestAction(Action.SPLIT);
      baseState.setSecondBestEV(splitEV);
   }
   else ; //Splitting is worse than the alternatives.
   return new Answer(baseState);
}

static double combinedStatesEV(State[] possibleStates, double[] probabilities) throws NoRecommendationException {
   if (possibleStates == null) {
      throw new NoRecommendationException("IllegalArgumentException");
   }
   if (Blackjack.debug()) {
      double sum = 0;
      for (int i = 0; i < probabilities.length; i++) {
         if (probabilities[i] > 0) {
            sum += probabilities[i];
         }
      }
      assert ((sum > 0.9999) && (sum < 1.0001)) : "Sum is " + sum;
   }
   double answer = 0;
   for (int i = 0; i < possibleStates.length; i++) {
      if (probabilities[i] > 0) {
         if (possibleStates[i].getExpectedValue() < -10) {
            throw new NoRecommendationException("IllegalStateException:Error discovered in combinedStatesEV");
         }
         answer += probabilities[i] * possibleStates[i].getExpectedValue();
      }
   }
   assert (answer < 20);
   return answer;
}

/**
 * If a probability is negative, it does not figure in the
 * corresponding State. This is crucial -- negative values indicate
 * an impossible state.
 */
static double combinedStatesEV(ArrayList<State> possibleStates,
        double[] probabilities) {
   if (possibleStates.isEmpty()) {
      throw new IllegalArgumentException();
   }
   //ERROR CHECKING
   double sum = 0;
   for (int i = 0; i < probabilities.length; i++) {
      if (probabilities[i] > 0) {
         sum += probabilities[i];
      }
   }
   assert ((sum > 0.9999) && (sum < 1.0001)) : "Sum is " + sum;
   double answer = 0;
   for (int i = 0; i < possibleStates.size(); i++) {
      if (probabilities[i] > 0) {
         if (possibleStates.get(i).getExpectedValue() < -10) {
            throw new IllegalStateException("Error discovered in combinedStatesEV");
         }
         answer += probabilities[i] * possibleStates.get(i).getExpectedValue();
      }
   }
   assert (answer < 20);
   return answer;
}

/**
 *
 * @param array of ints
 * @return An integer which is equal to the largest element in the array
 * @throws NoSuchElementException if you give it an empty or null array
 */
public static int arrayMaximum(int[] array) {
   if ((array == null) || (array.length == 0)) {
      throw new NoSuchElementException();
   }
   int q = array[0];
   for (int i = 1; i < array.length; i++) {
      if (array[i] > q) {
         q = array[i];
      }
   }
   return q;
}

static double[] floatsToDouble(float[] array) {
   double[] inDoubleForm = new double[array.length];
   for (int i = 0; i < array.length; i++) {
      inDoubleForm[i] = (double) array[i];
   }
   return inDoubleForm;
}

static double averageSecondBestEV(ArrayList<State> someStates,
        double[] probThisState, double sumOfProbs) {
   double total = 0;
   for (int i = 0; i < someStates.size(); i++) {
      total += someStates.get(i).getSecondBestEV() * probThisState[i] / sumOfProbs;
   }
   return total;
}

/**
 * Given a 2-D rectangular array of solved states, where each row
 * corresponds to an identical player hand, and each column an
 * identical dealer hand, returns the State with
 * the passed player firstCard and secondCard, and dealerCard (or a deep clone
 * of the
 * state if deepClone is set to true).
 *
 *
 * If it can't find the desired state it throws an IllegalArgumentException.
 *
 * @param solvedStates A two-dimensional ArrayList of solved states
 * @param firstCard Player's first card
 * @param secondCard Player's second card
 * @param dealerCard Dealer's card
 * @param deepClone If true, returns a deep clone of the desired State.
 * Otherwise, returns the State itself.
 * @return A deep clone of the desired State, cloned from the State
 * in solvedState to which it corresponds.
 *
 *
 */
static State findStartHand(ArrayList<ArrayList<State>> solvedStates,
        CardValue firstCard, CardValue secondCard, CardValue dealerCard,
        boolean deepClone) {
   //find first index. Assumed that each row has the same
   // State correctState;
   boolean wrongArrayPassed = true;
   int i;
   int j;
   for (i = 0; i < solvedStates.size(); i++) {
      if (((solvedStates.get(i).get(0).getFirstCardValue() == firstCard) && (solvedStates.get(i).get(0).getSecondCardValue() == secondCard)) || ((solvedStates.get(i).get(0).getFirstCardValue() == secondCard) && (solvedStates.get(i).get(0).getSecondCardValue() == firstCard))) {
         wrongArrayPassed = false;
         break;
      }
   }
   if (wrongArrayPassed) {
      throw new IllegalArgumentException();
   }
   wrongArrayPassed = true;
   for (j = 0; j < solvedStates.get(0).size(); j++) {
      if (solvedStates.get(i).get(j).getDealerUpCard().getCardValue() == dealerCard) {
         wrongArrayPassed = false;
         break;
      }
   }
   if (wrongArrayPassed) {
      throw new IllegalArgumentException();
   }
   if (deepClone) {
      return new State(solvedStates.get(i).get(j));
   }
   else {
      return solvedStates.get(i).get(j);
   }
}

/**
 * Sets every value of the passed array to 0.
 *
 * @param zero
 */
public static void zero(double[] zero) {
   for (int k = 0; k < zero.length; k++) {
      zero[k] = 0;
   }
}

/**
 * Sets every value of the passed array to 0.
 *
 * @param zero
 */
public static void zero(int[] zero) {
   for (int k = 0; k < zero.length; k++) {
      zero[k] = 0;
   }
}

/**
 * Draw all dealer cards randomly based on the current rule set. If the Rules
 * state that
 * the dealer has a hole card, that hole card must be added back in to this
 * array before calling
 * this function, otherwise, an exception should be thrown. The reason is that
 * the hole card
 * must have been pulled from the deck before the player drew his hand --
 * otherwise, the
 * deck probabilities are not accurate.
 *
 * @param theRules
 * @param dealerCards
 * @return
 * @throws IllegalStateException if the dealer has only one card
 */
public static ArrayList<Card> getDealerHand(Rules theRules,
        ArrayList<Card> dealerCards, Shoe myShoe) {
   if ((dealerCards.size() == 1) && (theRules.dealerHoleCard())) {
      if (Blackjack.debug()) {
         throw new IllegalStateException("Utilities.getDealerHand should only be called with one card in hand when"
                 + "the dealer receives no hole card. If the dealer has a hole card, it should be added back in to"
                 + "the deck before calling this function.");
      }
   }
   do {

      if (Utilities.handTotal(dealerCards) > 17) {
         return dealerCards; //Always stand on 18 or above. Or if I'm bust, then I'm also done.
      }
      if (Utilities.handTotal(dealerCards) == 17) //If you have 17
      {
         if (Utilities.isSoft(dealerCards)) // and it's soft
         {
            if (theRules.hitOn17()) ; //and you hit on soft 17s, then keep going
            else {
               return dealerCards; //and you stand on soft 17s, then return
            }
         }
         else {
            return dealerCards; //17 and your hand is hard ->return
         }
      }
      //I should hit now.
      dealerCards.add(myShoe.drawRandom());

   }
   while (true);

}

/**
 * Wrapper/convenience function for getDealerHand(Rules, ArrayList dealerCards)
 * This should only be used with no-hole games, because for hole card games you
 * should already
 * have two cards in hand (one is the hidden hole card) before finishing the
 * hand.
 *
 * @param theRules
 * @param dealerUpCard
 * @return Finished dealer hand.
 */
public static ArrayList<Card> getDealerHand(Rules theRules, Card dealerUpCard,
        Shoe myShoe) {
   ArrayList<Card> startingHand = new ArrayList<Card>();
   startingHand.add(dealerUpCard);
   return getDealerHand(theRules, startingHand, myShoe);

}

/**
 * Moves best action and EV to second place
 * and moves second best to first place.
 * UNTESTED
 *
 * @param aState
 *
 */
static public void swapTopChoices(State aState) {
   final Action futureBest = aState.getSecondBestAction();
   final double futureBestEV = aState.getSecondBestEV();
   aState.setSecondBestEV(aState.getExpectedValue());
   aState.setSecondBestAction(aState.getPreferredAction());
   aState.overWriteEV(futureBestEV);
   aState.setPreferredAction(futureBest);
}

/**
 * This helper method actually converts each 4 bytes into an integer.
 * There is also another way of doing it through bit manipulation.
 *
 * @param b The byte array to convert to int
 * @return The array of ints
 * http://nn-tech-blog.blogspot.com/2009/03/using-java-io-api-to-read-and-write.html
 */
public static int[] convertBytesToInteger(byte[] b) throws IOException {
   ByteArrayInputStream bai = new ByteArrayInputStream(b);
   DataInputStream dis = new DataInputStream(bai);
   int len = b.length;
   final int numOfBytesInInt = 4;
   int[] ints = new int[len / numOfBytesInInt]; //4 bytes = 1 int
   for (int i = 0, j = 0; i < len; i += numOfBytesInInt, j++) {
      ints[j] = dis.readInt();
   }
   return ints;
}

/**
 * Convenience function for exception handling while
 * attempting to close a Stream. Upon failure, I print the stack
 * trace and return false.
 *
 * @param Closeable stream
 * @return True if the stream was null or was successfully closed.
 * False otherwise.
 */
public static boolean attemptClosure(Closeable stream) {
   if (stream == null) {
      return true;
   }
   try {
      stream.close();
      return true;
   }
   catch (IOException ioe) {
      System.err.println("Stream could not close in Utilites.attemptClosure:");
      ioe.printStackTrace();
      return false;
   }
}

/**
 * Not tested, but barely used.
 * I've probably written this code in eight other places -- it'd be
 * nice to consolidate and have it all redirect here. But not
 * necessary.
 *
 * @param someCards
 * @return
 */
static boolean hasBlackjack(ArrayList<Card> someCards) {
   if (Utilities.handTotal(someCards) != 21) {
      return false;
   }
   if (someCards.size() != 2) {
      return false;
   }
   return true;

}

}
