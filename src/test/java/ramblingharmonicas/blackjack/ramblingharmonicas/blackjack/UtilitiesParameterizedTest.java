package ramblingharmonicas.blackjack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ramblingharmonicas.blackjack.cards.Card;
import ramblingharmonicas.blackjack.cards.CardValue;
import ramblingharmonicas.blackjack.cards.Suit;

@RunWith(Parameterized.class)
public class UtilitiesParameterizedTest {

final static private Card aTen = new Card(Suit.HEARTS, CardValue.JACK);
final static private Card anAce = new Card( Suit.CLUBS, CardValue.ACE);
final static private Card aTwo = new Card(Suit.SPADES, CardValue.TWO);
final static private Card aJack = new Card(Suit.HEARTS, CardValue.JACK);

@Parameterized.Parameters
public static Collection<Object[]> data() {
    List data = new ArrayList<Object []>();
    Object [] dataSet;
 
    ArrayList<Card> sampleHand = new ArrayList<Card>();
    boolean isSoft;
    boolean containsTen;
    int handTotal;
    
    sampleHand.add(aTen);
    sampleHand.add(aTen);
    sampleHand.add(anAce);
    isSoft = false;
    containsTen = true;
    handTotal = 21;
    data.add(new Object[]{sampleHand, isSoft, containsTen, handTotal});
    sampleHand = new ArrayList<Card>();
    
    sampleHand.add(anAce);
    sampleHand.add(anAce);
    sampleHand.add(anAce);
    isSoft = true;
    containsTen = false;
    handTotal = 13;
    data.add(new Object[]{sampleHand, isSoft, containsTen, handTotal});
    sampleHand = new ArrayList<Card>();
    
    sampleHand.add(aTwo);
    sampleHand.add(aTen);
    sampleHand.add(aJack);
    isSoft = false;
    containsTen = true;
    handTotal = 22;
    data.add(new Object[]{sampleHand, isSoft, containsTen, handTotal});
    sampleHand = new ArrayList<Card>();
    
    return data;
}

final boolean expectedIsSoft, expectedContainsTen;
final ArrayList<Card> myCards;
final int expectedHandTotal;
final int [] cardArray;

public UtilitiesParameterizedTest(ArrayList<Card> myHand, final boolean isSoft,
        final boolean containsTen, final int handTotal) {
    this.myCards = myHand;
    this.cardArray = Utilities.convertCardArraytoArray(myHand);
    this.expectedIsSoft = isSoft;
    this.expectedContainsTen = containsTen;
    this.expectedHandTotal = handTotal;
}

@Test
public void testHandSize() {
    assertEquals(myCards.size(), Utilities.handSize(cardArray));
}

@Test
public void testIsSoft() {
    assertEquals(expectedIsSoft, Utilities.isSoft(myCards));
    assertEquals(expectedIsSoft, Utilities.isSoft(cardArray, expectedHandTotal));
}

@Test
public void testContains() {
    assertEquals(expectedContainsTen, Utilities.contains(myCards, CardValue.TEN));
}

@Test
public void testHandTotal() {
    assertEquals(expectedHandTotal, Utilities.handTotal(myCards));
    assertEquals(expectedHandTotal, Utilities.handTotal(cardArray));
}

@Test (expected=IllegalArgumentException.class)
public void testRetrieveSingleCard() {
   Utilities.retrieveSingleCard(cardArray);
   //Dealer.retrieveSingleCard should throw an exception on two cards in hand.
}

}