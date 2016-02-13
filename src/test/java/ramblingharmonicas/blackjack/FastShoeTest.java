package blackjack;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import blackjack.cards.*;

@RunWith(Parameterized.class)
public class FastShoeTest {

	private final FastShoe aShoe;
	private final int numDecks;

	private final static int [] aceToNine = { Blackjack.ACECARD, Blackjack.TWOCARD,
		Blackjack.THREECARD, Blackjack.FOURCARD, Blackjack.FIVECARD, Blackjack.SIXCARD, 
		Blackjack.SEVENCARD, Blackjack.EIGHTCARD, Blackjack.NINECARD};
	private final static int [] aceToEight = { Blackjack.ACECARD, Blackjack.TWOCARD,
		Blackjack.THREECARD, Blackjack.FOURCARD, Blackjack.FIVECARD, Blackjack.SIXCARD, 
		Blackjack.SEVENCARD, Blackjack.EIGHTCARD, Blackjack.NINECARD};

	@Parameters
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[] [] {
				{1},{2},{3},{4},{5},{6},{7},{8}
		});
	}

	public FastShoeTest(int decks) {
		numDecks = decks;
		aShoe = new FastShoe(decks);
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
	   for (CardValue val : Blackjack.oneToTen) {
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
			assertEquals(aShoe.fastProbOfExcluding(card,  card+1), 0.08333333,
					Blackjack.SMALLEST_EPSILON);
		}
		for (int card : aceToNine) {
			assertEquals(aShoe.fastProbOfExcluding(card,  Blackjack.TENCARD), 0.11111111,
					Blackjack.SMALLEST_EPSILON);
		}
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