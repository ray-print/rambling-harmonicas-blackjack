package ramblingharmonicas.blackjack;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import ramblingharmonicas.blackjack.cards.*;

@RunWith(Parameterized.class)
public class FastShoeTest {

	private final FastShoe aShoe;
	private final int numDecks;
	private final int numberOfCards;
	private final static int [] aceToNine = { Blackjack.ACECARD, Blackjack.TWOCARD,
		Blackjack.THREECARD, Blackjack.FOURCARD, Blackjack.FIVECARD, Blackjack.SIXCARD, 
		Blackjack.SEVENCARD, Blackjack.EIGHTCARD, Blackjack.NINECARD};
	private final static int [] aceToEight = { Blackjack.ACECARD, Blackjack.TWOCARD,
		Blackjack.THREECARD, Blackjack.FOURCARD, Blackjack.FIVECARD, Blackjack.SIXCARD, 
		Blackjack.SEVENCARD, Blackjack.EIGHTCARD};

	@Parameters
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[] [] {
				{1},{2},{3},{4},{5},{6},{7},{8}
		});
	}

	public FastShoeTest(int decks) {
		numDecks = decks;
		aShoe = new FastShoe(decks);
		numberOfCards = 52 * numDecks;
	}

	@Test
	public void testDeckConstructor() {
		assert aShoe != null : "FastShoe constructor should work";
		assertEquals(aShoe.numberOfCards(), 52 * numDecks);
	}

	@Test
	public void testCardAddition() {
		aShoe.addCard(Blackjack.ACECARD);
		aShoe.addCard(Blackjack.JACKCARD);
		assertEquals(aShoe.numberOfCards(), 52 * numDecks + 2);
		aShoe.addCard(new Card(Suit.SPADES, CardValue.JACK));
	    aShoe.addCard(new Card(Suit.DIAMONDS, CardValue.QUEEN));
	    aShoe.addCard(new Card(Suit.DIAMONDS, CardValue.QUEEN));
	    aShoe.addCard(new Card(Suit.DIAMONDS, CardValue.KING));
	    assertEquals (52 * numDecks + 6, aShoe.numberOfCards());
	}

	@Test
	public void testCardRemoval() {
		aShoe.fasterDrawSpecific(Blackjack.TENCARD);
		assertEquals(aShoe.numberOfCards(), 52 * numDecks - 1);
		aShoe.fasterDrawSpecific(Blackjack.ACECARD);
		assertEquals(aShoe.numberOfCards(), 52 * numDecks - 2);
	}

	@Test
	public void testProbabilityOf() {
		for (int card : aceToNine) {
			assert aShoe.fastProbabilityOf(card) ==
					(double) (4 * numDecks) / (double) (52 * numDecks);
		}
		assert aShoe.fastProbabilityOf(Blackjack.TENCARD) ==
					(double) (16 * numDecks) / (double) (52 * numDecks);
	}

	@Test
	public void integrationTestTwoProbabilityOf () {
		   aShoe.addCard(new Card(Suit.SPADES, CardValue.JACK));
		   aShoe.addCard(new Card(Suit.DIAMONDS, CardValue.QUEEN));
		   aShoe.addCard(new Card(Suit.DIAMONDS, CardValue.QUEEN));
		   aShoe.addCard(new Card(Suit.DIAMONDS, CardValue.KING));
		   aShoe.addCard(new Card(Suit.DIAMONDS, CardValue.TEN));
		   aShoe.addCard(new Card(Suit.DIAMONDS, CardValue.ACE));
		   int numberCards = 52 * numDecks + 6;
		   for (CardValue cv : CardValue.twoToTen) {
		      if (cv == CardValue.TEN) {
		         assertEquals ( (5 + (16D * numDecks)) / numberCards, 
		        		 aShoe.fastProbabilityOf(Blackjack.TENCARD),
		        		 Blackjack.SMALLEST_EPSILON);
		         break;
		      }
		      assertEquals ( (4D * numDecks) / numberCards, aShoe.fastProbabilityOf(cv),
		    		  Blackjack.SMALLEST_EPSILON); 
		   }
	}
	
	@Test
	public void testProbabilityOfExcluding() {
		for (CardValue cv : CardValue.twoToTen) {
			if (cv != CardValue.TEN) {
				assertEquals ( 4D / (52D - 16D), aShoe.probabilityOfExcluding(cv,
	                 CardValue.TEN), Blackjack.SMALLEST_EPSILON); 
	        } 
			else {
	        	assertEquals ( 16D / (52D - 4D), aShoe.probabilityOfExcluding(cv, 
	        		CardValue.ACE), Blackjack.SMALLEST_EPSILON);
	        }
		}
	}
	@Test
	public void integrationTestProbablityOf() {
		aShoe.addCard(Blackjack.ACECARD);
		aShoe.addCard(Blackjack.JACKCARD);
		aShoe.fasterDrawSpecific(Blackjack.ACECARD);
		aShoe.fasterDrawSpecific(Blackjack.TENCARD);
		for (int i = 0; i < 4 * numDecks; i++) {
			aShoe.fasterDrawSpecific(Blackjack.TWOCARD);
		}
		assert aShoe.fastProbabilityOf(Blackjack.TWOCARD) < 0;
	}

	@Test
	public void testTheseThreeInOrder() {
	   final double totalCards = 52 * numDecks;
	   final double numberDecks = (double) numDecks;
	   double probability, expectedProbability, startingCards;
	   for (CardValue val : CardValue.oneToTen) {
		  probability = aShoe.probTheseThreeInOrder(val, val, val);
	      if (CardValue.TEN == val) {
             startingCards = 16D * numberDecks;
	      }
	      else {
             startingCards = 4D * numberDecks;
	      }
	      expectedProbability = (startingCards * (startingCards -1) * (startingCards -2) 
	              / ((totalCards) * (totalCards - 1D) * (totalCards - 2D)));
	      assertEquals(expectedProbability, probability, Blackjack.SMALLEST_EPSILON);
	   }
	}

	@Test
	public void testProbOfExcluding() {
		for (int card : aceToEight) {
			assertEquals(0.08333333, aShoe.fastProbOfExcluding(card,  card+1),
					Blackjack.SMALLEST_EPSILON);
		}
		for (int card : aceToNine) {
			assertEquals(0.11111111, aShoe.fastProbOfExcluding(card,  Blackjack.TENCARD), 
					Blackjack.SMALLEST_EPSILON);
		}
	}
	
	@Test
	public void testDeepClone() {
	   FastShoe shoeCopy = aShoe.deepClone();
	   assertEquals(shoeCopy.numberOfCards(), aShoe.numberOfCards());
	   assertEquals(shoeCopy.myStringKey(), aShoe.myStringKey());
	   aShoe.fastDrawSpecific(CardValue.ACE);
	   aShoe.fastDrawSpecific(CardValue.TWO);
	   assertEquals(numberOfCards -2, aShoe.numberOfCards());
	   assertEquals(numberOfCards, shoeCopy.numberOfCards());
	   shoeCopy.fastDrawSpecific(CardValue.THREE);
	   shoeCopy.fastDrawSpecific(CardValue.THREE);
	   shoeCopy.fastDrawSpecific(CardValue.FOUR);
	   assertEquals(numberOfCards -3, shoeCopy.numberOfCards());
	   assertEquals(numberOfCards -2, aShoe.numberOfCards());
	}
	
	/* TODO: Separate these tests. Then move over the key test, then I'm done with the
	 * FastShoe unit tests.
	 * 	   double probOfThree = 0;
		   probOfThree += aShoe.fastProbOfExcluding(Blackjack.THREECARD, Blackjack.ACECARD) * 7 / (numberOfCards - 1);
		   probOfThree += (1 - aShoe.fastProbOfExcluding(Blackjack.THREECARD, Blackjack.ACECARD)) * 8 / 107;
		   assert (aShoe.playerProbability(true, new Card(Suit.CLUBS, CardValue.TEN), CardValue.THREE)
		           == probOfThree);
		   assert (shoeCopy.probTheseThreeInOrder(CardValue.THREE, CardValue.THREE, CardValue.THREE)
		           == (6D / 107D) * (5D / 106D) * (4D / 105D));		*/

	
/* Untested
 * FastShoe(FastShoe myOriginal, int[] cardArray) and its three extremely similar functions
 *
 * Rename FastShoe functions where appropriate
 * then move Blackjack.card constants to like a constants file or something
 * AddCard (int cardIndex)
 * deepClone
 * fasterDrawSpecific(int cardindex)
 * fast ProbabilityOf (int CardIndex)
 * numberOfCards()
 * printContents()
 */
}