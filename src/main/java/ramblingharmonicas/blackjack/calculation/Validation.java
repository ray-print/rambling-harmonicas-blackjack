package ramblingharmonicas.blackjack.calculation;

import java.io.IOException;

import ramblingharmonicas.blackjack.Action;
import ramblingharmonicas.blackjack.NoRecommendationException;
import ramblingharmonicas.blackjack.Rules;
import ramblingharmonicas.blackjack.State;
import ramblingharmonicas.blackjack.Strategy;
import ramblingharmonicas.blackjack.Testers;
import ramblingharmonicas.blackjack.cards.CardValue;

public class Validation {

final public static boolean assertsOn;
static {
    boolean areAssertsOn = false;
    assert (areAssertsOn = true);    
    assertsOn = areAssertsOn;
}

public static void assertProbsAreOne(double probabilities []) {
    if (!assertsOn) {
        return;
    }
    double sum = 0;
    for (int i = 0; i < probabilities.length; i++) {
       if (probabilities[i] > 0) {
          sum += probabilities[i];
       }
    }
    assert ((sum > 0.9999) && (sum < 1.0001)) : "Sum is " + sum;
}
/**
 * Tests that the given strategy obeys some common blackjack advice.
 * Essentially a wrapper function for validateOneStrategy and
 * testTotalConsolidatedForConsolidation
 *
 * @param aStrategy
 * @exception NoRecommendationException if the strategy fails validation
 */
public static void validateStrategy(Strategy aStrategy) 
		throws NoRecommendationException, IOException {
   for (CardValue dealerCard : CardValue.oneToTen) {
      for (CardValue firstPlayerCard : CardValue.oneToTen) {
         for (CardValue secondPlayerCard : CardValue.oneToTen) {
            Validation.validateOneStrategy(dealerCard, firstPlayerCard, 
                    secondPlayerCard, aStrategy);
         }
      }
   }
   final boolean verbosity = false;

   if (aStrategy.getStrategyType() == Strategy.Skill.TOTAL_DEP) {
	   //TODO move this over too
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
 * @exception NoRecommendation if it fails 
 */
private static void validateOneStrategy(CardValue dealerCard,
        CardValue firstPlayerCard, CardValue secondPlayerCard,
        Strategy aStrategy) throws NoRecommendationException, IOException {
    Rules theRules = aStrategy.getMyRules();
    String msg;

    State myState = new State(firstPlayerCard, secondPlayerCard, dealerCard);
    Action bestAction;
    final int handTotal;
    boolean isSoft;
    bestAction = aStrategy.findBestAction(myState);

    //Always split aces when the dealer doesn't have a 10 or ace up.
    if (    (theRules.isPossible(Action.SPLIT, myState))
            && (bestAction != Action.SPLIT)
            && (firstPlayerCard == secondPlayerCard)
            && (firstPlayerCard == CardValue.ACE)) {
        if ((dealerCard != CardValue.TEN) && (dealerCard != CardValue.ACE)) {
            msg = "With two Aces and a dealer " + dealerCard + ", "
                    + "I chose to " + bestAction.toString();
            throw new NoRecommendationException(myState, theRules, aStrategy, 
                    msg);
        }
        return;
    }

    // Don't surrender with dealer up card of 2-7
    if ((dealerCard.value() < 8)
            && (dealerCard.value() != CardValue.ACE.value())
            && (bestAction == Action.SURRENDER)) {
        msg = "I chose to surrender when the dealer had less than an 8 up.";
        throw new NoRecommendationException(myState, theRules, aStrategy, msg);
    }

    if ( (firstPlayerCard == CardValue.ACE)
        || (secondPlayerCard == CardValue.ACE)) {
        isSoft = true;
        handTotal = firstPlayerCard.value() + secondPlayerCard.value() + 10;
    }
    else {
        isSoft = false;
        handTotal = firstPlayerCard.value() + secondPlayerCard.value();
    }


    // ALWAYS STAND ON 21
    if ( (handTotal == 21) && (bestAction != Action.STAND) ) {
        msg = "With a hand total of " + handTotal + " and a dealer "
                + dealerCard + ", I chose to " + bestAction.toString();
        throw new NoRecommendationException(myState, theRules, aStrategy, msg);
    }

    // NEVER TAKE INSURANCE
    if (myState.isInsuranceAdvised()) {
        msg = "With a hand total of " + handTotal + " and a dealer "
                + dealerCard + ", I chose to take insurance.";
        throw new NoRecommendationException(myState, theRules, aStrategy, msg);
    }

    // ALWAYS STAND ON HARD 20
    if ((handTotal == 20) && (!isSoft) && ((bestAction != Action.STAND))) {
        msg = "With a hand total of " + handTotal + " and a dealer "
                + dealerCard + ", I chose to "+ bestAction.toString();
        throw new NoRecommendationException(myState, theRules, aStrategy, msg);
    }

    // ALWAYS DOUBLE ON 11, IF POSSIBLE, ON DEALER 9 or under.
    if ((handTotal == 11) 
            && (theRules.isPossible(Action.DOUBLE, myState))
            && (dealerCard.value() != 10) && (dealerCard.value() != 1)
            && (bestAction != Action.DOUBLE)) {
        msg = "With a hand total of " + handTotal + " and a dealer " 
                + dealerCard + ", I chose to " + bestAction.toString() + 
                ", not double.";
        throw new NoRecommendationException(myState, theRules, aStrategy, msg);
    }

    // Always hit, double, or split if your hand total is 11 or under and
    // early surrender is not allowed. If early surrender is allowed, then
    // don't surrender unless the dealer has a 10 or ace up.
    if (handTotal <= 11) {
        if ((bestAction != Action.HIT) && (bestAction != Action.DOUBLE)
                && (bestAction != Action.SPLIT)) {
            if (!theRules.getEarlySurrender()
                    && !theRules.getEarlySurrenderNotOnAces()) {
                msg = "With a hand total of " + handTotal + ", I did not "
                        + "choose to hit or double; I chose to " + bestAction;
                throw new NoRecommendationException(myState, theRules, 
                        aStrategy, msg);
            }
            else
                if ((dealerCard != CardValue.TEN)
                        && (dealerCard != CardValue.ACE)) {
                    msg = "With a hand total of " + handTotal
                            + ", I did not choose to hit or double." +
                            "My chosen action is: " + bestAction;
                    throw new NoRecommendationException(myState, theRules, 
                            aStrategy, msg);
                }
                else
                 // Early surrender on a ten or ace. Take it.
                    ; 
        }
    }

    // STAND ON A HARD 14+ if the dealer has 2-6 up.
    boolean has2To6Up = (dealerCard.value() >= 2) && (dealerCard.value() <= 6);
    if ((handTotal > 14) && has2To6Up && (!isSoft)) {
        if ((bestAction != Action.STAND)
                && (bestAction != Action.SPLIT)) {
            msg = "With a hand total of " + handTotal
                    + ", I did not choose to stand or split." + 
                    "My chosen action is: " + bestAction;
            throw new NoRecommendationException(myState, theRules, 
                    aStrategy, msg);
        }

    }
    
    //TODO: Track down why this is always good advice.
    //Assuming no early surrender, with the players holding 8 8 or A A:
    // For 4 or more decks, with no hole card, hit if the dealer has an ace or 10; 
    // split otherwise.
    
    if ( (firstPlayerCard == secondPlayerCard)
         && (firstPlayerCard == CardValue.ACE) || (firstPlayerCard == CardValue.EIGHT)
         && (theRules.getEarlySurrender() == false)) {
        if ((theRules.dealerHoleCard() == false) && (theRules.getNumberOfDecks() >= 4)) {
            if ((dealerCard == CardValue.ACE) || (dealerCard == CardValue.TEN)) {
               assert (bestAction == Action.HIT);
            }
            else {
               assert (bestAction == Action.SPLIT);
            }
        }
        else 
            if ((theRules.getNumberOfDecks() >= 2)
                 && (firstPlayerCard == CardValue.EIGHT)) {
            assert (bestAction == Action.SURRENDER);
        }
        else
            if (theRules.dealerHoleCard()) {
                assert (bestAction == Action.SPLIT);
            }
      }
}

}
