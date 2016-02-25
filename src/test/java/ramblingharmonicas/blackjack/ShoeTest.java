package ramblingharmonicas.blackjack;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import ramblingharmonicas.blackjack.cards.Card;
import ramblingharmonicas.blackjack.cards.CardValue;

@RunWith(Parameterized.class)
public class ShoeTest {
private final Shoe aShoe;
private final int numDecks;
private final int numberOfCards;

@Parameters
public static Collection<Object[]> data() {
	return Arrays.asList(new Object[] [] {
			{1},{2},{3},{4},{5},{6},{7},{8}
	});
}

public ShoeTest(int decks) {
	numDecks = decks;
	aShoe = new Shoe(decks);
	numberOfCards = 52 * numDecks;
}

@Test
public void testDeckConstructor() {
	assert aShoe != null : "Shoe constructor should work";
	assertEquals(aShoe.numberOfCards(), numberOfCards);
}

@Test
public void testProbabilityOf() {
   double expectedProb = 4D / 52D;
   double expectedTenProb = 16D/52;
   for (CardValue i : CardValue.values()) {
	   if (i.value() == 10) {
		  assertEquals(expectedTenProb, aShoe.probabilityOf(i), Constants.SMALLEST_EPSILON);
		  continue;
	   }
	   assertEquals (expectedProb, aShoe.probabilityOf(i), Constants.SMALLEST_EPSILON);
   }
}
//TODO: Add drawSpecific test
@Test
public void testDeepClone() {
   Shoe secondShoe = aShoe.deepClone();
   assertEquals(numberOfCards, secondShoe.numberOfCards());
   for (CardValue i : CardValue.values()) {
	   assertEquals(aShoe.probabilityOf(i), secondShoe.probabilityOf(i), Constants.SMALLEST_EPSILON);
   }
   secondShoe.drawSpecific(CardValue.NINE);
   secondShoe.drawSpecific(CardValue.NINE);
   secondShoe.drawSpecific(CardValue.NINE);
   secondShoe.drawSpecific(CardValue.NINE);
   aShoe.drawSpecific(CardValue.TWO);
   aShoe.drawSpecific(CardValue.TWO);
   aShoe.drawSpecific(CardValue.TWO);
   aShoe.drawSpecific(CardValue.TWO);
   final double expectedNineProb = (4D * numDecks) / (52D * numDecks - 4D); 
   assertEquals(expectedNineProb, aShoe.probabilityOf(CardValue.NINE), Constants.SMALLEST_EPSILON);
   assertEquals(expectedNineProb, aShoe.probabilityOf(CardValue.ACE), Constants.SMALLEST_EPSILON);
   final double secondShoeExpectedNineProb = (4D * numDecks - 4D) / (52D * numDecks - 4D);
   if (numDecks != 1) {
   assertEquals(secondShoeExpectedNineProb, secondShoe.probabilityOf(CardValue.NINE), 
		   Constants.SMALLEST_EPSILON);
   }
   else {
	   assert (secondShoe.probabilityOf(CardValue.NINE) < 0) : "Shoe.probabilityOf should return a " +
		   "negative value if the card is not present in the shoe.";
   }
}

//Nice TODO: Test for exception throwing functionality
@Test
public void testDrawAppropriate() {
	Card drawnCard;
	Card secondDrawnCard;
	List<Card> scratch = new ArrayList<Card>();
	Rules theRules = new Rules(1);
	int handTotal;
	for (int i = 0; i < 50; i++) {
		//DrawMode.ALL_HARD
		drawnCard = aShoe.drawAppropriate(DrawMode.ALL_HARD, true, theRules);
	    secondDrawnCard = aShoe.drawSecondPlayerCard(DrawMode.ALL_HARD, drawnCard);
	    assertNotSame("DrawMode.ALL_HARD should not return an ace", drawnCard.getCardValue(),
			   CardValue.ACE);
	    assertNotSame("DrawMode.ALL_HARD should not return an ace", secondDrawnCard.getCardValue(),
			   CardValue.ACE);
	    aShoe.addCard(drawnCard);
	    aShoe.addCard(secondDrawnCard);

	    //DrawMode.HARD_12_16
	    drawnCard = aShoe.drawAppropriate(DrawMode.HARD_12_16, true, theRules);
	    secondDrawnCard = aShoe.drawSecondPlayerCard(DrawMode.HARD_12_16, drawnCard);
	    assertNotSame("DrawMode.HARD_12_16 should not return an ace", drawnCard.getCardValue(),
			   CardValue.ACE);
	    assertNotSame("DrawMode.HARD_12_16 should not return an ace", secondDrawnCard.getCardValue(),
			   CardValue.ACE);
	    handTotal = drawnCard.value() + secondDrawnCard.value();
	    assert handTotal >= 12 : "DrawMode.HARD_12_16 returned: " + handTotal;
	    assert handTotal <= 16 : "DrawMode.HARD_12_16 returned: " + handTotal;

	    aShoe.addCard(drawnCard);
	    aShoe.addCard(secondDrawnCard);

	    //DrawMode.ALL_SOFT
	    drawnCard = aShoe.drawAppropriate(DrawMode.ALL_SOFT, true, theRules);
	    secondDrawnCard = aShoe.drawSecondPlayerCard(DrawMode.ALL_SOFT, drawnCard);
      
	    if ((drawnCard.getCardValue() != CardValue.ACE)
              && (secondDrawnCard.getCardValue() != CardValue.ACE)) {
	    	assert false: "Ace not drawn to soft hand in drawAppropriate.";
	    }
	    aShoe.addCard(drawnCard);
	    aShoe.addCard(secondDrawnCard);

	    //DrawMode.ALL_SOFT_AND_HARD
	    drawnCard = aShoe.drawAppropriate(DrawMode.ALL_SOFT_AND_HARD, true, theRules);
	    secondDrawnCard = aShoe.drawSecondPlayerCard(DrawMode.ALL_SOFT_AND_HARD, drawnCard);
	    assertNotSame("DrawMode.ALL_SOFT_HAND should not draw split hands", drawnCard.value(),
    		  secondDrawnCard.value());
	    aShoe.addCard(drawnCard);
	    aShoe.addCard(secondDrawnCard);

	    //DrawMode.ALL_SPLITS
	    drawnCard = aShoe.drawAppropriate(DrawMode.ALL_SPLITS, true, theRules);
	    secondDrawnCard = aShoe.drawSecondPlayerCard(DrawMode.ALL_SPLITS, drawnCard);
	    assertSame("DrawMode.ALL_SPLITS should return a splittable hand.", drawnCard.value(),
    		  secondDrawnCard.value());
	    aShoe.addCard(drawnCard);
	    aShoe.addCard(secondDrawnCard);

	    //DrawMode.SOFT_OVER_16
	    drawnCard = aShoe.drawAppropriate(DrawMode.SOFT_OVER_16, true, theRules);
	    secondDrawnCard = aShoe.drawSecondPlayerCard(DrawMode.SOFT_OVER_16, drawnCard);
      
	    if ((drawnCard.getCardValue() != CardValue.ACE) && (secondDrawnCard.getCardValue() != CardValue.ACE)) {
	    	assert false: "SOFT_OVER_16 did not return an Ace in drawAppropriates:"
                 + drawnCard.toString() + " and " + secondDrawnCard.toString();
	    }
	    scratch.add(drawnCard);
	    scratch.add(secondDrawnCard);
	    if ((Utilities.handTotal(scratch) < 16) || (Utilities.handTotal(scratch) > 21)) {
	    	assert false:("SOFT_OVER_16 did not give the correct hand total in drawAppropriates:"
                 + drawnCard.toString() + " and " + secondDrawnCard.toString());
	    }
	    scratch.clear();
	    aShoe.addCard(drawnCard);
	    aShoe.addCard(secondDrawnCard);

	    //DrawMode.SOFT_UNDER_16
	    drawnCard = aShoe.drawAppropriate(DrawMode.SOFT_UNDER_16, true, theRules);
	    secondDrawnCard = aShoe.drawSecondPlayerCard(DrawMode.SOFT_UNDER_16, drawnCard);
	    if ((drawnCard.getCardValue() != CardValue.ACE) && (secondDrawnCard.getCardValue() != CardValue.ACE)) {
	    	assert false: "SOFT_UNDER_16 did not return an Ace in drawAppropriates:"
                 + drawnCard.toString() + " and " + secondDrawnCard.toString();
	    }
	    scratch.add(drawnCard);
	    scratch.add(secondDrawnCard);
      
	    if (Utilities.handTotal(scratch) > 16) {
	    	assert false: "SOFT_UNDER_16 did not give the correct hand total in drawAppropriates:"
                 + drawnCard.toString() + " and " + secondDrawnCard.toString();
	    }
	    scratch.clear();
	    aShoe.addCard(drawnCard);
	    aShoe.addCard(secondDrawnCard);

	    //DrawMode.DEALER_2_6
	    drawnCard = aShoe.drawAppropriate(DrawMode.DEALER_2_6, false, theRules);
	    if ((drawnCard.value() < 2) || (drawnCard.value() > 6)) {
	    	assert false:"drawAppropriate drew a " + drawnCard.toString() + ", not 2-6.";
	    }
	    aShoe.addCard(drawnCard);
	    //DrawModel.DEALER_7_A
	    drawnCard = aShoe.drawAppropriate(DrawMode.DEALER_7_A, false, theRules);
	    if ((drawnCard.value() >= 2) && (drawnCard.value() <= 6)) {
	    	assert false: "drawAppropriate draw a " + drawnCard.toString() + ", not 7-A.";
	    }
	    aShoe.addCard(drawnCard);
   }
	
}
}
