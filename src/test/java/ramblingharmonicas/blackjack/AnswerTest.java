package ramblingharmonicas.blackjack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;
import ramblingharmonicas.blackjack.cards.Card;
import ramblingharmonicas.blackjack.cards.CardValue;
import ramblingharmonicas.blackjack.cards.Suit;

public class AnswerTest {

    public static void runAllTests() {
        testByteToCardValue();
        testCardValueToByte();
        testConstructors();
        testAnswerHash();
    }

    /**
     * This function relies on the exact values of the byte when CardValues are
     * converted
     * to bytes. If those values change this function will probably throw an
     * exception.
     *
     */
    public static void testAnswerHash() {
        Set<Integer> answerHashes = new TreeSet<Integer>();
        int numberOfAddedHashes = 0;
        boolean splitPossible;
        byte first;
        byte second;
        byte dealer;
        for (first = 1; first < 11; first++) {
            for (second = first; second < 11; second++) //The order of first and second cards doesn't matter
            {
                for (dealer = 1; dealer < 11; dealer++) {
                    if (first == second) {
                        splitPossible = true;
                    }
                    else {
                        splitPossible = false;
                    }
                    if (!answerHashes.add(Answer.answerHash(first, second, dealer, false))) {
                        System.err.println("Answer hash error on bytes: First card = " + first + ", second card = " + second + ", dealer card = " + dealer + ".");
                        System.err.println("Splitting not recommended.");
                        System.err.println("My answer hash is: " + Answer.answerHash(first, second, dealer, false));
                        assert false;
                    }
                    if (splitPossible && (!answerHashes.add(Answer.answerHash(first, second, dealer, true)))) {
                        System.err.println("Answer hash error on bytes: First card = " + first + ", second card = " + second + ", dealer card = " + dealer + ".");
                        System.err.println("Splitting recommended.");
                        System.err.println("My answer hash is: " + Answer.answerHash(first, second, dealer, false));
                        assert false;
                    }
                }
            }
        }
    }

    /**
     * Test of cardValueToByte method, of class Answer.
     */
    public static void testCardValueToByte() {
        CardValue aCardValue = CardValue.ACE;
        byte expResult = 1;
        byte result = Answer.cardValueToByte(aCardValue);
        assert (expResult == result);
        // TODO review the generated test code and remove the default call to fail.
        //fail("The test case is a prototype.");
    }

    /**
     * Test of byteToCardValue method, of class Answer.
     */
    public static void testByteToCardValue() {
        byte myByte = 1;
        CardValue expResult = CardValue.ACE;
        CardValue result = Answer.byteToCardValue(myByte);
        assert (expResult == result);
    }

    /**
     * Performs one test on the constructors.
     *
     */
    public static void testConstructors() {
        State aState;
        Rules theRules = new Rules(8);
        theRules.setAccuracy(Rules.LOW_ACCURACY);
        ArrayList<Card> myCards = new ArrayList<Card>();
        myCards.add(new Card(Suit.SPADES, CardValue.TWO));
        myCards.add(new Card(Suit.CLUBS, CardValue.JACK));
        Card dealerCard = new Card(Suit.DIAMONDS, CardValue.TEN);
        FastShoe myShoe = new FastShoe(theRules.getNumberOfDecks());
        aState = new State(myCards, dealerCard);
        myShoe.fastDrawSpecific(CardValue.TWO);
        myShoe.fastDrawSpecific(CardValue.JACK);
        myShoe.fastDrawSpecific(CardValue.TEN);
        Answer anotherCopy = null;
        try {
            aState = Blackjack.PlayerRecursive(myShoe, aState, theRules);
            Answer anAnswer = new Answer(aState);
            assert (anAnswer.getBestAction() == Action.HIT);
            assert (anAnswer.getSecondBestAction() == Action.SURRENDER); //I'd think
            assert (anAnswer.getDealerCard() == CardValue.TEN);
            assert (anAnswer.getFirstPlayerCard() == CardValue.TWO);
            assert (anAnswer.getSecondPlayerCard() == CardValue.TEN);
            Answer sameAnswer = new Answer(aState);
            assert (anAnswer.equals(sameAnswer));
            assert (anAnswer.isComplete() == sameAnswer.isComplete());
            assert (anAnswer.myHashKey() == sameAnswer.myHashKey());
            /*
            System.out.println(anAnswer.hashCode() + ": this.hashCode().\n "
            + Answer.answerHash(
            Answer.cardValueToByte(anAnswer.getFirstPlayerCard()),
            Answer.cardValueToByte(anAnswer.getSecondPlayerCard()),
            Answer.cardValueToByte(anAnswer.getDealerCard()),
            false) + ": Answer.answerHash().\n" + aState.getAnswerHash(false)
            + ": State.getAnswerHash()."); */
            assert (aState.getAnswerHash(false) == anAnswer.myHashKey());
            try {
                Answer someAnswer = new Answer(Strategy.dummyByte, CardValue.TWO, CardValue.JACK, CardValue.QUEEN);
                assert false : "Answer constructor failed.";
            }
            catch (IOException ioe) {
            }
            final byte answerConsolidated = anAnswer.getConsolidatedActions();
            assert (answerConsolidated == 3);
            Answer copyOfAnswer = new Answer(answerConsolidated, CardValue.TWO, CardValue.JACK, CardValue.QUEEN);
            assert (copyOfAnswer.getBestAction() == Action.HIT);
            assert (copyOfAnswer.getSecondBestAction() == Action.SURRENDER); //I'd think
            assert (copyOfAnswer.getDealerCard() == CardValue.TEN);
            assert (copyOfAnswer.getFirstPlayerCard() == CardValue.TWO);
            assert (copyOfAnswer.myHashKey() == sameAnswer.myHashKey());
            assert (copyOfAnswer.isComplete() == false);
            assert (copyOfAnswer.getSecondPlayerCard() == CardValue.TEN);
            anotherCopy = new Answer((byte) 19, CardValue.TWO, CardValue.JACK, CardValue.QUEEN);
            assert (anotherCopy.getBestAction() == Action.SURRENDER) : anotherCopy.getBestAction();
            assert (anotherCopy.getSecondBestAction() == Action.STAND) : anotherCopy.getSecondBestAction();
            assert (anotherCopy.getDealerCard() == CardValue.TEN);
            assert (anotherCopy.getFirstPlayerCard() == CardValue.TWO);
            assert (anotherCopy.getSecondPlayerCard() == CardValue.TEN);
            assert (anotherCopy.myHashKey() == sameAnswer.myHashKey());
            assert (anotherCopy.isComplete() == false);
            //best action * 6 + secondbestaction
        }
        catch (NoRecommendationException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        catch (IOException io) {
            io.printStackTrace();
            throw new RuntimeException(io);
        }
        //anAnswer = new Answer(
    }
    
}
