package ramblingharmonicas.blackjack.calculation;

import java.io.IOException;

import ramblingharmonicas.blackjack.Action;
import ramblingharmonicas.blackjack.NoRecommendationException;
import ramblingharmonicas.blackjack.Rules;
import ramblingharmonicas.blackjack.State;
import ramblingharmonicas.blackjack.Strategy;
import ramblingharmonicas.blackjack.Strategy.Skill;
import ramblingharmonicas.blackjack.Testers;
import ramblingharmonicas.blackjack.Testers.StrategyTest;
import ramblingharmonicas.blackjack.cards.Card;
import ramblingharmonicas.blackjack.cards.CardValue;
import ramblingharmonicas.blackjack.cards.Suit;

public class Validation {

	/**
	 * Tests that the given strategy obeys some common blackjack advice.
	 * Essentially a wrapper function for testOneSolvedStrategy and
	 * testTotalConsolidatedForConsolidation
	 *
	 * @param aStrategy
	 */
	public static void validateSolvedStrategy(Strategy aStrategy) throws NoRecommendationException, IOException {
	   //Card DCard;
	   //Card firstPlayerCard;
	   //Card secondPlayerCard;
	   for (CardValue dealerCard : CardValue.oneToTen) {
	      for (CardValue firstPlayerCard : CardValue.oneToTen) {
	         for (CardValue secondPlayerCard : CardValue.oneToTen) {
	            Validation.testOneSolvedStrategy(dealerCard, firstPlayerCard, secondPlayerCard, aStrategy);
	         }
	      }
	   }
	   final boolean verbosity = false;
	
	   if (aStrategy.getStrategyType() == Strategy.Skill.TOTAL_DEP) {
	      Testers.StrategyTest.testTotalConsolidatedForConsolidation(verbosity, aStrategy);
	   }
	
	}

	/**
	 * The driving force behind validateSolvedStrategy.
	 * Tests the given strategy to make sure that it obeys common sense blackjack
	 * rules:
	 * Split Aces against dealer 9 and under
	 * Double on 11 against dealer 9 and under
	 * Don't stand if your hand Total is 11 or under, unless you can early surrender
	 * Stand on hard 20; always stand on blackjacks.
	 * Don't surrender on dealer 2-7 up.
	 * Never take insurance.
	 * If you have hard 14 and over with a dealer 2-6 up, you should always stand or
	 * split.
	 *
	 * @param dealerCard
	 * @param firstPlayerCard
	 * @param secondPlayerCard
	 * @param aStrategy
	 *
	 */
	private static void testOneSolvedStrategy(CardValue dealerCard,
	        CardValue firstPlayerCard, CardValue secondPlayerCard,
	        Strategy aStrategy) throws NoRecommendationException {
	   Rules theRules = aStrategy.getMyRules();
	   try {
	      State myState = new State(new Card(Suit.CLUBS, firstPlayerCard), new Card(Suit.CLUBS, secondPlayerCard),
	              new Card(Suit.SPADES, dealerCard));
	      Action chosenAction;
	      final int handTotal;
	      boolean isSoft;
	      //Answer theAnswer = aStrategy.findBestAnswer(new Shoe(theRules.getNumberOfDecks()), theRules, myState);
	      chosenAction = aStrategy.findBestAction(myState);
	
	      //Always split aces, if it's possible, when the dealer doesn't have a 10 or ace up.
	      if ((theRules.isPossible(Action.SPLIT, myState))
	              && (firstPlayerCard == secondPlayerCard) && (firstPlayerCard == CardValue.ACE)) {
	         if ((dealerCard != CardValue.TEN) && (dealerCard != CardValue.ACE)) {
	            if (chosenAction != Action.SPLIT) {
	               System.err.println("With two Aces and a dealer " + dealerCard + ", I chose to " + chosenAction.toString());
	               System.err.println("Here is my rule set: " + theRules.toString());
	               State.printStateStatus(myState, "Here is my state:");
	               throw new NoRecommendationException();
	            }
	         }
	         return;
	      }
	
	      //Don't surrender with dealer up card of 2-7
	      if ((dealerCard.value() < 8) && (dealerCard.value() != CardValue.ACE.value())
	              && (chosenAction == Action.SURRENDER)) {
	         System.err.println("I chose to surrender when the dealer had less than an 8 up.");
	         State.printStateStatus(myState, "");
	         aStrategy.print(true);
	         //re.printStackTrace(); No need, it'll never be caught.
	         throw new NoRecommendationException();
	
	      }
	
	      if ((firstPlayerCard == CardValue.ACE) || (secondPlayerCard == CardValue.ACE)) {
	         isSoft = true;
	         handTotal = firstPlayerCard.value() + secondPlayerCard.value() + 10;
	      }
	      else {
	         isSoft = false;
	         handTotal = firstPlayerCard.value() + secondPlayerCard.value();
	      }
	
	
	      //ALWAYS STAND ON 21
	      if (handTotal == 21) {
	         if (chosenAction != Action.STAND) {
	            System.err.println("With a hand total of " + handTotal + " and a dealer " + dealerCard + ", I chose to " + chosenAction.toString());
	            System.err.println("Here is my rule set: " + theRules.toString());
	            State.printStateStatus(myState, "Here is my state:");
	            throw new NoRecommendationException();
	
	         }
	      }
	
	      //NEVER TAKE INSURANCE
	      if (myState.isInsuranceAdvised()) {
	         System.err.println("With a hand total of " + handTotal + " and a dealer " + dealerCard + ", I chose to take insurance.");
	         System.err.println("Here is my rule set: " + theRules.toString());
	         State.printStateStatus(myState, "Here is my state:");
	         throw new NoRecommendationException();
	      }
	
	
	
	
	      //ALWAYS STAND ON HARD 20
	      if ((handTotal == 20) && (!isSoft)) {
	         if (chosenAction != Action.STAND) {
	            System.err.println("With a hand total of " + handTotal + " and a dealer " + dealerCard + ", I chose to " + chosenAction.toString());
	            System.err.println("Here is my rule set: " + theRules.toString());
	            State.printStateStatus(myState, "Here is my state:");
	            throw new NoRecommendationException();
	
	         }
	      }
	
	      //ALWAYS DOUBLE ON 11, IF POSSIBLE, ON DEALER 9 or under.
	      if ((handTotal == 11) && (theRules.isPossible(Action.DOUBLE, myState))
	              && (dealerCard.value() != 10) && (dealerCard.value() != 1)
	              && (chosenAction != Action.DOUBLE)) {
	         System.err.println("With a hand total of " + handTotal + " and a dealer " + dealerCard + ", I chose to " + chosenAction.toString() + ", not double.");
	         System.err.println("Here is my rule set: " + theRules.toString());
	         State.printStateStatus(myState, "Here is my state:");
	
	         throw new NoRecommendationException();
	      }
	
	      //Always hit, double, or split if your hand total is 11 or under and
	      //early surrender is not allowed. If early surrender is allowed, then
	      //don't surrender unless the dealer has a 10 or ace up.
	      if (handTotal <= 11) {
	         if ((chosenAction != Action.HIT) && (chosenAction != Action.DOUBLE)
	                 && (chosenAction != Action.SPLIT)) {
	            if (!theRules.getEarlySurrender() && !theRules.getEarlySurrenderNotOnAces()) {
	
	               System.err.println("With a hand total of " + handTotal + ", I did not choose to hit or double.");
	               System.err.println("Here is my rule set: " + theRules.toString());
	               System.err.println("My chosen action is: " + chosenAction);
	               try {
	                  Thread.sleep(1000);
	               }
	               catch (Exception e) {
	               }
	               State.printStateStatus(myState, "Here is my state:");
	               throw new NoRecommendationException();
	            }
	            else if ((dealerCard != CardValue.TEN)
	                    && (dealerCard != CardValue.ACE)) {
	
	               System.err.println("With a hand total of " + handTotal + ", I did not choose to hit or double.");
	               System.err.println("Here is my rule set: " + theRules.toString());
	               System.err.println("My chosen action is: " + chosenAction);
	               try {
	                  Thread.sleep(1000);
	               }
	               catch (Exception e) {
	               }
	               State.printStateStatus(myState, "Here is my state:");
	               throw new NoRecommendationException();
	
	            }
	            else ; //Early surrender on a ten or ace. Take it.
	         }
	      }
	
	
	      //STAND ON A HARD 14+ if the dealer has 2-6 up.
	      if ((handTotal > 14) && (dealerCard.value() >= 2) && (dealerCard.value() <= 6) && (!isSoft)) {
	         if ((chosenAction != Action.STAND) && (chosenAction != Action.SPLIT)) {
	            System.err.println("With a hand total of " + handTotal + ", I did not choose to hit or double.");
	            System.err.println("Here is my rule set: " + theRules.toString());
	            System.err.println("My chosen action is: " + chosenAction);
	            try {
	               Thread.sleep(1000);
	            }
	            catch (Exception e) {
	            }
	            State.printStateStatus(myState, "Here is my state:");
	            throw new NoRecommendationException();
	         }
	
	      }
	
	
	      //WHOLE FUNCTION ABOVE HERE
	   }
	   catch (IOException f) {
	      throw new NoRecommendationException(f);
	   }
	
	}

}
