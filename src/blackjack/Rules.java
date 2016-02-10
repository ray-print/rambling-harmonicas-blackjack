package blackjack;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;


/* All variable changes requires changes to these functions:
 * copy constructor, normal constructor, 
 * (hashcode and equals, which are not implemented), Strategy rules toggles, toString,
 * serialVersionUID, myHashCode, and any serialization related methods.
 *
 * rulesAutoToggles should only be set to false during testing mode, or when
 * instantiating a Rules class.
 * 
 * This is a purposely shallow implementation of Serializable. Serializable is so icky 
 * that it makes more sense just to manually serialize the object.
 */
final public class Rules implements Serializable
{
private boolean earlySurrender;
private boolean lateSurrender;
private boolean earlySurrenderNotOnAces;


/** I made up this number.
 * 
 */
private static final long serialVersionUID = 2000L;

/**
@implements Serializable interface
*/
private void readObject(ObjectInputStream myOIS) throws
        IOException, ClassNotFoundException
{
   myOIS.defaultReadObject();
   if (!rulesAutoToggles)
      throw new IOException("Serialized Rules objects must have auto toggles enabled.");
   doAutoToggles(); //Uses all the setters, so it validates as well; also
   //uses the internal setters in DoubleRules.
}
/**
@implements Serializable interface
*/
private void writeObject(ObjectOutputStream myOutputStream)
        throws IOException
{ myOutputStream.defaultWriteObject();
}
/**
 * Apparent chance of the dealer having this
 * many cards without standing or going bust (8 decks):
 * 4 cards -- 4.49 % 
 * 5 cards -- 0.5407 % 
 * 6 cards -- ~0.04304 % 
 * 7 cards -- ~0.0023413 %
 * 
 * Six cards is about 20 % faster than 7 cards. 
 *
 * At six cards for player and dealer, there is only a 0.0008606
 * chance (0.08606 %) of either one having an unbusted hand whose
 * total is less than hard 17.
 * These numbers are calculated from the functions in Probabilities.java.
 * 
 * Assuming a 100 % chance of this being the dealer up card; For a
 * dealer hand size limit of 5 cards: Value Error Ace 1.9 % Two 1.9
 * % Three 1.2 % Four 0.7 % Five 0.5 % 
 * 
 * 
 * Simply set this to 100 or
 * another large number to get the most accurate results.
 */
private int dealerMaxHandSize, playerMaxHandSize;

   /*This variable should never be directly modified; all changes should
    * go through the setter function, because the doubleRules class maintains its own
    * copy of this field.
    * Sets whether or not the rules change based on other rules.
    * This primarily affects doubling but can affect other rules as well.
    * (For example, setting late surrender to true would set all other surrender
    * options to false.)
    * Only should be set to false when creating rule sets. The reason is that
    * because of how the rules change each other, it might be possible that
    * some rule set is left unsolved.
    * 
    */
   private boolean rulesAutoToggles;

   public boolean getEarlySurrenderNotOnAces()
   {
      return earlySurrenderNotOnAces;
   }

   public void setEarlySurrenderNotOnAces(boolean earlySurrenderNotOnAces)
   {
     this.earlySurrenderNotOnAces = earlySurrenderNotOnAces;
      if (earlySurrenderNotOnAces && rulesAutoToggles)
         lateSurrender = earlySurrender=false;
         
   }

   public boolean getAutoToggles()
   {
      if (myDoubleRules.getAutoToggles() != rulesAutoToggles) //Error checking
      {      System.err.println("Mistake in setting auto toggles in the current rule set.");
      //This function is called by toString(), so it can't call toString itself. Instead, do:
         if (Blackjack.debug())
            throw new IllegalStateException("Double rules auto toggles are " +
                    myDoubleRules.getAutoToggles()
                 + ", and the Rules which own those double rules have auto toggles set to " + 
                 rulesAutoToggles);
      }
      return rulesAutoToggles;
   }

   /** This is a dangerous function. By default, this should be set to true.
    *  Only in certain special circumstances -- notably testing -- should this be
    * altered, and it should only be changed when the Rules are in a valid state,
    * because it doesn't check for validity or change the Rules to be valid.
    * 
    * @param RulesAutoToggle 
    */
   void setRulesAutoToggles(boolean rulesAutoT)
   {
      this.myDoubleRules.setAutoToggles(rulesAutoT);
      this.rulesAutoToggles = rulesAutoT;
   }

   /**
    *  Untested. Used to enable the toggling in Strategy.togglewhatevers
    * to work correctly. Its lack of testing has caused time-consuming problems.
    * 
    */
void doAutoToggles()
{
  setRulesAutoToggles(true);
//This also sets the rules auto toggles copy in DoubleRules
  
   this.setEarlySurrender(earlySurrender);
   this.setEarlySurrenderNotOnAces(earlySurrenderNotOnAces);
   this.setLateSurrender(lateSurrender); 
   this.setHitOn17(hitOn17);
   setBlackjackPayback(blackJackPayback);
   this.setHoleCard(dealerHoleCard);
   this.setCharlie(charlie);
   this.setHitSplitAces(hitSplitAces);
   setMaxNumberSplitHands(maxNumberSplitHands);
   setNumberDecks(numberOfDecks);
   setNumResplitAces(numResplitAces);
   this.myDoubleRules.setNotSplitAces(myDoubleRules.notSplitAces);
   this.myDoubleRules.setAlwaysPossible(myDoubleRules.alwaysPossible);
   this.myDoubleRules.setNotOnAces(myDoubleRules.notOnAces);
   this.myDoubleRules.setNotPostSplit(myDoubleRules.notPostSplit);
   this.myDoubleRules.setOnlyNineTenEleven(myDoubleRules.onlyNineTenEleven);
   this.myDoubleRules.setAnyTwoCards(myDoubleRules.anyTwoCards);
   this.myDoubleRules.setOnlyTenAndEleven(myDoubleRules.onlyTenAndEleven); 
   this.setOriginalBetsOnly(originalBetsOnly);
   //omg I put OnlyNineTenEleven in the above line by mistake
   //

   setAccuracy(CACHE_ACCURACY);
}

   public boolean isRulesAutoToggles()
   {
      return rulesAutoToggles;
   }

/**
 *  If set to true, this should (when implemented) make it so that in 
 * no-hole games, the penalty for a dealer blackjack is only that of
 * the original bet; doubles and split bets are considered a push.
 * Has no effect in hole-card games.
 * This variable is not a direct part of the Rules hash key function;
 * its only effect is that when it is true and the dealer has no hole
 * card, the hash key returns the same key as for the case where the
 * dealer does have a hole card.
 * 
 */
   private boolean originalBetsOnly;

   
   /** This has not been implemented in resolveHands/calculateEV. I just added
    * it completely to the Rules class. That's why I'm keeping it package-private.
    * 
    * 
    * @return 
    */
   boolean getOriginalBetsOnly()
   {
      return originalBetsOnly;
   }

   /** This has not been implemented in resolveHands/calculateEV. I just added
    * it completely to the Rules class. That's why I'm keeping it package-private.
    *  
    * @param originalBetsOnly 
    * 
    */
   void setOriginalBetsOnly(boolean originalBetsOnly)
   {
      this.originalBetsOnly = originalBetsOnly;
   }
/**
 * Public inner class to avoid rewriting getters and setters but to compartmentalize
 * doubling rules.
 */
public DoubleRules myDoubleRules;


private boolean hitOn17;


private double blackJackPayback; //default on 3 to 2.

private boolean hitSplitAces = true;
/**
 * If true, dealer checks for blackjack before players play.
 * This implementation does not support OBO no-hole card
 */
private boolean dealerHoleCard;
/**
 * This variable has not been implemented. 
 * Contains the number of cards needed to trigger an automatic win;
 * 0 if this rule doesn't exist.
 * UNIMPLEMENTED
 */
private int charlie;
/**
 * Number of decks in the shoe.
 */
private int numberOfDecks;
/**
 * Setting this value to 1 means that 1 split is allowed; 0 means
 * splitting is never allowed. 
 * For calculation purposes, the program currently does not differentiate
 * between 2 (1 resplit allowed) and any number higher than 2.
 *
 */
private int maxNumberSplitHands = 1;


public int getMaxNumberSplitHands()
{
   return maxNumberSplitHands;
}

public boolean getEarlySurrender()
{
   return earlySurrender;
}

public boolean getLateSurrender()
{
   return lateSurrender;
}


/**
 *
 *
 * @param maxNumberSplitHands Sets the maximum number of split
 * hands. Must be either 0, 1, or 2 (I haven't implemented more than
 * that.). This can be called "Resplit
 * allowed" and set to a boolean value in the UI.
 * 0 = No splits allowed, 1 = 1 split, 2 = 2 splits.
 * A future project could be to support multiple resplits.
 * @return the number of split hands previously allowed
 * @throws IllegalArgumentException for numbers less than 0 or greater than 2
 */
public int setMaxNumberSplitHands(int maxNumberSplitHands)
{
   if ((maxNumberSplitHands < 0) || (maxNumberSplitHands > 2)) {
      throw new IllegalArgumentException();
   }
   final int previousSplitsAllowed = this.maxNumberSplitHands;
   //yeah. I forgot the this in the above line and spent 15 min. tracking down
   //the error.
   this.maxNumberSplitHands = maxNumberSplitHands;
   return previousSplitsAllowed;
}


/**
 * 0 means that you are not allowed to split aces. 1 means you can split
 * them once, 2 means you can split them twice, etc.
 *
 */
private int numResplitAces;

public Rules()
{
   this(1);
}

/**
 * 
 * Used by Strategy constructor.
 * Performs an auto-test using the hashKey every time it's run, if Blackjack.debug() is true.
 */
public Rules (Rules otherRules)
{
   this.myDoubleRules = new DoubleRules (otherRules.getAutoToggles() );
   this.setRulesAutoToggles(otherRules.getAutoToggles());
   this.hitSplitAces = otherRules.hitSplitAces();
   this.maxNumberSplitHands = otherRules.getMaxNumberSplitHands();
   this.hitOn17 = otherRules.hitOn17();

   this.blackJackPayback = otherRules.getBlackJackPayback();
   this.setHoleCard(otherRules.dealerHoleCard());
   this.charlie = otherRules.getCharlie();
   this.numberOfDecks = otherRules.getNumberOfDecks();
   this.setNumResplitAces(otherRules.getNumResplitAces());
   myDoubleRules = new DoubleRules(otherRules.getAutoToggles());
   
   this.setEarlySurrender(otherRules.getEarlySurrender()); 
   this.setLateSurrender(otherRules.getLateSurrender()); //Must initialize this after hole card init.
   this.setEarlySurrenderNotOnAces(otherRules.getEarlySurrenderNotOnAces());
   
   this.myDoubleRules.setAlwaysPossible(otherRules.myDoubleRules.alwaysPossible());
   this.myDoubleRules.setNotOnAces(otherRules.myDoubleRules.notOnAces());
   this.myDoubleRules.setNotPostSplit(otherRules.noDoublePostSplit());
   this.myDoubleRules.setOnlyNineTenEleven(otherRules.myDoubleRules.onlyNineTenEleven());
   this.myDoubleRules.setAnyTwoCards(otherRules.myDoubleRules.anyTwoCards());
   this.myDoubleRules.setOnlyTenAndEleven(otherRules.myDoubleRules.onlyTenAndEleven());
   this.myDoubleRules.setNotSplitAces(otherRules.myDoubleRules.notSplitAces());
   this.setOriginalBetsOnly(otherRules.getOriginalBetsOnly());

   setAccuracy(otherRules.getAccuracy());   
   if (Blackjack.debug())
   {
      if (this.myHashKey() != otherRules.myHashKey())
      {
         System.err.println("Error in rules copy constructor. I tried to copy this rule set:");
         System.err.println(otherRules.toString());
         System.err.println("-------------------------");
         System.err.println("onto this rule set.");
         System.err.println(toString());
         System.err.println("And afterwards their hash keys did not match.");
         throw new IllegalStateException();
      }
   }
}

/**
 * DO NOT CHANGE THESE DEFAULTS -- DOING SO WILL MESS UP ALL THE TESTS.
 *
 *
 * @param numberDecks  must be between 1-9.
 */
public Rules(int numberDecks)
{
   this.myDoubleRules = new DoubleRules(true);
   setRulesAutoToggles(true);
   this.setEarlySurrender(false);
   this.setEarlySurrenderNotOnAces(false);
   this.setHitOn17(true);
   setBlackjackPayback(1.5D);
   this.setHoleCard(true);
   this.charlie = 0;
   this.hitSplitAces = false;
   setMaxNumberSplitHands(1); //You can split once by default.
   setNumberDecks(numberDecks);
   setNumResplitAces(1);
   myDoubleRules = new DoubleRules(true);
   this.setLateSurrender(true); 
   this.myDoubleRules.setAlwaysPossible(false);
   this.myDoubleRules.setNotOnAces(false);
   this.myDoubleRules.setNotPostSplit(false);
   this.myDoubleRules.setOnlyNineTenEleven(false);
   this.myDoubleRules.setAnyTwoCards(true);
   this.myDoubleRules.setOnlyTenAndEleven(false);
   this.myDoubleRules.setNotSplitAces(false);
   this.setOriginalBetsOnly(false); //Default: no effect, so this doesn't break tests I've written before
   setAccuracy(CACHE_ACCURACY);
}

/**
 * playerMaxHandSize = dealerMaxHandSize = 5;
 * 
 */
public static final int LOW_ACCURACY = 15; 


/**
 * playerMaxHandSize = 6;
 * dealerMaxHandSize = 5;
 * 
 */
public static final int MED_ACCURACY = 10; 
/**
 * Quite close to high accuracy, but much faster.
 * playerMaxHandSize = dealerMaxHandSize = 6;
 */
public static final int GOOD_ACCURACY = 20;

/** Infinite dealer hand size; player size at 6.
 */
public static final int CACHE_ACCURACY = 25;
/**
 * Assuredly good enough for all practical applications, nearly
 * indistinguishable from perfect accuracy.
 * playerMaxHandSize = 8;
 * dealerMaxHandSize = 10;
 *
 */
public static final int HIGH_ACCURACY = 30;
/**
 * No practical limit on the player or dealer hand size (both set to 100)
 * 
 */
public static final int MAX_ACCURACY = 50; 
//MAKE SURE ALL THESE VALUES ARE BETWEEN 10 and 99 for the sake of the dealer probability hash.

private int myAccuracy;

/**
 * Sets the accuracy and speed of the calculations by setting a maximum hand
 * size for the dealer and the player. 
 * @param myAccuracy Takes the following arguments:
 * Rules.LOW_ACCURACY (fastest) 
 * Rules.MED_ACCURACY
 * Rules.GOOD_ACCURACY (default; quite close to high accuracy, but
 * much faster) 
 * Rules.HIGH_ACCURACY (Assuredly good enough for all
 * practical applications, nearly indistinguishable from perfect)
 * Rules.HASH_ACCURACY Better than good accuracy; good when using a dealer cache.
 * Rules.MAX_ACCURACY (very slow, but perfect)
 *
 */
public void setAccuracy(int myAccuracy) throws IllegalArgumentException
{ this.myAccuracy = myAccuracy;
   switch (myAccuracy) {
   case LOW_ACCURACY:
      dealerMaxHandSize = playerMaxHandSize = 5;
      break;
   case MED_ACCURACY:
      dealerMaxHandSize = 5;
      playerMaxHandSize = 6;
      break;
   case GOOD_ACCURACY:
      playerMaxHandSize = 6;
      dealerMaxHandSize = 6;
      break;
   case CACHE_ACCURACY:
 /*Would like this to be at 7, but it's not fast enough.*/
      playerMaxHandSize = 6;      
      dealerMaxHandSize = 100; 
      break;
   case HIGH_ACCURACY:
      playerMaxHandSize = 8;    
      dealerMaxHandSize = 10;
      break;
   case MAX_ACCURACY:
      playerMaxHandSize = 100;
      dealerMaxHandSize = 100;
      break;
   default:
      throw new IllegalArgumentException();

   }
}

public boolean hitSplitAces()
{
   return this.hitSplitAces;
}
/**
 * 
 * @return
 * @deprecated use this.myDoubleRules.notSplitAces
 */
@Deprecated
public boolean noDoubleSplitAces()
{
   return this.myDoubleRules.notSplitAces();
}
/**
 * 
 * @return
 * @deprecated use this.myDoubleRules.notPostSplit()
 */
@Deprecated
public boolean noDoublePostSplit()
{
   return this.myDoubleRules.notPostSplit();
}

/** This variable is used only for calculation purposes; it has no effect
 * outside of fastDealerRecursive
 * 
 * @return 
 */
int getDealerMaxHandSize()
{
   return dealerMaxHandSize;
}

public int getAccuracy() {
 return myAccuracy;
}

/** This variable is used only for calculation purposes; it has no effect
 *  outside of Player's Recursive.
 * @return 
 */
public int getPlayerMaxHandSize()
{
   
   return playerMaxHandSize;
}

public int getNumberOfDecks()
{
   return numberOfDecks;
}

public boolean hitOn17()
{
   return hitOn17;
}

public boolean dealerHoleCard()
{
   return dealerHoleCard;
}
/** UNIMPLEMENTED
 * 
 * 
 * @return 
 */
int getCharlie ()
{
   return charlie; }

/**
 * Note special rules when the player has blackjack -- only standing and
 * insurance are possible.
 *
 * SEE COMMENT ON "HIT." This needs fixing. Check if the FIRST card
 * is an ace.
 * 
 * @param anAction Action to be tested
 * @param currentState Current state of Player's cards and actions.
 * @throws IllegalStateException when the hand is bust; since all actions are
 * impossible, calling isPossible is always meaningless. The
 * hand should be checked for bust before attempting to do any
 * action. If debugging is set to false, instead, this just returns false.
 * @throws IllegalArgumentException if anAction is Action.ERROR.
 * 
 * @return true if anAction is possible, false otherwise. The only
 * actions possible on a blackjack are insurance/stand. (Insurance takes
 * precedence and requires special handling)
 *
 * 
 */
public boolean isPossible(Action anAction, State currentState)
{
   if (currentState.isBust()) {
      
      if (Blackjack.debug()) 
      throw new IllegalStateException(
              "Function Rules.isPossible(Action,State) called with a bust hand.");
      return false;
   }
   //if = 0 we have a serious problem.
   if (currentState.numberCardsInHand() == 1) {
      return false; //No error, but nothing possible. (post-split land)
   }  /*   throw new RuntimeException("Function Rules.isPossible(Action,State) called on hand with only one"
    + " card in it -- probably a split hand, before another card was dealt to it.");
    */

   if (anAction == Action.STAND) {
        // System.out.println("Debugging - standing impossible"); 
        // if (true) return false;
      return true;
   }
   if (currentState.playerBJ()
           && (anAction != Action.INSURANCE) ) {
      return false; //Only standing and insuring are possible
   }
   else if (anAction == Action.HIT) 
   { //This is sometimes not possible on split aces.
      return hitPossible(currentState);
   }
   else if (anAction == Action.SURRENDER) {
      return surrenderPossible(currentState);
   }
   else if (anAction == Action.DOUBLE) {
      return doublePossible(currentState);
   }
   else if (anAction == Action.SPLIT) {
      return splitPossible(currentState);
   }
   else if (anAction == Action.INSURANCE) {
      return insurancePossible(currentState);
   }
   else {
      throw new IllegalArgumentException(
              "Function Rules.isPossible has been called with unsupported action "
              + anAction + ".");
   }
}

/**
 * See below for a long-hidden bug which never caused problems.
 *
 * @param currentState Current state
 * @return True if insurance is a possible action, viz.: 
 * The player has exactly two cards in hand. 
 * The dealer has not already checked for blackjack. 
 * The player's last action is null.
 * The player has not split.
 * The dealer has an ace up card.
 * 
 */
private boolean insurancePossible(State currentState)
{
   if ((currentState.numberCardsInHand() == 2)
           && (currentState.dealerBlackJackChecked() == false)
           && (currentState.lastAction() == null)
           && (currentState.getDealerUpCard().getCardValue() == CardValue.ACE)
           && (currentState.getTotalHands() == 0) ) {
      return true;
   }
   else {
      
      //currentState.setInsuranceTaken(false); // PRIOR BUG
      //Yes, but it's already been taken, then this function untakes it.
      return false;
   }

}

/**
 * Beware of the possibility of hitting in a no hole card game with
 * a dealer BJ. Assumedly, the dealer will not check for blackjack
 * until the end, so I'm safe.
 *
 * Can't hit if you or the dealer have a blackjack.
 *
 * @param currentState
 * @return
 */
private boolean hitPossible(State currentState)
{      //System.out.println("Debugging - hits impossible"); 
       // if (true) return false;
   if (currentState.playerBJ()) {
      return false;
   }
   if (currentState.dealerBlackJackChecked() && currentState.dealerHasBJ()) {
      return false;
   }
   if (hitSplitAces == true) {
      return true;
   }
   if (!currentState.firstCardIs(CardValue.ACE)) {
      return true;
   }
   //Have I split?? Because I know that I can't hit split aces and the first card is an ace.

   if (currentState.getTotalHands() > 0) {
      return false;
   }
   else {
      return true;
   }
}

/**
 * Assumes that 10, J, Q, and K are interchangable.
 *
 * Use getTimesResplitAces in State.
 *
 * @param currentState
 * @return
 */
private boolean splitPossible(State currentState)
{//Must have only 2 cards, of same CardValue, in hand.
   //Must not be over the maxnumber of split hands allowed.
   //if it's aces, check the resplit aces rules.
   //Also, if you can't hit, you can't split -- not true, because
   //perhaps you could resplit split aces, but not hit them.
   if (currentState.getTotalHands() >= maxNumberSplitHands) {
      return false;
   }


   //if (true) return false; //for debugging only 
   if (currentState.playerBJ()) {
      return false;
   }
   if (currentState.dealerBlackJackChecked() && currentState.dealerHasBJ()) {
      return false;
   }
//Assumes that all 10s are interchangable
   if (!(currentState.getFirstCardValue().value() == currentState.getSecondCardValue().value()) ) {
      return false;
   }
   if (currentState.numberCardsInHand() != 2) {
      return false;
   }
   //I know you have two cards in hand that are the same value.
   if ((currentState.getTotalHands()) >= this.maxNumberSplitHands) {
      return false;
   }
   //getTotal returns total number of hands minus 1.

   if ( (currentState.getNumberSplitAces() > numResplitAces)
                    &&
           (currentState.getFirstCardValue() == CardValue.ACE) )
   {
      return false;
   }

   return true;

}

/**
 * Untested.
 *
 *
 *
 * @param currentState
 * @return
 */
private boolean doublePossible(State currentState)
{
      //System.out.println("Debugging -- doubles always possible.");
      //if (true) return true;
   if (!hitPossible(currentState)) {
      return false; //like if you have split aces.
   }
   if (currentState.playerBJ()) {
      return false;
   }
   if (currentState.dealerBlackJackChecked() && currentState.dealerHasBJ()) {
      return false;
   }
   if (myDoubleRules.alwaysPossible() == true) {
      return true;
   }

   if (currentState.numberCardsInHand() != 2) {
      return false;  //I now know I have two cards in my current hand.
   }
   if ((currentState.contains(CardValue.ACE))
           && (myDoubleRules.notOnAces() == true)) {
      return false;
   }
   int handValue = currentState.handTotal(); // used only for 9-10-11  and 9-10 test.
   if (currentState.contains(CardValue.ACE)) 
   {//One or two aces in hand. Two aces can't be 9-10-11 anyway (only 12 or 2)
      handValue -= 10; // If you have 9-10-11, the ace must count low. Otherwise you couldn't have 9-10-11.
   }
   if (currentState.getTotalHands() == 0) //...in Java, 0 is 1.
   //meaning, I haven't split!
   {
      if (myDoubleRules.anyTwoCards() == true) {//System.out.println("I made it this far, by golly!");
         return true;
      }

      if (myDoubleRules.onlyTenAndEleven()) {
         if (handValue == 10 || handValue == 11) {
            return true;
         }
      }
      if (myDoubleRules.onlyNineTenEleven()) {
         if (handValue >= 9 && handValue <= 11) {
            return true;
         }
      }
      return false; //Either all these rules are false, or I haven't met their criterea

   }
   else //I've split. first check notPostSplit, then post-split aces, then any two cards, etc.
   {
      if (myDoubleRules.notPostSplit()) {
         return false;
      }
      if (myDoubleRules.notSplitAces()) {
         if (currentState.firstCardIs(CardValue.ACE)) {
            return false;
         }
      }

      if (myDoubleRules.anyTwoCards() == true) {
         return true;
      }

      if (myDoubleRules.onlyTenAndEleven()) {
         if (handValue == 10 || handValue == 11) {
            return true;
         }
      }
      if (myDoubleRules.onlyNineTenEleven()) {
         if (handValue >= 9 && handValue <= 11) {
            return true;
         }
      }
      return false; //Either all these rules are false, or I haven't met their criterea
   }


}

/**
 * Totally untested; requires further research as well. 
 * I'm saying you are not allowed to surrender post-split.
 * Each surrender rule is treated separately.
 *
 * @param currentState
 * @return
 */
private boolean surrenderPossible(final State currentState)
{    //System.out.println("Debugging - surrender impossible"); 
     //  if (true) return false;
   
   if (currentState.playerBJ()) {
      return false;
   }
   if ((earlySurrender == false) && (lateSurrender == false) &&
        earlySurrenderNotOnAces == false) {
      //Surrendering not allowed under rule set
      return false;
   }
   if (currentState.getTotalHands() > 0) {
      //Surrendering not allowed if you've split
      return false;
   }

   if (currentState.numberCardsInHand() > 2) {
      return false; // You can never surrender with more than 2 cards.
   }    //Except to bikini-clad babes at Golden Gate Casino. True story.
   
   if (earlySurrender == true) // you can always surrender with any two cards in hand.
   {
      return true;
   }
   
   if (earlySurrenderNotOnAces == true)
   { if (currentState.getDealerUpCard().getCardValue() == CardValue.ACE)
      return false;
   else return true;
   }
          
   
   if (lateSurrender)
   {
      assert (this.dealerHoleCard());
      final CardValue dealerCardValue = currentState.getDealerUpCard().getCardValue();
      if ((dealerCardValue == CardValue.ACE) || (dealerCardValue == CardValue.TEN)) {
         if (currentState.dealerBlackJackChecked() == false)
                 {
            return false;
                 } 
      }
      return true; //either dealer Blackjack has already been checked, or the dealer's
      //up card is not a 10 or Ace.
   }
   assert false; //Logically impossible to be here
   return false;

}

public double getBlackJackPayback()
{
   return blackJackPayback;

}

public void setEarlySurrender(boolean earlyS)
{
   earlySurrender = earlyS;
   if (earlyS && rulesAutoToggles)
      lateSurrender = this.earlySurrenderNotOnAces = false;
      
}
/**
 * If the dealer has no hole card, setting this to true gives the dealer
 * a hole card, because Late Surrender with No Hole Card (Australian Late Surrender) 
 * is not currently supported.
 * 
 * If true, sets early surrender to false and early surrender vs. 10 only to false
 * 
 * @param lateSurrender 
 * Late surrender and no-hole card are currently unimplemented.
 * Late surrender with no-hole card means that the hand is only counted
 * as a valid surrender if the dealer does not have a blackjack; otherwise, the
 * player still loses everything. Apparently this rule is used in certain Australian
 * and Asian casinos.
 * 
 */
public boolean setLateSurrender(boolean lateS)
{
   if (!dealerHoleCard && rulesAutoToggles && lateS)
   {
      setHoleCard(true);
      /*
     if (Blackjack.debug())
        throw new IllegalArgumentException("Can't set late surrender to true while the dealer has no"
                + "hole card.");
     System.err.println("Rules.setLateSurrender:"
             + " can't set late surrender to true while the dealer has no"
                + "hole card.");
      return false; */
      
   }
   this.lateSurrender = lateS;
 
   if (lateS && rulesAutoToggles)
      earlySurrender = this.earlySurrenderNotOnAces = false;
      return true;
}

public void setHitOn17(boolean hitOn17)
{
   this.hitOn17 = hitOn17;
}
/**
 * @throws IllegalArgumentException if BJ is less than even money or more than 9.99
 * (9.99 chosen because of how I wrote the hashkey.), but only if debugging is set to on.
 * 
 */
public void setBlackjackPayback(double blackJackPayback)
{
   if ( (blackJackPayback < 0.99) || (blackJackPayback > 9.99) )
   {
      if (Blackjack.debug() )
         throw new IllegalArgumentException();
   }
   this.blackJackPayback = blackJackPayback;
}

public void setHitSplitAces(boolean hitSplitAces)
{
   this.hitSplitAces = hitSplitAces;
}
/**
 * If set to false, this also sets late surrender to false, since there is no late surrender
 * in a no hole card game.
 * @param HoleCard 
 * 
 */
public void setHoleCard(boolean HoleCard)
{
   this.dealerHoleCard = HoleCard;
   if (!HoleCard && rulesAutoToggles)
      lateSurrender =false;
}

/**
 * NOT IMPLEMENTED
 *
 *
 * @param charlie Sets number of cards needed to trigger automatic
 * Charlie win. If this is 2 or below, Charlies are not part of the
 * rule set (no automatic win). Charlie cannot be more than 9 because of
 * the rule hash key algorithm.
 */
void setCharlie(int charlie)
{
   if (charlie <= 2) {
      this.charlie = 0;
   }
   if (charlie > 9) {
      throw new IllegalArgumentException();
   }
   else {
      this.charlie = charlie;
   }
}

/**
 * This is actually a boolean -- aces can be resplit or not. Valid
 * values are either 0 or 1.
 *
 * @param numResplitAces
 */
public void setNumResplitAces(int numResplitAces)
{
   if ((numResplitAces < 0) || (numResplitAces > 1)) {
      throw new IllegalArgumentException(
              "The number of resplit aces must be either 0 or 1.");
   }

   this.numResplitAces = numResplitAces;
}



/** Tested??
 *  This requires that Charlie,
 * maxNumberSplitHands, numResplitAces, and numberOfDecks are
 * between 0 and 9.
 * Does not save myAccuracy, playerHandvalueDraw (I think this variable was removed)
 * , playerMaxHandSize, or dealerMaxHandSize.
 * 
 * Parts of the rules which don't affect the strategy, or which have
 * not been included in this hash:
 * -Dealer and player max hand size, player max hand
 * value 
 * -Any other strategy approximations...? 
 * -Doubles do not convert to precise numbers. I have to round doubles first.
 * -I have added originalBetsOnly in such a way so that it does not add additional information
 * to the hash.
 */

public long myHashKey()
{
   long sum = 0;
   int power = 0; //Do all booleans first.

   final boolean effectiveHoleCard;
if (originalBetsOnly && !dealerHoleCard) //OBO only is relevant when there's no hole card.
	effectiveHoleCard = true;
else effectiveHoleCard = dealerHoleCard;

   
   //All booleans in normal class  - 6
   sum += ((long) (effectiveHoleCard       ? 1 : 0)) << power++;
   sum += ((long) (earlySurrender          ? 1 : 0)) << power++;
   sum += ((long) (lateSurrender           ? 1 : 0)) << power++;
   sum += ((long) (earlySurrenderNotOnAces ? 1 : 0)) << power++;
   sum += ((long) (hitOn17                 ? 1 : 0)) << power++;
   sum += ((long) (hitSplitAces            ? 1 : 0)) << power++;

   
   //All booleans in double rule set  - 7
   sum += ((long) (myDoubleRules.alwaysPossible() ? 1 : 0)) << power++;
   sum += ((long) (myDoubleRules.anyTwoCards() ? 1 : 0)) << power++;
   sum += ((long) (myDoubleRules.notOnAces() ? 1 : 0)) << power++;
   sum += ((long) (myDoubleRules.notPostSplit() ? 1 : 0)) << power++;
   sum += ((long) (myDoubleRules.notSplitAces() ? 1 : 0)) << power++;
   sum += ((long) (myDoubleRules.onlyNineTenEleven() ? 1 : 0)) << power++;
   sum += ((long) (myDoubleRules.onlyTenAndEleven() ? 1 : 0)) << power++;

   //All integer values, which are between 0 and 9.
   sum *= 10000;
   sum += charlie             * 1;
   sum += maxNumberSplitHands* 10;
   sum += numResplitAces    * 100;
   sum += numberOfDecks    * 1000;
   
   sum *= 1000;
   sum +=(long) (1000 * this.blackJackPayback); 
   //Approximation to 4 significant digits
   
   
   //The sum is my hash.
   assert ( (sum > 0) && (sum <= Long.MAX_VALUE) ): "Hash key overflow."; 
   
   //ok now for the fun part.
   return scrambler(sum);
}

private long scrambler(long value)
{
  final double fun = (double) value;
  final double y;
y = fun *1.5D - 1000D + (fun/100D) * Math.abs(
Math.log (fun/1953D) ) * Math.pow(fun/197D, 0.25D)
+ Math.abs(Math.sin(82D*fun))/100D;
   
   return (long) y;
   
}

/**
 * 
 * @param number Number of decks, between 1 and 8.
 * @throws IllegalArgumentException if number is not between 1 and 8.
 * 
 */
public void setNumberDecks(int number)
{ if ( (number < 1) || (number > 8) )
   throw new IllegalArgumentException("There must be no less than 1 "
           + "and no more than 8 decks in the shoe.");
else this.numberOfDecks = number;

}

/**
 * UNTESTED
 *
 *
 *
 * @param aState
 * @param manualDeal set to TRUE if a hand is actually being played, false otherwise.
 * The reason is that "hit" is not considered a possible action if the player has reached the max
 * hand limit when doing calculations, but it is possible in a normal game.
 * @return The number of possible actions, not including insurance. However, if it's 0, this throws
 * an assertion error.
 * @assert false if there are no possible player actions.
 */
public int numPossibleActions(State aState, boolean manualDeal)
{
   int num = 0;
   for (Action anAction : Action.values()) {
      if ((anAction == Action.INSURANCE) || (anAction == Action.ERROR)) {
         continue; //These do not count as valid actions.
      }

      if (isPossible(anAction, aState)) {
         
         if ( (anAction == Action.HIT)  && (aState.numberCardsInHand() >= this.playerMaxHandSize )
                 && (manualDeal == false) ) ;
         else num++;
      }
   }
   if (num == 0) {
      assert false: "Invalid state -- if there are no possible actions,"
              + " I should not be trying to evaluate what they are.";
   }
   return num;
}

public int getNumResplitAces()
{
   return this.numResplitAces;
}

/**
   Any addition I make to the rules in
 * the future should be added to this string. 

 * @return 
 */
@Override
public String toString ()
{ StringBuilder s = new StringBuilder();
s.append(System.getProperty("line.separator"));
s.append("Long hash: ").append(this.myHashKey()).append(". ");
s.append("Number of Decks: ").append(this.numberOfDecks).append(". ");
s.append("Rules auto-toggles: ").append(this.rulesAutoToggles).append(". ");
s.append("Can hit split aces: ").append(this.hitSplitAces).append(".");

s.append(System.getProperty("line.separator"));
s.append("Dealer hole card: ").append(this.dealerHoleCard()).append(".");
s.append(" Original bets only: ").append(this.originalBetsOnly).append(".");
s.append(" Early surrender: ").append(this.earlySurrender).append(".");
s.append(" Late surrender: ").append(this.lateSurrender).append(".");
s.append(System.getProperty("line.separator"));
s.append(" Early surrender but not on Aces: ").append(this.earlySurrenderNotOnAces).append(".");
s.append(System.getProperty("line.separator"));
s.append("BJ payback: ").append(this.blackJackPayback).append(".");
s.append(" Hit on soft 17: ").append(this.hitOn17).append(".");
s.append(" Max # split hands: ").append(this.maxNumberSplitHands).append(".");
s.append(" # Resplit Aces: ").append(this.numResplitAces).append(".");
s.append(System.getProperty("line.separator"));
s.append(" Charlies (unused):").append(this.getCharlie()).append(".");
s.append(" Player max hand size: ").append(this.playerMaxHandSize).append(".");
s.append(" Dealer max hand size: ").append(this.dealerMaxHandSize).append(".");

//NOT SHOWN: accuracy, playerHandValueDraw<-which is now always at 1 except for Max accuracy
s.append("Doubling rules:");
s.append(System.getProperty("line.separator"));
s.append(" Can double post-split: ").append(!this.myDoubleRules.notPostSplit()).append(".");
s.append(" Any two cards: ").append(this.myDoubleRules.anyTwoCards()).append(".");
s.append(" Not on aces: ").append(this.myDoubleRules.notOnAces()).append(".");
s.append(" Not on split aces: ").append(this.myDoubleRules.notSplitAces()).append(".");
s.append(System.getProperty("line.separator"));
s.append(" Only 9-10-11:").append(this.myDoubleRules.onlyNineTenEleven()).append(".");
s.append(" Only 10-11: ").append(this.myDoubleRules.onlyTenAndEleven()).append(".");
s.append(" Always possible: ").append(this.myDoubleRules.alwaysPossible()).append(".");


   return s.toString();
}
}
