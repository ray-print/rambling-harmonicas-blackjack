package ramblingharmonicas.blackjack;

import java.util.ArrayList;
import org.junit.Test;
import static org.junit.Assert.*;
import ramblingharmonicas.blackjack.cards.Card;
import ramblingharmonicas.blackjack.cards.CardValue;
import ramblingharmonicas.blackjack.cards.Suit;

public class UtilitiesTest {

@Test public void testRetrieveSingleCard() {
   int[] someCards = new int[10];
   Utilities.zero(someCards);
   someCards[0] = 1;
   assertEquals(0, Utilities.retrieveSingleCard(someCards));

   Utilities.zero(someCards);
   someCards[9] = 1;
   assertEquals(9, Utilities.retrieveSingleCard(someCards));
}

@Test
public void testGetDealerHand() {
    ArrayList<Card> initialCards = new ArrayList<Card>();

    final Rules theRules = new Rules(1);
    theRules.setHoleCard(false);
    Shoe myShoe = new Shoe(theRules.getNumberOfDecks());

    Card two = new Card(Suit.CLUBS, CardValue.TWO);
    initialCards.add(two);
    int handTotal;
    for (int i = 0; i < 10000; i++) {
       initialCards = Utilities.getDealerHand(theRules, initialCards, myShoe);
       handTotal = Utilities.handTotal(initialCards);
       assert (handTotal > 16) : "Handtotal is " + handTotal;
       assert (handTotal < 27) : "Handtotal is " + handTotal;
       myShoe = new Shoe(theRules.getNumberOfDecks());
       initialCards.clear();
       initialCards.add(two);
    }
}
}