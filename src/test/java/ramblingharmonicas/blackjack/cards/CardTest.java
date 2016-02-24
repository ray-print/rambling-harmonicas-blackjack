package ramblingharmonicas.blackjack.cards;
import static org.junit.Assert.*;

import org.junit.Test;

public class CardTest {
	@Test
	public void testCards() {
	   Card Funitude = new Card(Suit.HEARTS, CardValue.ACE);
	   assert (Funitude.getCardValue() == CardValue.ACE);
	   assert (Funitude.getSuit() == Suit.HEARTS);
	}
}
