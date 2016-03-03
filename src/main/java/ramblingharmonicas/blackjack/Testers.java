package ramblingharmonicas.blackjack;
import ramblingharmonicas.blackjack.cards.*;

import java.io.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

/**
 ***** TODO: Move all tests in this file into JUnit.  ****************
 * TODO: Move all code which solves and then stores data sets into separate file.
 * TODO: Move all non-tests into appropriate files.
 *
 */
public class Testers {

public static void callTests() {
	Testers.allFastTests();

	Strategy someStrat = new Strategy( new Rules(8), Strategy.Skill.TOTAL_DEP);
	 someStrat.testToggles();
}

public static void allSlowTests() {
       /* Don't run these tests when the raw data files don't exist, or
    * when calculations are deactivated.*/
    runAllCalculations(false);
    consolidateFiles(false);
    validateNonConsolidatedFiles(false);
    //This should throw an exception quickly if calculations are deactivated
    

	validateConsolidatedFiles(true); //This takes about 14 minutes.

}
private static void runAllCalculations(boolean verbosity) {
   System.out.println("Completing all calculations. This takes ~80 hours if not already done.-------");
   final int[] deckArray = Strategy.solvedNumberOfDecks;//new int[]{1,2,4,6,8};

   //Create the Strategy and give it the proper skill level.
   Strategy allStrategies = new Strategy(new Rules(1), Strategy.Skill.COMP_DEP);
   allStrategies.solveStoreEverything(deckArray[4], verbosity);

   allStrategies.solveStoreEverything(deckArray[0], verbosity);
   allStrategies.solveStoreEverything(deckArray[1], verbosity);
   allStrategies.solveStoreEverything(deckArray[2], verbosity); //~1 day per line.
   allStrategies.solveStoreEverything(deckArray[3], verbosity);
   /*
    * these ones will be 5x faster: (b/c I'm not changing the BJ payback array)
    * These do not get saved in the Total EV map.
    */
   allStrategies = new Strategy(new Rules(1), Strategy.Skill.TOTAL_DEP);

   for (int i = 0; i < deckArray.length; i++) {
      allStrategies.solveStoreEverything(deckArray[i], verbosity);
   }

   System.out.println("Calculations complete.");

}

private static void runValidateConsolidateCalculations(boolean verbosity) {

   runAllCalculations(verbosity);
   consolidateFiles(verbosity);
   validateConsolidatedFiles(verbosity);
   validateNonConsolidatedFiles(verbosity);
}

/**
 * This takes roughly 20 minutes.
 *
 * @param verbosity
 *
 */
private static void validateConsolidatedFiles(boolean verbosity) {
   System.out.println("Starting validation of consolidated files.");
   Strategy allStrategies = new Strategy(new Rules(1), Strategy.Skill.COMP_DEP);
   allStrategies.validateCalculations(verbosity, Strategy.CONSOLIDATED_BY_DECKS_FILES);
   allStrategies = new Strategy(new Rules(1), Strategy.Skill.TOTAL_DEP);
   allStrategies.validateCalculations(verbosity, Strategy.CONSOLIDATED_BY_DECKS_FILES);
   System.out.println("Consolidated validations are complete.");

}

public static void validateNonConsolidatedFiles(boolean verbosity) {
   long k = System.currentTimeMillis();
   System.out.println("Starting validation of non-consolidated files.");
   Strategy allStrategies = new Strategy(new Rules(1), Strategy.Skill.TOTAL_DEP);
   allStrategies.validateCalculations(verbosity, Strategy.MANY_SMALL_FILES);

   allStrategies = new Strategy(new Rules(1), Strategy.Skill.COMP_DEP);
   allStrategies.validateCalculations(verbosity, Strategy.MANY_SMALL_FILES);

   final long q = System.currentTimeMillis() - k;
   System.out.println("Non-consolidated validations are complete. Minutes taken: " + (q / 60000));


}

public static void consolidateFiles(boolean verbosity) {
   long k = System.currentTimeMillis();
   System.out.println("Consolidating strategy files.");

   //Create the Strategy and give it the proper skill level.
   Strategy allStrategies = new Strategy(new Rules(1), Strategy.Skill.COMP_DEP);
   allStrategies.consolidateAllFiles(verbosity);
   allStrategies = new Strategy(new Rules(1), Strategy.Skill.TOTAL_DEP);
   allStrategies.consolidateAllFiles(verbosity);
   final long q = System.currentTimeMillis() - k;
   System.out.println("Consolidation is complete. Minutes taken: " + (q / 60000));
}

/**
 * Basic tests of Strategy class.
 *
 * At one point, these tests required maximum accuracy -- cache accuracy
 * was not accurate enough. Now, they pass with cache accuracy.
 *
 */
static public class StrategyTest {
private Rules theRules;
private Shoe myShoe;
private Strategy myStrategy;
private State aState;
private ArrayList<Card> myCards = new ArrayList<Card>();

public StrategyTest() {
   initAll();
}

private void initAll() {
   theRules = new Rules(1);
   theRules.setAccuracy(Rules.CACHE_ACCURACY);
   myShoe = new Shoe(theRules.getNumberOfDecks());
   myStrategy = new Strategy(theRules, Strategy.Skill.COMP_DEP);
}

public void runAllTests(boolean verbosity) throws IOException, NoRecommendationException {
   final long rulesKey = theRules.myHashKey();


   testFindBestActionOmniBus(verbosity);
   if (rulesKey != theRules.myHashKey()) {
      throw new RuntimeException("Rules mismatch");
   }

   initAll();
   testVeryEasyStrategy(verbosity);
   initAll();
   testSolveAndStore(verbosity);
   if (rulesKey != theRules.myHashKey()) {
      throw new RuntimeException("Rules mismatch");
   }
   if (verbosity) {
      System.out.println("First two tests in StrategyTest are complete.");
   }
   testAnswerLoad(verbosity);
   initAll();
   if (rulesKey != theRules.myHashKey()) {
      throw new RuntimeException("Rules mismatch");
   }

   if (verbosity) {
      System.out.println("First three tests in StrategyTest.runAllTests are complete.");
   }

   initAll();
   testConsolidateHardAndOmniBus(verbosity);
   initAll();

   Rules testRules = new Rules(4);
   Strategy someStrat = new Strategy(testRules, Strategy.Skill.TOTAL_DEP);
   testTotalConsolidatedForConsolidation(verbosity, someStrat);

   if (rulesKey != theRules.myHashKey()) {
      throw new RuntimeException("Rules mismatch");
   }


}

public void testVeryEasyStrategy(boolean verbosity) throws NoRecommendationException, IOException {
   Strategy supaEZ = new Strategy(theRules, Strategy.Skill.VERY_EASY);
   State scratch;
   for (CardValue playerOne : CardValue.values()) {
      for (CardValue playerTwo : CardValue.values()) {
         for (CardValue dealer : CardValue.values()) {
            scratch = new State(playerOne, playerTwo, dealer);
            supaEZ.findBestAction(scratch);
            supaEZ.findSecondBestAction(scratch);
         }
      }
   }
   if (verbosity) {
      System.out.println("Test passed: the easiest strategy fills in the hashmap for all card combinations.");
   }
}

/**
 * Tests about 4 Strategy recommendations. Calls testSplitRecommendations.
 *
 * @param verbosity
 * @throws IOException
 * @throws ClassNotFoundException
 *
 */
public void testFindBestActionOmniBus(boolean verbosity) throws IOException {
   myStrategy = new Strategy(theRules, Strategy.Skill.COMP_DEP);
   myCards.add(new Card(Suit.SPADES, CardValue.TWO));
   myCards.add(new Card(Suit.SPADES, CardValue.FIVE));
   aState = new State(myCards, new Card(Suit.CLUBS, CardValue.FIVE));

   theRules.setAccuracy(Rules.LOW_ACCURACY);

   Action anAction = null;
   try {
      anAction = myStrategy.findBestAction(aState, theRules, myShoe);
   }
   catch (NoRecommendationException e) {
      e.printStackTrace();
      assert false;

   }
   assert (anAction == Action.HIT);

   myCards.clear();
   myCards.add(new Card(Suit.SPADES, CardValue.ACE));
   myCards.add(new Card(Suit.SPADES, CardValue.ACE));
   aState = new State(myCards, new Card(Suit.CLUBS, CardValue.ACE));
   aState.setDealerBlackjack(false);
   try {
      anAction = myStrategy.findBestAction(aState, theRules, myShoe);
   }
   catch (NoRecommendationException e) {
      e.printStackTrace();
      assert false;
   }
   if (anAction != Action.SPLIT) {
      assert (false);
   }

   testSplitRecommendations(verbosity);


   /*myStrategy = new Strategy(theRules, Strategy.Skill.ADVANCED);
    myCards.add(new Card(Suit.SPADES, CardValue.TWO));
    myCards.add(new Card(Suit.SPADES, CardValue.FIVE));
    aState = new State(myCards, new Card(Suit.CLUBS, CardValue.FIVE));

    theRules.setAccuracy(Rules.LOW_ACCURACY);

    Action anAction = null;
    try
    {
    anAction = myStrategy.findBestAction(myShoe, theRules, aState);
    }
    catch (NoRecommendationException e)
    {
    e.printStackTrace();
    assert false;

    }
    assert (anAction == Action.HIT);*/
   Rules someRules = new Rules(2);
   someRules.setEarlySurrenderNotOnAces(true);
   someRules.myDoubleRules.setOnlyNineTenEleven(true);
   someRules.myDoubleRules.setNotPostSplit(false);
   someRules.setHitSplitAces(true);
   someRules.setHitOn17(false);
   someRules.setHoleCard(false);
   someRules.setMaxNumberSplitHands(2);
   //http://www.beatingbonuses.com/bjstrategy.php?decks=2&soft17=stand&doubleon=9to11s&peek=off&das=on&dsa=on&resplits=on&surrender=early&opt=1&btn=Calculate
   myStrategy = new Strategy(someRules, Strategy.Skill.TOTAL_DEP);
   try {
      myStrategy.solve(someRules);
      aState = new State(CardValue.TWO, CardValue.TEN, CardValue.THREE);

      anAction = myStrategy.findBestAction(aState);
      assert (anAction == Action.HIT);
      anAction = myStrategy.findBestAction(new State(CardValue.SEVEN,
              CardValue.SEVEN, CardValue.TEN));
      //Answer theAnswer = myStrategy.findBestAnswer
      //        (new Shoe(someRules.getNumberOfDecks()), someRules, aState);
      //System.out.println(theAnswer);
      assert (anAction == Action.SURRENDER) : "I chose to " + anAction;

      aState = new State(CardValue.ACE, CardValue.SEVEN, CardValue.ACE);
      // aState.setDealerBlackjack(false);  // ?
      anAction = myStrategy.findBestAction(aState);
      assert (anAction == Action.HIT);
      anAction = myStrategy.findBestAction(new State(CardValue.NINE, CardValue.NINE,
              CardValue.SEVEN));
      assert (anAction == Action.STAND);
      anAction = myStrategy.findBestAction(new State(CardValue.FOUR, CardValue.FOUR,
              CardValue.FIVE));
      assert (anAction == Action.SPLIT);
      anAction = myStrategy.findBestAction(new State(CardValue.EIGHT, CardValue.THREE,
              CardValue.TEN));
      assert (anAction == Action.HIT);
      aState = new State(CardValue.EIGHT, CardValue.EIGHT, CardValue.TEN);
      anAction = myStrategy.findBestAction(aState);
      assert (anAction == Action.SURRENDER);

   }
   catch (NoRecommendationException nre) {
      throw new RuntimeException(nre);

   }

}

/**
 * This tests two or three split recommendations.
 *
 * Called by testFindBestActionOmniBus
 *
 */
private void testSplitRecommendations(boolean verbosity) throws IOException {


   Card dealerCard = new Card(Suit.CLUBS, CardValue.TEN);
   myCards.clear();
   myCards.add(new Card(Suit.CLUBS, CardValue.SIX));
   myCards.add(new Card(Suit.CLUBS, CardValue.SIX));
   aState = new State(myCards, dealerCard);
   aState.setDealerBlackjack(false);
   Action anAction = null;
   try {
      anAction = myStrategy.findBestAction(aState, theRules, myShoe);
   }
   catch (NoRecommendationException e) {
      e.printStackTrace();
      assert (false);
   }
   assert anAction == Action.HIT;

   myCards.clear();
   myCards.add(new Card(Suit.CLUBS, CardValue.SEVEN));
   myCards.add(new Card(Suit.CLUBS, CardValue.SEVEN));
   aState = new State(myCards, dealerCard);
   aState.setDealerBlackjack(false);
   try {
      anAction = myStrategy.findBestAction(aState, theRules, myShoe);
   }
   catch (NoRecommendationException e) {
      e.printStackTrace();
      assert (false);
   }

   if (anAction != Action.SURRENDER) { //This has failed before.
      if (anAction == null) {
         throw new NullPointerException();
      }
      System.out.println("Recommended action against a dealer 10, player 7 7, is "
              + anAction.toString());
      System.out.println("The rule says that surrendering is possible: "
              + theRules.isPossible(Action.SURRENDER, aState));
      Answer hard, split;
      hard = myStrategy.pullAnswer(aState.getAnswerHash(false));
      if (hard == null) {
         throw new NullPointerException();
      }
      try {
         split = myStrategy.pullAnswer(aState.getAnswerHash(true));
         assert false : "Splitting is much worse than hitting or standing for 10-7-7, so this"
                 + " should be null.";
      }
      catch (NoSuchElementException e) { //As expected; splitting is not a good solution.
      }
      ArrayList<Answer> someAnswers = new ArrayList<Answer>();
      someAnswers.add(hard);

      assert false;
   }

   myCards.clear();
   myCards.add(new Card(Suit.CLUBS, CardValue.EIGHT));
   myCards.add(new Card(Suit.CLUBS, CardValue.EIGHT));
   aState = new State(myCards, dealerCard);
   aState.setDealerBlackjack(false);
   try {
      anAction = myStrategy.findBestAction(aState, theRules, myShoe);
   }
   catch (NoRecommendationException e) {
      e.printStackTrace();
      assert (false);
   }
   assert anAction == Action.SPLIT;


}

static public void printTestToggleReport() {
   System.out.println("testToggles was called " + numberRulesSets + " times.");
   System.out.println("It accepted " + numberSetsAccepted + " rule sets.");
   System.out.println("The map size is " + listOfRulesKeys.size());

}

static int numberRulesSets;
static int numberSetsAccepted;
static Set listOfRulesKeys = new TreeSet();

/**
 * This is a helper function. It should not be called on its own.
 *
 * @param someRules
 */
static public void testToggles(Rules someRules) {
   numberRulesSets++;
   if (listOfRulesKeys.add(someRules.myHashKey())) {
      numberSetsAccepted++;
   }
   else
    ;


}

/**
 * Checks that solveAndStore is not throwing exceptions.
 * If this Strategy has not already been saved, this will solve and save it.
 * If this Strategy has been saved, this will load the strategy.
 *
 *
 * @param verbosity
 * @throws IOException
 * @throws NoRecommendationException
 *
 */
public void testSolveAndStore(boolean verbosity) throws IOException, NoRecommendationException {
   myStrategy = new Strategy(theRules, Strategy.Skill.COMP_DEP);
   final boolean actingSolo = true;
   if (!myStrategy.solveAndStore(theRules, actingSolo)) {
      throw new IOException();
   }
   if (verbosity) {
      System.out.println("Testing solve and store functionality--------------------");
      System.out.println(theRules.myHashKey() + " should have been saved to file.");

   }
   if (!myStrategy.solveAndStore(new Rules(8), actingSolo)) {
      throw new IOException();
   }
   if (verbosity) {
      System.out.println((new Rules(8)).myHashKey() + " should have been saved to file.");
   }
}

/**
 * Clocks loading time for two files.
 *
 *
 * @param verbosity
 * @throws IOException
 */
public void testAnswerLoad(boolean verbosity) throws IOException {
   final long initTime = System.currentTimeMillis();
   theRules = new Rules(1);
   Testers.testTotalEV.wrapperCheckTotalEV(-0.000001, theRules, verbosity);
   theRules = new Rules(8);
   Testers.testTotalEV.wrapperCheckTotalEV(0.00608, theRules, verbosity);

   if (verbosity) {
      System.out.println("Loading two files took: " + (System.currentTimeMillis() - initTime) + " ms.");
   }
}

/**
 * This does NOT test the actual EVs. It only checks that the total-dependent
 * strategy matches the composition-dependent strategy everywhere it should.
 *
 * Low accuracy is NOT good enough.
 */
public void testConsolidateHardAndOmniBus(boolean verbosity) {  //FACTORS OUT THE CHANCE OF DEALER BLACKJACK
   try {
      Rules someRules = new Rules(2);
      someRules.setMaxNumberSplitHands(2);
      someRules.setHitOn17(false);

      Strategy aStrategy = new Strategy(someRules, Strategy.Skill.TOTAL_DEP);
//      http://wizardofodds.com/games/blackjack/appendix/3b/


      State someState = new State(CardValue.NINE, CardValue.TWO, CardValue.ACE);
      assert (aStrategy.findBestAction(someState) == Action.DOUBLE);      //-> Hit comp-dep
      if (verbosity) {  //haven't loaded the rules into the strategy before the first assert
         System.out.println("StrategyTest.testConsolidateHardAndOmniBus: Here is a rule set, "
                 + " and its total-dependent strategy, which I am testing.");
         System.out.println(someRules);
         aStrategy.print(true);
      }
      someState = new State(CardValue.EIGHT, CardValue.THREE, CardValue.ACE);
      assert (aStrategy.findBestAction(someState) == Action.DOUBLE);    //-> Hit comp-dep
      someState = new State(CardValue.TEN, CardValue.TWO, CardValue.FOUR);
      assert (aStrategy.findBestAction(someState) == Action.STAND);    //-> Hit comp-dep
      someState = new State(CardValue.SEVEN, CardValue.EIGHT, CardValue.TEN);
      someState.setDealerBlackjack(false); //That way you can legally surrender:
      assert (aStrategy.findBestAction(someState) == Action.SURRENDER) :
              (aStrategy.findBestAnswer(new Shoe(someRules.getNumberOfDecks()), someRules, someState)).toString();
      //-> Hit comp-dep
      //This last one fails.

      someRules = new Rules(1);
      someRules.setHitOn17(false);
      someRules.setLateSurrender(false);
      //http://wizardofodds.com/games/blackjack/appendix/3a/
      //"Boss Media single deck"
      someState = new State(CardValue.SIX, CardValue.TWO, CardValue.FIVE);
      assert (aStrategy.findBestAction(someState, someRules) == Action.DOUBLE);
      //You should hit if you're playing composition-dependent.
      //Total-dependent should say to hit.
      if (verbosity) {  //haven't loaded the rules into the strategy before the first assert
         System.out.println("StrategyTest.testConsolidateHardAndOmniBus: Here is a rule set, "
                 + " and its total-dependent strategy, which I am testing.");
         System.out.println(someRules);
         aStrategy.print(true);
      }
      someState = new State(CardValue.SIX, CardValue.TWO, CardValue.SIX);
      assert (aStrategy.findBestAction(someState, someRules) == Action.DOUBLE); //Hit comp-dep
      someState = new State(CardValue.TEN, CardValue.TWO, CardValue.SIX);
      assert (aStrategy.findBestAction(someState, someRules) == Action.STAND); //Hit comp-dep

      someState = new State(CardValue.TEN, CardValue.TWO, CardValue.FOUR);
      assert (aStrategy.findBestAction(someState, someRules) == Action.STAND); //Hit comp-dep


      //Below here is a test done without using the Strategy framework
      ArrayList<ArrayList<State>> hardAnswers;
      ArrayList<ArrayList<State>> totalDependent;
      ArrayList<ArrayList<State>> softAnswers;
      ArrayList<Answer> splitAnswers;

      //theRules = new Rules(1);
      final long key = theRules.myHashKey();
      if (key != (new Rules(1).myHashKey())) {
         throw new RuntimeException("Rules corruption in Strategy class test.");
      }
      myStrategy = new Strategy(theRules, Strategy.Skill.TOTAL_DEP);

      hardAnswers = Calculation.solveHardPlayersRecursive(theRules, false);
      totalDependent = Calculation.solveHardPlayersRecursive(theRules, false);
      softAnswers = Calculation.solveSoftPlayersRecursive(theRules, false);
      splitAnswers = Calculation.calculateAllSplitValues(theRules, hardAnswers, softAnswers, false);

      if (verbosity) {
         Testers.printStrategy(hardAnswers, theRules.toString(), false);
         Testers.printStrategy(softAnswers, "", false);
         ///??TODO: Add Answer validation here?
      }


      Calculation.consolidateIntoTotalDependent(totalDependent, theRules);

      if (verbosity) {
         System.out.println("\nConsolidated hard table:");
         Testers.printStrategy(totalDependent, "", false);

      }
//Compare the two tables now. They should be identical except for 8 and 12.
      // Testers.printStrategy(
      for (int i = 0; i < hardAnswers.size(); i++) {
         for (int j = 0; j < hardAnswers.get(0).size(); j++) {
            if ((hardAnswers.get(i).get(j).handTotal() == 8)
                    && (hardAnswers.get(i).get(j).getDealerUpCard().getCardValue() == CardValue.SIX))
            ; //it's okay, a player 6 and 2 vs. a dealer 6 should hit not double.
            //http://wizardofodds.com/games/blackjack/appendix/3c/
            //The appendix and his other notes do not take surrender into account.
            //He's got a separate page for it, and that's why I initially thought I was wrong.
            else if ((hardAnswers.get(i).get(j).handTotal() == 12)
                    && ((hardAnswers.get(i).get(j).getDealerUpCard().getCardValue() == CardValue.THREE)
                    || (hardAnswers.get(i).get(j).getDealerUpCard().getCardValue() == CardValue.FOUR)))
            ;
            else {
               if ((hardAnswers.get(i).get(j).getBestAction()
                       != totalDependent.get(i).get(j).getBestAction())
                       && (hardAnswers.get(i).get(j).getBestAction() != Action.SURRENDER)
                       && (totalDependent.get(i).get(j).getBestAction() != Action.SURRENDER)) {
                  if (key != theRules.myHashKey()) {
                     throw new RuntimeException("Rules corruption in Strategy test.");
                  }

                  State.printStateStatus(hardAnswers.get(i).get(j), "Hard state recommends:");
                  State.printStateStatus(totalDependent.get(i).get(j), "Total dependent recommends:");
                  assert (false);
                  //A previous function here changed the calculation accuracy, which is why this failed at one point.
               }



            }

         }
      }
   }
   catch (NoRecommendationException q) {
      q.printStackTrace();
      throw new RuntimeException();
   }
   catch (IOException io) {
      throw new RuntimeException(io);
   }


}

/**
 * Tests the given Strategy to check that it's truly total-dependent and none of
 * it
 * is composition-dependent. Used to test that the conversion from
 * composition-dependent
 * to total-dependent works.
 *
 * @param verbosity
 * @param aStrategy
 */
public static void testTotalConsolidatedForConsolidation(boolean verbosity,
        Strategy aStrategy)
        throws NoRecommendationException, IOException {
   Action[] bestAction = new Action[21];
   Action[] secondBestAction = new Action[21];
   State scratch;
   int handTotal;
   Action scratchBest, scratchSecond;

   for (CardValue dealerCard : CardValue.values()) {
      setActionsForHandTotals(false, bestAction, secondBestAction, aStrategy, dealerCard);
      setActionsForHandTotals(true, bestAction, secondBestAction, aStrategy, dealerCard);
      assert (bestAction[5] != null);
      for (CardValue k : CardValue.twoToTen) {
         for (CardValue j : CardValue.twoToTen) {
            if (k == j) {
               continue;
            }
            handTotal = k.value() + j.value();
            scratch = new State(k, j, dealerCard);
            scratchBest = aStrategy.findBestAction(scratch);
            scratchSecond = aStrategy.findSecondBestAction(scratch);

            /*The split table is completely separate; even if it doesn't call for splitting,
             it may call for another action. For example, you surrender on a 7-7 vs. 10
             * on single deck hit 17. So if the two cards are the same, they just should not
             be looked at at all when testing for total consolidation.
             *
             * */
            if (bestAction[handTotal] != scratchBest) {
               System.err.println("Testers.StrategyTest.testTotalConsolidatedForConsolidation: "
                       + " When holding " + k + " and " + j + ", my preferred action is to "
                       + scratchBest + ". However, the recommended action in another state with the same"
                       + " hand total is to " + bestAction[handTotal] + ".");
               System.err.println("The dealer has a " + dealerCard + " in hand.");
               System.out.println("Here are my strategy tables:");
               aStrategy.print(true);
               throw new NoRecommendationException();
            }
            if (secondBestAction[handTotal] != scratchSecond) {
               System.err.println("Testers.StrategyTest.testTotalConsolidatedForConsolidation: "
                       + " When holding " + k + " and " + j + ", my preferred second best action is to "
                       + scratchSecond + ". However, the recommended action in another state with the same"
                       + " hand total is to " + secondBestAction[handTotal] + ".");
               System.out.println("Here are my strategy tables:");
               aStrategy.print(true);
               System.err.println("The dealer has a " + dealerCard + " in hand.");
               throw new NoRecommendationException();



            }
         }
      }




   }





}

/**
 * Helper function for testTotalConsolidatedForConsolidation
 * This tries to assign the expected action for each given hand total.
 */
private static void setActionsForHandTotals(boolean lowHands,
        Action[] bestAction,
        Action[] secondBestAction, Strategy aStrategy, CardValue dealerCard)
        throws NoRecommendationException, IOException {
   State scratch;
   CardValue firstCard, secondCard;
   int handTotal;
   final CardValue[] doNotSet;
   if (lowHands) {
      // doNotSet = new CardValue[] { CardValue.TWO, CardValue.TWO};
      firstCard = CardValue.TWO;
   }
   else {
      // doNotSet = new CardValue[] { CardValue.TWO, CardValue.TEN};
      firstCard = CardValue.TEN;
   }

   for (CardValue k : CardValue.twoToTen) { //if ( (k == doNotSet[0]) || (k == doNotSet[1]) )
      // continue;
      handTotal = k.value() + firstCard.value();
      scratch = new State(k, firstCard, dealerCard);
      bestAction[handTotal] = aStrategy.findBestAction(scratch);
      secondBestAction[handTotal] = aStrategy.findSecondBestAction(scratch);
   }
   assert (bestAction[12] != null);
}

} //Class end

public static void testStrategy(boolean verbosity) {
   StrategyTest myTest = new StrategyTest();
   try {
      myTest.runAllTests(verbosity);
   }
   catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException();
   }
   catch (NoRecommendationException s) {
      s.printStackTrace();
      throw new RuntimeException();
   }

}

public static void allFastTests() {
   //Faster tests -- those which don't require solving the strategy.
   DealerCache.setCache(DealerCache.Status.FULL_CACHE);
   final long startTime = System.currentTimeMillis();
   testInsuranceGoodIdea();
   Testers.testResolveHands();
   Testers.testOverloadedDealer(); //Utilities functions
   Testers.testDealerHCP();

   Testers.testState();

   Testers.testRulesHash(false); //Generates ~410k unique keys at last count.
   System.out.println("Quick tests took " + (System.currentTimeMillis() - startTime) + " ms.");
   
   //Longer tests
   Testers.testStrategy(false); //This test is time-consuming
   Testers.testTotalEV.doAll(false, false); //Verbosity, saving
   Testers.testResplitEVs();
}

/**
 *
 * @param theRules
 * @param solvedHard
 * @param solvedSoft
 * @param dealerBlackjackPossible
 *
 */
public static void viewRawSplitEV(Rules theRules,
        ArrayList<ArrayList<State>> solvedHard,
        ArrayList<ArrayList<State>> solvedSoft, boolean dealerBlackjackPossible) {
   try {

      int i = 0, j = 0;
      for (CardValue q : CardValue.values()) {
         System.out.println("For dealer up card " + q.toString() + ":");
         j = 0;
         for (CardValue d : CardValue.values()) {
            System.out.println(
                    d.toString() + ": " + Calculation.splitSolve(theRules,
                    d, q, dealerBlackjackPossible));
            j++;
            if (j == 10) {
               break;
            }
         }

         i++;
         if (i == 10) {
            break;
         }
      }
   }
   catch (NoRecommendationException e) {
      e.printStackTrace();
      throw new RuntimeException(e);

   }
}

/**
 * Tests, at a very minimum level, some of the simple State functions.
 * Also tests, in a non-comprehensive way, the State copy constructor
 */
public static void testState() {

   Card twoSpades = new Card(Suit.SPADES, CardValue.TWO);
   Card twoDiamonds = new Card(Suit.DIAMONDS, CardValue.TWO);
   Card dealerCard = new Card(Suit.HEARTS, CardValue.ACE);
   State myState = new State(twoSpades, twoDiamonds, dealerCard);
   State otherState = new State(myState);
   myState.action(Action.INSURANCE);
   myState.action(Action.HIT, new Card(Suit.DIAMONDS,
           CardValue.THREE));
   myState.action(Action.HIT, new Card(Suit.SPADES, CardValue.ACE));

   myState.action(Action.STAND); //You have 18, dealer has an ace.
   assert (myState.getTotalBetAmount() < 1.5 + Constants.EPSILON);
   assert (myState.getTotalBetAmount() > 1.5 - Constants.EPSILON);
   assert (myState.getHandResult(0, 17, false) == State.WIN);
   // hand 0, dealer total 17, no dealer BJ
   assert (myState.getHandResult(0, 18, false) == State.PUSH);
   assert (myState.getHandResult(0, 19, false) == State.LOSE);

   otherState.action(Action.SPLIT);
   otherState.postSplitDraw(new Card(Suit.CLUBS, CardValue.QUEEN));
   otherState.action(Action.DOUBLE, new Card(Suit.DIAMONDS,
           CardValue.ACE));
   otherState.nextHand();
   otherState.postSplitDraw(new Card(Suit.DIAMONDS, CardValue.ACE));
   otherState.action(Action.HIT, new Card(Suit.SPADES,
           CardValue.TEN));
   otherState.action(Action.HIT, new Card(Suit.HEARTS,
           CardValue.NINE));

   assert (otherState.getTotalBetAmount() < 3 + Constants.EPSILON);
   assert (otherState.getTotalBetAmount() > 3 - Constants.EPSILON);
   assert (otherState.getHandResult(1, 25, false) == State.LOSE);
   assert (otherState.getHandResult(1, 21, true) == State.LOSE);
   try {
      assert (otherState.getHandResult(1, 18, true) == State.LOSE):
              "State.getHandResult failed test -- no exception thrown"
              + " when it was told the dealer had a blackjack and a hand total of 18.";
   }
   catch (IllegalArgumentException iae) {
   }
   assert (otherState.getHandResult(1, 18, false) == State.LOSE);
   assert (otherState.getHandResult(0, 25, false) == State.WIN);
   assert (otherState.getHandResult(0, 21, true) == State.LOSE);
   assert (otherState.getHandResult(0, 18, false) == State.LOSE);

   assert (otherState.getTotalHands() == 1);
   assert (otherState.lastAction() == Action.HIT);
   assert (!otherState.playerBJ());
   assert (!otherState.dealerBlackJackChecked());
   assert (otherState.allDone());
   assert (otherState.handTotal() == 22);
   assert (otherState.isBust());


   assert (myState.handTotal() == 18);
   assert (myState.allDone() == true);
   assert (myState.getDealerUpCard().getCardValue() == dealerCard.getCardValue());
   assert (myState.getDealerUpCard().getSuit() == dealerCard.getSuit());
   assert (myState.firstCardIs(CardValue.TWO));
   assert (myState.getCurrentHand() == 0);
   assert (myState.getSecondCard().getSuit() == Suit.DIAMONDS);
   assert (myState.getNumberSplitAces() == 0);

   State aState = new State(new Card(Suit.SPADES, CardValue.TEN), new Card(Suit.CLUBS,
           CardValue.ACE), dealerCard);
   aState.action(Action.INSURANCE);
   aState.action(Action.STAND);

   assert (aState.getTotalBetAmount() < 1.5 + Constants.EPSILON);
   assert (aState.getTotalBetAmount() > 1.5 - Constants.EPSILON);

   assert (aState.getHandResult(0, 25, false) == State.BLACKJACK);
   assert (aState.getHandResult(0, 21, true) == State.PUSH);
   assert (aState.getHandResult(0, 21, false) == State.BLACKJACK);
   assert (aState.getHandResult(0, 18, false) == State.BLACKJACK);

}



/**
 * Includes no tests, but useful for debugging DealerRecursive
 *
 * @param myCards
 * @param Deck
 * @param myRules
 * @param displayDetails
 * @param displayBaseInfo
 *
 */
static private void printAndTestFastDealerRecursive(
        ArrayList<Card> myCards,
        FastShoe Deck, Rules myRules,
        boolean displayDetails, boolean displayBaseInfo) {
   int i;
   final long startTime = System.currentTimeMillis();
   long iterationSum = Blackjack.fastDealerIterations;
   int[] handArray;
   handArray = Utilities.convertCardArraytoArray(myCards);
   final double[] epicFail =
           Calculation.DealerRecursive(handArray, Deck, myRules);
   double probability_sum = 0;

   if (displayBaseInfo) {
      System.out.println();
      System.out.println("The dealer's starting card(s): ");

      for (i = 0; i < myCards.size(); i++) {
         System.out.print(
                 myCards.get(i).getCardValue().toString() + " of "
                 + myCards.get(i).getSuit().toString() + "  ");
      }
      System.out.println();
   }

   if (displayDetails) {
      for (i = 0; i < epicFail.length; i++) {
         if (i == 0) {
            System.out.println("The chance of busting is: "
                    + epicFail[0]);
         }
         else if (i == 1) {
            System.out.println(
                    "The chance of a natural blackjack is: "
                    + epicFail[i]);
         }
         else {
            System.out.println(
                    "The chance of the dealer getting a "
                    + (15 + i) + " is " + epicFail[i]);
         }
         probability_sum += epicFail[i];
      }
      System.out.println(
              "All those probabilities add up to: " + probability_sum);

   }
   if (displayBaseInfo) {
      System.out.println(
              "I have done " + (Blackjack.fastDealerIterations - iterationSum)
              + " iterations. The current set took " + (System.currentTimeMillis() - startTime) + " milliseconds.");
   }

}

/**
 * Used solely for debugging DealerRecursive
 *
 * SETS HOLE CARD TO FALSE, MESSES AROUND WITH DECK
 *
 * @param numDecks Number of decks
 * @param iterations 13 * number of tests to perform
 * @param minverbosity Allow minimal verbosity
 * @param maxverbosity Allow maximum verbosity
 *
 */
@Deprecated
static void testFastDealerRecursive(final int numDecks,
        final int iterations,
        boolean minverbosity,
        boolean maxverbosity) {
   FastShoe Deck = new FastShoe(numDecks);
   int i;
   Rules myRules = new Rules();
   myRules.setHoleCard(false);
   myRules.setAccuracy(Rules.MAX_ACCURACY);
   final int cardsInDeck = Deck.numberOfCards();
   ArrayList<Card> myCards = new ArrayList<Card>();
   long kk = 0;
   //    Card startingCard = new Card(Suit.CLUBS, CardValue.ACE);
   //   myCards.add(startingCard);
   long j = System.currentTimeMillis();
   // myCards.add(new Card(Suit.CLUBS, CardValue.ACE));
   //  printAndTestDealerRecursive(myCards, Deck, myRules, true);
   ArrayList<CardValue> reverse = new ArrayList<CardValue>();
   reverse.add(CardValue.KING);
   reverse.add(CardValue.QUEEN);
   reverse.add(CardValue.JACK);
   reverse.add(CardValue.TEN);
   reverse.add(CardValue.NINE);
   reverse.add(CardValue.EIGHT);
   reverse.add(CardValue.SEVEN);
   reverse.add(CardValue.SIX);
   reverse.add(CardValue.FIVE);
   reverse.add(CardValue.FOUR);
   reverse.add(CardValue.THREE);
   reverse.add(CardValue.TWO);
   reverse.add(CardValue.ACE);


   for (i = 0; i < iterations; i++) {
      for (CardValue k : reverse) {
         kk++;
         myCards.add(new Card(Suit.CLUBS, k));
         Deck.fastDrawSpecific(k);
         printAndTestFastDealerRecursive(myCards, Deck, myRules,
                 minverbosity, maxverbosity);
         myCards.remove(0);
         Deck.addCard(new Card(Suit.CLUBS, k));
         if (cardsInDeck != Deck.numberOfCards()) {
            throw new RuntimeException("Yippee I found an error.");
         }
         if (!myCards.isEmpty()) {
            throw new RuntimeException(
                    "Yippee I found an error.!");
         }

      }
   }
   System.out.println(
           "The fast dealer recursive function was called "
           + Blackjack.fastDealerIterations + " times in total, and " + kk
           + " original times. \nThe hole card draw was checked "
           + Blackjack.holeCardCheck + " times.");
   System.out.println(
           "A total of " + (System.currentTimeMillis() - j)
           + " milliseconds has elapsed; each base call took "
           + ((double) ((System.currentTimeMillis() - j)) / ((double) kk)) + " ms.");
   System.out.println(
           "I used " + numDecks + " decks and a max hand size of "
           + myRules.getDealerMaxHandSize() + " cards.");

}

/**
 * Helper function for main insurance tester
 *
 *
 * @param myShoe
 * @param PCardOne
 * @param PCardTwo
 * @param dealerCard
 * @return
 */
static private boolean insuranceTester(FastShoe myShoe,
        CardValue PCardOne,
        CardValue PCardTwo,
        CardValue dealerCard) {
   boolean theAnswer;
   ArrayList<Card> startingHand = new ArrayList<Card>();
   startingHand.add(new Card(Suit.CLUBS, PCardOne));
   startingHand.add(new Card(Suit.CLUBS, PCardTwo));
   myShoe.fastDrawSpecific(PCardOne);
   myShoe.fastDrawSpecific(PCardTwo);
   myShoe.fastDrawSpecific(dealerCard);
   State myState = new State(startingHand, new Card(Suit.CLUBS,
           dealerCard));
   Rules theRules = new Rules();
   if (dealerCard == CardValue.ACE) {
      theAnswer = Strategy.insuranceGoodIdea(myShoe, theRules,
              myState);

   }
   else {
      theAnswer = false;
   }
//startingHand.clear();
   myShoe.addCard(new Card(Suit.CLUBS, PCardOne));
   myShoe.addCard(new Card(Suit.CLUBS, PCardTwo));
   myShoe.addCard(new Card(Suit.CLUBS, dealerCard));

   /* if (theAnswer)
    {System.out.println("My hand is ");
    for (int ii = 0; ii <startingHand.size(); ii++)
    {  System.out.println(startingHand.get(ii).getCardValue() + " ");
    }

    } */

   return theAnswer;



}

static public boolean testInsuranceGoodIdea() {
   FastShoe myShoe = new FastShoe(1);

   Card PCardOne = new Card(Suit.CLUBS, CardValue.TWO);
   Card PCardTwo = new Card(Suit.CLUBS, CardValue.NINE);
   Card dealerCard;
   for (CardValue q : CardValue.values()) {
      insuranceTester(myShoe, PCardOne.getCardValue(),
              PCardTwo.getCardValue(), q);

   }

   dealerCard = new Card(Suit.CLUBS, CardValue.ACE);
   for (CardValue pcone : CardValue.values()) {
      for (CardValue pctwo : CardValue.values()) {
         if (insuranceTester(myShoe, pcone, pctwo,
                 dealerCard.getCardValue())) {
            System.out.println(
                    "Test failed, it thought insurance was a good idea under normal"
                    + " cirumstances.");
            System.out.println("Player cards: " + pcone + " and " + pctwo + "vs. a dealer" + dealerCard);
            System.out.println("The shoe:" + myShoe);
            assert false;
         }
      }
   }

   if (myShoe.numberOfCards() != 52) {
      System.out.println(
              "Error in testing function testPlayersRecursiveHelpers");
      assert false;
   }

   int i = 0;
   for (CardValue p : CardValue.values()) {
      if (i != 0) {
         for (int q = 0; q < 2; q++) {
            myShoe.fastDrawSpecific(p);
         }
      }
      i++;
      if (i == 8) {
         break;
      }
   }
//myShoe.printContents();

   for (CardValue pcone : CardValue.values()) {
      for (CardValue pctwo : CardValue.values()) {
         if (!insuranceTester(myShoe, pcone, pctwo,
                 dealerCard.getCardValue())) {
            System.out.println(
                    "Test failed, it thought insurance was a bad idea with a"
                    + " 10-heavy deck.");
            assert false;
         }
      }
   }

   return true;
}

/**
 * Used to test getDealerHand in Utilities (the function that does the work, not
 * the convenience functions).
 * This eats up a decent amount of time.
 *
 * @param startingCards
 * @param theRules
 */
static void testGetDealerHand(ArrayList<Card> startingCards, Rules theRules) {
   int[] manualResults = new int[7];
   double[] manualProbs = new double[7];
   Shoe myShoe = new Shoe(theRules.getNumberOfDecks());
   final int[] handArray = Utilities.convertCardArraytoArray(startingCards);;

   ArrayList<Card> cloneOfStartHand = new ArrayList<Card>();
   for (int i = 0; i < startingCards.size(); i++) {
      cloneOfStartHand.add(new Card(startingCards.get(i)));
   }

   final double[] calculatedResults = Calculation.DealerRecursive(handArray, new FastShoe(myShoe), theRules);

   ArrayList<Card> results;
   final double ITERATIONS = 50000;
   int handTotal;
   for (int i = 0; i < ITERATIONS; i++) {
      myShoe = new Shoe(theRules.getNumberOfDecks());

      results = Utilities.getDealerHand(theRules, startingCards, myShoe);
      handTotal = Utilities.handTotal(results);
      if (handTotal > 21) {
         manualResults[0]++;
      }
      else if (Utilities.hasBlackjack(results)) {
         manualResults[1]++;
      }
      else {
         manualResults[(handTotal - 15)]++;
      }



      startingCards.clear();
      for (int j = 0; j < cloneOfStartHand.size(); j++) {
         startingCards.add(new Card(cloneOfStartHand.get(j)));
      }
      /*
       //    * endProbabilities[0] = P(Bust)
       * endProbabilities[1] = P(Natural Blackjack)
       * endProbabilities[2] = 17 */
   }
   int sum = 0;


//System.out.print("Manual count: ");
   for (int j = 0; j < manualResults.length; j++) { //System.out.print(manualResults[j] + "   ");
      manualProbs[j] = ((double) (manualResults[j])) / ((double) ITERATIONS);
      sum += manualResults[j];
   }

   assert (sum == ITERATIONS);

   for (int k = 0; k < manualProbs.length; k++) {

      if (calculatedResults[k] > 0.0001) {
         if (((manualProbs[k] > (1.05 * calculatedResults[k])))
                 || ((manualProbs[k] < (0.95 * calculatedResults[k])))) {
            System.err.println("Test failed on element " + k + ": " + manualProbs[k] + " vs. "
                    + "a calculated value of " + calculatedResults[k]);
            System.err.println("Calculated result: ");

            for (int i = 0; i < calculatedResults.length; i++) {
               System.err.print(calculatedResults[i] + "   ");
            }

            System.err.println("Manual result: ");
            for (int i = 0; i < calculatedResults.length; i++) {
               System.err.print(manualProbs[i] + "   ");
            }

            throw new AssertionError();
         }
      }

   }

}

/**
 * Called by testResolveHands. Contains different scenarios to test the math
 * behind Blackjack.resolveHands.
 *
 * @return
 */
private static void advancedTestResolveHands() {
   FastShoe myShoe = new FastShoe(3);
   Rules theRules = new Rules();
   State alwaysCloned; //reinitialized for every test.
   double value;
   ArrayList<Card> myCards = new ArrayList<Card>();

//Scenario 1: 9/10/10 & 9/A vs. dealer Ace; no-hole card. Insurance taken.
   theRules.setHoleCard(false);
   Card PCardOne = new Card(Suit.CLUBS, CardValue.NINE);
   Card PCardTwo = new Card(Suit.CLUBS, CardValue.NINE);
   Card dealerCard = new Card(Suit.SPADES, CardValue.ACE);

   myShoe.fasterDrawSpecific(PCardOne.getCardValue());
   myShoe.fasterDrawSpecific(PCardTwo.getCardValue());
   myShoe.fasterDrawSpecific(dealerCard.getCardValue());
   alwaysCloned = new State(PCardOne, PCardTwo, dealerCard);

   CardValue q = CardValue.KING;

   assert (!Strategy.insuranceGoodIdea(myShoe, theRules, alwaysCloned));
   alwaysCloned.action(Action.INSURANCE); //TESTING
   alwaysCloned.action(Action.SPLIT);
   alwaysCloned.postSplitDraw(new Card(Suit.HEARTS, q), myShoe);

   alwaysCloned.action(Action.HIT, new Card(Suit.HEARTS, q), myShoe);

   alwaysCloned.nextHand();

   alwaysCloned.postSplitDraw(new Card(Suit.HEARTS, CardValue.ACE),
           myShoe);
   alwaysCloned.action(Action.STAND);

   alwaysCloned = Calculation.resolveHands(alwaysCloned, myShoe,
           theRules);


   assert (alwaysCloned.getTotalBetAmount() < 2.5 + Constants.EPSILON);
   assert (alwaysCloned.getTotalBetAmount() > 2.5 - Constants.EPSILON) :
           "The total bet amount claims it is " + alwaysCloned.getTotalBetAmount();

   value = alwaysCloned.getExpectedValue();


   //OK. To calculate first do insurance:
   double probOfTen = myShoe.probabilityOf(CardValue.TEN);
   double calculatedValue = 0;
   calculatedValue = probOfTen * 1 + (1 - probOfTen) * -0.5;
   //System.out.println("Insurance is worth " + calculatedValue);
   //Then do losing hand:
   calculatedValue -= 1;
   //Then do blackjack -- A 10 is insta-lose, otherwise nothing happens.
   //calculatedValue = probOfTen * -1; (should be done already by DealerRecursive)

   int[] dealerCards = new int[10];
   ArrayList<Card> dealerArrayList = new ArrayList<Card>();
   dealerArrayList.add(dealerCard);
   dealerCards = Utilities.convertCardArraytoArray(dealerArrayList);
   double[] dealerResults = Calculation.DealerRecursive(dealerCards, myShoe, theRules);

   double intermediateCalculatedValue = 0;
   //Dealer has 21, either natural or from drawing. You lose.
   final double probOf21 = dealerResults[1] + dealerResults[dealerResults.length - 1];
   //System.out.println(probOf21 + " is the chance of the dealer having 21.");
   intermediateCalculatedValue += (probOf21) * -1;
   final double probOfPush = dealerResults[dealerResults.length - 2]; //Nothing happens on push.


   intermediateCalculatedValue += (1 - probOf21 - probOfPush) * 1; //You win.
   //System.out.println(intermediateCalculatedValue + " is the intermediate value.");

   calculatedValue += intermediateCalculatedValue;

   //Then multiple all non-blackjack possibilities by 1- probOfTen
   //endProbabilities[0] = P(Bust)
   // * endProbabilities[1] = P(Natural Blackjack)
   //  * endProbabilities[2] = 17
   // 3 18 4 19 5 20 6 21
   //System.out.println(calculatedValue + " is the calculated value.");

   assert ((calculatedValue - Constants.EPSILON < value) && (value < calculatedValue + Constants.EPSILON)) :
           value + " is the result; the expected value is " + calculatedValue;




//Scenario 2: A/10 vs. dealer blackjack; hole card. Insurance taken.
   //RESET ALL VARS
   myShoe = new FastShoe(3);
   myCards.clear();
   PCardOne = new Card(Suit.CLUBS, CardValue.TEN);
   PCardTwo = new Card(Suit.CLUBS, CardValue.ACE);
   dealerCard = new Card(Suit.SPADES, CardValue.ACE);
   theRules.setHoleCard(true);

   myCards.add(PCardOne);
   myCards.add(PCardTwo);
   myShoe.fasterDrawSpecific(PCardOne.getCardValue());
   myShoe.fasterDrawSpecific(PCardTwo.getCardValue());
   myShoe.fasterDrawSpecific(dealerCard.getCardValue());
   alwaysCloned = new State(myCards, dealerCard);

   Strategy.insuranceGoodIdea(myShoe, theRules, alwaysCloned);
   //Why did I call this??

   alwaysCloned.action(Action.INSURANCE); //TESTING
   alwaysCloned.setDealerBlackjack(true);

   alwaysCloned = Calculation.resolveHands(alwaysCloned, myShoe,
           theRules);
   value = alwaysCloned.getExpectedValue();
   assert ((0.999 < value) && (value < 1.001));

//Scenario 3: A/10 vs. no-hole-card dealer drawing on a 10. Insurance not taken.
   //RESET ALL VARS
   myShoe = new FastShoe(3);
   myCards.clear();
   PCardOne = new Card(Suit.CLUBS, CardValue.TEN);
   PCardTwo = new Card(Suit.CLUBS, CardValue.ACE);
   dealerCard = new Card(Suit.SPADES, CardValue.TEN);
   theRules.setHoleCard(false);

   myCards.add(PCardOne);
   myCards.add(PCardTwo);
   myShoe.fasterDrawSpecific(PCardOne.getCardValue());
   myShoe.fasterDrawSpecific(PCardTwo.getCardValue());
   myShoe.fasterDrawSpecific(dealerCard.getCardValue());
   alwaysCloned = new State(myCards, dealerCard);

   if (theRules.isPossible(Action.INSURANCE, alwaysCloned)) {
      Strategy.insuranceGoodIdea(myShoe, theRules, alwaysCloned);
   }
   //alwaysCloned.setDealerBlackjack(true);
   alwaysCloned.action(Action.STAND);
   alwaysCloned = Calculation.resolveHands(alwaysCloned, myShoe,
           theRules);
   value = alwaysCloned.getExpectedValue();
   assert ((1.3921 < value) && (value < 1.3922));

   //Scenario 4: 3 split hands. 10 5 6,  10 A,  10 6 6(double) vs. a dealer 6. No dealer hole card.
   myShoe = new FastShoe(3);
   myCards.clear();
   PCardOne = new Card(Suit.CLUBS, CardValue.TEN);
   PCardTwo = new Card(Suit.CLUBS, CardValue.TEN);
   dealerCard = new Card(Suit.SPADES, CardValue.SIX);
   theRules.setHoleCard(false);

   myCards.add(PCardOne);
   myCards.add(PCardTwo);
   myShoe.fasterDrawSpecific(PCardOne.getCardValue());
   myShoe.fasterDrawSpecific(PCardTwo.getCardValue());
   myShoe.fasterDrawSpecific(dealerCard.getCardValue());
   alwaysCloned = new State(myCards, dealerCard);

   if (theRules.isPossible(Action.INSURANCE, alwaysCloned)) {
      Strategy.insuranceGoodIdea(myShoe, theRules, alwaysCloned);
   }

   alwaysCloned.action(Action.SPLIT);
   alwaysCloned.action(Action.HIT, new Card(Suit.CLUBS,
           CardValue.FIVE), myShoe);
   alwaysCloned.action(Action.HIT, new Card(Suit.DIAMONDS,
           CardValue.SIX), myShoe);
   alwaysCloned.action(Action.STAND);
   alwaysCloned.nextHand();
   alwaysCloned.postSplitDraw(new Card(Suit.CLUBS, CardValue.TEN),
           myShoe);
   alwaysCloned.action(Action.SPLIT);
   alwaysCloned.postSplitDraw(new Card(Suit.CLUBS, CardValue.ACE),
           myShoe);
   alwaysCloned.action(Action.STAND);
   alwaysCloned.nextHand();
   alwaysCloned.postSplitDraw(new Card(Suit.HEARTS, CardValue.SIX),
           myShoe);
   alwaysCloned.action(Action.DOUBLE, new Card(Suit.CLUBS,
           CardValue.SIX),
           myShoe);


   alwaysCloned = Calculation.resolveHands(alwaysCloned, myShoe,
           theRules);
   value = alwaysCloned.getExpectedValue();
   assert ((-0.2064 < value) && (value < -0.2052)) : "Expected result was between -0.2062 and -0.2050; actual"
           + " result is " + value;




}

/**
 * Helper function for testDealerHCP
 *
 * @param DCard
 * @param firstPCard
 * @param secondPCard
 * @param theRules
 * @param myShoe
 * @param dealerBJPossible
 * @return
 */
static State testPlayerRecursive(final Card DCard, final Card firstPCard,
        final Card secondPCard, final Rules theRules, FastShoe myShoe,
        final boolean dealerBJPossible) {
   ArrayList<Card> myCards = new ArrayList<Card>();
   myCards.add(firstPCard);
   myCards.add(secondPCard);
   State myState = new State(myCards, DCard);
   if (!dealerBJPossible) {
      myState.setDealerBlackjack(false);
   }
   try {
      return Calculation.PlayerRecursive(myShoe, myState, theRules);
   }
   catch (NoRecommendationException e) {
      e.printStackTrace();
      throw new RuntimeException(e);

   }
}
//State resolveHands(State finishedState, FastShoe myShoe, Rules theRules)
//solves for EV.

/**
 * Tests some resolveHands scenarios.
 *
 *
 */
static void testResolveHands() {
   FastShoe myShoe = new FastShoe(3);
   Rules theRules = new Rules();

   Card PCardOne = new Card(Suit.CLUBS, CardValue.NINE);
   Card PCardTwo = new Card(Suit.CLUBS, CardValue.NINE);
   Card dealerCard = new Card(Suit.SPADES, CardValue.ACE);

   myShoe.fasterDrawSpecific(PCardOne.getCardValue());
   myShoe.fasterDrawSpecific(PCardTwo.getCardValue());
   myShoe.fasterDrawSpecific(dealerCard.getCardValue());

   State aState = new State(PCardOne, PCardOne, dealerCard);

   final CardValue q = CardValue.TWO;


   assert (Strategy.insuranceGoodIdea(myShoe, theRules, aState) == false);
   aState.action(Action.INSURANCE); //TESTING
   aState.setDealerBlackjack(false); //TESTING
   aState.action(Action.SPLIT);

   myShoe.fasterDrawSpecific(q);
   aState.postSplitDraw(new Card(Suit.HEARTS, q));

   myShoe.fasterDrawSpecific(q);
   aState.action(Action.HIT, new Card(Suit.HEARTS, q));
   aState.action(Action.STAND);

   aState.nextHand();
   myShoe.fasterDrawSpecific(CardValue.ACE);
   aState.postSplitDraw(new Card(Suit.HEARTS,
           CardValue.ACE));
   aState.action(Action.STAND);

   aState = Calculation.resolveHands(aState, myShoe,
           theRules);



   int[] dealerCards = new int[10];
   Utilities.zero(dealerCards);

   dealerCards[ aState.getDealerUpCard().value() - 1] = 1;
   double[] dealerProbs = Calculation.DealerRecursive(dealerCards, myShoe, theRules);
   //13 and 20. That's what both of these hands have. I also took insurance. Dang that's way
   //too many calculations.

   //Insurance first
   double expectedEV = -0.5;



   //13 hand.
   expectedEV += 1 * dealerProbs[0]; // -.59
   expectedEV += -1 * (1 - dealerProbs[0]);

   //20 hand. Lose on 21. Nothing on 20. Win otherwise.
   assert (dealerProbs[1] < Constants.EPSILON);
   double notWin = dealerProbs[dealerProbs.length - 1];  // = 6
   expectedEV += -1 * notWin;
   notWin += dealerProbs[dealerProbs.length - 2];
   expectedEV += 1 * (1 - notWin);


   assert (aState.getExpectedValue() < expectedEV + Constants.EPSILON);
   assert (aState.getExpectedValue() > expectedEV - Constants.EPSILON);


   advancedTestResolveHands();
}


/* Prints the hard or soft strategy table in its raw form.
 *
 */
static void printStrategy(ArrayList<ArrayList<State>> solvedStates,
        String message, final boolean printSecondBest) {
   final String newLineAndTab = "\n\t   ";
   int i, j, jj, k;
   //System.out.println("Dealer card at top, player hands on side.");
   System.out.println(message);
   System.out.println(
           "Total\t       2        3        4        5        6        7        8        9        10       A");
   for (i = 0; i < solvedStates.size(); i++) {
      System.out.print(
              solvedStates.get(i).get(0).getFirstCard().getCardValue().value()
              + "," + solvedStates.get(i).get(0).getSecondCard().getCardValue().value() + "\t       ");
      for (j = 0; j < solvedStates.get(i).size(); j++) {
         System.out.print(
                 solvedStates.get(i).get(j).getBestAction().abbrev() + "        ");
      }
      System.out.print(newLineAndTab); //Should I use %t ?
      for (jj = 0; jj < solvedStates.get(i).size(); jj++) {
         System.out.format("%+.4f  ",
                 solvedStates.get(i).get(jj).getExpectedValue());
      } //print EV on new line.
      if (printSecondBest) {
         System.out.print(newLineAndTab + "    ");
         for (j = 0; j < solvedStates.get(i).size(); j++) {
            System.out.print(solvedStates.get(i).get(j).getSecondBestAction().abbrev()
                    + "        ");
         }
         System.out.print(newLineAndTab);
         for (jj = 0; jj < solvedStates.get(i).size(); jj++) {
            System.out.format("%+.4f  ",
                    solvedStates.get(i).get(jj).getSecondBestEV());
         }
      }
      System.out.println();
   }


}

/**
 * Helper function for testOverloadedDealer
 *
 *
 * @param test
 * @param corrHandTotal
 * @param corrHandSize
 * @param isSoft
 */
private static void testOverloadedDealer(final int[] test,
        final int corrHandTotal,
        final int corrHandSize,
        final boolean isSoft) {
   if (Utilities.handTotal(test) != corrHandTotal) {
      throw new RuntimeException("Test failed.");
   }
   if (Utilities.handSize(test) != corrHandSize) {
      throw new RuntimeException("Test failed.");
   }
   if (Utilities.isSoft(test, corrHandTotal) != isSoft) {
      throw new RuntimeException("Test failed.");
   }


}

public static void testOverloadedDealer() {
   int[] myCards = new int[10];
   Utilities.zero(myCards);
   testOverloadedDealer(myCards, 0, 0, false);
   Utilities.zero(myCards);
   myCards[Constants.ACECARD] = 1;
   testOverloadedDealer(myCards, 11, 1, true);
   myCards[Constants.TENCARD] = 1;
   testOverloadedDealer(myCards, 21, 2, true);
   myCards[Constants.NINECARD] = 1;
   testOverloadedDealer(myCards, 20, 3, false);
   myCards[Constants.SEVENCARD] = 1;
   testOverloadedDealer(myCards, 27, 4, false);
   Utilities.zero(myCards);
   myCards[Constants.ACECARD] = 4;
   testOverloadedDealer(myCards, 14, 4, true);

   ArrayList<Card> testHand = new ArrayList<Card>();
   Rules someRules = new Rules(1);
   CardValue j = CardValue.THREE;
   for (CardValue k : CardValue.values()) //for (CardValue j : CardValue.values())
   {
      testHand.add(new Card(Suit.CLUBS, k));
      testHand.add(new Card(Suit.CLUBS, j));
      testGetDealerHand(testHand, someRules);

   }
}



/**
 * Tests the rules hash for all values. See rules hash key function for more
 * information.
 *
 *
 *
 */
static public void testRulesHash(boolean verbose) {
   Strategy someStrategy = new Strategy(new Rules(), Strategy.Skill.COMP_DEP);

   someStrategy.testRulesKeys(verbose);

}

/**
 * Tests the numbers for two scenarios.
 * Since the specific EVs are not stored in files, forceSolve must be set to
 * true
 * so that values can be recalculated here.
 *
 */
static public void testResplitEVs() {

   double value;
   Rules theRules = new Rules(2);
   theRules.setNumResplitAces(1); //Aces can be resplit.
   theRules.setMaxNumberSplitHands(2); //other things can be resplit
   theRules.setHitSplitAces(false);
   theRules.setAccuracy(Rules.CACHE_ACCURACY);
   theRules.setHitOn17(false);
   Strategy myStrategy = new Strategy(theRules, Strategy.Skill.COMP_DEP);

   myStrategy.setCalculationDeactivated(false);
   myStrategy.setForceSolve(true);

   State aState;
   try {
      aState = new State(new Card(Suit.CLUBS, CardValue.THREE),
              new Card(Suit.CLUBS, CardValue.THREE),
              new Card(Suit.CLUBS, CardValue.TWO));
      value = myStrategy.findBestEV(theRules, aState);

      assert (value > (1 + Constants.FIVE_PERCENT_ERROR) * -0.12446) : value + " for best action: "
              + myStrategy.findBestAction(aState, theRules);

      assert (value < (1 - Constants.FIVE_PERCENT_ERROR) * -0.12446) : value;

      aState = new State(new Card(Suit.CLUBS, CardValue.EIGHT),
              new Card(Suit.CLUBS, CardValue.EIGHT),
              new Card(Suit.CLUBS, CardValue.TWO));
      assert (myStrategy.findBestEV(theRules, aState) < (1 + Constants.FIVE_PERCENT_ERROR) * 0.097472);
      assert (myStrategy.findBestEV(theRules, aState) > (1 - Constants.FIVE_PERCENT_ERROR) * 0.097472);

      aState = new State(new Card(Suit.CLUBS, CardValue.FOUR),
              new Card(Suit.CLUBS, CardValue.FOUR),
              new Card(Suit.CLUBS, CardValue.SIX));
      assert (myStrategy.findBestEV(theRules, aState) < (1 + Constants.FIVE_PERCENT_ERROR) * 0.199142);
      assert (myStrategy.findBestEV(theRules, aState) > (1 - Constants.FIVE_PERCENT_ERROR) * 0.199142);

      aState = new State(new Card(Suit.CLUBS, CardValue.NINE),
              new Card(Suit.CLUBS, CardValue.NINE),
              new Card(Suit.CLUBS, CardValue.SIX));
      assert (myStrategy.findBestEV(theRules, aState) < (1 + Constants.FIVE_PERCENT_ERROR) * 0.44897);
      assert (myStrategy.findBestEV(theRules, aState) > (1 - Constants.FIVE_PERCENT_ERROR) * 0.44897);

      //Now change the rules.
      theRules.myDoubleRules.setNotPostSplit(true);

      aState = new State(new Card(Suit.CLUBS, CardValue.SEVEN),
              new Card(Suit.CLUBS, CardValue.SEVEN),
              new Card(Suit.CLUBS, CardValue.TWO));
      assert (myStrategy.findBestEV(theRules, aState) > (1 + Constants.FIVE_PERCENT_ERROR) * -0.179005) : myStrategy.findBestEV(theRules, aState);
      assert (myStrategy.findBestEV(theRules, aState) < (1 - Constants.FIVE_PERCENT_ERROR) * -0.179005);

      aState = new State(new Card(Suit.CLUBS, CardValue.EIGHT),
              new Card(Suit.CLUBS, CardValue.EIGHT),
              new Card(Suit.CLUBS, CardValue.TWO));
      value = myStrategy.findBestEV(theRules, aState);

      assert (value < (1 + Constants.FIVE_PERCENT_ERROR) * 0.01726) :
              value + " for best action: " + myStrategy.findBestAction(aState, theRules);

      assert (value > (1 - Constants.FIVE_PERCENT_ERROR) * 0.01726) : value + " for best action: "
              + myStrategy.findBestAction(aState, theRules);
      // These two numbers are from http://www.bjstrat.net/cgi-bin/cdca.cgi
      //


      aState = new State(new Card(Suit.CLUBS, CardValue.THREE),
              new Card(Suit.CLUBS, CardValue.THREE),
              new Card(Suit.CLUBS, CardValue.SIX));
      assert (myStrategy.findBestEV(theRules, aState) < (1 + Constants.FIVE_PERCENT_ERROR) * 0.0905487);
      assert (myStrategy.findBestEV(theRules, aState) > (1 - Constants.FIVE_PERCENT_ERROR) * 0.0905487);

      aState = new State(new Card(Suit.CLUBS, CardValue.NINE),
              new Card(Suit.CLUBS, CardValue.NINE),
              new Card(Suit.CLUBS, CardValue.SIX));
      assert (myStrategy.findBestEV(theRules, aState) < (1 + Constants.FIVE_PERCENT_ERROR) * 0.389554);
      assert (myStrategy.findBestEV(theRules, aState) > (1 - Constants.FIVE_PERCENT_ERROR) * 0.389554);



   }
   catch (NoRecommendationException nre) {
      throw new RuntimeException(nre);
   }
   catch (IOException io) {
      throw new RuntimeException(io);
   }

}

/**
 * Mothballed -- far too slow to be of any practical use. Can be used to
 * do one-off testing? Might be useful if the cache were changed, player
 * decisions were set in stone, and some other modifications made.
 *
 * @param playerCardValue
 * @param DealerCardValue
 * @param numberOfDekcs
 * @param theRules
 *
 */
static public void testDirectSplitAlgorithm(CardValue playerCard,
        CardValue dealCard, Rules theRules) {
   try {

      ArrayList<Card> myCards = new ArrayList<Card>();
      myCards.add(new Card(Suit.CLUBS, playerCard));
      myCards.add(new Card(Suit.HEARTS, playerCard));
      Card dealerCard = new Card(Suit.DIAMONDS, dealCard);
      FastShoe myShoe = new FastShoe(theRules.getNumberOfDecks());
      myShoe.fasterDrawSpecific(playerCard);
      myShoe.fasterDrawSpecific(playerCard);
      myShoe.fasterDrawSpecific(dealCard);
      State myState = new State(myCards, dealerCard);
      myState = Calculation.PlayerRecursive(myShoe, myState, theRules);

//And...goodbye 7+ hours. */
      State.printStateStatus(myState,
              "Splitting " + playerCard + " vs. dealer " + dealCard + ", level " + theRules.getAccuracy()
              + " accuracy.[low med good high max]");
   }
   catch (NoRecommendationException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
   }

}

/**
 * A single test to check correct functioning of hole card probabilities.
 *
 *
 */
static public void testDealerHCP() {
   /*When the dealer has an ace or ten and not a blackjack and a hole card. */


   Card firstPCard = new Card(Suit.HEARTS, CardValue.NINE);
   Card secondPCard = new Card(Suit.CLUBS, CardValue.TWO);
   Card DCard = new Card(Suit.SPADES, CardValue.ACE);
   Rules theRules = new Rules(1);
   theRules.setAccuracy(Rules.MAX_ACCURACY);
   FastShoe myShoe = new FastShoe(theRules.getNumberOfDecks());

   myShoe.fasterDrawSpecific(CardValue.ACE);
   myShoe.fasterDrawSpecific(CardValue.NINE);
   myShoe.fasterDrawSpecific(CardValue.TWO);

   State solvedState =
           Testers.testPlayerRecursive(DCard, firstPCard, secondPCard, theRules, myShoe, false);
   assert (solvedState.getBestAction() == Action.DOUBLE) : "Correct action not picked";
   assert ((solvedState.getExpectedValue() < 0.18192 + Constants.EPSILON)
           && (solvedState.getExpectedValue() > 0.18192 - Constants.EPSILON)) :
           "Expected value not calculated correctly for player 9-2 vs. dealer Ace:"
           + solvedState.getExpectedValue();




}

/**
 * Pretty sure this assumes the dealer does not have blackjack.
 *
 * @param theRules
 * @param accuracyLevel
 *
 */
static public void viewRawStrategy(Rules theRules, String accuracyLevel,
        final boolean viewSecondBest) {
   try {

      final long startTime = System.currentTimeMillis();
      final String rulesString = theRules.toString();
      ArrayList<ArrayList<State>> solvedHard = new ArrayList<ArrayList<State>>();
      ArrayList<ArrayList<State>> solvedSoft = new ArrayList<ArrayList<State>>();
      System.out.println(accuracyLevel + ", " + rulesString);
      solvedSoft = Calculation.solveSoftPlayersRecursive(theRules, false);
      Testers.printStrategy(solvedSoft, "", viewSecondBest);
      solvedHard = Calculation.solveHardPlayersRecursive(theRules, false);

      System.out.println("---------------------------------------");
      Testers.printStrategy(solvedHard, "", viewSecondBest);


      System.out.println("\nExpected values for splitting:");
      Testers.viewRawSplitEV(theRules, solvedHard, solvedSoft, false);

      System.out.println("Total time: " + (System.currentTimeMillis() - startTime) + " ms");
      if (!rulesString.equals(theRules.toString())) {
         System.out.println("Rules were corrupted.");
      }

   }
   catch (NoRecommendationException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
   }

}

public static class testTotalEV {
private static boolean saveAll = false;

/**
 * TO DO
 * Only one test, add more.
 *
 * @param verbosity
 */
public static void totalEVFourDeckStand17HoleCard(boolean verbosity) {

   Rules theRules;
   theRules = new Rules(4);
   theRules.setAccuracy(Rules.CACHE_ACCURACY);
   theRules.setHitSplitAces(true);
   theRules.setHitOn17(false);
   theRules.myDoubleRules.setOnlyTenAndEleven(true);

   //EV figure from: Wizard Of Odds
   wrapperCheckTotalEV(0.0035484, theRules, verbosity); 
}

/**
 * Performs 3+ tests
 *
 * @param verbosity
 *
 */
public static void totalEVEightDeckHit17HoleCard(boolean verbosity) {
   Rules theRules;
   theRules = new Rules(8);
   theRules.setAccuracy(Rules.CACHE_ACCURACY);
   theRules.setHitSplitAces(false);
   theRules.setHitOn17(true);
   //theRules.myDoubleRules.setOnlyTenAndEleven(true);
   theRules.setHoleCard(true);
   wrapperCheckTotalEV(0.0060762, theRules, verbosity);


   theRules = new Rules(8);
   theRules.setAccuracy(Rules.CACHE_ACCURACY);
   theRules.setHitSplitAces(false);
   theRules.setHitOn17(true);
   theRules.setMaxNumberSplitHands(2);
   theRules.setNumResplitAces(1);
   theRules.setHoleCard(true);
   theRules.myDoubleRules.setOnlyTenAndEleven(true);
   theRules.myDoubleRules.setNotPostSplit(true);
   theRules.setBlackjackPayback(1.5);
   theRules.setLateSurrender(false);
   wrapperCheckTotalEV(0.0091384, theRules, verbosity);
   //NEXT TEST
   theRules = new Rules(8);
   theRules.setAccuracy(Rules.CACHE_ACCURACY);
   theRules.setHitSplitAces(false);
   theRules.setHitOn17(true);
   //theRules.myDoubleRules.setOnlyTenAndEleven(true);
   theRules.setHoleCard(true);
   theRules.setMaxNumberSplitHands(2);
   theRules.setNumResplitAces(1);
   theRules.setLateSurrender(true);
   theRules.setBlackjackPayback(1.0);
   wrapperCheckTotalEV(0.02768, theRules, verbosity);

   theRules = new Rules(8);
   wrapperCheckTotalEV(0.00608, theRules, verbosity);
}

/**
 *
 *
 * @param verbosity
 */
public static void totalEVOneDeckStand17NoHole(boolean verbosity) {
   Rules theRules;
   theRules = new Rules(1);
   theRules.setAccuracy(Rules.CACHE_ACCURACY); //to make it faster
   theRules.setHitSplitAces(true);
   theRules.setHitOn17(false);
   //theRules.myDoubleRules.setOnlyTenAndEleven(true);
   theRules.setHoleCard(false);
   theRules.setEarlySurrenderNotOnAces(true);
   theRules.myDoubleRules.setAnyTwoCards(true);
   theRules.setBlackjackPayback(6.0 / 5.0);
   wrapperCheckTotalEV(0.0102, theRules, verbosity);

   theRules = new Rules(1);
   theRules.setHitOn17(false);
   theRules.setAccuracy(Rules.CACHE_ACCURACY);
   theRules.setHitSplitAces(true);
   wrapperCheckTotalEV(-0.0031627, theRules, verbosity);

}

/**
 * Does 5 + tests
 *
 * @param verbosity
 */
public static void totalEVFourDeckStand17NoHole(boolean verbosity) {
   Rules theRules;
   theRules = new Rules(4);
   theRules.setAccuracy(Rules.CACHE_ACCURACY);
   theRules.setHitSplitAces(false);
   theRules.setHitOn17(false);
   theRules.myDoubleRules.setNotPostSplit(true);
   theRules.setHoleCard(false);
   theRules.setEarlySurrender(true);
   theRules.setMaxNumberSplitHands(2);
   theRules.setNumResplitAces(0);
   wrapperCheckTotalEV(-3.4E-4, theRules, verbosity);

   theRules = new Rules(4);
   theRules.setAccuracy(Rules.CACHE_ACCURACY);
   theRules.setHitSplitAces(true);
   theRules.setHitOn17(false);
   theRules.setBlackjackPayback(2.0);
   theRules.setHoleCard(false);
   theRules.myDoubleRules.setNotPostSplit(true);
   theRules.setMaxNumberSplitHands(2);
   theRules.setNumResplitAces(1);
   wrapperCheckTotalEV(-0.01884, theRules, verbosity);

   theRules = new Rules(4);
   theRules.setAccuracy(Rules.CACHE_ACCURACY);
   theRules.setHitSplitAces(true);
   theRules.setHitOn17(false);
   theRules.myDoubleRules.setOnlyTenAndEleven(true);
   theRules.setHoleCard(false);
   wrapperCheckTotalEV(0.005373, theRules, verbosity);

   theRules = new Rules(4);
   theRules.setAccuracy(Rules.CACHE_ACCURACY);
   theRules.setHitOn17(false);
   theRules.setHitSplitAces(false);
   theRules.myDoubleRules.setNotPostSplit(true);
   theRules.myDoubleRules.setOnlyTenAndEleven(true);
   theRules.setNumResplitAces(1);
   theRules.setHoleCard(false);
   theRules.setLateSurrender(false);
   theRules.setEarlySurrender(false);
   theRules.setBlackjackPayback(6.0 / 5.0);
   wrapperCheckTotalEV(0.0217278, theRules, verbosity);

   theRules = new Rules(4);
   theRules.setAccuracy(Rules.CACHE_ACCURACY);
   theRules.setHitSplitAces(true);
   theRules.setHitOn17(false);
   theRules.setMaxNumberSplitHands(2);
   theRules.setNumResplitAces(1);
   theRules.setHoleCard(false);
   theRules.setBlackjackPayback(1.5);
   theRules.setEarlySurrenderNotOnAces(true);
   wrapperCheckTotalEV(1.3E-4, theRules, verbosity);
}

/**
 * Does 6+ tests
 *
 * @param verbosity
 */
public static void totalEVTwoDeckHit17HoleCard(boolean verbosity) {
   Rules theRules;
   theRules = new Rules(2);
   theRules.setAccuracy(Rules.CACHE_ACCURACY);
   theRules.setHitSplitAces(true);
   theRules.setHitOn17(true);
   theRules.setMaxNumberSplitHands(2);
   theRules.setNumResplitAces(0);
   theRules.setHoleCard(true);
   theRules.setLateSurrender(true);
   theRules.myDoubleRules.setNotPostSplit(true);
   theRules.myDoubleRules.setNotSplitAces(false);
   wrapperCheckTotalEV(0.00302, theRules, verbosity);

   //theRules.myDoubleRules.setOnlyNineTenEleven(true); MODIFIED
   theRules = new Rules(2); //Changed from 2. 2->8 decks is about 1 % less accurate.
   theRules.setAccuracy(Rules.CACHE_ACCURACY);
   theRules.setHitSplitAces(true);
   theRules.setHitOn17(true);
   theRules.setMaxNumberSplitHands(2);
   theRules.setNumResplitAces(1); // Off by 1.5 % on 2 decks. Off by 2.5 % on 8 decks.
   theRules.setHoleCard(true);
   theRules.myDoubleRules.setNotPostSplit(false);
   theRules.setBlackjackPayback(1.5);

   theRules.setEarlySurrenderNotOnAces(false);
   theRules.setLateSurrender(false);
   wrapperCheckTotalEV(0.0018, theRules, verbosity);


   theRules = new Rules(2);
   theRules.setAccuracy(Rules.CACHE_ACCURACY);
   theRules.setHitSplitAces(false); //
   theRules.setHitOn17(true);
   theRules.setMaxNumberSplitHands(2);
   theRules.setNumResplitAces(0);
   theRules.setHoleCard(true);
   theRules.myDoubleRules.setNotPostSplit(true);
   theRules.myDoubleRules.setNotSplitAces(false); //true = Can't check on Wizard site
   theRules.setBlackjackPayback(1.5);
   theRules.setLateSurrender(true);
   wrapperCheckTotalEV(0.004625, theRules, verbosity);

   // NEXT TEST
   theRules = new Rules(2);
   theRules.setAccuracy(Rules.CACHE_ACCURACY);
   theRules.setHitSplitAces(true); //This is where I go wrong. Hitting split aces.
   theRules.setHitOn17(true);
   theRules.setMaxNumberSplitHands(2);
   theRules.setNumResplitAces(0); //Fails at 1 = 0.002657  //but also 0 = .0030468 or .00301
   theRules.setHoleCard(true);
   theRules.myDoubleRules.setNotPostSplit(true);
   theRules.myDoubleRules.setNotSplitAces(false); //true = Can't check on Wizard site
   theRules.setBlackjackPayback(1.5);
   theRules.setLateSurrender(true);
   wrapperCheckTotalEV(0.0030468, theRules, verbosity);
   // NEXT TEST
   theRules = new Rules(2);
   theRules.setAccuracy(Rules.CACHE_ACCURACY);
   theRules.setHitSplitAces(true);
   theRules.setHitOn17(true);
   theRules.setMaxNumberSplitHands(2);
   theRules.setNumResplitAces(0);
   theRules.setHoleCard(true);
   theRules.myDoubleRules.setNotPostSplit(true);
   theRules.myDoubleRules.setNotSplitAces(false);
   theRules.setBlackjackPayback(1.5);
   theRules.setLateSurrender(true);
   wrapperCheckTotalEV(0.00303, theRules, verbosity);

   theRules = new Rules(2);
   theRules.setAccuracy(Rules.CACHE_ACCURACY);
   theRules.setHitSplitAces(false);
   theRules.setHitOn17(true);
   theRules.setMaxNumberSplitHands(2);
   theRules.setNumResplitAces(1);
   theRules.setHoleCard(true);
   theRules.setBlackjackPayback(6.0 / 5.0);
   theRules.setEarlySurrender(true);
   wrapperCheckTotalEV(0.01005, theRules, verbosity);
}

public static void totalEVOneDeckHit17NoHole(boolean verbosity) {

   Rules theRules;

   theRules = new Rules(1);
   theRules.setHitOn17(true);
   theRules.setHoleCard(false);
   theRules.myDoubleRules.setAnyTwoCards(true);
   theRules.setHitSplitAces(true);
   theRules.setNumResplitAces(1);
   theRules.setMaxNumberSplitHands(2);
   theRules.setBlackjackPayback(2);
   theRules.setEarlySurrenderNotOnAces(true);
   wrapperCheckTotalEV(-0.0257, theRules, verbosity);
   //http://www.bjstrat.net/cgi-bin/cdca.cgi

   theRules = new Rules(1);
   theRules.setHitOn17(true);
   theRules.setAccuracy(Rules.CACHE_ACCURACY);
   theRules.myDoubleRules.setNotPostSplit(true);

   //theRules.myDoubleRules.setOnlyTenAndEleven(true); //Screws it up?
   theRules.setNumResplitAces(1); //Should have no impact on answer
   theRules.setHitSplitAces(true);
   theRules.setHoleCard(false);
   theRules.setLateSurrender(false);
   theRules.setEarlySurrender(false);
   theRules.setBlackjackPayback(6.0D / 5.0D);
   wrapperCheckTotalEV(0.015467, theRules, verbosity);  //WoO

   //Total-consolidated
   //OneDeckHit17NoHole
         /*
    theRules = new Rules(1);
    theRules.setHitOn17(true);
    theRules.setHoleCard(false);
    theRules.setMaxNumberSplitHands(2);
    theRules.setNumResplitAces(1);
    theRules.myDoubleRules.setNotPostSplit(true);
    theRules.setHitSplitAces(false);
    wrapperCheckTotalEV(.002612, theRules, verbosity, Strategy.Skill.BASIC);
    //http://www.beatingbonuses.com/houseedge.htm
    //http://www.bjstrat.net/cgi-bin/cdca.cgi  (average of these two)
    Mothballed. Not solving for total-consolidated.
    * */


}

private static void wrapperCheckTotalEV(double expectedEV, Rules theRules,
        boolean verbosity) {
   wrapperCheckTotalEV(expectedEV, theRules, verbosity, Strategy.Skill.COMP_DEP);
}

private static void wrapperCheckTotalEV(double expectedEV, Rules theRules,
        boolean verbosity, Strategy.Skill myLevel) {
   Strategy myStrats = new Strategy(theRules, myLevel);
   final long initial = System.currentTimeMillis();
   double totalEV;
   final boolean percentErrorPass;
   try {

      totalEV = myStrats.getHouseEdge();
      if (saveAll) {
         myStrats.store();
      }
      percentErrorPass = checkTotalEV(expectedEV, totalEV, theRules, myStrats, verbosity);

      if (verbosity) {
         NumberFormat s = new DecimalFormat();
         s.setMaximumFractionDigits(6);
         DealerCache.printCacheStatus();
         System.out.println("House edge: " + s.format(totalEV) + " calculated and " + s.format(expectedEV) + " expected (" + s.format(100 * Math.abs(totalEV - expectedEV) / Math.min(totalEV, expectedEV)) + "% error) for these rules:" + theRules);
         System.out.println("This test took " + (System.currentTimeMillis() - initial) + " ms.");
         System.out.println("--------------------------------------------------");
      }
   }
   catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
   }
}

private static boolean checkTotalEV(double expectedHouseEdge, double totalEV,
        Rules theRules, Strategy myStrats, boolean verbosity) {
   final boolean percentErrorPass;
   final boolean testPass;
   if (expectedHouseEdge > 0) {
      percentErrorPass = ((totalEV < expectedHouseEdge * (1 + Constants.FIVE_PERCENT_ERROR)) && (totalEV > expectedHouseEdge * (1 - Constants.FIVE_PERCENT_ERROR)));
      testPass = (percentErrorPass || ((totalEV < expectedHouseEdge + Constants.MAXIMUM_ABSOLUTE_ERROR) && (totalEV > expectedHouseEdge - Constants.MAXIMUM_ABSOLUTE_ERROR)));
   }
   else {
      percentErrorPass = ((-1 * totalEV < -1 * expectedHouseEdge * (1 + Constants.FIVE_PERCENT_ERROR)) && (-1 * totalEV > -1 * expectedHouseEdge * (1 - Constants.FIVE_PERCENT_ERROR)));
      testPass = (percentErrorPass || ((-1 * totalEV < -1 * expectedHouseEdge + Constants.MAXIMUM_ABSOLUTE_ERROR) && (-1 * totalEV > -1 * expectedHouseEdge - Constants.MAXIMUM_ABSOLUTE_ERROR)));
   }
   if (!testPass) {

      System.err.println("Total EV test failed");
      System.err.println("Rules:" + theRules.toString());
      System.err.println("Computed house edge: " + totalEV + "; expected house edge: " + expectedHouseEdge);
      try {
         Thread.sleep(100);
      }
      catch (Exception e) {
      }
      myStrats.print(true);
      try {
         Thread.sleep(100);
      }
      catch (Exception e) {
      }
      assert false;
   }
   if (!percentErrorPass && verbosity) {
      System.out.println("Off by more than 5 % on this test. Split strategy table: ");
      myStrats.print(true);
   }
   return percentErrorPass;
}

/**
 * This computes the house edge for over 20 different scenarios*
 *
 * @param verbosity
 *
 *
 * *Single deck is barely tested.
 */
static public void doAll(boolean verbosity, boolean savingStatus) {
   saveAll = savingStatus;
   totalEVOneDeckHit17NoHole(verbosity);
   totalEVOneDeckStand17NoHole(verbosity);

   //Miscellaneous
   DealerCache.clearCache();
   if (verbosity) {
      System.out.println("All single deck tests passed.");
   }
   DealerCache.clearCache();


   totalEVTwoDeckHit17HoleCard(verbosity);
   if (verbosity) {
      System.out.println("All double deck tests passed.");
   }
   DealerCache.clearCache();

   totalEVFourDeckStand17NoHole(verbosity);
   totalEVFourDeckStand17HoleCard(verbosity);
   if (verbosity) {
      System.out.println("All four deck tests passed.");
   }
   DealerCache.clearCache();



   totalEVEightDeckHit17HoleCard(verbosity);
   if (verbosity) {
      System.out.println("All eight deck tests passed.");
   }
   DealerCache.clearCache();

   //need a stand-hole and a hit no-hole
   //10-14 check resplits
   //Surrendering with no-hole card seems to me to be ALWAYS an early surrender.
   //Do more tests to verify that the issue is the combination of surrender and no-hole, or if
   //it is something else.

   if (verbosity) {
      System.out.println("All total EV tests passed.");
   }
}

}

}
