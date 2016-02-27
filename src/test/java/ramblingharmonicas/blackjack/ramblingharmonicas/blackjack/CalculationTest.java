/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ramblingharmonicas.blackjack;

import org.junit.Test;
import static org.junit.Assert.*;
import ramblingharmonicas.blackjack.cards.CardValue;

public class CalculationTest {

public CalculationTest() {}

@Test
public void testHard12 () throws NoRecommendationException { //should make JUnit barf?
    Rules theRules = new Rules(8);
    theRules.setAccuracy(Rules.LOW_ACCURACY);
    FastShoe myShoe = new FastShoe(theRules.getNumberOfDecks());
    myShoe.fastDrawSpecific(CardValue.TWO);
    myShoe.fastDrawSpecific(CardValue.JACK);
    myShoe.fastDrawSpecific(CardValue.TEN);
    State aState = new State(CardValue.TWO, CardValue.JACK, CardValue.TEN);
    aState = Blackjack.PlayerRecursive(myShoe, aState, theRules);
    assert (aState.getBestAction() == Action.HIT);
    assert (aState.getSecondBestAction() == Action.SURRENDER);
}

}