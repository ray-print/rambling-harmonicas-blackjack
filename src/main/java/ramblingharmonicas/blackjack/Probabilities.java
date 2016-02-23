package ramblingharmonicas.blackjack;

import java.util.*;
import ramblingharmonicas.blackjack.cards.*;

/**
 * This class may be of theoretical interest but is of no practical use.
 * 
 */
public class Probabilities {
/**
 *
 * @param dealerCard Dealer up card
 * @param numberOfCards Number of cards in dealer hand.
 * @return The likelihood that the dealer will draw numberOfCards cards
 * without going bust or having hard 17 / soft 18 or better, given the dealer up
 * card.
 */
static double largeHandProbability(CardValue dealerCard, int numberOfCards,
        final FastShoe myDeck, final Rules theRules) {
   int cardsInHand = 1;
   ArrayList<Card> myCards = new ArrayList<Card>();
   final double probabilityInitialCard = myDeck.probabilityOf(dealerCard);
   myDeck.fastDrawSpecific(dealerCard);
   myCards.add(new Card(Suit.CLUBS, dealerCard));
   return DealerProbabilityRecursive(myCards, myDeck, theRules, probabilityInitialCard, numberOfCards);
}

private static double DealerProbabilityRecursive(final ArrayList<Card> myCards,
        final FastShoe myDeck, final Rules theRules, double probability,
        int numberOfCards) {
   int i, j, k;
   double totalDealerProbability = 0;
   if (Utilities.isBust(myCards)) {
      return 0;
   }

   int handValue = Utilities.handTotal(myCards);

   if (handValue >= 18) {
      return 0;
   }
   if (handValue == 17) {
      if ((theRules.hitOn17() == false) || (!Utilities.isSoft(myCards))) {
         return 0;
      }
   }
   // Otherwise, hit on Soft 17.


   if (myCards.size() >= numberOfCards) {
      return probability;

   }
//PROBABILITIES SECTION START
   double[] Probabilities = new double[10];
   boolean tenDraw = true, aceDraw = true;
   CardValue[] oneToTen = new CardValue[10];
   i = 0;
   for (CardValue q : CardValue.values()) {
      oneToTen[i++] = q;
      if (i == 10) {
         break;
      }
   }

   if ((myCards.size() == 1) && (theRules.dealerHoleCard() == true)) {
      if (myCards.get(0).getCardValue() == CardValue.ACE) {
         i = 0;
         Probabilities[9] = 0;
         tenDraw = false;
         for (CardValue val : oneToTen) {
            if (val.value() != CardValue.TEN.value()) {
               Probabilities[i++] = myDeck.probabilityOfExcluding(val, CardValue.TEN);
            }
         }
      }
      else if (myCards.get(0).value() == CardValue.TEN.value())
      {
         i = 0;
         Probabilities[0] = 0;
         aceDraw = false;

         for (CardValue val : oneToTen) {
            if (val != CardValue.ACE) {
               Probabilities[i] = myDeck.probabilityOfExcluding(val, CardValue.ACE);
            }
            i++;
         }
      }
      else {
         i = 0;
         for (CardValue val : oneToTen) {
            Probabilities[i++] = myDeck.probabilityOf(val);
         }
      }
   }
   else {  
      i = 0;
      for (CardValue val : oneToTen) {
         Probabilities[i++] = myDeck.probabilityOf(val);
      }
   }

   Card drawnCard;
   i = 0;

   for (CardValue val : oneToTen) {
      if ((val == CardValue.ACE) && (aceDraw == false)) {
         i++;
         continue;
      }
      if ((val.value() == 10) && (tenDraw == false)) {
         i++;
         continue;
      }
      if (Probabilities[i] > 0) {


         drawnCard = myDeck.fastDrawSpecific(val);

         myCards.add(drawnCard);
         totalDealerProbability += DealerProbabilityRecursive
                 (myCards, myDeck, theRules, Probabilities[i] * probability, numberOfCards);

         myCards.remove(drawnCard);
         myDeck.addCard(drawnCard);


      }
      i++;
   }

   return totalDealerProbability; // Done
}

/* Prints out the chances that the dealer will have a hands of a given length, given a certain
 * upcard.
 */
static double DealerRecursiveSandBox(final ArrayList<Card> myCards,
        final FastShoe myDeck, final Rules theRules, double probHere) {
   Blackjack.dealerIterations++;
   double totalProbability = 0;
   long oneStart = System.currentTimeMillis();
   double[] endProbabilities = new double[7];
   int i;
   for (i = 0; i < endProbabilities.length; i++) {
      endProbabilities[i] = 0;
   }
   final int handValue = Utilities.handTotal(myCards);
   if (handValue > 21) {

      return 0;
   }
   if (handValue >= 17) {
      switch (handValue) {
         case 21:
            if (myCards.size() == 2) {
               endProbabilities[1] = 1;

               return 0;
            }
            else {
               endProbabilities[6] = 1;

               return 0;
            }
         case 20:
            endProbabilities[5] = 1;

            return 0;
         case 19:
            endProbabilities[4] = 1;

            return 0;
         case 18:
            endProbabilities[3] = 1;

            return 0;
         case 17:
            if ((theRules.hitOn17() == false) || (!Utilities.isSoft(myCards))) {
               endProbabilities[2] = 1;

               return 0;
            }
            else ;
            break; // Soft 17.
         }
   }
   if (myCards.size() >= 5) {
      System.out.println("There is " + probHere + "chance of having a: ");
      for (Card k : myCards) {
         System.out.print(k.value() + "  ");
      }
      System.out.println();
      return probHere;
   }

   long twoStart = System.currentTimeMillis();

   double[] Probabilities = new double[10];
   boolean tenDraw = true;
   boolean aceDraw = true;
   CardValue[] oneToTen = new CardValue[10];
   i = 0;
   for (CardValue q : CardValue.values()) {
      oneToTen[i++] = q;
      if (i == 10) {
         break;
      }
   }
   if ((myCards.size() == 1) && (theRules.dealerHoleCard() == true)) {
      if (myCards.get(0).getCardValue() == CardValue.ACE) {
         i = 0;
         Probabilities[9] = 0;
         tenDraw = false;
         for (CardValue val : oneToTen) {
            if (val.value() != CardValue.TEN.value()) {
               Probabilities[i] = myDeck.probabilityOfExcluding(val, CardValue.TEN);
            }
            i++;
         }
      }
      else if (myCards.get(0).value() == CardValue.TEN.value()) {
         i = 0;
         Probabilities[0] = 0;
         aceDraw = false;
         for (CardValue val : oneToTen) {
            if (val != CardValue.ACE) {
               Probabilities[i] = myDeck.probabilityOfExcluding(val, CardValue.ACE);
            }
            i++;
         }
      }
      else {
         i = 0;
         for (CardValue val : oneToTen) {
            Probabilities[i++] = myDeck.probabilityOf(val);
         }
      }
   }
   else {
      i = 0;
      for (CardValue val : oneToTen) {
         Probabilities[i++] = myDeck.probabilityOf(val);
      }
   }

   Card drawnCard;
   double[] scratch;

   i = 0;
   long threeStart;
   for (CardValue val : oneToTen) {
      if ((val == CardValue.ACE) && (aceDraw == false)) {
         i++;
         continue;
      }
      if ((val.value() == 10) && (tenDraw == false)) {
         i++;
         continue;
      }
      if (Probabilities[i] > 0) {
         threeStart = System.currentTimeMillis();
         drawnCard = myDeck.fastDrawSpecific(val);
         myCards.add(drawnCard);

         totalProbability += DealerRecursiveSandBox(myCards, myDeck, theRules, probHere * Probabilities[i]);
         myCards.remove(drawnCard);
         myDeck.addCard(drawnCard);
      }
      i++;
   }
   return totalProbability;
}

/**
 * This is a diagnostic function used to indicate what value the dealer
 * max hand size should be. Since I'm using a probability cache, this is not
 * very useful.
 *
 */
static void dealerProbabilities(int numberOfDecks) {
   int i;
   int j;
   int k;
   Rules myRules = new Rules();
   FastShoe myShoe = new FastShoe(numberOfDecks);
   double probabilitySum = 0;

   double probability;
   for (i = 2; i < 12; i++) {
      System.out.println("With " + numberOfDecks + " decks, the odds of the dealer having ");
      System.out.println(i + " cards or more without standing or going bust is: ");
      for (CardValue c : CardValue.values()) {
         if (CardValue.JACK == c) {
            break; // This line is very important. Because 10s are interchangable.
         }
         probability = Probabilities.largeHandProbability(c, i, myShoe.deepClone(), myRules);
         System.out.println(probability + " for a " + c.toString());
         probabilitySum += probability;
      }
      System.out.print(probabilitySum + " for all cards combined.");
      probabilitySum = 0;
      System.out.println();
   }
}

/**
 * Mothballed.
 */
static void testDealerRecursiveSandBox(int numberOfDecks) {
   ArrayList<Card> myCards = new ArrayList<Card>();
   FastShoe myShoe = new FastShoe(numberOfDecks);
   Card pulledCard = myShoe.fastDrawSpecific(CardValue.FIVE);
   myCards.add(pulledCard);
   Rules theRules = new Rules();
   System.out.println("The total of all those is: " +
           Probabilities.DealerRecursiveSandBox(myCards, myShoe, theRules, 1));
}

}
