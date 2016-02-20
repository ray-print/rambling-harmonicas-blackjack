package ramblingharmonicas.blackjack;

import java.io.IOException;
import java.util.*;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import joptsimple.ValueConversionException;
import joptsimple.ValueConverter;
import ramblingharmonicas.blackjack.cards.*;
import ramblingharmonicas.blackjack.rules.Surrender;

//TODO: Refactor to move some functionality to Utilities or Strategy or some new class(es).
public class Blackjack {
	
static public long dealerIterations = 0;
static public long fastDealerIterations = 0;
static public long playerIterations = 0;
static public long holeCardCheck = 0;
final static int ACECARD = 0; //Represents INDEX POSITION, not value.
final static int TWOCARD = 1;
final static int THREECARD = 2;
final static int FOURCARD = 3;
final static int FIVECARD = 4;
final static int SIXCARD = 5;
final static int SEVENCARD = 6;
final static int EIGHTCARD = 7;
final static int NINECARD = 8;
final static int TENCARD = 9;
final static int JACKCARD = 9;
final static int QUEENCARD = 9;
final static int KINGCARD = 9;
private static boolean DEBUGGING = true;

public static void main (String [] args) throws NoRecommendationException, IOException {
	Rules theRules = parseArguments(args); 
	if (theRules == null) {
	   return;
	}

	Strategy someStrat = new Strategy(theRules, Strategy.Skill.COMP_DEP);
	someStrat.solve(theRules);
	someStrat.print();
}

private static Rules parseArguments(String [] args) {
	OptionParser parser = createOptionsParser();
	try {
		Rules theRules = createRules(parser, args);
		//TODO: have it return a Strategy.
		return theRules;
	}
	catch (OptionException exception) {
		System.out.println(exception);
		try {
			parser.printHelpOn(System.out);
		}
		finally {
			return null;
		}
	}
	catch (ClassCastException exception) {
		System.out.println(exception);
		try {
			parser.printHelpOn(System.out);
		}
		finally {
			return null;
		}
	}

}
private static final class Options {
	static final String DECKS = "decks";
	static final String DOUBLE_RULES = "double";
	static final String [] DOUBLE_AFTER_SPLIT = {"double-after-split", "das"};
	static final String [] HELP = { "help", "?" };
	static final String HIT_SOFT_17 = "hit-soft-17";
	static final String [] HIT_SPLIT_ACES = {"hsa", "hit-split-aces"};
	static final String NO_HOLE_CARD = "no-hole-card";
	static final String RESPLIT_ALLOWED = "resplit-allowed";
	static final String SURRENDER = "surrender";

	static enum Double {ANY_TWO_CARDS, TEN_ELEVEN, NINE_TEN_ELEVEN, HARD_ONLY};
	//TODO: Investigate if there's any good reason why this isn't already an enum
};


private static Rules createRules(OptionParser parser, String [] args) throws OptionException,
ClassCastException{
	OptionSet options = parser.parse(args);
	if (options.has(Options.HELP[0])) {
		try {
			parser.printHelpOn(System.out); 
		}
		finally {
			return null;
		}
	}
	int decks = (Integer) options.valueOf(Options.DECKS);
	if (decks < 1 || decks > 8) {
		//Better to have this done by the parser
		System.out.println("The number of decks must be between 1 and 8.");
		return null;
	}
	Rules theRules = new Rules( decks);
	theRules.setHoleCard (!((Boolean) options.valueOf(Options.NO_HOLE_CARD)));
	
	theRules.setHitOn17  (  (Boolean) options.valueOf(Options.HIT_SOFT_17));
	theRules.setHitSplitAces  (  (Boolean) options.valueOf(Options.HIT_SPLIT_ACES[0]));
	
	String surrenderOption = ((String) options.valueOf(Options.SURRENDER)).toUpperCase();
	Surrender surrender = Surrender.valueOf( surrenderOption );
	switch (surrender) {
		case EARLY:
			theRules.setEarlySurrender(true);
			break;
		case EARLY_EXCLUDING_ACES:
			theRules.setEarlySurrenderNotOnAces(true);
			break;
		case LATE:
			theRules.setLateSurrender(true);
		case NONE:
			theRules.setLateSurrender(true); // = Set all other surrenders are false
			theRules.setLateSurrender(false);
	}

	final boolean resplitAllowed = ( (Boolean) options.valueOf(Options.RESPLIT_ALLOWED));
	if (resplitAllowed) {
		theRules.setMaxNumberSplitHands(2);
	}
	else {
		theRules.setMaxNumberSplitHands(1);
	}

	String doubleSetting = (String) options.valueOf(Options.DOUBLE_RULES);
	Options.Double doubleRule = Options.Double.valueOf(doubleSetting);
	switch(doubleRule) {
		case ANY_TWO_CARDS:
			theRules.myDoubleRules.setAnyTwoCards(true);
			break;
		case NINE_TEN_ELEVEN:
			theRules.myDoubleRules.setOnlyNineTenEleven(true);
			break;
		case TEN_ELEVEN:
			theRules.myDoubleRules.setOnlyTenAndEleven(true);
			break;
		case HARD_ONLY:
			theRules.myDoubleRules.setNotOnAces(true);
			break;
	}
	
	final boolean doubleAfterSplit = (Boolean) options.valueOf(Options.DOUBLE_AFTER_SPLIT[0]);
	theRules.myDoubleRules.setNotPostSplit(!doubleAfterSplit);

	return theRules;
}

private static OptionParser createOptionsParser() {
	OptionParser parser = new OptionParser();
	parser.acceptsAll( Arrays.asList(Options.HELP), "Show help" ).forHelp();
    //TODO: Add option for forcing calculations and for skill level (Total-dep, comp-dep)
	parser.accepts(Options.DECKS, "Number of decks (1-8)").withRequiredArg().ofType(Integer.class)
			.defaultsTo(8).describedAs("decks");
	parser.accepts(Options.HIT_SOFT_17, "Dealer hits on soft 17").withOptionalArg()
			.ofType(Boolean.class).defaultsTo(true);
	parser.acceptsAll(Arrays.asList(Options.HIT_SPLIT_ACES), "The player can hit on split aces")
			.withOptionalArg().ofType(Boolean.class).defaultsTo(true);
	parser.accepts(Options.NO_HOLE_CARD, "The dealer does not draw a hole card until after "
			+ "the player's hand is done (European style).").withOptionalArg()
			.ofType(Boolean.class).defaultsTo(false);
	parser.accepts(Options.SURRENDER, "The player can surrender his hand. Late surrender occurs "
			+ "after the dealer has checked for blackjack. Early surrender, an extremely rare "
			+ "rule, allows the player to surrender before the dealer has checked for blackjack.")
			.withOptionalArg().defaultsTo("LATE")
			.describedAs("early, early_excluding_aces, late, or none");
			//.ofType(Surrender.class); (JOptSimple would auto-parses this and mess up the case)
	parser.accepts(Options.RESPLIT_ALLOWED, "The player is allowed to split one more time after"
			+ " her first split. (Sorry, multiple resplits are not supported by the engine.)")
			.withOptionalArg().ofType(Boolean.class).defaultsTo(true);
	parser.accepts(Options.DOUBLE_RULES, "any_two_cards means the player can"
			+ " double with any two cards in hand. nine_ten_eleven and ten_eleven restrict doubling"
			+ " to those hand totals only (also with only two cards in hand). Hard only means the"
			+ " player can't double if she has an ace in hand.").withOptionalArg()
			.defaultsTo("ANY_TWO_CARDS").describedAs("any_two_cards, nine_ten, nine_ten_eleven,"
					+ " hard_only");
			//Same comment re: enum parsing as Surrender option
	parser.acceptsAll(Arrays.asList(Options.DOUBLE_AFTER_SPLIT), "The player is allowed to double"
			+ " after splitting.").withOptionalArg().ofType(Boolean.class).defaultsTo(true);
	return parser;
}
/**
 *
 * @return True if debugging is enabled, false otherwise
 * @deprecated Use asserts for doing assertions; for logging, a separate
 * set-up is to be created
  */
public static boolean debug() {
   if (DEBUGGING) {
      return true;
   }
   else {
      return false;
   }
}

/**
 * Given a finished State (a player is done deciding what to do; all hands are
 * finished), this tells you whether or not the dealer needs to deal. 
 * It also may change the current player hand (State.currentHand). Throws an 
 * IllegalStateException if not all hands are done or if there is a logic error.
 *
 * THEN:
 * - Call State.calculateEV(theRules) if no deal is needed. OR
 * - If a deal was needed, display the dealer's down card, or draw one if he 
 * has no hole card.
 *
 * If the dealer does have BJ (it's a no hole card game), and a deal was
 * necessary, tell State that fact. (If a deal was NOT necessary, 
 * do NOT tell State that, just call calculateEV(theRules) )
 *
 * If a deal is needed, then deal the dealer out; then call
 * State.calculateEV(dealerProbabilities, theRules), where dealerProbabilites is
 * the simplified version of the dealer's hand.
 */
public static boolean dealNecessary(State finishedState, Rules theRules) {
   if (!finishedState.allDone()) {
      State.printStateStatus(finishedState, "Error hand:");
      throw new IllegalStateException(
              "Function resolveHands called before all hands were finished.");
   }
   boolean dealNecessary = false;
   finishedState.resetCurrentHand();
   boolean flag = true;

   if (theRules.dealerHoleCard() && finishedState.dealerBlackJackChecked()) {
      if (finishedState.dealerHasBJ() && (finishedState.lastAction() == null)) {
         dealNecessary = false;
         flag = false;
      }
      //You have done nothing, he had blackjack.
      else if (finishedState.playerBJ()) {
         dealNecessary = false;
         flag = false;
      }
      //You have BJ and he doesn't.
   }

   while (flag) {
      switch (finishedState.lastAction()) {
         case STAND:
            dealNecessary = true;
            flag = false;
            break;
         case DOUBLE:
         case HIT:
            if (!finishedState.isBust()) {
               dealNecessary = true;
               flag = false;
               break;
            }
            break;
         case SURRENDER:
            break;
         default:
            throw new IllegalStateException(
                    "I'm in dealNecessary and I don't know what to do.");
      }
      if (!finishedState.nextHand()) {
         flag = false;
      }

   }

   //Regardless of anything else, if you take insurance in a no hole card game
   //the dealer must go, to see if he has blackjack.. 
   //Even if you bust or surrender all your hands.
   //TODO: Did I code that in here?
   return dealNecessary;
}

/**
 * Call this when the player's actions are done to finalize the
 * state. Tested under four different, fairly complicated
 * scenarios. This is done right before comparing EVs of the
 * possible States. Specifically, when ALL the split hands are
 * resolved.
 *
 * This is a wrapper function for State.calculateEV. This is the function that
 * creates and uses the dealer probability cache.
 *
 */
static State resolveHands(State finishedState, FastShoe myShoe, Rules theRules) {
   int i, j, k;

   final boolean dealNecessary = dealNecessary(finishedState, theRules);
   if (!dealNecessary) {
      finishedState.calculateEV(theRules);
   }
   else if (dealNecessary) {
      int[] dealerCards = new int[10];
      float[] dealerProbabilities; 

      String keyForMap = DealerCache.getKeyForMap(myShoe, finishedState.getDealerUpCard().value() - 1, theRules);
      Utilities.zero(dealerCards);

      dealerCards[ finishedState.getDealerUpCard().value() - 1] = 1;

      final DealerCache.Status myCacheStatus = DealerCache.getCacheStatus();
      // Should clear out the cache on certain rule changes: different 
      // number of decks, the dealer can hit or stand on soft 17, 
      // perhaps hole/no hole card.
      // The Cache is only to be used with a full shoe

      DealerCache.initCache(); 
      if ((myCacheStatus != DealerCache.Status.NO_CACHE) && 
    		  DealerCache.dealerProbabilitiesCache.containsKey(keyForMap)) {
         assert DealerCache.incrementHits();
         dealerProbabilities = DealerCache.dealerProbabilitiesCache.get(keyForMap);
         finishedState.calculateEV(dealerProbabilities, theRules);
      }
      else {
         try {
            double[] solvedDealerProbs = DealerRecursive(dealerCards, myShoe, theRules);
            assert DealerCache.incrementMisses();
            if ((myCacheStatus != DealerCache.Status.NO_CACHE)
                    && (((theRules.getNumberOfDecks() * 52) - DealerCache.CACHE_DEPTH) <= myShoe.numberOfCards())) {

               if (DealerCache.dealerProbabilitiesCache.size() < DealerCache.Status.SMALL_CACHE.getSize()) {
                  DealerCache.dealerProbabilitiesCache.put(keyForMap, Utilities.doublesToFloat(solvedDealerProbs));
               }
               else if (myCacheStatus == DealerCache.Status.FULL_CACHE) {
                  DealerCache.dealerProbabilitiesCache.put(keyForMap, Utilities.doublesToFloat(solvedDealerProbs));
               }
            }

            finishedState.calculateEV(solvedDealerProbs, theRules);
         }
         catch (IllegalArgumentException e) {
            State.printStateStatus(finishedState, 
                    "And here is my error state.");
            throw new IllegalArgumentException(e);

         }
         catch (IllegalStateException e) {
            State.printStateStatus(finishedState, 
                    "Hole card probabilities not correctly loaded.");
            throw new IllegalStateException(e);
         }
      }
   }
   return finishedState;
}

/**
 * Used when the dealer hand size has maxed out (see dealer hand size limit
 * in Rules)
 * Utterly untested; Estimate of the dealer probabilities for
 * terminal hands; calculates the chances of going bust on the next
 * card and standing on the next card; assumes those values are the
 * same for the card after that and that that card (the next-next
 * card) is the last card. This underestimates the actual bust
 * probability, because with every card there's a higher chance of
 * the dealer going bust.
 *
 * @param endProbabilities Approximate dealer end probabilities.
 * @param myDeck
 * @param handValue
 * Ignoring the case where you would hit on soft 17. P(Bust),
 * P(Natural BJ), P(17), P(18), P(19), P(20), P(21)
 * SHOULD BE PRIVATE
 */
protected static void setApproxProbabilities(
        double[] endProbabilities, FastShoe myDeck,
        final int handValue, boolean isSoft) {
   if ((handValue <= 11) || (isSoft)) 
      //Stupid dealer has not even gotten started. Fooey. All bets off.
   {
      endProbabilities[0] = 1D / 6D;
      for (int q = 2; q < endProbabilities.length; q++) {
         endProbabilities[q] = 1D / 6D;
      }

   }
   else {
      //Hard hand total is between 12 and 16. Relatively easy calculation. Also
      //of dubious accuracy.
      final double probUnder = myDeck.probGettingThisOrLess(
              21 - handValue);
      //Chance of not going bust on next card
      final double probBustNextCard = 1D - probUnder;

      //you're undone if you get 16 or less. otherwise you're done.
      final double probUndoneNextCard = myDeck.probGettingThisOrLess(
              16 - handValue); 
      

      final double probDoneNextCard = probUnder - probUndoneNextCard;
      if (Math.abs(probBustNextCard + probDoneNextCard) < 0.001) {
         endProbabilities[0] = probBustNextCard;
      }
      else {
         //These are all doubles, no casting needed.
         endProbabilities[0] = probBustNextCard
                 + (probBustNextCard * probUndoneNextCard)
                 / (probBustNextCard + probDoneNextCard); 
      }
      for (int i = 2; i < endProbabilities.length; i++) {
         endProbabilities[i] = (1D - endProbabilities[0]) / 5D;
      }
   }

   //Check probabilities
   double sum = 0;
   for (int j = 0; j < endProbabilities.length; j++) {
      sum += endProbabilities[j];
   }
   
   if (Math.abs(sum - 1) > 0.0001) //Sum should equal 1. Hence this should be zero.
   {
      StringBuilder builder = new StringBuilder();
      for (int i = 0; i < endProbabilities.length; i++) {
         builder.append("\nElement " + i + " is " + endProbabilities[i]);
      }
      builder.append("\nHandvalue is " + handValue + ". isSoft is " + isSoft);
      throw new AssertionError(builder.toString());
   }
}

/**
 *
 * EVENTUALLY MAKE THIS PRIVATE (and non-static)
 *
 * @return A 2-D arraylist of solved states. Composition-dependent.
 * Each row contains the same two player up cards and the dealer up
 * cards, in order. The rows should be sorted by hand total. Does
 * NOT include splitting possibilities. Only for hard hands.
 *
 * @param possibleDealerBJ If set to false, excludes the possibility of a dealer
 * blackjack.
 */
static ArrayList<ArrayList<State>> solveHardPlayersRecursive(Rules theRules,
        final boolean possibleDealerBJ) {
   final FastShoe myShoe = new FastShoe(theRules.getNumberOfDecks());
   ArrayList<Card> myCards = new ArrayList<Card>();
   final int actualSplitsAllowed = theRules.getMaxNumberSplitHands();
   theRules.setMaxNumberSplitHands(0);
   State duh;
   ArrayList<ArrayList<State>> solvedStates = new ArrayList<ArrayList<State>>();
   int rowNum = -1;
   int colNum = 0;
   for (CardValue pCardOne : CardValue.twoToTen) {
      for (CardValue pCardTwo : CardValue.twoToTen) {
         if (pCardTwo.value() >= pCardOne.value()) {
            solvedStates.add(new ArrayList<State>());
            rowNum++;
            for (CardValue dealerCard : CardValue.twoToAce) {
               myCards.clear();
               myCards.add(new Card(Suit.CLUBS, pCardOne));
               myCards.add(new Card(Suit.CLUBS, pCardTwo));
               if (dealerCard == null) {
                  throw new RuntimeException();
               }
               duh = new State(myCards, new Card(Suit.CLUBS, dealerCard));
               if (!possibleDealerBJ) {
                  duh.setDealerBlackjack(false);
               }
               myShoe.fasterDrawSpecific(pCardOne);
               myShoe.fasterDrawSpecific(pCardTwo);
               myShoe.fasterDrawSpecific(dealerCard);
               try {
                  duh = PlayerRecursive(myShoe, duh, theRules);
               }
               catch (NoRecommendationException e) {} 
               //TODO -- empty catch blocks are bad
               if (Blackjack.debug()) {
                  if ((duh.getPreferredAction() == duh.getSecondBestAction()) && (!duh.playerBJ())) {
                     System.out.println(theRules);
                     State.printStateStatus(duh, "Top two preferred actions are the same.");
                     assert false;
                  }
               }
               solvedStates.get(rowNum).add(duh);
               myShoe.addCard(pCardOne);
               myShoe.addCard(pCardTwo);
               myShoe.addCard(dealerCard);
               if (myShoe.numberOfCards() != 52 * theRules.getNumberOfDecks()) {
                  throw new RuntimeException();
               }
            }
         }
         else ; //do nothing
      }
   }
   Collections.sort(solvedStates, new Comparator<ArrayList<State>>() {
      @Override
      public int compare(ArrayList<State> q, ArrayList<State> p) {
         return q.get(0).handTotal() - p.get(0).handTotal();
      }

   });
   theRules.setMaxNumberSplitHands(actualSplitsAllowed);
   return solvedStates;
}

/**
 * Needs testing. Make private.
 * Used with a pristine Shoe when calculating the basic strategy.
 *
 */
static ArrayList<Answer> calculateAllSplitValues(Rules theRules,
        ArrayList<ArrayList<State>> hardAnswers,
        ArrayList<ArrayList<State>> softAnswers, final boolean possibleDealerBJ)
        throws NoRecommendationException {
   double scratch;
   ArrayList<Answer> splitAnswers = new ArrayList<Answer>();
   for (CardValue PCard : CardValue.oneToTen) {
      for (CardValue DCard : CardValue.oneToTen) {
         scratch = splitSolve(theRules, PCard, DCard, possibleDealerBJ);
         if (scratch > 50) {
            System.out.println("Player: " + PCard + ", dealer: " + DCard + ", value: " + scratch);
         }
         splitAnswers.add(Utilities.splitEVtoAnswer(scratch, PCard, DCard, hardAnswers, softAnswers));
      }
   }
   return splitAnswers;
}

/**
 * I make a million calls to .size; to make this function faster, avoid those
 * calls.
 * How can calling a simple getter take 8 % of the whole program time??? :(
 * To make this faster, I need to make State more lightweight. If I don't intend
 * to use the splitting functionality, I could get rid of some stuff.
 *
 * @param myShoe
 * @param myState
 * @param theRules
 * @return
 *
 *
 * Test all functions in this. Simplify this behemoth. Consider
 * making a big function that would do all the EV calculations and
 * return the finished state. (everything after double bestEV =
 * -500); it'd just go something like return bestOfAll ( and take 6
 * arraylists, the shoe, the state, and the rules) That's why I
 * hesitate, 'cause that'd also be a behemoth function.
 */
static State PlayerRecursive(final FastShoe myShoe, final State myState,
        final Rules theRules) throws NoRecommendationException {
   int i;
   final Card dealerCard = myState.getDealerUpCard();
   final boolean dealerHoleCard = theRules.dealerHoleCard();
   State scratch;
   if (myState.numberCardsInHand() == 1) {
      ArrayList<State> postSplitDrawResults = new ArrayList<State>();
      double[] probPostSplitDrawResults = new double[10];
      Card drawnCard;
      if (!myState.testPostSplit()) {
         throw new NoRecommendationException("IllegalStateException: "
                 + "I have one card in hand, " + 
                 "but I haven't come from another split hand or myself.");
      }
      i = 0;
      for (CardValue q : CardValue.oneToTen) {
         probPostSplitDrawResults[i++] = myShoe.playerProbability(dealerHoleCard, dealerCard, q);
      }
      i = 0;
      for (CardValue val : CardValue.oneToTen) {
         scratch = new State(myState);
         if (probPostSplitDrawResults[i] > 0) {
            drawnCard = myShoe.fastDrawSpecific(val);
            scratch.postSplitDraw(drawnCard);
            scratch = PlayerRecursive(myShoe, scratch, theRules);
            myShoe.addCard(drawnCard);
         }
         else {
            probPostSplitDrawResults[i] = -10000.0;
         }
         postSplitDrawResults.add(scratch);
         i++;
      }
      if (postSplitDrawResults.size() > 0) {
         double postSplitEV = Utilities.combinedStatesEV(postSplitDrawResults, probPostSplitDrawResults);
         if (myState.numberCardsInHand() != 1) {
            throw new IllegalStateException("Yay I found an error at ~ postSplitDraw end of PlayerRecursive");
         }
         myState.setEV(postSplitEV);
         return myState;
      }
   }
   if (myState.isBust()) {
      if (myState.nextHand()) {
         return PlayerRecursive(myShoe, myState, theRules);
      }
      else {
         return Blackjack.resolveHands(myState, myShoe, theRules);
      }
   }
   final int playerMaxHandSize = theRules.getPlayerMaxHandSize();
   State earlySurrenderState = null;
   boolean earlySurrenderPossible = false;
   ArrayList<State> actionResults = new ArrayList<State>();
   boolean hitPossible = false;
   State[] hitResults = new State[10];
   double[] probHitResults = new double[10];
   State[] doubleResults = new State[10];
   boolean doublePossible = false;
   boolean possibleDealerBJOnHoleCard = false;
   State dealerBJWithHoleCard = null;
   double probDealerBJ = -500;
   Action bestAction = null;
   Action secondBestAction = null;
   if (((dealerCard.value() == CardValue.TEN.value()) || (dealerCard.value() == CardValue.ACE.value())) && (!myState.dealerBlackJackChecked())) {
      if (theRules.isPossible(Action.SURRENDER, myState)) {
         earlySurrenderPossible = true;
         earlySurrenderState = new State(myState);
         earlySurrenderState.action(Action.SURRENDER);
         Blackjack.resolveHands(earlySurrenderState, myShoe, theRules);
      }
      if (theRules.isPossible(Action.INSURANCE, myState)) {
         if (Strategy.insuranceGoodIdea(myShoe, theRules, myState)) {
            myState.action(Action.INSURANCE);
         }
      }
      if (dealerHoleCard) {
         if (dealerCard.value() == CardValue.TEN.value()) {
            probDealerBJ = myShoe.fastProbabilityOf(CardValue.ACE);
         }
         else if (dealerCard.value() == CardValue.ACE.value()) {
            probDealerBJ = myShoe.fastProbabilityOf(CardValue.TEN);
         }
         dealerBJWithHoleCard = new State(myState);
         dealerBJWithHoleCard.setDealerBlackjack(true);
         possibleDealerBJOnHoleCard = true;
         myState.setDealerBlackjack(false);
      }
      else {
         probDealerBJ = -5000000;
      }
   }
   if (theRules.isPossible(Action.STAND, myState)) {
      scratch = new State(myState);
      scratch.action(Action.STAND);
      if (scratch.nextHand()) {
         scratch = PlayerRecursive(myShoe, scratch, theRules);
      }
      else {
         scratch = Blackjack.resolveHands(scratch, myShoe, theRules);
      }
      actionResults.add(scratch);
   }
   if (theRules.isPossible(Action.SURRENDER, myState) && theRules.getLateSurrender()) {
      scratch = new State(myState);
      assert ((theRules.getEarlySurrender() == false) && (theRules.getEarlySurrenderNotOnAces() == false)) : 
              theRules.toString();
      scratch.action(Action.SURRENDER);
      if (scratch.nextHand()) {
         scratch = PlayerRecursive(myShoe, scratch, theRules);
      }
      else {
         scratch = Blackjack.resolveHands(scratch, myShoe, theRules);
      }
      actionResults.add(scratch);
   }
   if (theRules.isPossible(Action.SPLIT, myState)) {
      scratch = new State(myState);
      scratch.action(Action.SPLIT);
      scratch = PlayerRecursive(myShoe, scratch, theRules);
      actionResults.add(scratch);
   }
   Card drawnCard;
   if (myState.numberCardsInHand() < playerMaxHandSize) {
      if (theRules.isPossible(Action.HIT, myState)) {
         hitPossible = true;
         i = 0;
         for (CardValue q : CardValue.oneToTen) {
            hitResults[i] = new State(myState);
            probHitResults[i] = myShoe.playerProbability(dealerHoleCard, dealerCard, q);
            i++;
         }
         i = 0;
         for (CardValue val : CardValue.oneToTen) {
            if (probHitResults[i] > 0) {
               drawnCard = myShoe.fastDrawSpecific(val);
               hitResults[i].action(Action.HIT, drawnCard);
               hitResults[i] = PlayerRecursive(myShoe, hitResults[i], theRules);
               myShoe.addCard(drawnCard);
            }
            else {
               probHitResults[i] = -1000;
            }
            i++;
         }
      }
      if (theRules.isPossible(Action.DOUBLE, myState)) {
         doublePossible = true;
         for (i = 0; i < CardValue.oneToTen.length; i++) {
            doubleResults[i] = new State(myState);
         }
         i = 0;
         for (CardValue val : CardValue.oneToTen) {
            if (probHitResults[i] > 0) {
               drawnCard = myShoe.fastDrawSpecific(val);
               doubleResults[i].action(Action.DOUBLE, drawnCard);
               if (doubleResults[i].nextHand()) {
                  doubleResults[i] = PlayerRecursive(myShoe, doubleResults[i], theRules);
               }
               else {
                  doubleResults[i] = Blackjack.resolveHands(doubleResults[i], myShoe, theRules);
               }
               myShoe.addCard(drawnCard);
            }
            else ; // it's impossible to draw that card.
            i++;
         }
      }
   }
   //Whew. I now have all the possible states, and their expected values.
   //If I have a choice of what to do, then pick the best choice and return it. (use function)
   //If I don't have a choice, combine everything, calculate EV ...
   //it's just like the busted hands scenario.
   double secondBestEV = -500;
   double bestEV = -500;
   double currentEV;

   if (doublePossible) {
      currentEV = Utilities.combinedStatesEV(doubleResults, probHitResults);
      if (currentEV > bestEV) {
         if (bestAction != null) {
            secondBestEV = bestEV;
            secondBestAction = bestAction;
         }
         bestEV = currentEV;
         bestAction = Action.DOUBLE;
      }
      else if (currentEV > secondBestEV) {
         secondBestEV = currentEV;
         secondBestAction = Action.DOUBLE;
      }
   }
   if (hitPossible) {
      currentEV = Utilities.combinedStatesEV(hitResults, probHitResults);
      if (currentEV > bestEV) {
         if (bestAction != null) {
            secondBestEV = bestEV;
            secondBestAction = Action.deepClone(bestAction);
         }
         bestEV = currentEV;
         bestAction = Action.HIT;
      }
      else if (currentEV > secondBestEV) {
         secondBestEV = currentEV;
         secondBestAction = Action.HIT;
      }
   }
   for (i = 0; i < actionResults.size(); i++) {
      if (actionResults.get(i).getExpectedValue() > bestEV) {
         if (bestAction != null) {
            secondBestEV = bestEV;
            secondBestAction = Action.deepClone(bestAction);
         }
         bestEV = actionResults.get(i).getExpectedValue();
         if (actionResults.get(i).testPostSplit()) {
            bestAction = Action.SPLIT;
         }
         else {
            bestAction = actionResults.get(i).lastAction();
         }
      }
      else if (actionResults.get(i).getExpectedValue() > secondBestEV) {
         secondBestEV = actionResults.get(i).getExpectedValue();
         if (actionResults.get(i).testPostSplit()) {
            secondBestAction = Action.SPLIT;
         }
         else {
            secondBestAction = actionResults.get(i).lastAction();
            if (actionResults.get(i).lastAction() == null) {
               assert false;
            }
         }
      }
   }
   if (possibleDealerBJOnHoleCard) {
      if (dealerHoleCard) {
         dealerBJWithHoleCard = Blackjack.resolveHands(dealerBJWithHoleCard, myShoe, theRules);
         if ((probDealerBJ < 0) || (probDealerBJ > 1)) {
            assert (false);
         }
         bestEV = probDealerBJ * dealerBJWithHoleCard.getExpectedValue() + (1 - probDealerBJ) * bestEV;
         secondBestEV = probDealerBJ * dealerBJWithHoleCard.getExpectedValue() + (1 - probDealerBJ) * secondBestEV;
      }
      else {
         assert (false) : "Dealer can't have an early BJ in a no-hole game.";
         throw new NoRecommendationException ("Dealer can't have an early BJ in a no-hole game.");
      }
   }
   if (bestAction == null) {
      State.printStateStatus(myState, "Action doom.");
      System.out.println("I have " + actionResults.size() + " elements in action and hitting is" + (hitPossible ? "" : "not") + " possible .");
      System.out.println("\n\n\n");
      State.printStateStatus(actionResults.get(0), "This is what's in my action vector:");
      throw new NoRecommendationException("Ya I screwed up this time. bestAction null pointer exception.");
   }
   if (secondBestAction == null) {
      if ((theRules.numPossibleActions(myState, false) != 1) && (myState.numberCardsInHand() < playerMaxHandSize)) {
         State.printStateStatus(myState, "Second action doom.");
         DealerCache.printCacheStatus();
         System.out.println(bestAction + " is my preferred action.");
         System.out.println("I have " + actionResults.size() + " elements in action and hitting is" + (hitPossible ? "" : "not") + " possible .");
         System.out.println("\n\n\n");
         System.out.println("There are " + theRules.numPossibleActions(myState, false) + " actions possible.");
         System.out.println(theRules);
         for (Action anAction : Action.values()) {
            if ((anAction == Action.INSURANCE) || (anAction == Action.ERROR)) {
               continue;
            }
            if (theRules.isPossible(anAction, myState)) {
               if (anAction == Action.HIT) {
                  System.out.println("myState.numberCardsInHand() is " + myState.numberCardsInHand() + " and " + "theRules.getPlayerMaxHandSize() is " + theRules.getPlayerMaxHandSize());
               }
               if ((anAction == Action.HIT) && (myState.numberCardsInHand() >= theRules.getPlayerMaxHandSize())) ;
               else {
                  System.out.println(anAction + " is possible.");
               }
            }
         }
         try {
            Thread.sleep(1000);
         }
         catch (Exception e) {
         }
         assert false : "IllegalStateException: secondBestAction set incorrectly in Player's Recursive.";
      }
      secondBestAction = Action.deepClone(bestAction);
      secondBestEV = bestEV;
   }
   if (!earlySurrenderPossible) {
      myState.setPreferredAction(bestAction);
      myState.setSecondBestAction(secondBestAction);
      myState.setEV(bestEV);
      myState.setSecondBestEV(secondBestEV);
      return myState;
   }
   else {
      assert (earlySurrenderState != null);
      if (earlySurrenderState.getExpectedValue() > bestEV) {
         myState.setEV(earlySurrenderState.getExpectedValue());
         if (bestAction == Action.SURRENDER) {
            System.err.println("Early surrender is the best option. However, I've already" + " checked for surrender earlier, so I checked surrender options twice, so " + "I shouldn't be here.");
            myState.setSecondBestEV(secondBestEV);
            myState.setSecondBestAction(secondBestAction);
            System.out.println("This is my state:" + myState.toString());
            assert false;
            return myState;
         }
         else {
            myState.setPreferredAction(Action.SURRENDER);
            myState.setSecondBestEV(bestEV);
            myState.setSecondBestAction(bestAction);
            return myState;
         }
      }
      else if (earlySurrenderState.getExpectedValue() > secondBestEV) {
         myState.setPreferredAction(bestAction);
         myState.setEV(bestEV);
         myState.setSecondBestEV(earlySurrenderState.getExpectedValue());
         myState.setSecondBestAction(Action.SURRENDER);
         return myState;
      }
      else {
         myState.setPreferredAction(bestAction);
         myState.setSecondBestAction(secondBestAction);
         myState.setEV(bestEV);
         myState.setSecondBestEV(secondBestEV);
         return myState;
      }
   }
}

/**
 * * Near duplicate of solveHardPlayersRecursive. Seems to work as
 * expected. EVENTUALLY MAKE THIS PRIVATE (and non-static)
 *
 * @param theRules
 * @return
 */
static ArrayList<ArrayList<State>> solveSoftPlayersRecursive(Rules theRules,
        final boolean possibleDealerBJ) throws NoRecommendationException {
   final FastShoe myShoe = new FastShoe(theRules.getNumberOfDecks());
   ArrayList<Card> myCards = new ArrayList<Card>();
   final int actualSplitsAllowed = theRules.getMaxNumberSplitHands();
   theRules.setMaxNumberSplitHands(0);
   State currentState;
   ArrayList<ArrayList<State>> solvedStates = new ArrayList<ArrayList<State>>();
   int rowNum = -1;
   int colNum = 0;
   final CardValue pCardOne = CardValue.ACE;
   for (CardValue pCardTwo : CardValue.oneToTen) {
      solvedStates.add(new ArrayList<State>());
      rowNum++;
      for (CardValue dealerCard : CardValue.twoToAce) {
         myCards.clear();
         myCards.add(new Card(Suit.CLUBS, pCardOne));
         myCards.add(new Card(Suit.CLUBS, pCardTwo));
         if (dealerCard == null) {
            throw new RuntimeException();
         }
         currentState = new State(myCards, new Card(Suit.CLUBS, dealerCard));
         if (!possibleDealerBJ) {
            currentState.setDealerBlackjack(false);
         }
         myShoe.fasterDrawSpecific(pCardOne);
         myShoe.fasterDrawSpecific(pCardTwo);
         myShoe.fasterDrawSpecific(dealerCard);
         solvedStates.get(rowNum).add(PlayerRecursive(myShoe, currentState, theRules));
         myShoe.addCard(pCardOne);
         myShoe.addCard(pCardTwo);
         myShoe.addCard(dealerCard);
         if (myShoe.numberOfCards() != 52 * theRules.getNumberOfDecks()) {
            throw new RuntimeException();
         }
      }
   }
   theRules.setMaxNumberSplitHands(actualSplitsAllowed);
   return solvedStates;
}

/**
 * Helper function for splitSolve
 *
 *
 * @param cardDrawProbs An array which represents the chances of drawing that
 * card index
 * @param myShoe The current shoe
 * @param theRules The rules
 * @param possibleDealerBJ Whether or not the dealer can have blackjack
 * @param PCard Player's card
 * @param DCard Dealer's card
 * @return An array of expected values.
 */
static double[] getBestEVOfSplitStates(FastShoe myShoe, Rules theRules,
        boolean possibleDealerBJ, CardValue PCard, CardValue DCard) 
        throws NoRecommendationException {
   double[] cardDrawProbs = myShoe.getAllProbs();
   double[] bestEVOfStates = new double[10];
   State scratch;
   ArrayList<Card> myCards = new ArrayList<Card>();
   int i = 0;
   for (CardValue q : CardValue.oneToTen) {
      if (cardDrawProbs[q.value() - 1] > 0) {
         myShoe.fasterDrawSpecific(q);
         assert (
              myShoe.numberOfCards() == (theRules.getNumberOfDecks() * 52 - 4) 
           || myShoe.numberOfCards() == (theRules.getNumberOfDecks() * 52 - 5)
           || myShoe.numberOfCards() == (theRules.getNumberOfDecks() * 52 - 6));
         if (       (PCard == CardValue.ACE) && (q == CardValue.TEN) 
                 || (PCard == CardValue.TEN) && (q == CardValue.ACE) ) {
            myCards.add(new Card(Suit.DIAMONDS, CardValue.ACE));
            myCards.add(new Card(Suit.CLUBS, CardValue.EIGHT));
            scratch = new State(myCards, new Card(Suit.CLUBS, DCard));
            scratch.action(Action.HIT, new Card(Suit.SPADES, CardValue.TWO));
         }
         else {
            myCards.add(new Card(Suit.HEARTS, PCard));
            myCards.add(new Card(Suit.CLUBS, q));
            scratch = new State(myCards, new Card(Suit.CLUBS, DCard));
         }
         if (!possibleDealerBJ) {
            scratch.setDealerBlackjack(false);
         }
         if (theRules.dealerHoleCard() == true) {
            scratch.setDealerBlackjack(false);
         }
         scratch = PlayerRecursive(myShoe, scratch, theRules);
         myCards.clear();
         myShoe.addCard(q);
         if ((    theRules.noDoublePostSplit() 
                 && scratch.getPreferredAction() == Action.DOUBLE) 
                 || ((PCard == CardValue.ACE) 
                 && (theRules.noDoubleSplitAces()) 
                 && (scratch.getPreferredAction() == Action.DOUBLE))) {
            bestEVOfStates[i] = scratch.getSecondBestEV();
         }
         else {
            bestEVOfStates[i] = scratch.getExpectedValue();
         }
      }
      else {
         bestEVOfStates[i] = -500000;
      }
      i++;
   }
   return bestEVOfStates;
}

/** 
 * 
  * Possible future speed optimizations: 
 * - Some functions calls could be inlined :(
 * 
 * myCards is [10] array with 0 meaning 0 cards of that kind. 0 1
 * 2 3 etc. Ace Two Three Four 
 *
 * endProbabilities[0] = P(Bust)
 * endProbabilities[1] = P(Natural Blackjack)
 * endProbabilities[2] = 17 etc.
 *
 */
static double[] DealerRecursive(final int[] myCards, final FastShoe myDeck,
        final Rules theRules) {
   double[] endProbabilities = new double[7];
   int i;
   for (i = 0; i < endProbabilities.length; i++) {
      endProbabilities[i] = 0;
   }
   final int handValue = Utilities.handTotal(myCards);
   final int cardsInHand = Utilities.handSize(myCards);
   if (handValue > 21) {
      endProbabilities[0] = 1;
      return endProbabilities;
   }
   if (handValue >= 17) {
      switch (handValue) {
         case 21:
            if (cardsInHand == 2) {
               endProbabilities[1] = 1;
               return endProbabilities;
            }
            else {
               endProbabilities[6] = 1;
               return endProbabilities;
            }
         case 20:
            endProbabilities[5] = 1;
            return endProbabilities;
         case 19:
            endProbabilities[4] = 1;
            return endProbabilities;
         case 18:
            endProbabilities[3] = 1;
            return endProbabilities;
         case 17:
            if ((theRules.hitOn17() == false) 
                 || !Utilities.isSoft(myCards, handValue)) {
               endProbabilities[2] = 1;
               return endProbabilities;
            }
            else ;
            break; // Soft 17.
         }
   }
   if (cardsInHand >= theRules.getDealerMaxHandSize()) {
      endProbabilities[0] = 1;
      Blackjack.setApproxProbabilities(endProbabilities, myDeck, handValue, Utilities.isSoft(myCards, handValue));
      return endProbabilities;
   }
   double[] Probabilities = myDeck.getDealerProbabilities(cardsInHand, theRules.dealerHoleCard(), myCards);
   //Solved for all the probabilities. Now start hitting me.
   double[] scratch;
   int j;
   for (i = 0; i < myCards.length; i++) {
      if (Probabilities[i] > 0) {
         try {
            myDeck.fasterDrawSpecific(i);
         }
         catch (IllegalArgumentException problem) {
            throw new IllegalArgumentException();
         }
         myCards[i] += 1;
         scratch = DealerRecursive(myCards, myDeck, theRules);
         for (j = 0; j < scratch.length; j++) {
            endProbabilities[j] = scratch[j] * Probabilities[i] + endProbabilities[j];
         }
         myCards[i] += -1;
         myDeck.addCard(i);
      }
   }
   return endProbabilities;
}

/**
 *
 * @param someStates
 * @return True if some states are advising a different first or second action
 * than
 * the others are. False if all of the recommended states are the same.
 */
static boolean anyDisagreementHere(ArrayList<State> someStates) throws NoRecommendationException {
   assert (!someStates.isEmpty());
   final Action firstAction = someStates.get(0).getPreferredAction();
   final Action secondAction = someStates.get(0).getSecondBestAction();
   for (int i = 1; i < someStates.size(); i++) {
      if (someStates.get(i).getFirstCardValue() == someStates.get(i).getSecondCardValue()) {
         throw new NoRecommendationException();
      }
      if (firstAction != someStates.get(i).getPreferredAction()) {
         return true;
      }
      if (secondAction != someStates.get(i).getSecondBestAction()) {
         return true;
      }
   }
   return false;
}

/**
 * Find if there is any disagreement as to what to do; if so, it gets ugly
 * really fast.
 * I'll make the following simplifications to avoid as much ugliness as I can:
 * --One action must be the best.
 * --The probability of each state is the same as that of any other state.
 * --If an action is not listed as being the top 2 actions by a state, I will
 * assume that its
 * EV in that state is 10 % worse than that state's second best action.
 * That's not good enough. Add in probability scaling now.
 *
 * . If not, weight all of the EVs
 * //based on their relative probability (Prob of this hand)/(Prob of all hands
 * of this
 * //card value), for the top three actions.
 *
 * @param probThisState
 * @param sumOfProbs
 * @param similarStates
 */
static void solveConsolidationAndReplace(double[] probThisState,
        double sumOfProbs, ArrayList<State> similarStates) 
        throws NoRecommendationException {
   assert (similarStates.size() == probThisState.length);
   if (similarStates.size() <= 1) {
      return;
   }
   Action bestAction = Action.ERROR;
   Action secondBestAction = Action.ERROR;
   double bestEV = -40000;
   double secondBestEV = -40000;
   int i;
   boolean discord = Blackjack.anyDisagreementHere(similarStates);
   if (!discord) {
      bestAction = similarStates.get(0).getPreferredAction();
      secondBestAction = similarStates.get(0).getSecondBestAction();
   }
   else {
      //ICK
      //find top score, second best score. Find associated action. (same index)
      //Scale according to size of similarStates.
      double[] score = new double[4];
      Utilities.zero(score);
      Action[] possibleActions = new Action[4];
      possibleActions[0] = Action.HIT;
      possibleActions[1] = Action.STAND;
      possibleActions[2] = Action.SURRENDER;
      possibleActions[3] = Action.DOUBLE;
      int j = 0;
      for (Action anAction : possibleActions) {
         for (i = 0; i < similarStates.size(); i++) {
            if (similarStates.get(i).getPreferredAction() == anAction) {
               score[j] += similarStates.get(i).getExpectedValue() * probThisState[i] / sumOfProbs;
            }
            else if (similarStates.get(i).getSecondBestAction() == anAction) {
               score[j] += similarStates.get(i).getSecondBestEV() * probThisState[i] / sumOfProbs;
            }
            else {
               score[j] += (similarStates.get(i).getSecondBestEV() - 0.1) * probThisState[i] / sumOfProbs;
            }
         }
         j++;
      }
      int bestIndex = -1;
      int secondBestIndex = -1;
      double bestScore = -50;
      double secondBestScore = -51;
      for (j = 0; j < score.length; j++) {
         if (score[j] > bestScore) {
            secondBestIndex = bestIndex;
            secondBestScore = bestScore;
            bestIndex = j;
            bestScore = score[j];
         }
         else if (score[j] > secondBestScore) {
            secondBestScore = score[j];
            secondBestIndex = j;
         }
      }
      bestAction = possibleActions[bestIndex];
      secondBestAction = possibleActions[secondBestIndex];
      bestEV = bestScore;
      secondBestEV = secondBestScore;
   }
   for (int k = 0; k < similarStates.size(); k++) {
      if (!discord) {
         if (!bestAction.equals(similarStates.get(k).getPreferredAction())) {
            throw new NoRecommendationException("Error in anyDisAgreement here: there was indeed disagreement."
                    + " The best action was thought to be " + bestAction
                    + ", but here is a state that says otherwise: " + similarStates.get(k).toString());
         }
         if (!secondBestAction.equals(similarStates.get(k).getSecondBestAction())) {
            throw new NoRecommendationException("Error in anyDisAgreement here: there was indeed disagreement."
                    + " The second best action was thought to be " + secondBestAction
                    + ", but here is a state that says otherwise: " + similarStates.get(k).toString());
         }

      }
      else {
         similarStates.get(k).setPreferredAction(bestAction);
         similarStates.get(k).setSecondBestAction(secondBestAction);
         similarStates.get(k).overWriteEV(bestEV);
         similarStates.get(k).setSecondBestEV(secondBestEV);
     }
   }
}

/**
 * Factors in the chance of the dealer having blackjack, including insurance in
 * a hole card game.
 * Otherwise, just returns original split EV. If probDealerBJ
 * is less than 0, it just returns originalSplitEV
 * Helper function for splitSolve.
 *
 */
private static double splitApproxIncludingDealerBJ(Rules theRules,
        final double probDealerBJ, final double originalSplitEV, CardValue PCard,
        CardValue DCard) {
   if ((theRules.dealerHoleCard() == true) && (probDealerBJ > 0)) {
      State scratch = new State(PCard, PCard, DCard);
      if (scratch.isInsuranceAdvised()) {
         return (1 - probDealerBJ) * (originalSplitEV - 0.5) + (probDealerBJ) * 0;
      }
      return (1 - probDealerBJ) * (originalSplitEV) + (probDealerBJ) * -1;

   }
   return originalSplitEV;
}

private static double noHitSplitAcesSolve(CardValue DCard, CardValue PCard,
        boolean possibleDealerBJ, FastShoe myShoe, Rules theRules) 
        throws NoRecommendationException {
   State scratch = new State(PCard, PCard, DCard);
   if ( (!possibleDealerBJ) && 
        ( (DCard == CardValue.TEN) || (DCard == CardValue.ACE)) ) {
      scratch.setDealerBlackjack(false);
   }
   scratch = Blackjack.PlayerRecursive(myShoe, scratch, theRules);
   if (scratch.getPreferredAction() == Action.SPLIT) {
      return scratch.getExpectedValue();
   }
   else if (scratch.getSecondBestAction() == Action.SPLIT) {
      return scratch.getSecondBestEV();
   }
   else {
      if (!theRules.dealerHoleCard() && 
          ( (DCard == CardValue.TEN) || (DCard == CardValue.ACE)) ) 
         ;
      else {
         StringBuilder builder = new StringBuilder();
         builder.append("Splitting aces is the third best option.\n")
                .append(theRules)
                .append("I'm in Strategy.noHitSplitAcesSolve\n")
                .append("Here is my state:\n")
                .append(scratch);
         assert false: builder.toString();
      }
      return -100.0;
   }
}

/**
 * Tested under 1 rule set; mostly works, but off by a large margin in certain
 * rare circumstances (when only one deck is being used). Don't know why.
 * Note the special treatment of dealer blackjacks. In a hole card game, they
 * are impossible on split hands, since you'd never split. 
 * However, they still must be factored into this
 * decision, since they are factored into the other decisions. So: completely
 * discount them when figuring out the probability, then factor them back in
 * at the end.
 *
 * Could use better testing; watch carefully for changes to the rule set that
 * persist after this function is done.
 * This always assumes a pristine shoe.
 *
 * @param theRules
 * @param PCard Player CardValue
 * @param DCard Dealer CardValue
 * @return The approximate expected value of splitting, given the
 * Rule Set, a pristine shoe, dealer CardValue DCard, and a pair of
 * PCard CardValue's in hand. Tries to take into account any
 * post-split doubling restriction. Essentially, calculates the
 * approximate split value, duplicate the Hard entry (or soft for A
 * A), then modify it, if needed, if splitting is at least as good
 * as the 2nd best answer.
 *
 * Need to test resplit numbers and rules variations. MAKE THIS
 * PRIVATE AND NON-STATIC LATER.
 * @throws IllegalArgumentException if being asked to do an illegal action
 */
static double splitSolve(Rules theRules, CardValue PCard, CardValue DCard,
        final boolean possibleDealerBJ) throws NoRecommendationException {
   final long originalRulesHash = theRules.myHashKey();

   final boolean acePlayer = (PCard == CardValue.ACE) ? true : false;
   FastShoe myShoe = new FastShoe(theRules.getNumberOfDecks());
   myShoe.fasterDrawSpecific(DCard);
   myShoe.fasterDrawSpecific(PCard);
   myShoe.fasterDrawSpecific(PCard);
   double probDealerBJ;
   if (possibleDealerBJ) {
      if (DCard == CardValue.TEN) {
         probDealerBJ = myShoe.fastProbabilityOf(CardValue.ACE);
      }
      else if (DCard == CardValue.ACE) {
         probDealerBJ = myShoe.fastProbabilityOf(CardValue.TEN);
      }
      else {
         probDealerBJ = -1000;
      }
   }
   else {
      probDealerBJ = -1000;
   }
   if ((acePlayer) && !theRules.hitSplitAces()) {
      return noHitSplitAcesSolve(DCard, PCard, possibleDealerBJ, myShoe, theRules);
   }
   final boolean actualEarlySurrender = theRules.getEarlySurrender();
   final boolean actualLateSurrender = theRules.getLateSurrender();
   final boolean actualEarlySurrenderNotOnAces = theRules.getEarlySurrenderNotOnAces();
   final double actualBlackJackPayback = theRules.getBlackJackPayback();
   final int maxNumberSplitHands = theRules.getMaxNumberSplitHands();


   //Garbage values -- these should never be used.
   theRules.setBlackjackPayback(9.98);
   theRules.setEarlySurrender(false);
   theRules.setLateSurrender(false);
   theRules.setMaxNumberSplitHands(0);
   theRules.setEarlySurrenderNotOnAces(false);

   final boolean resplitPossible;
   if ((maxNumberSplitHands > 1) && !(acePlayer && (theRules.getNumResplitAces() == 0))) {
      resplitPossible = true;
   }
   else {
      resplitPossible = false;
   }
   double[] bestEVOfStates = Blackjack.getBestEVOfSplitStates(myShoe, theRules, 
           possibleDealerBJ, PCard, DCard);
   //BUTT E-Z ALGORITHM BELOW
   //The way I understand it, if there is no resplitting, then I can just say:
   // sumEVDualProb = 2 * (weightedEV of bestEVOfStates); and be done with it.
   // If there is resplitting, I need to do another calculation, wherein I
   // replace bestEVOfStates[PCard] with its resplit calculation, which is just
   // the weighted EV of bestEVOfResplitStates [] = getBestEVOfSplitStates(yada)
   // , called after I remove the one extra card to it.

   double penUltimateEV;

   penUltimateEV = combinedProbSplitApprox(bestEVOfStates, myShoe, PCard, 
           resplitPossible, theRules, possibleDealerBJ, DCard);
   assert (penUltimateEV < 10) : penUltimateEV + " is the problem.";
   final double splitEVAnswer = splitApproxIncludingDealerBJ(theRules, 
           probDealerBJ, penUltimateEV, PCard, DCard);
   theRules.setBlackjackPayback(actualBlackJackPayback);
   theRules.setEarlySurrender(actualEarlySurrender);
   theRules.setLateSurrender(actualLateSurrender);
   theRules.setMaxNumberSplitHands(maxNumberSplitHands);
   theRules.setEarlySurrenderNotOnAces(actualEarlySurrenderNotOnAces);
   assert (originalRulesHash == theRules.myHashKey());
   return splitEVAnswer;
}

static double combinedProbSplitApprox(final double[] bestEVOfFirstSplitHand,
        FastShoe myShoe, CardValue PCard, final boolean resplitAllowed,
        Rules theRules, boolean dealerBJPossible, CardValue DCard) 
        		throws NoRecommendationException {
   final double[] probNextCard = myShoe.getAllProbs();
   double probThisCombo;
   double evNoResplit = 0;
   double evResplit = 0;
   double sumTest = 0;
   double probPossResplit = 0;
   double[] bestEVOfSecondSplitHand;
   double[] probOfSecondCard;
   double bestEVOfResplit = 0;

   double fastEVOfResplit = 0;
   myShoe.fasterDrawSpecific(PCard);
   final double[] fastProbCardsOnResplit = myShoe.getAllProbs();
   final double[] fastBestEVOfThirdSplitHand = Blackjack.getBestEVOfSplitStates
           (myShoe, theRules, dealerBJPossible, PCard, DCard);
   for (int kk = 0; kk < fastBestEVOfThirdSplitHand.length; kk++) {
      if (fastProbCardsOnResplit[kk] > 0) {
         fastEVOfResplit += fastBestEVOfThirdSplitHand[kk] * fastProbCardsOnResplit[kk];
      }
   }
   myShoe.addCard(PCard);
   for (CardValue firstDrawnCard : CardValue.oneToTen) {
      myShoe.fasterDrawSpecific(firstDrawnCard);
      probOfSecondCard = myShoe.getAllProbs();
      bestEVOfSecondSplitHand = bestEVOfFirstSplitHand;
      for (CardValue secondDrawnCard : CardValue.oneToTen) {
         if ((probNextCard[(firstDrawnCard.value() - 1)] > 0) && (probOfSecondCard[(secondDrawnCard.value() - 1)] > 0)) {
            probThisCombo = probNextCard[(firstDrawnCard.value() - 1)] * probOfSecondCard[secondDrawnCard.value() - 1];
            if (Blackjack.debug()) {
               sumTest += probThisCombo;
            }
            evNoResplit += probThisCombo * (bestEVOfFirstSplitHand[(firstDrawnCard.value() - 1)] + bestEVOfSecondSplitHand[(secondDrawnCard.value() - 1)]);
            if (evNoResplit > 10) {
               System.err.println("Strategy.combinedProbSplitApprox:" + "probThisCombo: " + probThisCombo + "; bestEVOfFirstSplitHand[ (firstDrawnCard.value() -1)]: " + bestEVOfFirstSplitHand[(firstDrawnCard.value() - 1)] + "bestEVOfSecondSplitHand[ (secondDrawnCard.value() -1)])" + bestEVOfSecondSplitHand[(secondDrawnCard.value() - 1)]);
               assert false;
            }
            if (resplitAllowed && (firstDrawnCard == PCard)) {
               evResplit += probThisCombo * (fastEVOfResplit * 3);
            }
            else if (resplitAllowed && (secondDrawnCard == PCard)) {
               evResplit += probThisCombo * (bestEVOfFirstSplitHand[(firstDrawnCard.value() - 1)] + 2 * fastEVOfResplit);
            }
            else {
               evResplit += probThisCombo * (bestEVOfFirstSplitHand[(firstDrawnCard.value() - 1)] + bestEVOfSecondSplitHand[(secondDrawnCard.value() - 1)]);
            }
         }
      }
      myShoe.addCard(firstDrawnCard);
   }
   assert ((sumTest < 1.001) && (sumTest > 0.999));

   if (!resplitAllowed) {
      return evNoResplit;
   }
   if (evResplit > evNoResplit) {
      return evResplit;
   }

   return evNoResplit;
   
}

/**
 *
 *
 * @param hardAnswers Modifies hardAnswers so that all player hands
 * with the same hand value and same dealer up card have the same
 * preferred Action and second best Action and same EV.
 * Excluding split hands. Don't consolidate them.
 *
 * Consolidate the
 * split hands too; if I actually can split and want to, I'll be looking at
 * the split result anyway; if I can't split, then ...well, depending on the
 * rule set and current situation, it may or may not be possible that I have
 * the hard hand which has two of the same hand value. I'm going to do things
 * in the simplest way here. I don't think it'll have a big impact.
 *
 * (I wrote before:
 * EXCLUDING
 * split hands. Do NOT consolidate those, just leave them. In all
 * the other hands I couldn't split, so the results would be off. )
 * This is set to protected for testing purposes.
 */
static void consolidateIntoTotalDependent(
        ArrayList<ArrayList<State>> hardAnswers, Rules theRules) 
        throws NoRecommendationException {
   ArrayList<State> similarStates = new ArrayList<State>();
   ArrayList<State> columnOfStates = new ArrayList<State>();
   FastShoe myShoe = new FastShoe(theRules.getNumberOfDecks());
   double sumOfProbs;
   double[] probThisState;
   for (int column = 0; column < hardAnswers.get(0).size(); column++) {
      for (int handValue = 4; handValue < 19; handValue++) {
         columnOfStates.clear();
         sumOfProbs = 0;
         similarStates.clear();
         for (int j = 0; j < hardAnswers.size(); j++) {
            columnOfStates.add(hardAnswers.get(j).get(column));
         }
         similarStates = retrieveStatesOfHandValue(handValue, columnOfStates);
         assert (similarStates != null);
         assert (similarStates.size() <= columnOfStates.size());
         probThisState = new double[similarStates.size()];
         for (int ii = 0; ii < similarStates.size(); ii++) {
            probThisState[ii] = myShoe.probTheseThreeInOrder(
                    similarStates.get(ii).getFirstCardValue(), 
                    similarStates.get(ii).getSecondCardValue(), 
                    similarStates.get(ii).getDealerUpCard().getCardValue());
            sumOfProbs += probThisState[ii];
         }
         Blackjack.solveConsolidationAndReplace(probThisState, sumOfProbs, similarStates);
      }
   }
}

/**
 * EXCLUDES STATES which has cards of the same value -- those are possible split
 * hands and I believe that usually the player has the option to split with
 * them.
 * CONTAINS EXCESS BUG CHECKING for debugging purposes.
 *
 */
private static ArrayList<State> retrieveStatesOfHandValue(final int handValue,
        ArrayList<State> theStates) {
   ArrayList<State> constantHandValue = new ArrayList<State>();
   for (int i = 0; i < theStates.size(); i++) {
      if ((theStates.get(i).handTotal() == handValue) && 
         (theStates.get(i).getFirstCardValue() != theStates.get(i).getSecondCardValue())) {
         constantHandValue.add(theStates.get(i));
      }
   }
   for (int j = 0; j < (constantHandValue.size() - 1); j++) {
      if (constantHandValue.get(j).getDealerUpCard().value() == constantHandValue.get(j + 1).getDealerUpCard().value()) ;
      else {
         assert false;
      }
      if (constantHandValue.get(j).handTotal() != constantHandValue.get(j + 1).handTotal()) {
         State.printStateStatus(constantHandValue.get(j), "First failing state");
         State.printStateStatus(constantHandValue.get(j + 1), "Second failing state");
         assert false;
      }
   }
   return constantHandValue;
}

}
