package ramblingharmonicas.blackjack;

import java.io.IOException;
import java.io.Serializable;
import ramblingharmonicas.blackjack.cards.*;

/**
 * This should be package-private. Strategy is the go-between between Answers
 * and the outside world.
 * The two ways I've chosen to save this to file: direct object serialization --
 * which has far too much overhead, so is not currently being used --
 * or converting this class into a single byte, by
 * dropping the EV data, putting together the actions, and having the location
 * of the data encode the cards being stored.
 * TODO: Deprecate the first method (direct object serialization) and add on
 * a database
 */
class Answer implements Serializable {
private final byte dealerCard, firstPlayerCard, secondPlayerCard;
private final byte bestAction, secondBestAction;
private final float bestEV, secondBestEV;
/**
 * A complete answer is one that has a solved bestEV and secondBestEV.
 * An incomplete answer has the default bestEV and secondBestEV (-1000),
 * because it's been recreated from a file.
 *
 */
private final transient boolean complete;
static private final transient long serialVersionUID = 1001L;

/**
 * @param aCardValue
 * @return
 */
static protected byte cardValueToByte(CardValue aCardValue) {
   return (byte) (aCardValue.value());
}

/**
 * This simply calls CardValue.cardValueFromInt(byte). It has the same
 * return value as well, except that 11 is considered an invalid number.
 *
 * @param myByte converts this to a CardValue. Must be a number between
 * 1 and 10.
 * @return
 */
static protected CardValue byteToCardValue(byte myByte) {
   if (myByte >= 11) {
      throw new IllegalArgumentException("Invalid byte: " + myByte);
   }
   return CardValue.cardValueFromInt(myByte);
}

protected CardValue getDealerCard() {
   return byteToCardValue(dealerCard);
}

protected CardValue getFirstPlayerCard() {
   return byteToCardValue(firstPlayerCard);
}

protected CardValue getSecondPlayerCard() {
   return byteToCardValue(secondPlayerCard);
}

protected Action getBestAction() {
   return byteToAction(bestAction);
}

protected Action getSecondBestAction() {
   return byteToAction(secondBestAction);
}

/**
 * Only can be used on complete answers (those which have a bestEV and
 * secondBestEV set). See notes on variable "complete."
 * Throws an exception otherwise.
 */
protected float getBestEV() {
   if (complete) {
      return bestEV;
   }
   assert false : 
           "Attempted to pull the best EV from a non-complete Answer.";
   return -1000;
   
}

/**
 * Only can be used on complete answers (those which have a bestEV and
 * secondBestEV set). See notes on variable "complete."
 * Throws an exception otherwise.
 *
 */
protected float getSecondBestEV() {
   if (complete) {
      return secondBestEV;
   }
   assert false :
           "Attempted to pull the best EV from a non-complete Answer.";
   return -1000;
}

/**
 * Changing these values involves at least changing one of the constructors,
 * if not other parts of the code as well.
 * Valid for HIT, STAND, DOUBLE, SURRENDER, and SPLIT.
 * Asserts false for insurance since it should never be stored here (it's not
 * part of basic strategy)
 *
 * Throws an exception on Action.ERROR.
 *
 * @param anAction
 * @return
 */
static private byte actionToByte(Action anAction) {
   switch (anAction) {
      case HIT:
         return 0;
      case STAND:
         return 1;
      case DOUBLE:
         return 2;
      case SURRENDER:
         return 3;
      case SPLIT:
         return 4;
      case INSURANCE:
         assert false : "Insurance is never part of basic strategy.";
         return 5;
   }

   throw new IllegalStateException();
}

/**
 * Throws exception if the byte can't be correctly translated
 * to an action.
 *
 * @param myByte
 */
static private Action byteToAction(byte myByte) {
   switch (myByte) {
      case 0:
         return Action.HIT;
      case 1:
         return Action.STAND;
      case 2:
         return Action.DOUBLE;
      case 3:
         return Action.SURRENDER;
      case 4:
         return Action.SPLIT;
      case 5:
         assert false : "Insurance is never part of basic strategy";
         return Action.INSURANCE;
      default:
         //What if the file is corrupted?
         assert false : "Action.ERROR is never part of basic strategy";
         return Action.ERROR;
   }
}

/**
 * Convenience function for answerHash.
 *
 *
 * @return The hash key for this answer.
 *
 */
int myHashKey() {
   if ((bestAction == 4) || (secondBestAction == 4)) {
      try {
         return answerHash(firstPlayerCard, secondPlayerCard, dealerCard, true);
      }
      catch (IllegalArgumentException q) {
         return answerHash(firstPlayerCard, secondPlayerCard, dealerCard, false);
      }
   }
   else {
      return answerHash(firstPlayerCard, secondPlayerCard, dealerCard, false);
   }

}

@Deprecated
@Override
public int hashCode() //If this contains a split, it'll do a split hash; otherwise, a regular hash.
{ ////4 means split.
   return myHashKey();
}

/**
 * Needs testing -- it had failed at least one test at one point.
 *
 * Not currently used in any code other than testing code (see
 * Testers.testConstructors). Would be nice to have this tested and
 * available for testing purposes.
 *
 * @param obj Object to compare to
 * @return True if they're the same, false otherwise. The player card order can
 * be reversed.
 *
 *
 */
@Override
public boolean equals(Object obj) {
   if (obj == null) {
      return false;
   }
   if (getClass() != obj.getClass()) {
      return false;
   }
   final Answer other = (Answer) obj;
   if (other == this) {
      return true;
   }
   if (this.complete != other.isComplete()) {
      return false;
   }

   if (this.getDealerCard() != other.getDealerCard()) {
      return false;
   }
   if (this.getFirstPlayerCard() != other.getFirstPlayerCard()) {
      if (this.getFirstPlayerCard() != other.getSecondPlayerCard()) {
         return false;
      }
      else {
         if (this.getSecondPlayerCard() != other.getFirstPlayerCard()) {
            return false;
         }
         else {
            return true;
         }
      }
   }
   if (this.getSecondPlayerCard() != other.getSecondPlayerCard()) {
      return false;
   }
   if ((this.getBestAction() != other.getBestAction())
           || (this.getSecondBestAction() != other.getSecondBestAction())) {
      return false;
   }
   //Now we need float number comparisons.

   if (this.complete == false) //Don't need to compare EVs, they're both empty
   {
      return true;
   }


   final float otherBestEV = other.getBestEV();
   if ((otherBestEV < (this.bestEV * (1 + Constants.EPSILON)))
           && (otherBestEV > (this.bestEV * (1 - Constants.EPSILON)))) {
      return false;
   }

   final float otherSecondBestEV = other.getSecondBestEV();
   if ((otherSecondBestEV < (this.secondBestEV * 1.01))
           && (otherSecondBestEV > (this.secondBestEV * 0.99))) {
      return false;
   }


   return true;
}

/**
 * UNTESTED?
 * JavaDocs need verification -- Does the map have an entry for hands
 * where splitting is possible, but not recommended as either the first
 * or second action?
 * This returns the key to be used in the hash map. Every hand that can
 * be split actually has two separate keys. However, just because it can
 * be split doesn't mean there's a split entry for it in the map -- no entry
 * will be created if splitting was neither the first best nor second best
 * action.
 *
 * This function could fail if the bytes used to represent the cards have a
 * value of 0.
 *
 * @param firstPlayer
 * @param secondPlayer
 * @param dealerCard
 * @throws IllegalArgumentException if you're asking for a split answer on a
 * non-split hand
 * @return
 */
static protected int answerHash(byte firstPlayer, byte secondPlayer,
        byte dealerCard, boolean splitRecommended) {
   if (splitRecommended) {
      if (firstPlayer == secondPlayer) {
         return (int) dealerCard + 100000 * ((int) firstPlayer);
      }
      throw new IllegalArgumentException("answerHash called with a recommended "
      + "split, despite the fact that\n "
      + "the player can't split becuase his cards are different.");
   }

//The order of the player cards does not matter to the caller. So I should
//always put them
//in the same order, here, so I always get the same result. Highest card second.
   if (firstPlayer <= secondPlayer) {
      return (int) dealerCard + 100 * (int) firstPlayer + 10000 * (int) secondPlayer;
   }
   else {
      return (int) dealerCard + 100 * (int) secondPlayer + 10000 * (int) firstPlayer;
   }
}

/**
 * UNTESTED. Currently unused.
 * Should work even with multiple cards in hand.
 *
 * Convenience function for Answer(byte consolidatedAction,
 * CardValue firstPlayerCard, CardValue secondPlayerCard,
 * CardValue dealerCard). (1-line function.)
 *
 * @param bestAction
 * @param secondBestAction
 * @param aState
 * @throws IOException (Unlikely)
 *
 */
Answer(Action bestAction, Action secondBestAction, State aState) throws
        IOException {
   this(getConsolidatedActions(Answer.actionToByte(bestAction),
           Answer.actionToByte(secondBestAction)),
           Answer.byteToCardValue(State.getEffectiveCard(aState, 1)),
           Answer.byteToCardValue(State.getEffectiveCard(aState, 2)),
           Answer.byteToCardValue(State.getEffectiveCard(aState, 0)));
}

/**
 * Convenience function for Answer(byte consolidatedAction,
 * CardValue firstPlayerCard, CardValue secondPlayerCard,
 * CardValue dealerCard). (1-line function.)
 *
 * @param bestAction
 * @param secondBestAction
 * @param firstPlayerCard
 * @param secondPlayerCard
 * @param dealerCard
 * @throws IOException
 */
Answer(Action bestAction, Action secondBestAction, CardValue firstPlayerCard,
        CardValue secondPlayerCard,
        CardValue dealerCard) throws IOException {
   this(getConsolidatedActions(Answer.actionToByte(bestAction),
           Answer.actionToByte(secondBestAction)),
           firstPlayerCard, secondPlayerCard, dealerCard);
}

/**
 * Constructor used to reconstruct Answer from raw file data; sets EVs to -1000.
 * Two other constructors also redirect here.
 * UNTESTED.
 */
Answer(byte consolidatedAction, CardValue firstPlayerCard,
        CardValue secondPlayerCard, CardValue dealerCard) throws IOException {
   this.complete = false;
   this.bestEV = this.secondBestEV = -1000;
   this.firstPlayerCard = Answer.cardValueToByte(firstPlayerCard);
   this.secondPlayerCard = Answer.cardValueToByte(secondPlayerCard);
   this.dealerCard = Answer.cardValueToByte(dealerCard);
   if ((consolidatedAction < 0) || (consolidatedAction > 35)) {
      throw new IOException("Error in reconstructing Answer from byte "
              + consolidatedAction + " -- expected values are between 0 and 35."
              + " Dummybyte is " + Strategy.dummyByte);
   }
   this.secondBestAction = (byte) (consolidatedAction % 6);
   this.bestAction = (byte) ((consolidatedAction - this.secondBestAction) / 6);
   if ((this.bestAction == this.secondBestAction) && (!hasBlackjack())) {
      System.err.println("Error in Answer constructor -- suggested actions are"
              + " identical.");
      throw new IOException("Error in reconstructing Answer -- suggested actions"
              + " are identical:"
              + byteToAction(this.bestAction));
   }
}

/**
 * Note the use of the converter functions (byteToCardValue). The conversion
 * should always be done by these functions to save work later on -- beats
 * having
 * to change multiple functions if I need to change the values again.
 *
 * @return
 */
private boolean hasBlackjack() {
   if ((CardValue.ACE == byteToCardValue(this.firstPlayerCard))
           && (CardValue.TEN == byteToCardValue(this.secondPlayerCard))) {
      return true;
   }
   else if ((CardValue.TEN == byteToCardValue(this.firstPlayerCard))
           && (CardValue.ACE == byteToCardValue(this.secondPlayerCard))) {
      return true;
   }
   else {
      return false;
   }
}

/**
 * Used to succinctly serialize an Answer into one byte. Used
 * by Strategy.
 * TODO: Test this function
 * 
 * @return
 */
byte getConsolidatedActions() {
   if ((this.bestAction == this.secondBestAction) && !hasBlackjack()) {
      System.err.print(this);
      throw new RuntimeException("Actions identical during save.");
   }

   return getConsolidatedActions(this.bestAction, this.secondBestAction);
}

/**
 * This function needs to be changed if the coding for Actions changes.
 *
 *
 * @param bestAction
 * @param secondBestAction
 * @return
 */
private static byte getConsolidatedActions(byte bestAction,
        byte secondBestAction) {
   return (byte) (bestAction * 6 + secondBestAction);
}

boolean isComplete() {
   return complete;
}

protected Answer(State aState, boolean consolidated) {
   this.bestAction = actionToByte(aState.getPreferredAction());
   this.secondBestAction = actionToByte(aState.getSecondBestAction());
   this.bestEV = (float) aState.getExpectedValue();
   this.secondBestEV = (float) aState.getSecondBestEV();
   this.firstPlayerCard = cardValueToByte(aState.getFirstCard().getCardValue());
   this.secondPlayerCard = cardValueToByte(aState.getSecondCard().getCardValue());
   this.dealerCard = cardValueToByte(aState.getDealerUpCard().getCardValue());
   this.complete = true;


   if ((aState.getPreferredAction() == aState.getSecondBestAction())
           && (!aState.playerBJ())) {
      State.printStateStatus(aState, "Error in answer constructor: top "
              + "two actions are the same.");
      assert false;
   }

   if (!checkValidEV(consolidated)) {
      State.printStateStatus(aState, "Error in Answer constructor - "
              + "EV is incorrect.");
      assert false;
   }
}

protected Answer(State aState) {
   this(aState, false);
}

/**
 * Do not call me on incomplete answers -- I'll assert false and return false.
 * Helper function for State constructor.
 *
 *
 * @return
 */
private boolean checkValidEV(boolean consolidated) {
   if (((bestEV < -10) || (bestEV > 10)
           || (secondBestEV < -10) || (secondBestEV > 10))
           || ((!consolidated) && (secondBestEV > bestEV))) {

      assert !((bestEV < -10) || (bestEV > 10) || (secondBestEV > bestEV)
              || (secondBestEV < -10) || (secondBestEV > 10));
      return false;

   }
   return true;
}

@Override
public String toString() {
   String ln = System.getProperty("line.separator");
   StringBuilder s = new StringBuilder();
   s.append("This answer ");
   if (complete) {
      s.append("is complete: ").append(ln);
   }
   else {
      s.append(" is not complete: ").append(ln);
   }

   s.append("Dealer card: ").append(getDealerCard()).append(". "
           + "Player cards: ").append(
           getFirstPlayerCard()).append(" and ").append(getSecondPlayerCard())
           .append(", recommended" + " action of ").append(getBestAction())
           .append(", with an EV of ")
           .append((String.valueOf(bestEV)));
   s.append(".").append(ln).append(" Second best action is to ")
           .append(getSecondBestAction()).append(" with an EV of ")
           .append(String.valueOf(secondBestEV));

   s.append(ln).append("My code is ").append(myHashKey());
   return s.toString();
}

}
