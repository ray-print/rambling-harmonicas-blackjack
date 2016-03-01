package ramblingharmonicas.blackjack;

import java.util.ArrayList;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;
import ramblingharmonicas.blackjack.cards.Card;
import ramblingharmonicas.blackjack.cards.CardValue;
import ramblingharmonicas.blackjack.cards.Suit;

public class RulesTest {

public RulesTest() {}

private Rules theRules;
private State myState;

@Before
public void clearRules() {
    theRules = new Rules();
}
@Test
public void testIsSplitPossible() {
    for (CardValue card: CardValue.values()) {
        myState = new State(card, card, CardValue.TEN);
        assert theRules.isPossible(Action.SPLIT, myState): 
        "Splitting impossible in this state" + myState;
    }
    myState = new State(CardValue.TEN, CardValue.KING, CardValue.TWO);
    assert theRules.isPossible(Action.SPLIT, myState):
        "Splitting impossible in this state" + myState;
}

@Test
public void testNumPossibleActions() {
    myState = new State(CardValue.TEN, CardValue.JACK, CardValue.TWO);
    assert (theRules.numPossibleActions(myState, false) == 5);
    myState.action(Action.HIT, new Card(Suit.SPADES, CardValue.ACE));
    assert (theRules.numPossibleActions(myState, true) == 2);
    
    myState = new State(CardValue.THREE, CardValue.THREE, CardValue.NINE);
    theRules.setLateSurrender(false);
    theRules.myDoubleRules.setOnlyNineTenEleven(true);
    assert (theRules.numPossibleActions(myState, true) == 3);
    
    myState = new State(CardValue.EIGHT, CardValue.EIGHT, CardValue.EIGHT);
    myState.action(Action.SPLIT);
    myState.postSplitDraw(new Card(Suit.SPADES, CardValue.TEN));
    assert (theRules.numPossibleActions(myState, true) == 2);
}

@Test
public void testStandPossible() {
    for (CardValue firstCard : CardValue.values()) {
        for (CardValue secondCard: CardValue.values()) {
            myState = new State(firstCard, secondCard, CardValue.TWO);
            assert (theRules.isPossible(Action.STAND, myState));
        }
    }
}
/**
 * Note that the copy constructor is
 * also tested every time it runs, by Rules.myHashKey .
 *
 */
@Test
public void testCopyConstructor() {
   Rules theRules, otherRules;
   theRules = new Rules(1);
   otherRules = new Rules(theRules);
   assert theRules.equals(otherRules);

   theRules.setAccuracy(Rules.LOW_ACCURACY);
   theRules.setCharlie(5);
   theRules.setEarlySurrender(true);
   theRules.setHitSplitAces(true);
   theRules.setNumberDecks(2);
   theRules.setMaxNumberSplitHands(2);
   theRules.myDoubleRules.setNotOnAces(true);
   theRules.myDoubleRules.setOnlyNineTenEleven(true);
   theRules.myDoubleRules.setNotPostSplit(true);
   otherRules = new Rules(theRules);
   assert theRules.equals(otherRules);

   theRules = new Rules(otherRules);
   theRules.setRulesAutoToggles(false);
   theRules.myDoubleRules.setAnyTwoCards(false);
   theRules.myDoubleRules.setOnlyNineTenEleven(true);
   theRules.myDoubleRules.setOnlyTenAndEleven(true);
   otherRules = new Rules(theRules);
   assert theRules.equals(otherRules);
}


}