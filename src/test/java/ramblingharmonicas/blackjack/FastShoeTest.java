package ramblingharmonicas.blackjack;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

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
		assertEquals(aShoe.numberOfCards(), numberOfCards);
	}

	@Test
	public void testCardAddition() {
		aShoe.addCard(Blackjack.ACECARD);
		aShoe.addCard(Blackjack.JACKCARD);
		assertEquals(aShoe.numberOfCards(), numberOfCards + 2);
		aShoe.addCard(new Card(Suit.SPADES, CardValue.JACK));
	    aShoe.addCard(new Card(Suit.DIAMONDS, CardValue.QUEEN));
	    aShoe.addCard(new Card(Suit.DIAMONDS, CardValue.QUEEN));
	    aShoe.addCard(new Card(Suit.DIAMONDS, CardValue.KING));
	    assertEquals (numberOfCards + 6, aShoe.numberOfCards());
	}

	@Test
	public void testCardRemoval() {
		aShoe.fasterDrawSpecific(Blackjack.TENCARD);
		assertEquals(aShoe.numberOfCards(), numberOfCards - 1);
		aShoe.fasterDrawSpecific(Blackjack.ACECARD);
		assertEquals(aShoe.numberOfCards(), numberOfCards - 2);
	}

	@Test
	public void testProbabilityOf() {
		for (int card : aceToNine) {
			assert aShoe.probabilityOf(card) ==
					(double) (4 * numDecks) / (double) (numberOfCards);
		}
		assert aShoe.probabilityOf(Blackjack.TENCARD) ==
					(double) (16 * numDecks) / (double) (numberOfCards);
	}

	@Test
	public void integrationTestTwoProbabilityOf () {
		   aShoe.addCard(new Card(Suit.SPADES, CardValue.JACK));
		   aShoe.addCard(new Card(Suit.DIAMONDS, CardValue.QUEEN));
		   aShoe.addCard(new Card(Suit.DIAMONDS, CardValue.QUEEN));
		   aShoe.addCard(new Card(Suit.DIAMONDS, CardValue.KING));
		   aShoe.addCard(new Card(Suit.DIAMONDS, CardValue.TEN));
		   aShoe.addCard(new Card(Suit.DIAMONDS, CardValue.ACE));
		   int numberCards = numberOfCards + 6;
		   for (CardValue cv : CardValue.twoToTen) {
		      if (cv == CardValue.TEN) {
		         assertEquals ( (5 + (16D * numDecks)) / numberCards, 
		        		 aShoe.probabilityOf(Blackjack.TENCARD),
		        		 Constants.SMALLEST_EPSILON);
		         break;
		      }
		      assertEquals ( (4D * numDecks) / numberCards, aShoe.probabilityOf(cv),
		    		  Constants.SMALLEST_EPSILON); 
		   }
	}
	
	@Test
	public void testProbabilityOfExcluding() {
		for (CardValue cv : CardValue.twoToTen) {
			if (cv != CardValue.TEN) {
				assertEquals ( 4D / (52D - 16D), aShoe.probabilityOfExcluding(cv,
	                 CardValue.TEN), Constants.SMALLEST_EPSILON); 
	        } 
			else {
	        	assertEquals ( 16D / (52D - 4D), aShoe.probabilityOfExcluding(cv, 
	        		CardValue.ACE), Constants.SMALLEST_EPSILON);
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
		assert aShoe.probabilityOf(Blackjack.TWOCARD) < 0;
	}

	@Test
	public void testTheseThreeInOrder() {
	   double probability, expectedProbability, startingCards;
	   for (CardValue val : CardValue.oneToTen) {
		  probability = aShoe.probTheseThreeInOrder(val, val, val);
	      if (CardValue.TEN == val) {
             startingCards = 16D * numDecks;
	      }
	      else {
             startingCards = 4D * numDecks;
	      }
	      expectedProbability = (startingCards * (startingCards -1) * (startingCards -2) 
	              / ((numberOfCards) * (numberOfCards - 1D) * (numberOfCards - 2D)));
	      assertEquals(expectedProbability, probability, Constants.SMALLEST_EPSILON);
	   }
	}

	@Test
	public void testProbOfExcluding() {
		for (int card : aceToEight) {
			assertEquals(0.08333333, aShoe.fastProbOfExcluding(card,  card+1),
					Constants.SMALLEST_EPSILON);
		}
		for (int card : aceToNine) {
			assertEquals(0.11111111, aShoe.fastProbOfExcluding(card,  Blackjack.TENCARD), 
					Constants.SMALLEST_EPSILON);
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
	
	@Test
	public void testPlayerProbability() {
		//dealer has a ten, what is the change of you drawing a three? (Given the dealer has no ace)
		double probOfThree = 0;
		aShoe.fasterDrawSpecific(CardValue.TEN);
		double probOfDealerDrawingThree = 
				aShoe.fastProbOfExcluding(Blackjack.THREECARD, Blackjack.ACECARD);
		probOfThree += 
				probOfDealerDrawingThree * 
				(4 * numDecks - 1) / (numberOfCards - 2);
		probOfThree += (1 - probOfDealerDrawingThree) * 
				(4 * numDecks) / (numberOfCards - 2);
		double result = aShoe.playerProbability(true, new Card(Suit.CLUBS, CardValue.TEN), CardValue.THREE);
		assertEquals(probOfThree, result, Constants.SMALLEST_EPSILON);
	}
	
	/**
	 * Tests the fast shoe key used in the dealer probability cache.
	 * This could use vast improving. Specifically, a recursive solution
	 * to go to an arbitrary depth, and a permutation formula to solve
	 * for the total number of expected keys at a given depth.
	 */
	@Test
	public void testFastShoeKey() {
	   final int DECK_CHOICES = 61;
	   Set<String> allTheKeys = new TreeSet<String>();
	   for (int i = 1; i < DECK_CHOICES; i++) {
	      FastShoe myShoe = new FastShoe(i);
	      for (int cv1 = 0; cv1 < 10; cv1++) {
	         myShoe.fasterDrawSpecific(cv1);
	         allTheKeys.add(myShoe.myStringKey());
	         for (int cv2 = 0; cv2 < 10; cv2++) {
	            myShoe.fasterDrawSpecific(cv2);
	            allTheKeys.add(myShoe.myStringKey());
	            myShoe.addCard(cv2);
	         }
	         myShoe.addCard(cv1);

	      }
	   }
	   final int twoRemovedKeys = 55 * (DECK_CHOICES - 1);
	   final int oneRemovedKeys = 10 * (DECK_CHOICES - 1);
	   assertEquals(oneRemovedKeys + twoRemovedKeys, allTheKeys.size());
	   //55 permutations while removing 2 cards
	}
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