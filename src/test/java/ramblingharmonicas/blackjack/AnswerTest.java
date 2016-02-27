package ramblingharmonicas.blackjack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import ramblingharmonicas.blackjack.cards.Card;
import ramblingharmonicas.blackjack.cards.CardValue;
import ramblingharmonicas.blackjack.cards.Suit;

public class AnswerTest {


@Rule
public ExpectedException thrown= ExpectedException.none();
/**
 * This function relies on the exact values of the byte when CardValues are
 * converted to bytes. If those values change this function will probably throw an
 * exception.
 */
@Test
public void testAnswerHash() {
    Set<Integer> answerHashes = new TreeSet<Integer>();
    boolean splitPossible;
    byte first;
    byte second;
    byte dealer;
    for (first = 1; first < 11; first++) {
        for (second = first; second < 11; second++) {
            for (dealer = 1; dealer < 11; dealer++) {
                assert (answerHashes.add(Answer.answerHash(first, second, dealer, false))):
                       "Answer hash error on bytes: First card = " + first + ", second card = " 
                        + second + ", dealer card = " + dealer + "." + "Splitting not recommended."
                     + "My answer hash is: " + Answer.answerHash(first, second, dealer, false);
                splitPossible = (first == second);
                if (splitPossible) {
                    assert (answerHashes.add(Answer.answerHash(first, second, dealer, true))):
                        "Answer hash error on bytes: First card = " + first + ", second card = " 
                        + second + ", dealer card = " + dealer + ". Splitting recommended." + 
                        "My answer hash is: " + Answer.answerHash(first, second, dealer, false);
                }
            }
        }
    }
}

@Test
public void testCardValueToByte() {
    CardValue aCardValue = CardValue.ACE;
    byte expResult = 1;
    byte result = Answer.cardValueToByte(aCardValue);
    assertEquals(expResult, result);
}

@Test
public void testByteToCardValue() {
    byte myByte = 1;
    CardValue expResult = CardValue.ACE;
    CardValue result = Answer.byteToCardValue(myByte);
    assertEquals(expResult, result);
}

@Test(expected=IOException.class)
public void testConstructorIOException() throws IOException {
    Answer someAnswer = new Answer(Strategy.dummyByte, CardValue.TWO,
            CardValue.JACK, CardValue.QUEEN, false);
}

//TODO: Split these tests up
@Test
public void testConstructors() throws IOException {
    CardValue firstCard = CardValue.TWO, secondCard = CardValue.JACK,
            //JACK will be converted to TEN by Answer
            dealerCard = CardValue.TEN;
    State aState = new State(firstCard, secondCard, dealerCard);
    //TODO Refactor State and Answer -- they are far too intertwined.
    
    Answer anotherCopy;
    Answer anAnswer = new Answer(true, 0.5F,0.5F, firstCard, secondCard, dealerCard, 
            Action.HIT, Action.SURRENDER);
    assert (anAnswer.getBestAction() == Action.HIT);
    assert (anAnswer.getSecondBestAction() == Action.SURRENDER); 
    assert (anAnswer.getDealerCard() == dealerCard);
    assert (anAnswer.getFirstPlayerCard() == firstCard);
    assert (anAnswer.getSecondPlayerCard() == CardValue.TEN);
    
    Answer sameAnswer = new Answer(true, 0.5F,0.5F, firstCard, secondCard, dealerCard, 
            Action.HIT, Action.SURRENDER);
    assert (anAnswer.equals(sameAnswer));
    assert (sameAnswer.equals(anAnswer));
    assert (anAnswer.isComplete() == sameAnswer.isComplete());
    assert (anAnswer.myHashKey() == sameAnswer.myHashKey());
    assert (aState.getAnswerHash(false) == anAnswer.myHashKey());
    final byte answerConsolidated = anAnswer.getConsolidatedActions();
    assert (answerConsolidated == 3);
    
    Answer copyOfAnswer = new Answer(answerConsolidated, CardValue.TWO, 
            CardValue.JACK, CardValue.QUEEN, false);
    assert (copyOfAnswer.getBestAction() == Action.HIT);
    assert (copyOfAnswer.getSecondBestAction() == Action.SURRENDER);
    assert (copyOfAnswer.getDealerCard() == dealerCard);
    assert (copyOfAnswer.getFirstPlayerCard() == firstCard);
    assert (copyOfAnswer.myHashKey() == sameAnswer.myHashKey());
    assert (copyOfAnswer.isComplete() == false);
    assert (copyOfAnswer.getSecondPlayerCard() == CardValue.TEN);
    //best action * 6 + secondbestaction
    
    anotherCopy = new Answer((byte) 19, CardValue.TWO, CardValue.JACK, CardValue.QUEEN, false);
    assert (anotherCopy.getBestAction() == Action.SURRENDER) : 
            anotherCopy.getBestAction();
    assert (anotherCopy.getSecondBestAction() == Action.STAND) : 
            anotherCopy.getSecondBestAction();
    assert (anotherCopy.getDealerCard() == dealerCard);
    assert (anotherCopy.getFirstPlayerCard() == firstCard);
    assert (anotherCopy.getSecondPlayerCard() == CardValue.TEN);
    assert (anotherCopy.myHashKey() == sameAnswer.myHashKey());
    assert (anotherCopy.isComplete() == false);
}
    
}
