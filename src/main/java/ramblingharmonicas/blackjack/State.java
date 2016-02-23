package ramblingharmonicas.blackjack;

import java.util.*;
import ramblingharmonicas.blackjack.cards.*;
/**  *
 *
 * Constructors:
 *
 * Tracks the current state of the game, up to the point when the player
 * is done. (Excluding dealer blackjack in hole-card games, this does not store,
 * track, or solve for the dealer's hand. See the Utilities class for that.)
 * When the dealer's hand has been solved, call State.calculateEV with the
 * correct information, and it will set the expected value for this State.
 * Alternatively, getHandResult can be used to get the per-hand result, when the player has
 * multiple hands.
 *
 */
public class State {
/**
 * Maximum allowable hands.
 * TODO: Default this to 100 so that clients can use it safely, but make sure that 
 * usedForCalculation is called before any calculations are done.
 */
static private int MAX_NUMBER_HANDS = 3;

//TODO: Make these an enum.
static public final int PUSH = 10;
static public final int SURRENDER = 11;
static public final int WIN = 12;
static public final int LOSE = 13;
static public final int BLACKJACK = 14;
/**
 * TODO: Finish optimization of State.
 * If usedForCalculation is true, it should
 * set a variety of options -- specifically, State should use no ArrayLists/enums,
 * switching to plain arrays and constants. Optimize being true should also disable
 * all functions which use those (have them all throw errors). This requires a separate
 * set of functions which take/use arrays and enums.
 * @param usedForCalculation If true, this sets the max number of hands in
 * State to 3. If false, it sets the max number of hands to 20. Note this is
 * static.
 */
public static void usedForCalculation(final boolean usedForCalculation) {
   if (usedForCalculation) {
      MAX_NUMBER_HANDS = 3;
   }
   else {
      MAX_NUMBER_HANDS = 100;
   }
}

/**
 * @deprecated ...what is the point of this function
 */
public static void printStateStatus(State myState, String str) {
   System.out.println(str + myState.toString());
}

/**
 * Stores the cards in all of the player's hands
 *
 */
private ArrayList<ArrayList<Card>> myHands = new ArrayList<ArrayList<Card>>();
/**
 * A list of the actions the player has taken.
 *
 */
private ArrayList<ArrayList<Action>> playerActions = new ArrayList<ArrayList<Action>>();
/**
 * This is used for hands that split. It is the current
 * index of myHands and of playerActions. In other words, it's 0 when there are
 * no splits;
 * 1 if there's been 1 split and you're on the second hand, and so on.
 */
private int currentHand = 0;
/**
 * Total number of hands, minus one.
 *
 */
private int totalHands = 0;
private boolean insuranceAdvised;

/**
 * This function should never be called directly.
 * It should only be called via Strategy.insuranceGoodIdea.
 *
 * @param insuranceAdvised
 */
void setInsuranceAdvised(boolean insuranceAdvised) {   
   this.insuranceAdvised = insuranceAdvised;
}

private ArrayList<Boolean> handDone = new ArrayList<Boolean>();
/**
 * Stores whether or not a given hand is busted.
 *
 */
private boolean[] busted;
private Card dealerUpCard;
private boolean dealerBJChecked = false;
/**
 * When calculating the right Strategy, insurance is always taken when advised.
 * See Strategy function insuranceGoodIdea
 */
private boolean insuranceTaken = false;

/**
 * Strategy.insuranceGoodIdea tells you whether or not you should take
 * insurance. This function, despite its name, does not do so, unless you've already
 * ran that function.
 * TODO: Refactor to make this less confusing
 * @return
 */
public boolean isInsuranceAdvised() {
   return insuranceAdvised;
}

public boolean isInsuranceTaken() {
   return insuranceTaken;
}

private void setInsuranceTaken(boolean insuranceTaken) {
   this.insuranceTaken = insuranceTaken;
}

private double expectedValue = -100000000;
/**
 * Used by the calculation functions to store the best possible action to take.
 *
 */
private Action preferredAction = Action.ERROR;
/**
 * When the first best action is unavailable, do this.
 * In some circumstances -- player blackjack -- the only option will be to
 * stand.
 * In that case, this will stay at Action.ERROR, and it won't be an error.
 * In most cases, however, it would be an error for this to remain ERROR after
 * calculations have been finished.
 */
private Action secondBestAction = Action.ERROR;

/** This function can only be used if State.setDealerBlackjack has been called first.
 *
 * @throws IllegalStateException if the dealer blackjack status has not been set yet.
 */
public boolean dealerHasBJ() {
   if (dealerBJChecked == false) {
      throw new IllegalStateException("Dealer blackjack has not yet been checked.");
   }
   return dealerBJ;
}

private boolean dealerBJ;

/**
 *
 *
 * @return true if, in the current game state, the dealer has already checked
 * for blackjack.
 * False otherwise.
 */
public boolean dealerBlackJackChecked() {
   return dealerBJChecked;
}

/**
 * TODO: Test this function
 * Tells the State function that the dealer does or does not have a blackjack.
 * Ends play on all hands if the dealer does have a blackjack.
 *
 * @param blackJack True if the dealer does have blackjack. False otherwise.
 * @throws IllegalStateException if the dealer up card is not a 10-value card
 * or an ace and this function is told that the dealer does have a blackjack.
 *
 */
public boolean setDealerBlackjack(boolean blackJack) {
   if ((dealerUpCard.value() != CardValue.TEN.value())
           && (dealerUpCard.getCardValue() != CardValue.ACE)
           && (blackJack)) {
      throw new IllegalStateException("The dealer cannot have blackjack"
              + " if he has a " + dealerUpCard + " face up.");
   }

   dealerBJ = blackJack;
   dealerBJChecked = true;
   if (blackJack) {
      for (int i = 0; i < totalHands + 1; i++) {
         handDone.set(i, true);
      }

   }


   return blackJack;
}

/**
 * Tested once. Seems to contains a deep clone of all the hands played.
 * Not tested for a State having multiple hands.
 *
 * @return
 */
ArrayList<ArrayList<Card>> getMyHands() {
   ArrayList<ArrayList<Card>> clone = new ArrayList<ArrayList<Card>>();
   int i, j;

   for (i = 0; i < totalHands + 1; i++) {
      clone.add(new ArrayList<Card>()); 
      for (j = 0; j < myHands.get(i).size(); j++) {
         clone.get(i).add(new Card(myHands.get(i).get(j)));
      }
   }
   
   return clone;
}

/**
 * @return a deep clone of all actions taken by the player in all hands.
 */
ArrayList<ArrayList<Action>> getActions() {
   int i, j;
   ArrayList<ArrayList<Action>> clone = new ArrayList<ArrayList<Action>>();
   //ArrayList<Action> eachRow = new ArrayList<Action>();
   for (i = 0; i < totalHands + 1; i++) {
      clone.add(new ArrayList<Action>());
      for (j = 0; j < playerActions.get(i).size(); j++) {
         clone.get(i).add(Action.deepClone(playerActions.get(i).get(j)));
      }

   }

   return clone;
}

/**
 * Makes a deep clone of State. State getters return deep clones to prevent
 * data corruption.
 *
 * @param toBeCloned
 */
public State(State toBeCloned) {
   if (toBeCloned.numberCardsInHand() == 0) //Only checks cards in current hand.
   {
      printStateStatus(toBeCloned, "The state I'm trying to clone has 0 cards in it.");
      throw new IllegalArgumentException();

   }
   this.busted = toBeCloned.areWeBusted();
   this.currentHand = toBeCloned.getCurrentHand();
   this.dealerUpCard = toBeCloned.getDealerUpCard();
   this.dealerBJChecked = toBeCloned.dealerBlackJackChecked();
   if (dealerBJChecked) {
      this.dealerBJ = toBeCloned.dealerHasBJ();
   }
   this.expectedValue = toBeCloned.getExpectedValue();
   this.handDone = toBeCloned.areHandsDone();
   this.insuranceAdvised = toBeCloned.isInsuranceAdvised();
   setInsuranceTaken(toBeCloned.isInsuranceTaken());

   this.playerActions = toBeCloned.getActions();
   this.myHands = toBeCloned.getMyHands();
   this.totalHands = toBeCloned.getTotalHands();
   Action mypreference;
   try {
      this.preferredAction = toBeCloned.getPreferredAction();
   }
   catch (IllegalStateException q) {
   } //Hasn't been set, that's fine.
   this.secondBestAction = toBeCloned.getSecondBestAction();
   this.secondBestEV = toBeCloned.getSecondBestEV();

}

/**
 * This constructor is to be used for testing purposes, when the suit is
 * irrelevant.
 *
 * @param pCardOne First player CardValue
 * @param pCardTwo Second player CardValue
 * @param dCard Dealer CardValue
 */
public State(CardValue pCardOne, CardValue pCardTwo, CardValue dCard) {
   this(new Card(Suit.CLUBS, pCardOne), new Card(Suit.CLUBS, pCardTwo),
           new Card(Suit.HEARTS, dCard));
}

/**
 * Initializes a State.
 *
 * @param firstPlayerCard
 * @param secondPlayerCard
 * @param dealerCard
 */
public State(final Card firstPlayerCard, final Card secondPlayerCard,
        final Card dealerCard) {
   if ((firstPlayerCard == null) || (secondPlayerCard == null) || (dealerCard == null)) {
      throw new NullPointerException("State constructor called with null Cards");
   }

   ArrayList<Card> myHand = new ArrayList<Card>();
   myHand.add(firstPlayerCard);
   myHand.add(secondPlayerCard);
   setBaseState(myHand, dealerCard);
}

/**
 *
 * Throws exception if the initial hand size is not equal to 2 or if the
 * arguments are null.
 * Deprecated because other functions should not know -- and it's annoying
 * for them to have
 * to know -- about the internal representation of each hand.
 *
 * @param startingHand
 * @param dealerCard
 */
@Deprecated
public State(ArrayList<Card> startingHand, final Card dealerCard) {
   if ((startingHand == null) || (dealerCard == null)) {
      throw new NullPointerException();
   }
   if (startingHand.size() != 2) {
      throw new IllegalArgumentException();
   }

   setBaseState(startingHand, dealerCard);

}

/**
 * Should be called only from the constructor.
 *
 * @param originalCards
 * @param dealerCard
 */
private void setBaseState(ArrayList<Card> startingHand, final Card dealerCard) {
   ArrayList<Card> cloned = new ArrayList<Card>();
   for (Card i : startingHand) {
      cloned.add(new Card(i));
   }

   busted = new boolean[MAX_NUMBER_HANDS];

   myHands.add(cloned);
   this.dealerUpCard = new Card(dealerCard);
   playerActions.add(new ArrayList<Action>());
   handDone.add(false);
   busted[0] = false;

}

public Card getDealerUpCard() {
   return new Card(dealerUpCard);
}

/**
 * Returns true if the player's current hand total is over 21. Returns false
 * otherwise.
 *
 */
public boolean isBust() {
   return busted[currentHand];
}

/**
 * Returns true if the player's hand total is more than 21.(Aces count as one
 * here.)
 * If the player is bust, this also sets the internal busted array to true
 * for the current hand, and sets the current hand's done status to true.
 *
 */
private boolean amIBust() {
   int sum = 0;
   for (int j = 0; j < myHands.get(currentHand).size(); j++) {
      sum += myHands.get(currentHand).get(j).value();
   }
   if (sum > 21) {
      handDone.set(currentHand, true);
      busted[currentHand] = true;
      return true;
   }
   return false;

}

/**
 *
 * Returns hand value of current hand. Aces count 11 if they can, 1 if they
 * can't.
 */
public int handTotal() {
   return Utilities.handTotal(myHands.get(currentHand));
}

/**
 * Takes the specified action. May lead to strange results if the action is
 * impossible.
 * Before this is called, it should be verified that the action is indeed
 * possible -- call Rules.isPossible(Action, State) to check.
 *
 * Upon action completion, this will correctly marks the hand as done or not
 * done.
 *
 * Note that split actions stack. Meaning, if I have a hand, and I split it, and
 * I split it again,
 * the first two actions in that hand are listed as splits.
 *
 *
 * @throws IllegalStateException 
 * -If the player's hand is marked as being done, or
 * -If being told to take insurance, and:
 *
 * The player has 3+ cards in hand OR
 * The player has already split OR
 * The dealer has already checked for blackjack OR
 * Insurance has already been taken.
 *
 */
public void action(final Action myAction) {
   if (handDone.get(currentHand)) {
      throw new IllegalStateException("State.action(Action,) should not be "
              + " called if the current hand is already done.");
   }

   switch (myAction) {
      case STAND:
         playerActions.get(currentHand).add(Action.STAND);
         handDone.set(currentHand, true);
         break;
      case SURRENDER:
         playerActions.get(currentHand).add(Action.SURRENDER);
         handDone.set(currentHand, true);
         break;
      case SPLIT:
         if ((totalHands + 1) >= busted.length) {
            throw new IllegalStateException("The max number of hands is " + State.MAX_NUMBER_HANDS
                    + ", but I am being told to split. The length of the busted array is "
                    + busted.length + "This is my state: " + toString());
         }
         playerActions.get(currentHand).add(Action.SPLIT);
         //Create new hand
         totalHands++;
         playerActions.add(new ArrayList<Action>()); 

         myHands.add(new ArrayList<Card>());
         myHands.get(totalHands).add(myHands.get(currentHand).get(1));
         
         //Adds the second card to the newly created hand, at the end of the hand array.
         myHands.get(currentHand).remove(1);
         handDone.add(false);
         busted[totalHands] = false;
         break;
      case INSURANCE:
         if (insuranceTaken) {
            throw new IllegalStateException("State.action(action): "
                    + "Insurance has already been taken for this hand. State status:" + this);
         }
         if (dealerBJChecked) {
            throw new IllegalStateException("State.action(Action.INSURANCE):"
                    + "The dealer has already checked for blackjack this hand:" + this);
         }
         if (numberCardsInHand() != 2) {
            throw new IllegalStateException("State.action(Action.INSURANCE):"
                    + "The player has more than two cards in hand:" + this);
         }
         if (myHands.size() > 1) {
            throw new IllegalStateException("State.action(Action.INSURANCE):"
                    + "The player cannot buy insurance after having split:" + this);
         }
         setInsuranceTaken(true);
         break;
      default:
         throw new IllegalArgumentException("State.action(Action) called with invalid action: " 
                 + myAction);
   }

}

/**
 * Makes the players hit or double by adding that action and drawing the
 * specified card.
 *
 * @throws IllegalArgumentException if it's called with an Action other than
 * double or hit.
 * @throws IllegalStateException if it's called when the current hand is marked
 * as being done.
 * This task (checking if the hand is done) should be done by whichever function
 * called it.
 *
 * @param myAction Adds this action to the action list of the current hand.
 * @param myCard Adds this card to the current hand.
 *
 */
public void action(final Action myAction, final Card myCard) {
   if ((myAction != Action.DOUBLE)
           && (myAction != Action.HIT)) {
      throw new IllegalArgumentException("State.action(Action, Card) can"
              + " only be called with the hit and double actions.");
   }
   if (handDone.get(currentHand)) {
      throw new IllegalStateException("State.action(Action, Card) should not be "
              + " called if the current hand is already done.");
   }
   playerActions.get(currentHand).add(myAction);
   myHands.get(currentHand).add(new Card(myCard));
   if (amIBust() == true) //Also sets the done status and busted array correctly
   {
      return;
   }

   if (myAction == Action.DOUBLE) {
      handDone.set(currentHand, Boolean.TRUE); //After you double, the hand is done.
   }

}

/**
 * Makes the players hit or double by adding that action and drawing the
 * specified card
 * from the specified shoe.
 *
 * @throws IllegalArgumentException if it's called with an Action other than
 * double or hit.
 * @throws IllegalStateException if it's called when the current hand is marked
 * as being done.
 * This task (checking if the hand is done) should be done by whichever function
 * called it.
 *
 * TODO: This function should really just call action(Action, Card) to avoid code
 * duplication.
 *
 * @param myAction Adds this action to the action list of the current hand.
 * @param myCard Adds this card to the current hand.
 *
 */
Card action(final Action myAction, final Card myCard, FastShoe myShoe) {
   if ((myAction != Action.DOUBLE)
           && (myAction != Action.HIT)) {
      throw new IllegalArgumentException("State.action(Action, Card) can"
              + " only be called with the hit and double actions.");
   }
   if (handDone.get(currentHand)) {
      throw new IllegalStateException("State.action(Action, Card) should not be "
              + " called if the current hand is already done.");
   }

   myShoe.fasterDrawSpecific(myCard.getCardValue());

   playerActions.get(currentHand).add(Action.deepClone(myAction));
   myHands.get(currentHand).add(new Card(myCard));
   if (amIBust() == true) {
      return myCard;
   }

   if (myAction == Action.DOUBLE) {
      handDone.set(currentHand, Boolean.TRUE);
   }
   return myCard;

}

/**
 * Adds the specified card to the current hand.
 *
 * @throws IllegalStateException if the player is not starting a split hand
 * or if the player has more than 1 card in hand.
 *
 *
 * @param myCard Adds this card to hand after a split action.
 */
public void postSplitDraw(Card myCard) {
   if (playerActions.isEmpty()) //Covers my call to lastAction below.
   {
      throw new IllegalStateException("State.postSplitDraw(Card) called when the Action array is empty.");
   }
   if (!testPostSplit()) {
      throw new IllegalStateException("State.postSplitDraw(Card) called when the last action was not a split.");
   }
   if (myHands.get(currentHand).size() != 1) {
      throw new IllegalStateException("State.postSplitDraw(Card) called when the current hand has more than 1 card in it.");
   }

   myHands.get(currentHand).add(myCard);
}

/**
 * *
 * Note that the second and subsequent hands do NOT have a split listed as the
 * first action.
 * This version of the function is NOT called in PRecursive, I reckon.
 * TODO: Add test for this if not present
 */
void postSplitDraw(Card myCard, FastShoe myShoe) {
   postSplitDraw(myCard);
   myShoe.fasterDrawSpecific(myCard.getCardValue());
}

/**
 * Gets the index of the current hand (the one the player is currently playing.)
 * If all hands are done, this value is indeterminate.
 *
 */
public int getCurrentHand() {
   return currentHand; 
}

/** TODO: Fix this, it's unnecessarily confusing. (It should return the total number of hands.)
 * @return The total number of player hands, minus one. For example, if the
 * player has one hand,
 * this will return zero.
 */
public int getTotalHands() {
   return totalHands;
}

/**
 * @return Number of cards in current hand.
 */
public int numberCardsInHand() {
   return myHands.get(currentHand).size();
}

/**
 * This is set only by a calculateEV function, after all the player's hands
 * and the dealer's hand (if necessary) have been dealt. It is the total value
 * for all hands. 0 indicates the player neither lost nor gained money; 1.5
 * indicates
 * that the player earned 1.5 times his original bet amount; -2 indicates that
 * the player
 * lost 2 times his original bet amount. To get a per-hand breakdown, use
 * getHandResult
 * with a valid handIndex (0 = first hand, 1 = second hand, etc.).
 *
 * @return A large negative number when this value has not been solved for.
 *
 *
 */
public double getExpectedValue() {
   return expectedValue;
}

/**
 * The expected value of this hand when the course of action followed by
 * SecondBestAction
 * has been taken.
 */
double getSecondBestEV() {
   return secondBestEV;
}

/**
 * @return Deep clone of busted array.
 */
public boolean[] areWeBusted() {
   boolean[] copy = new boolean[MAX_NUMBER_HANDS];
   System.arraycopy(busted, 0, copy, 0, MAX_NUMBER_HANDS);
   return copy;
}

/**
 *
 *
 * @return
 * @deprecated This is now a simply array, not an arraylist.
 */
@Deprecated
public ArrayList<Boolean> areHandsDone() {
   ArrayList<Boolean> clone = new ArrayList<Boolean>();
   for (int i = 0; i < totalHands + 1; i++) {
      clone.add((Boolean) (boolean) handDone.get(i)); 
   }  
   return clone;
}

/**
 *
 * @return whether or not hand is done -- was the last action a stand, double,
 * or surrender? Is the player bust? If so, this returns true.
 * This function should be used to figure out what is happening and then plan
 * for it.
 * When a hand is done, nextHand() should be called, in case the player has
 * split.
 *
 */
boolean isHandDone() {
   return handDone.get(currentHand);
}

/**
 * @return The last action taken in current hand. Returns null if no
 * action was taken.
 */
public Action lastAction() {
   if (playerActions.get(currentHand).isEmpty()) {
      return null;
   }
   return playerActions.get(currentHand).get((playerActions.get(currentHand).size() - 1));
}

/**
 *
 * @param handIndex 0 for the 1st hand, 1 for the 2nd etc.
 * @return Last action taken in the hand referred to by handIndex. Null if no
 * action was taken.
 * @throws IllegalArgumentException if handIndex is negative, or refers to a
 * hand that doesn't exist (is greater than totalHands)
 */
public Action lastAction(int handIndex) {
   if ((handIndex > totalHands) || (handIndex < 0)) {
      throw new IllegalArgumentException("State.lastAction: handIndex is not valid: " + handIndex);
   }
   final int lastElemIndex = playerActions.get(handIndex).size() - 1;
   if (lastElemIndex == -1) {
      return null;
   }
   
   return playerActions.get(handIndex).get(lastElemIndex);
   
}

/**
 * @return True if the player's current hand has the specified card value in it.
 */
boolean contains(final CardValue aCard) {
   for (int i = 0; i < myHands.get(currentHand).size(); i++) {
      if (myHands.get(currentHand).get(i).value() == aCard.value()) {
         return true;
      }
   }
   return false;
}

/**
 *
 * @param myCard CardValue to be tested.
 * @return True if the first card is equal to the passed CardValue, false
 * otherwise.
 *
 */
boolean firstCardIs(final CardValue myCardValue) {
   if (myCardValue.value() == myHands.get(currentHand).get(0).value()) {
      return true;
   }
   return false;
}

public Card getFirstCard() {
   return new Card(myHands.get(currentHand).get(0));
}

public Card getSecondCard() {
   return new Card(myHands.get(currentHand).get(1));
}

public CardValue getFirstCardValue() {
   if (myHands.get(currentHand).size() == 0) {
      printStateStatus(this, "I have no cards in hand -- this should be impossible.");
      throw new IllegalStateException();
   }

   return myHands.get(currentHand).get(0).getCardValue();
}

public CardValue getSecondCardValue() {
   if (myHands.get(currentHand).size() < 2) {
      printStateStatus(this, "I have no cards in hand -- this should be impossible.");
      throw new IllegalStateException();
   }
   return myHands.get(currentHand).get(1).getCardValue();
}

/**
 * @exception IllegalStateException if preferredAction has not been set yet.
 */
Action getPreferredAction() {
   if (preferredAction == null) {
      throw new IllegalStateException("The preferred action has not "
              + "been set yet, but State.getPreferredAction() was called.");
   }

   return preferredAction;
}

/**
 * Untested. Dubious.
 *
 *
 * @param bestAction
 */
protected void setPreferredAction(Action bestAction) {
   preferredAction = bestAction;
}

protected void setSecondBestAction(Action secondBestAction) {
   this.secondBestAction = secondBestAction;

}

Action getSecondBestAction() {
   return secondBestAction;
}

private double secondBestEV = -50000;

void setSecondBestEV(double secondBestEV) {
   this.secondBestEV = secondBestEV;

}

/**
 * Should only be called after a split hand has done its last action.
 * @return False if there's no next hand.
 */
public boolean nextHand() {
   if (currentHand == totalHands) {
      return false;
   }

   currentHand++;
   return true;
}

/**
 * Should be used solely to evaluate the total EV when the player is finished.
 */
protected void resetCurrentHand() {
   for (int i = 0; i < totalHands + 1; i++) {
      if (!handDone.get(i)) {
         throw new IllegalStateException("Function State.resetCurrentHand called before all "
                 + "hands were finished.");
      }
   }
   currentHand = 0;

}

/**
 * Untested
 *
 * @param number New value of expected value
 * @throws errors if
 */
protected void setEV(double number) {
   if (expectedValue > -450) {
      throw new IllegalStateException("State.setEV(double) reports that someone has already set eV to "
              + "something other than its original state.");
   }
   assert !(number > 50) : "expected Value is not over 50, that's impossible.";

   expectedValue = number;
}

/**
 * Used to overwrite EV. Called by approx split function.
 * TODO: Is there a more elegant way to do this?
 *
 * @param number
 */
protected void overWriteEV(double number) {
   assert !((number > 50) || (number < -50)) :
           "expected Value is not over  or less than 50, that's impossible:" + number;

   expectedValue = number;
}

/**
 * @return True if all hands have been played; false otherwise.
 */
public boolean allDone() {
   for (int i = 0; i < totalHands + 1; i++) {
      if (!handDone.get(i)) {
         return false;
      }
   }

   return true;
}

/**
 * Calculates the EV.
 *
 * Do NOT call me when the dealer has some kind of hand (other than blackjack).
 * Do NOT call me in a no-hole-card game with dealer blackjack, unless the
 * blackjack is irrelevant
 * because you've lost all your hands via some other method.
 * Call me when:
 * -All hands are finished and either bust or surrendered,
 * -Or you have a blackjack in a hole card game.
 * -Assumes that, in a no-hole-card game, you did NOT buy insurance.
 *
 * At the end, currentHand will be the same as totalHands.
 *
 * Needs serious testing.
 */
public void calculateEV(Rules theRules) {
   resetCurrentHand(); //Checks for done status, too.
   double theAnswer = 0;
   Action previousAction;

   //Icky code
   if (playerBJ()) {
      if (insuranceTaken && !dealerBJ) {
         theAnswer += -0.5; //The dealer does not have blackjack, so you lose this bet.

         theAnswer += theRules.getBlackJackPayback();
      }
      else if (insuranceTaken && dealerBJ) {
         theAnswer += 1;
         //Push -- no money on your blackjack
      }
      else if (!insuranceTaken && !dealerBJ) {
         theAnswer += theRules.getBlackJackPayback();
      }
      //In the last case, you don't take insurance and the dealer does have blackjack
      //it's a push
      setEV(theAnswer);
      return;
   }

   //A do while loop for multiple player hands. Blackjacks already dealt with above.
   do {

      previousAction = lastAction();

      if (insuranceTaken && currentHand == 0) {
         if (!dealerBJ) {
            theAnswer += -0.5;
         }
         else {
            theAnswer += 1;
         }
      }

      if (busted[currentHand]) {
         if (previousAction == Action.HIT) {
            theAnswer += -1;
         }
         else if (previousAction == Action.DOUBLE) {
            theAnswer += -2;
         }
         else {
            System.out.println("in calculateEV, some logic error.");
         }
      }
      else if (previousAction == Action.SURRENDER) {
         theAnswer += -0.5;
      }
      else if (dealerBJ) {
         theAnswer += -1;
      }
      else {
         printStateStatus(this, "Testing.");
         throw new IllegalStateException("State.calculcateEV called with live hands.");

      }
   }
   while (nextHand());

   setEV(theAnswer);
}

/**
 * Not tested.
 * Wrapper/convenience function for calculateEV(double[],Rules); call me
 * after you use Utilities.getDealerHand.
 * Sets dealer blackjack, then calls calculateEV(int, Rules, boolean), which
 * calls calculateEV(double[], Rules)
 */
public void calculateEV(ArrayList<Card> dealerCards, Rules theRules) {
   final int dealerHandTotal = Utilities.handTotal(dealerCards);
   if ((dealerHandTotal == 21) && (dealerCards.size() == 2)) {
      calculateEV(dealerHandTotal, theRules, true); //Dealer BJ
   }
   else {
      calculateEV(dealerHandTotal, theRules, false); //No dealer BJ
   }
}

/**
 * Wrapper function for calculateEV(double[],Rules).
 * Converts handTotal to dealer Probability array then passes it on to
 * calculation function.
 * UNTESTED
 * If the player has a blackjack, and the dealer does not, this function
 * passes control on to calculateEV(theRules), since the dealer's hand total is
 * not
 * between 17 and 26 (no deal was necessary)
 *
 * @throws IllegalArgumentException if dealerHandTotal is not between 17 and 26.
 * @param dealerHandTotal
 * @param theRules
 * @param dealerBlackjack
 */
public void calculateEV(final int dealerHandTotal, Rules theRules,
        boolean dealerBlackjack) {
   double[] dealerProbabilities = new double[7];
   for (int j = 0; j < dealerProbabilities.length; j++) {
      dealerProbabilities[j] = 0;
   }
   setDealerBlackjack(dealerBlackjack);
   if (dealerBlackjack) {
      dealerProbabilities[1] = 1;

   }
   else if ((dealerHandTotal > 21) && (dealerHandTotal < 27)) {
      dealerProbabilities[0] = 1; //BUST
   }
   else {
      switch (dealerHandTotal) {
         case 17:
            dealerProbabilities[2] = 1;
            break;
         case 18:
            dealerProbabilities[3] = 1;
            break;
         case 19:
            dealerProbabilities[4] = 1;
            break;
         case 20:
            dealerProbabilities[5] = 1;
            break;
         case 21:
            dealerProbabilities[6] = 1;
            break;
         default:
            if (!playerBJ()) {
               throw new IllegalArgumentException("Dealer hand total is not between 17-26.");
            }
            else { //There was no deal.
               //Which is OK if the player has a blackjack and the dealer does not.
               if (dealerHasBJ()) {
                  throw new IllegalStateException("Logic error in calculateEV: "
                          + "CalculateEV claims that the dealer does and doesn't have blackjack.");
               }
               calculateEV(theRules);
               return;
            }
      }
   }
   calculateEV(dealerProbabilities, theRules);


}

/**
 * Convenience function for calculateEV (double[], Rules)
 *
 * @param dealerProbabilities
 * @param theRules
 */
protected void calculateEV(final float[] dealerProbabilities, Rules theRules) {
   calculateEV(Utilities.floatsToDouble(dealerProbabilities), theRules);
}

/**
 * Convenience function to give the result of each hand:
 * State.PUSH, State.SURRENDER, State.WIN, State.LOSE, or State.BLACKJACK
 * State.BLACKJACK is only returned if you have a blackjack and the dealer does
 * not. If the
 * dealer does have a blackjack, this would return State.PUSH if you as well
 * have a blackjack.
 *
 * This should be changed if Australian Late Surrender is implemented (it'd have
 * to take
 * theRules as an argument, too)
 *
 * @param handIndex
 * @return
 * State.PUSH, State.SURRENDER, State.WIN, State.LOSE, or State.BLACKJACK
 *
 */
public int getHandResult(int handIndex, int dealerHandTotal, boolean dealerBJ) {
   Action lastAction = lastAction(handIndex); 

   if (dealerBJ && (dealerHandTotal != 21)) {
      throw new IllegalArgumentException("The dealer cannot have blackjack if her hand total is "
              + dealerHandTotal);
   }

   if (lastAction == Action.SURRENDER) {
      return State.SURRENDER;
   }
   if (busted[handIndex]) {
      return State.LOSE;
   }


   if (dealerBJ) //Mutually exclusive: Early surrender; Insurance/Player blackjack.
   {
      if (playerBJ()) {
         return State.PUSH;//Mutual blackjack. Push.
      }
      else {
         return State.LOSE;
      }
   }
   else if (playerBJ()) {
      return State.BLACKJACK;
   }

   if ((dealerHandTotal < 17)) {
      throw new IllegalStateException("Dealer hand total cannot be less than 17 when the player does"
              + "not have blackjack and the player has not busted or surrendered this hand.");
   }

   assert ((lastAction == Action.STAND) || (lastAction == Action.DOUBLE)) :
           "State.getHandResult: Logic error";
   if (dealerHandTotal > 21) //Dealer bust
   {
      return State.WIN;
   }
   final int handTotal = Utilities.handTotal(myHands.get(handIndex));
   if (handTotal < dealerHandTotal) {
      return State.LOSE;
   }
   if (handTotal == dealerHandTotal) {
      return State.PUSH;
   }
   if (handTotal > dealerHandTotal) {
      return State.WIN;
   }
   throw new IllegalStateException("It's logically impossible for me to get here.");

}

/**
 * Does no error checking on passed dealer array.
 * I'm assuming you can win an insurance bet even if you later go bust. I don't
 * know the truth.
 * Dealer array:
 * an array of doubles:
 * P(Bust), P(natural BJ), P(17), P(18), P(19), P(20), P(21)
 * 0 1 2 3 4 5 6 Array index
 *
 */
protected void calculateEV(final double[] dealerProbabilities, Rules theRules) {
   resetCurrentHand(); //Checks for done status, too.
   double theAnswer = 0;
   double intermediateAnswer = 0;
   int handValue;
   Action previousAction;

//This blackjack section is a duplicate of the section in calculateEV.
//It deals only with natural blackjacks when the dealer has a hole card.
   //Otherwise, it'll do the loop.
   if (theRules.dealerHoleCard()) { //This line is critical. In a no-hole game you could have
      //multiple losing hands, in which case this logic wouldn't work.

      if (dealerBJ) //Mutually exclusive: Early surrender; Insurance/Player blackjack.
      { 
         if (playerActions.get(currentHand).size() == 1) //I have done a previous action.
         {
            if (lastAction() == Action.SURRENDER) //early surrender. Before insurance
            {
               theAnswer += -0.5;
            }
         }
         else //No surrender, hence insurance is possible.
         {
            if (insuranceTaken) {
               theAnswer += 1;
            }
            if (playerBJ()) 
               ;//Mutual blackjack. Push. 0 is 0.
            else {
               theAnswer += -1;
            }
         }

         setEV(theAnswer);
         return;
      }
      else if (playerBJ()) {
         if (insuranceTaken) {
            theAnswer += -0.5; //The dealer does not have blackjack, so you lose this bet.
         }
         theAnswer += theRules.getBlackJackPayback();
         setEV(theAnswer);
         return;
      }
   }

   do {
      previousAction = lastAction();

      if (busted[currentHand]) {
         if (previousAction == Action.HIT) {
            theAnswer += -1;
         }
         else if (previousAction == Action.DOUBLE) {
            theAnswer += -2;
         }
         else {
            throw new IllegalStateException("in calculateEV, some dumb error.");
         }

         if (insuranceTaken && currentHand == 0) //Don't do this once per hand.
         {
            theAnswer += dealerProbabilities[1] * 1;
            theAnswer += (1 - dealerProbabilities[1]) * -0.5;
         }
      }
      else if (previousAction == Action.SURRENDER) {  
         theAnswer += -0.5;
      }
      else if ((previousAction == Action.STAND) || (previousAction == Action.DOUBLE)) 
         //Possible actions left: Stood or doubled. In either case, I'm alive!!
      {  //At this point, the only way the dealer can have BJ is if it's a no-hole card game,
         //Because I checked for hole-card BJs earlier.
         if (playerBJ()) {
            theAnswer += (1 - dealerProbabilities[1]) * theRules.getBlackJackPayback(); 
            //you win unless he has blackjack
            theAnswer += dealerProbabilities[1] * 0; //No payout for dealer blackjack.
         }
         else {
            // I know there is no player blackjack. Dealer may still have BJ if it's no-hole.
            handValue = handTotal();
            intermediateAnswer = 0;
            assert (dealerProbabilities.length == 7);
            intermediateAnswer += dealerProbabilities[1] * -1; //dealer blackjack (no hole)
            //Rumors of some bug around here?

            intermediateAnswer += dealerProbabilities[0] * +1;  //dealer bust
            for (int i = 2; i < dealerProbabilities.length; i++) {
               if ((i + 15) > handValue) {
                  intermediateAnswer += dealerProbabilities[i] * -1;
               }
               else if ((i + 15) == handValue) 
                  ; //push, no change
               else {
                  intermediateAnswer += dealerProbabilities[i] * 1;
               }
            }
            if (previousAction == Action.DOUBLE) {
               intermediateAnswer *= 2;
            }
            theAnswer += intermediateAnswer;
         }


         if (insuranceTaken && currentHand == 0) //Only count insurance once; not once per hand.
         {
            theAnswer += dealerProbabilities[1] * 1;
            theAnswer += (1 - dealerProbabilities[1]) * -0.5;
         } //Insurance is dealt with the same regardless of whether or not the player had blackjack.
      }
      else {
         throw new IllegalStateException("State.calculcateEV called with live hands.");
      }
   }
   while (nextHand());



   setEV(theAnswer);
}

/**
 * @return True if your current hand is worth 21; it has two cards in it; and
 * you only have one hand.
 */
public boolean playerBJ() { //Your first hand has two cards
   if ((myHands.get(0).size() == 2) && (totalHands == 0) && (handTotal() == 21)) {
      return true;
   }
   return false;   
}

public int getNumberSplitAces() {
   if (myHands.get(0).get(0).getCardValue() != CardValue.ACE) {
      return 0;
   }
   return totalHands;

}

/** TODO: Simplify this function, clarify that it's meant to test if a split was the last
 * action a player took.
 */
boolean testPostSplit() {
   if (playerActions.get(0).isEmpty()) {
      return false;
   }
   if (totalHands == 0) {
      return false;
   }

   if ((lastAction() != null) && (lastAction() != Action.SPLIT)) {
      return false; //I've done something that wasn't splitting in the current hand
   }

   if (numberCardsInHand() != 1) {
      return false;
   }


   return true;
}

/**
 * TODO: Should be a duplicate of the hash function belonging to the private Answer
 * Class. Make this a utilities function, not belonging to any object.
 * But it can be called by this function (after the error checking) and the
 * Answer function.
 *
 * @return The answer hash corresponding to this state; will be a split answer
 * hash if TRUE
 * is passed; will be a normal answer hash if FALSE is passed.
 *
 * If I have multiple cards in hand, I need to convert those cards in to the
 * two card equivalent since all my tables are based on two cards in hand.
 * Should never be called with a Perfect Skill.
 *
 * TOTALLY UNTESTED. Especially check that it gives the right hand value for a
 * variety of hands.
 */
protected int getAnswerHash(boolean splitAnswerDesired) {
   if (splitAnswerDesired && (this.numberCardsInHand() != 2)) {
      System.err.println("My calling function wanted me to get a split answer, but I have more than"
              + " 2 cards in hand.");
      assert false;
   }
   final byte effectiveFirstCard = State.getEffectiveCard(this, 1);
   final byte effectiveSecondCard = State.getEffectiveCard(this, 2);
   final byte dealerCard = State.getEffectiveCard(this, 0);

   //Return the Answer involving splits if desired. Even if a split is possible,
   //if it's not one of the top two actions, it won't be findable in the hashmap.
   try {
      return Answer.answerHash(effectiveFirstCard,
              effectiveSecondCard,
              dealerCard, splitAnswerDesired);
   }
   catch (IllegalArgumentException p) {
      System.err.print("Split hash desired, but splitting isn't even possible.");
      throw p;
   }
}

public boolean handIsSoft() {
   return Utilities.isSoft(myHands.get(currentHand));
}

/**
 * Used to get the byte-equivalent of:
 * Card 0 = dealer Card
 * Card 1 = First effective player card (for answer purposes)
 * Card 2 = Second effective player card (for answer purposes)
 * for the passed State. This is used as a helper function in the process
 * of converting a State to an Answer.
 *
 * @throws IllegalArgumentException
 * @param aState
 * @param cardNumber throws exception if not between 0 and 2.
 * @return
 */
static byte getEffectiveCard(State aState, int cardNumber) {
   if ((cardNumber < 0) || (cardNumber > 2)) {
      throw new IllegalArgumentException("State.getEffectiveCard called with cardNumber: " + 
              cardNumber);
   }
   if (cardNumber == 0) {
      return Answer.cardValueToByte(aState.getDealerUpCard().getCardValue());
   }

   byte effectiveFirstCard;
   byte effectiveSecondCard;

   final int numberCardsInHand = aState.numberCardsInHand();
   final int handTotal = aState.handTotal();
   if (numberCardsInHand == 2) {
      effectiveFirstCard =
              Answer.cardValueToByte(aState.getFirstCardValue());
      effectiveSecondCard =
              Answer.cardValueToByte(aState.getSecondCardValue());
   }
   else if (numberCardsInHand > 2) {

      if (aState.handIsSoft()) {
         effectiveFirstCard = Answer.cardValueToByte(CardValue.ACE);
         CardValue q = CardValue.cardValueFromInt(handTotal - 11);
         effectiveSecondCard = Answer.cardValueToByte(q);

      }
      else if (handTotal < 12) //HARD
      {
         effectiveFirstCard = Answer.cardValueToByte(CardValue.TWO);
         CardValue q = CardValue.cardValueFromInt(handTotal - 2);
         effectiveSecondCard = Answer.cardValueToByte(q);
      }
      else //Hard, handTotal 12 or higher.
      {
         effectiveFirstCard = Answer.cardValueToByte(CardValue.TEN);
         CardValue q = CardValue.cardValueFromInt(handTotal - 10);
         effectiveSecondCard = Answer.cardValueToByte(q);
      }

   }
   else {
      throw new IllegalStateException("I have " + numberCardsInHand + " cards in hand.");
   }

   if (cardNumber == 1) {
      return effectiveFirstCard;
   }
   if (cardNumber == 2) {
      return effectiveSecondCard;
   }
   else {
      throw new IllegalStateException("Can't logically be here");
   }

}

@Override
public String toString() {
   StringBuilder sb = new StringBuilder();
   String ln = System.getProperty("line.separator");
   sb.append(ln);
   ArrayList<ArrayList<Action>> myActions = getActions();
   final int totalNumberOfHands = getTotalHands() + 1;
   sb.append("I ");
   if (isInsuranceTaken()) {
      sb.append("took ");
   }
   else {
      sb.append("did not take ");
   }
   sb.append("insurance; I was ");
   if (isInsuranceAdvised()) {
      sb.append("advised ");
   }
   else {
      sb.append("not advised ");
   }
   sb.append("to do so.").append(ln);
   sb.append("I am on hand #").append(getCurrentHand()).append(" of ")
           .append(getTotalHands()).append(", with ").append(numberCardsInHand())
           .append(" cards in hand.").append(ln);
   if (!isBust()) {
      sb.append("The current hand is worth ").append(handTotal()).append(ln);
   }
   if (isBust()) {
      sb.append("The current hand is bust.").append(ln);
   }
   else {
      sb.append("The current hand is not bust.").append(ln);
   }
   sb.append("The busted array is ").append(State.MAX_NUMBER_HANDS).append(" element(s) long:");
   for (boolean k : busted) {
      sb.append(" ").append(k);
   }
   sb.append(ln).append(ln);
   for (int i = 0; i < totalNumberOfHands; i++) {
      sb.append("My #").append(i + 1).append(" hand is: ");
      for (int j = 0; j < myHands.get(i).size(); j++) {
         sb.append(myHands.get(i).get(j)).append("  ");
      }
      sb.append(ln).append("The hand is ");
      if (!areHandsDone().get(i)) {
         sb.append("not ");
      }
      sb.append("done.");
      sb.append(ln).append("The hand total is ").append(Utilities.handTotal(myHands.get(i)));
      sb.append(ln).append("The following actions were taken on the hand:");
      for (int j = 0; j < myActions.get(i).size(); j++) {
         sb.append(myActions.get(i).get(j)).append("  ");
      }
      sb.append(ln).append(ln);
      if (busted[i]) {
         sb.append("I am bust.");
      }
      else {
         sb.append("I am not bust.");
      }
   }
   sb.append("The dealer has a/an ").append(getDealerUpCard().toString()).append(".").append(ln);
   sb.append("My expected value is ").append(getExpectedValue()).append(". ").append(ln);
   sb.append("I do ");
   if (!playerBJ()) {
      sb.append("not ");
   }
   sb.append("have a blackjack.");
   if (dealerBlackJackChecked()) {
      sb.append("The dealer does ");
      if (!dealerHasBJ()) {
         sb.append(" not ");
      }
      sb.append("have a blackjack.").append(ln);
   }
   else {
      sb.append("The dealer has not yet checked for blackjack.").append(ln);
   }
   try {
      Action bestAction = getPreferredAction();
      sb.append("The best action has been defined as a ").append(bestAction).append(ln);
   }
   catch (IllegalStateException q) {
      sb.append("The best action has not been defined.").append(ln);
   }
   try {
      sb.append("The second best action has been defined as a ").append(secondBestAction)
              .append(ln);
   }
   catch (IllegalStateException s) {
      sb.append("The second best action has not been defined.").append(ln);
   }
   sb.append("My second best EV is: ").append(getSecondBestEV()).append(ln);
   if ((numberCardsInHand() < 2) || isBust())  ; //Either of these will lead to exceptions
   else {
      sb.append("My no-split answer hash is: ").append(getAnswerHash(false)).append(ln);
   }

   if (numberCardsInHand() < 2) ;
   else if ((getFirstCardValue() == getSecondCardValue()) && numberCardsInHand() == 2) {
      sb.append("My split answer hash is: ").append(getAnswerHash(true)).append(ln);
   }

   return sb.toString();
}

/**
 * Untested.
 *
 * @return The total amount of money laid out on the table in a hand,
 * assuming that the initial bet on the first hand was 1.
 *
 */
public double getTotalBetAmount() {
   double total = 0;
   if (insuranceTaken) {
      total += 0.5;
   }
   total += (totalHands + 1) * 1; 
   for (int i = 0; i <= totalHands; i++) {
      if (lastAction(i) == Action.DOUBLE) {
         total += 1;
      }

   }
   return total;

}

}
