package blackjack;
import blackjack.cards.*;
import java.io.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

/**
 * All of the tests for this blackjack package.
 *
 *
 */
public class Testers {
public static void main(String[] args) throws 
        NoRecommendationException, IOException, ClassNotFoundException {
   State.usedForCalculation(true);

   final boolean verbosity = false;
   Testers.allFastTests();

   /* Don't run these tests when I do not have the raw files in the directory, and
    * when calculations are deactivated.
    //runAllCalculations(false);
    //consolidateFiles(false);
    * validateNonConsolidatedFiles(false);
    //This should throw an exception quickly if calculations are deactivated
    */

//validateConsolidatedFiles(true); //This takes about 14 minutes.

   Strategy someStrat = new Strategy(new Rules(1), Strategy.Skill.COMP_DEP);
//someStrat.testToggles();
   Rules theRules = new Rules(2);
   theRules.setEarlySurrender(true);
   theRules.setHoleCard(true);
   someStrat.setCalculationDeactivated(true);
   someStrat.setMapLoadDeactivated(true);
   someStrat.solve(theRules);
   someStrat.print();

//Testers.testStrategy(false); //This test is time-consuming


   Testers.testTotalEV.doAll(false, false); //Verbosity, saving
   //Blackjack.printCacheStatus();
//Testers.testResplitEVs();
   //Blackjack.printCacheStatus();


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
   testGetStrategyType();
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
      anAction = myStrategy.findBestAction(myShoe, theRules, aState);
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
      anAction = myStrategy.findBestAction(myShoe, theRules, aState);
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
      anAction = myStrategy.findBestAction(myShoe, theRules, aState);
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
      anAction = myStrategy.findBestAction(myShoe, theRules, aState);
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

      Testers.simplePrintAnswer(someAnswers, theRules, false);
      assert false;
   }

   myCards.clear();
   myCards.add(new Card(Suit.CLUBS, CardValue.EIGHT));
   myCards.add(new Card(Suit.CLUBS, CardValue.EIGHT));
   aState = new State(myCards, dealerCard);
   aState.setDealerBlackjack(false);
   try {
      anAction = myStrategy.findBestAction(myShoe, theRules, aState);
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
 * Test of getStrategyType method, of class Strategy.
 */
public void testGetStrategyType() {
   Strategy.Skill expResult = Strategy.Skill.COMP_DEP;
   Strategy.Skill result = myStrategy.getStrategyType();
   assert (expResult == result);

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
         aStrategy.print();
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
      assert (aStrategy.findBestAction(someRules, someState) == Action.DOUBLE);
      //You should hit if you're playing composition-dependent.
      //Total-dependent should say to hit.
      if (verbosity) {  //haven't loaded the rules into the strategy before the first assert
         System.out.println("StrategyTest.testConsolidateHardAndOmniBus: Here is a rule set, "
                 + " and its total-dependent strategy, which I am testing.");
         System.out.println(someRules);
         aStrategy.print();
      }
      someState = new State(CardValue.SIX, CardValue.TWO, CardValue.SIX);
      assert (aStrategy.findBestAction(someRules, someState) == Action.DOUBLE); //Hit comp-dep
      someState = new State(CardValue.TEN, CardValue.TWO, CardValue.SIX);
      assert (aStrategy.findBestAction(someRules, someState) == Action.STAND); //Hit comp-dep

      someState = new State(CardValue.TEN, CardValue.TWO, CardValue.FOUR);
      assert (aStrategy.findBestAction(someRules, someState) == Action.STAND); //Hit comp-dep


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

      hardAnswers = Blackjack.solveHardPlayersRecursive(theRules, false);
      totalDependent = Blackjack.solveHardPlayersRecursive(theRules, false);
      softAnswers = Blackjack.solveSoftPlayersRecursive(theRules, false);
      splitAnswers = Blackjack.calculateAllSplitValues(theRules, hardAnswers, softAnswers, false);

      if (verbosity) {
         Testers.printStrategy(hardAnswers, theRules.toString(), false);
         Testers.printStrategy(softAnswers, "", false);
         Testers.simplePrintAnswer(splitAnswers, theRules, false);
      }



      Blackjack.consolidateIntoTotalDependent(totalDependent, theRules);

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
               if ((hardAnswers.get(i).get(j).getPreferredAction()
                       != totalDependent.get(i).get(j).getPreferredAction())
                       && (hardAnswers.get(i).get(j).getPreferredAction() != Action.SURRENDER)
                       && (totalDependent.get(i).get(j).getPreferredAction() != Action.SURRENDER)) {
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
private static void testTotalConsolidatedForConsolidation(boolean verbosity,
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
      for (CardValue k : Blackjack.twoToTen) {
         for (CardValue j : Blackjack.twoToTen) {
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
               aStrategy.print();
               throw new NoRecommendationException();
            }
            if (secondBestAction[handTotal] != scratchSecond) {
               System.err.println("Testers.StrategyTest.testTotalConsolidatedForConsolidation: "
                       + " When holding " + k + " and " + j + ", my preferred second best action is to "
                       + scratchSecond + ". However, the recommended action in another state with the same"
                       + " hand total is to " + secondBestAction[handTotal] + ".");
               System.out.println("Here are my strategy tables:");
               aStrategy.print();
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

   for (CardValue k : Blackjack.twoToTen) { //if ( (k == doNotSet[0]) || (k == doNotSet[1]) )
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

public static void testCards() {
   Suit Suittester = Suit.CLUBS;
   assert (Suittester == Suit.CLUBS);
   Suittester = Suit.DIAMONDS;
   assert (Suittester == Suit.DIAMONDS);
   Suittester = Suit.SPADES;
   assert (Suittester == Suit.SPADES);
   Suittester = Suit.HEARTS;
   assert (Suittester == Suit.HEARTS);

   CardValue val = CardValue.ACE;
   assert (val == CardValue.ACE);

   Card Funitude = new Card(Suittester, val);
   assert (Funitude.getCardValue() == CardValue.ACE);
   assert (Funitude.getSuit() == Suit.HEARTS);

   Shoe Deck = new Shoe(2);
   assert (Deck.numberOfCards() == 52 * 2);


}

/**
 * Tests that the given strategy obeys some common blackjack advice.
 * Essentially a wrapper function for testOneSolvedStrategy and
 * testTotalConsolidatedForConsolidation
 *
 * @param aStrategy
 */
static void validateSolvedStrategy(Strategy aStrategy) throws NoRecommendationException, IOException {
   //Card DCard;
   //Card firstPlayerCard;
   //Card secondPlayerCard;
   for (CardValue dealerCard : Blackjack.oneToTen) {
      for (CardValue firstPlayerCard : Blackjack.oneToTen) {
         for (CardValue secondPlayerCard : Blackjack.oneToTen) {
            testOneSolvedStrategy(dealerCard, firstPlayerCard, secondPlayerCard, aStrategy);
         }
      }
   }
   final boolean verbosity = false;

   if (aStrategy.getStrategyType() == Strategy.Skill.TOTAL_DEP) {
      StrategyTest.testTotalConsolidatedForConsolidation(verbosity, aStrategy);
   }

}

/**
 * The driving force behind validateSolvedStrategy.
 * Tests the given strategy to make sure that it obeys common sense blackjack
 * rules:
 * Split Aces against dealer 9 and under
 * Double on 11 against dealer 9 and under
 * Don't stand if your hand Total is 11 or under, unless you can early surrender
 * Stand on hard 20; always stand on blackjacks.
 * Don't surrender on dealer 2-7 up.
 * Never take insurance.
 * If you have hard 14 and over with a dealer 2-6 up, you should always stand or
 * split.
 *
 * @param dealerCard
 * @param firstPlayerCard
 * @param secondPlayerCard
 * @param aStrategy
 *
 */
private static void testOneSolvedStrategy(CardValue dealerCard,
        CardValue firstPlayerCard, CardValue secondPlayerCard,
        Strategy aStrategy) throws NoRecommendationException {
   Rules theRules = aStrategy.getMyRules();
   try {
      State myState = new State(new Card(Suit.CLUBS, firstPlayerCard), new Card(Suit.CLUBS, secondPlayerCard),
              new Card(Suit.SPADES, dealerCard));
      Action chosenAction;
      final int handTotal;
      boolean isSoft;
      Answer theAnswer = aStrategy.findBestAnswer(new Shoe(theRules.getNumberOfDecks()), theRules, myState);
      chosenAction = aStrategy.findBestAction(myState);

      //Always split aces, if it's possible, when the dealer doesn't have a 10 or ace up.
      if ((theRules.isPossible(Action.SPLIT, myState))
              && (firstPlayerCard == secondPlayerCard) && (firstPlayerCard == CardValue.ACE)) {
         if ((dealerCard != CardValue.TEN) && (dealerCard != CardValue.ACE)) {
            if (chosenAction != Action.SPLIT) {
               System.err.println("With two Aces and a dealer " + dealerCard + ", I chose to " + chosenAction.toString());
               System.err.println("Here is my rule set: " + theRules.toString());
               State.printStateStatus(myState, "Here is my state:");
               throw new NoRecommendationException();
            }
         }
         return;
      }

      //Don't surrender with dealer up card of 2-7
      if ((dealerCard.value() < 8) && (dealerCard.value() != CardValue.ACE.value())
              && (chosenAction == Action.SURRENDER)) {
         System.err.println("I chose to surrender when the dealer had less than an 8 up.");
         System.err.println(theAnswer);
         State.printStateStatus(myState, "");
         aStrategy.print();
         //re.printStackTrace(); No need, it'll never be caught.
         throw new NoRecommendationException();

      }

      if ((firstPlayerCard == CardValue.ACE) || (secondPlayerCard == CardValue.ACE)) {
         isSoft = true;
         handTotal = firstPlayerCard.value() + secondPlayerCard.value() + 10;
      }
      else {
         isSoft = false;
         handTotal = firstPlayerCard.value() + secondPlayerCard.value();
      }


      //ALWAYS STAND ON 21
      if (handTotal == 21) {
         if (chosenAction != Action.STAND) {
            System.err.println("With a hand total of " + handTotal + " and a dealer " + dealerCard + ", I chose to " + chosenAction.toString());
            System.err.println("Here is my rule set: " + theRules.toString());
            State.printStateStatus(myState, "Here is my state:");
            throw new NoRecommendationException();

         }
      }

      //NEVER TAKE INSURANCE
      if (myState.isInsuranceAdvised()) {
         System.err.println("With a hand total of " + handTotal + " and a dealer " + dealerCard + ", I chose to take insurance.");
         System.err.println("Here is my rule set: " + theRules.toString());
         State.printStateStatus(myState, "Here is my state:");
         throw new NoRecommendationException();
      }




      //ALWAYS STAND ON HARD 20
      if ((handTotal == 20) && (!isSoft)) {
         if (chosenAction != Action.STAND) {
            System.err.println("With a hand total of " + handTotal + " and a dealer " + dealerCard + ", I chose to " + chosenAction.toString());
            System.err.println("Here is my rule set: " + theRules.toString());
            State.printStateStatus(myState, "Here is my state:");
            throw new NoRecommendationException();

         }
      }

      //ALWAYS DOUBLE ON 11, IF POSSIBLE, ON DEALER 9 or under.
      if ((handTotal == 11) && (theRules.isPossible(Action.DOUBLE, myState))
              && (dealerCard.value() != 10) && (dealerCard.value() != 1)
              && (chosenAction != Action.DOUBLE)) {
         System.err.println("With a hand total of " + handTotal + " and a dealer " + dealerCard + ", I chose to " + chosenAction.toString() + ", not double.");
         System.err.println("Here is my rule set: " + theRules.toString());
         State.printStateStatus(myState, "Here is my state:");

         throw new NoRecommendationException();
      }

      //Always hit, double, or split if your hand total is 11 or under and
      //early surrender is not allowed. If early surrender is allowed, then
      //don't surrender unless the dealer has a 10 or ace up.
      if (handTotal <= 11) {
         if ((chosenAction != Action.HIT) && (chosenAction != Action.DOUBLE)
                 && (chosenAction != Action.SPLIT)) {
            if (!theRules.getEarlySurrender() && !theRules.getEarlySurrenderNotOnAces()) {

               System.err.println("With a hand total of " + handTotal + ", I did not choose to hit or double.");
               System.err.println("Here is my rule set: " + theRules.toString());
               System.err.println("My chosen action is: " + chosenAction);
               try {
                  Thread.sleep(1000);
               }
               catch (Exception e) {
               }
               State.printStateStatus(myState, "Here is my state:");
               throw new NoRecommendationException();
            }
            else if ((dealerCard != CardValue.TEN)
                    && (dealerCard != CardValue.ACE)) {

               System.err.println("With a hand total of " + handTotal + ", I did not choose to hit or double.");
               System.err.println("Here is my rule set: " + theRules.toString());
               System.err.println("My chosen action is: " + chosenAction);
               try {
                  Thread.sleep(1000);
               }
               catch (Exception e) {
               }
               State.printStateStatus(myState, "Here is my state:");
               throw new NoRecommendationException();

            }
            else ; //Early surrender on a ten or ace. Take it.
         }
      }


      //STAND ON A HARD 14+ if the dealer has 2-6 up.
      if ((handTotal > 14) && (dealerCard.value() >= 2) && (dealerCard.value() <= 6) && (!isSoft)) {
         if ((chosenAction != Action.STAND) && (chosenAction != Action.SPLIT)) {
            System.err.println("With a hand total of " + handTotal + ", I did not choose to hit or double.");
            System.err.println("Here is my rule set: " + theRules.toString());
            System.err.println("My chosen action is: " + chosenAction);
            try {
               Thread.sleep(1000);
            }
            catch (Exception e) {
            }
            State.printStateStatus(myState, "Here is my state:");
            throw new NoRecommendationException();
         }

      }


      //WHOLE FUNCTION ABOVE HERE
   }
   catch (IOException f) {
      throw new NoRecommendationException(f);
   }

}

public static void allFastTests() {
   //Faster tests -- those which don't require solving the strategy.
   Blackjack.setCache(Blackjack.FULL_CACHE);
   final long startTime = System.currentTimeMillis();
   testInsuranceGoodIdea();
   Testers.testResolveHands();
   Testers.testCards();
   Testers.testOverloadedDealer(); //Utilities functions
   Testers.testOverLoadFastShoe(1);
   Testers.testShoe();
   Testers.dealerClassTest();
   Testers.testDealerHCP();

   Testers.basicRulesTester();
   Testers.testRulesConstructors();
   Testers.testState();
   AnswerTest.runAllTests();

   Testers.testRulesHash(false); //Generates ~410k unique keys at last count.
   //false = no verbosity
   System.out.println("Quick tests took " + (System.currentTimeMillis() - startTime) + " ms.");

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
                    d.toString() + ": " + Blackjack.splitSolve(theRules,
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
 * Helper function for testShoe
 *
 *
 * @param numberDecks
 * @throws ShuffleNeededException
 */
private static void shoeCopyConstructorTest(int numberDecks) throws ShuffleNeededException {
   Shoe firstShoe = new Shoe(numberDecks);
   Card myAce = new Card(Suit.CLUBS, CardValue.ACE);

   Shoe secondShoe = firstShoe.deepClone();
   assert (firstShoe.numberOfCards() == 52 * numberDecks);
   assert (secondShoe.numberOfCards() == 52 * numberDecks);

   secondShoe.drawSpecific(CardValue.NINE);
   secondShoe.drawSpecific(CardValue.NINE);
   secondShoe.drawSpecific(CardValue.NINE);
   secondShoe.drawSpecific(CardValue.NINE);
   firstShoe.drawSpecific(CardValue.TWO);
   firstShoe.drawSpecific(CardValue.TWO);
   firstShoe.drawSpecific(CardValue.TWO);
   firstShoe.drawSpecific(CardValue.TWO);
   final double numDecks = numberDecks;

   assert (firstShoe.fastProbabilityOf(CardValue.NINE)
           < ((double) (4D * numDecks) / (double) (52D * numDecks - 4D)) + Blackjack.EPSILON) :
           firstShoe.fastProbabilityOf(CardValue.NINE) + " calculated, expected"
           + (((4D * numDecks) / (52D * numDecks - 4D)) + Blackjack.EPSILON);



   assert (firstShoe.fastProbabilityOf(CardValue.ACE)
           > ((4D * numDecks) / (52D * numDecks - 4D)) - Blackjack.EPSILON);

   assert (secondShoe.fastProbabilityOf(CardValue.NINE)
           < ((4D * numDecks - 4D) / (52D * numDecks - 4D) + Blackjack.EPSILON));

   if (numberDecks != 1) {
      assert (secondShoe.fastProbabilityOf(CardValue.NINE)
              > ((4D * numDecks - 4D) / (52D * numDecks - 4D) - Blackjack.EPSILON));
   }
   else {
      assert (secondShoe.fastProbabilityOf(CardValue.NINE) < 0);
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
   assert (myState.getTotalBetAmount() < 1.5 + Blackjack.EPSILON);
   assert (myState.getTotalBetAmount() > 1.5 - Blackjack.EPSILON);
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

   assert (otherState.getTotalBetAmount() < 3 + Blackjack.EPSILON);
   assert (otherState.getTotalBetAmount() > 3 - Blackjack.EPSILON);
   assert (otherState.getHandResult(1, 25, false) == State.LOSE);
   assert (otherState.getHandResult(1, 21, true) == State.LOSE);
   try {
      assert (otherState.getHandResult(1, 18, true) == State.LOSE);
      throw new IllegalStateException("State.getHandResult failed test -- no exception thrown"
              + " when it was told the dealer had a blackjack and a hand total of 18.");
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

   assert (aState.getTotalBetAmount() < 1.5 + Blackjack.EPSILON);
   assert (aState.getTotalBetAmount() > 1.5 - Blackjack.EPSILON);

   assert (aState.getHandResult(0, 25, false) == State.BLACKJACK);
   assert (aState.getHandResult(0, 21, true) == State.PUSH);
   assert (aState.getHandResult(0, 21, false) == State.BLACKJACK);
   assert (aState.getHandResult(0, 18, false) == State.BLACKJACK);

}

/**
 * Clearly inadequate testing.
 *
 *
 */
static void basicRulesTester() {
   Rules theseRules = new Rules();
   assert (theseRules.hitOn17());
   assert (theseRules.dealerHoleCard());
   ArrayList<Card> startingCards = new ArrayList<Card>();
   startingCards.add(new Card(Suit.CLUBS, CardValue.TEN));
   startingCards.add(new Card(Suit.SPADES, CardValue.JACK));

   State myState = new State(startingCards, new Card(Suit.DIAMONDS,
           CardValue.TWO));

   assert (theseRules.isPossible(Action.STAND, myState));
   assert (theseRules.isPossible(Action.SPLIT, myState)) : myState.toString();
   assert (theseRules.numPossibleActions(myState, false) == 5);
   myState.action(Action.HIT, new Card(Suit.SPADES, CardValue.ACE));
   assert (theseRules.numPossibleActions(myState, true) == 2);

   myState = new State(CardValue.THREE, CardValue.THREE, CardValue.NINE);
   theseRules = new Rules(1);
   theseRules.setLateSurrender(false);
   theseRules.myDoubleRules.setOnlyNineTenEleven(true);
   assert (theseRules.numPossibleActions(myState, true) == 3);

   myState = new State(CardValue.EIGHT, CardValue.EIGHT, CardValue.EIGHT);
   myState.action(Action.SPLIT);
   myState.postSplitDraw(new Card(Suit.SPADES, CardValue.TEN));
   assert (theseRules.numPossibleActions(myState, true) == 2);

}

/**
 * Currently just tests the copy constructor. Note that the copy constructor is
 * also
 * tested every time it runs, by the hash.
 *
 */
static void testRulesConstructors() {
   Rules theRules, otherRules;
   theRules = new Rules(1);
   otherRules = new Rules(theRules);
   testRulesCopyConstructor(theRules, otherRules);

   theRules.setAccuracy(Rules.LOW_ACCURACY);
   theRules.setCharlie(5);
   theRules.setEarlySurrender(true);
   theRules.setHitSplitAces(true);
   theRules.setNumberDecks(2);
   theRules.setMaxNumberSplitHands(2);
   theRules.myDoubleRules.setNotOnAces(true);
   theRules.myDoubleRules.setOnlyNineTenEleven(true);
   theRules.myDoubleRules.setNotPostSplit(true);
   otherRules = new Rules(theRules);
   testRulesCopyConstructor(theRules, otherRules);

   theRules = new Rules(otherRules);
   theRules.setRulesAutoToggles(false);
   theRules.myDoubleRules.setAnyTwoCards(false);
   theRules.myDoubleRules.setOnlyNineTenEleven(true);
   theRules.myDoubleRules.setOnlyTenAndEleven(true);
   otherRules = new Rules(theRules);
   testRulesCopyConstructor(theRules, otherRules);
}

/**
 * Tests the copy constructor
 *
 * @param theRules
 * @param otherRules
 */
static private void testRulesCopyConstructor(Rules theRules, Rules otherRules) {
   assert (theRules.getBlackJackPayback() < otherRules.getBlackJackPayback() + Blackjack.EPSILON);
   assert (theRules.getBlackJackPayback() > otherRules.getBlackJackPayback() - Blackjack.EPSILON);
   assert (theRules.getCharlie() == otherRules.getCharlie());
   assert (theRules.dealerHoleCard() == otherRules.dealerHoleCard());
   assert (theRules.getDealerMaxHandSize() == otherRules.getDealerMaxHandSize());
   assert (theRules.getEarlySurrender() == otherRules.getEarlySurrender());
   assert (theRules.getEarlySurrenderNotOnAces() == otherRules.getEarlySurrenderNotOnAces());
   assert (theRules.hitOn17() == otherRules.hitOn17());
   assert (theRules.hitSplitAces() == otherRules.hitSplitAces());
   assert (theRules.getLateSurrender() == otherRules.getLateSurrender());
   assert (theRules.getMaxNumberSplitHands() == otherRules.getMaxNumberSplitHands());
   assert (theRules.getAccuracy() == otherRules.getAccuracy());
   assert (theRules.getNumResplitAces() == otherRules.getNumResplitAces());
   assert (theRules.getNumberOfDecks() == otherRules.getNumberOfDecks());
   assert (theRules.getPlayerMaxHandSize() == otherRules.getPlayerMaxHandSize());
   assert (theRules.getAutoToggles() == otherRules.getAutoToggles());
   assert (theRules.myDoubleRules.alwaysPossible() == otherRules.myDoubleRules.alwaysPossible());
   assert (theRules.myDoubleRules.anyTwoCards() == otherRules.myDoubleRules.anyTwoCards());
   assert (theRules.myDoubleRules.notOnAces() == otherRules.myDoubleRules.notOnAces());
   assert (theRules.myDoubleRules.notSplitAces() == otherRules.myDoubleRules.notSplitAces());
   assert (theRules.myDoubleRules.onlyNineTenEleven() == otherRules.myDoubleRules.onlyNineTenEleven());
   assert (theRules.myDoubleRules.onlyTenAndEleven() == otherRules.myDoubleRules.onlyTenAndEleven());
//Boring. But probably useful.

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
   int[] handArray = new int[10];
   Utilities.convertCardArraytoArray(myCards, handArray);
   final double[] epicFail =
           Blackjack.DealerRecursive(handArray, Deck, myRules);
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

public static void dealerClassTest() {
   ArrayList<Card> myCards = new ArrayList<Card>();
   int failedTests = 0;
   int totalTests = 0;
   int success = 0;
   Card anAce = new Card(Suit.CLUBS, CardValue.ACE);
   Card aTwo = new Card(Suit.SPADES, CardValue.TWO);
   Card aTen = new Card(Suit.DIAMONDS, CardValue.TEN);
   Card aJack = new Card(Suit.HEARTS, CardValue.JACK);
   Card aFive = new Card(Suit.CLUBS, CardValue.FIVE);

   myCards.add(anAce);
   myCards.add(anAce);
   myCards.add(anAce);

   if (Utilities.contains(myCards, CardValue.FIVE)) {
      failedTests++;
   }
   else {
      success++;
   }

   totalTests++;
   if (Utilities.isBust(myCards)) {
      failedTests++;
   }
   else {
      success++;
   }

   if (!Utilities.isSoft(myCards)) {
      failedTests++;
   }
   else {
      success++;
   }

   if (!(Utilities.handTotal(myCards) == 13)) {
      failedTests++;
   }
   else {
      success++;
   }
   myCards.remove(0);
   myCards.remove(0);
   myCards.remove(0);

   myCards.add(aTen);
   myCards.add(aTen);
   myCards.add(anAce);

   if (!Utilities.contains(myCards, CardValue.JACK)) {
      failedTests++;
   }
   else {
      success++;
   }

   if (Utilities.isBust(myCards)) {
      failedTests++;
   }
   else {
      success++;
   }

   if (Utilities.isSoft(myCards)) {
      failedTests++;
   }
   else {
      success++;
   }

   if (!(Utilities.handTotal(myCards) == 21)) {
      failedTests++;
   }
   else {
      success++;
   }
   myCards.remove(0);
   myCards.remove(0);
   myCards.remove(0);


   myCards.add(aTwo);
   myCards.add(aTen);
   myCards.add(aJack);
   if (failedTests > 0) {
      System.out.println("Dealer class test: Failed tests by this point");
      assert false;
   }
   if (Utilities.contains(myCards, CardValue.ACE)) {
      failedTests++;
   }
   else {
      success++;
   }

   if (!Utilities.isBust(myCards)) {
      failedTests++;
   }
   else {
      success++;
   }

   if (Utilities.isSoft(myCards)) {
      failedTests++;
   }
   else {
      success++;
   }

   if (!(Utilities.handTotal(myCards) == 22)) {
      failedTests++;
   }
   else {
      success++;
   }
   myCards.remove(0);
   myCards.remove(0);
   myCards.remove(0);


   if (failedTests > 0) {
      System.out.println(
              "Test conducted on four functions of the Dealer class:");
      System.out.println(
              success + " tests worked; " + failedTests + " did not work.");
      assert false;
   }


   int[] someCards = new int[10];
   Utilities.zero(someCards);
   someCards[0] = 1;
   assert (Utilities.retrieveSingleCard(someCards) == 0) : "Dealer.retrieveSingleCard failed.";
   someCards[1] = 1;
   try {
      Utilities.retrieveSingleCard(someCards);
      assert (false) : "Dealer.retrieveSingleCard did not throw exception on two cards in hand.";
   }
   catch (IllegalArgumentException e) {
   }
   someCards[1] = someCards[0] = 0;
   someCards[9] = 1;
   assert (Utilities.retrieveSingleCard(someCards) == 9) : "Dealer.retrieveSingleCard failed.";

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
   final int[] handArray = new int[10];

   Utilities.convertCardArraytoArray(startingCards, handArray);

   ArrayList<Card> cloneOfStartHand = new ArrayList<Card>();
   for (int i = 0; i < startingCards.size(); i++) {
      cloneOfStartHand.add(new Card(startingCards.get(i)));
   }

   final double[] calculatedResults = Blackjack.DealerRecursive(handArray, new FastShoe(myShoe), theRules);

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

   alwaysCloned = Blackjack.resolveHands(alwaysCloned, myShoe,
           theRules);


   assert (alwaysCloned.getTotalBetAmount() < 2.5 + Blackjack.EPSILON);
   assert (alwaysCloned.getTotalBetAmount() > 2.5 - Blackjack.EPSILON) :
           "The total bet amount claims it is " + alwaysCloned.getTotalBetAmount();

   value = alwaysCloned.getExpectedValue();


   //OK. To calculate first do insurance:
   double probOfTen = myShoe.fastProbabilityOf(CardValue.TEN);
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
   dealerCards = Utilities.convertCardArraytoArray(dealerArrayList, dealerCards);
   double[] dealerResults = Blackjack.DealerRecursive(dealerCards, myShoe, theRules);

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

   assert ((calculatedValue - Blackjack.EPSILON < value) && (value < calculatedValue + Blackjack.EPSILON)) :
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

   alwaysCloned = Blackjack.resolveHands(alwaysCloned, myShoe,
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
   alwaysCloned = Blackjack.resolveHands(alwaysCloned, myShoe,
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


   alwaysCloned = Blackjack.resolveHands(alwaysCloned, myShoe,
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
      return Blackjack.PlayerRecursive(myShoe, myState, theRules);
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

   aState = Blackjack.resolveHands(aState, myShoe,
           theRules);



   int[] dealerCards = new int[10];
   Utilities.zero(dealerCards);

   dealerCards[ aState.getDealerUpCard().value() - 1] = 1;
   double[] dealerProbs = Blackjack.DealerRecursive(dealerCards, myShoe, theRules);
   //13 and 20. That's what both of these hands have. I also took insurance. Dang that's way
   //too many calculations.

   //Insurance first
   double expectedEV = -0.5;



   //13 hand.
   expectedEV += 1 * dealerProbs[0]; // -.59
   expectedEV += -1 * (1 - dealerProbs[0]);

   //20 hand. Lose on 21. Nothing on 20. Win otherwise.
   assert (dealerProbs[1] < Blackjack.EPSILON);
   double notWin = dealerProbs[dealerProbs.length - 1];  // = 6
   expectedEV += -1 * notWin;
   notWin += dealerProbs[dealerProbs.length - 2];
   expectedEV += 1 * (1 - notWin);


   assert (aState.getExpectedValue() < expectedEV + Blackjack.EPSILON);
   assert (aState.getExpectedValue() > expectedEV - Blackjack.EPSILON);


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
                 solvedStates.get(i).get(j).getPreferredAction().abbrev() + "        ");
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
   myCards[Blackjack.ACECARD] = 1;
   testOverloadedDealer(myCards, 11, 1, true);
   myCards[Blackjack.TENCARD] = 1;
   testOverloadedDealer(myCards, 21, 2, true);
   myCards[Blackjack.NINECARD] = 1;
   testOverloadedDealer(myCards, 20, 3, false);
   myCards[Blackjack.SEVENCARD] = 1;
   testOverloadedDealer(myCards, 27, 4, false);
   Utilities.zero(myCards);
   myCards[Blackjack.ACECARD] = 4;
   testOverloadedDealer(myCards, 14, 4, true);

   testGetDealerHand();

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
 * Crude test of getDealerHand
 * Run by testOverloadedDealer
 *
 */
private static void testGetDealerHand() {

   ArrayList<Card> initialCards = new ArrayList<Card>();

   int handTotal;
   final Rules theRules = new Rules(1);
   Shoe myShoe = new Shoe(theRules.getNumberOfDecks());

   theRules.setHoleCard(false);
   Card two = new Card(Suit.CLUBS, CardValue.TWO);
   initialCards.add(two);
   double[] resultTotals = new double[7]; //not implemented yet.

   for (int i = 0; i < 10000; i++) {
      initialCards = Utilities.getDealerHand(theRules, initialCards, myShoe);
      handTotal = Utilities.handTotal(initialCards);
      assert (handTotal > 16) : "Handtotal is " + handTotal;
      assert (handTotal < 27) : "Handtotal is " + handTotal;


      myShoe = new Shoe(theRules.getNumberOfDecks());
      initialCards.clear();
      initialCards.add(two);
   }


}

/**
 * Helper function for testShoe
 * Tests most functionality of drawAppropriates.
 * Does not test for exception throwing functionality, though.
 *
 * @param myShoe
 * @throws ShuffleNeededException
 *
 */
public static void testDrawAppropriateFunctions(Shoe myShoe) throws ShuffleNeededException {
   Card drawnCard;
   Card secondDrawnCard;
   List<Card> scratch = new ArrayList<Card>();
   Rules theRules = new Rules(1);
//Apparently each number takes less than 0.0057 ms to generate. Not that long after all.
   for (int i = 0; i < 1000; i++) {

      drawnCard = myShoe.drawAppropriate(DrawMode.ALL_HARD, true, theRules);
      secondDrawnCard = myShoe.drawAppropriate(DrawMode.ALL_HARD, drawnCard);
      if ((drawnCard.getCardValue() == CardValue.ACE)
              || (secondDrawnCard.getCardValue() == CardValue.ACE)) {
         throw new RuntimeException("drawAppropriate drew the wrong card.");
      }
      myShoe.addCard(drawnCard);
      myShoe.addCard(secondDrawnCard);

      //Strategy.GameMode.ALL_HARD

      drawnCard = myShoe.drawAppropriate(DrawMode.HARD_12_16, true, theRules);
      secondDrawnCard = myShoe.drawAppropriate(DrawMode.HARD_12_16, drawnCard);
      if ((drawnCard.getCardValue() == CardValue.ACE)
              || (secondDrawnCard.getCardValue() == CardValue.ACE)) {
         throw new RuntimeException("drawAppropriate drew an ace on Shoe.HARD_TOTAL_12_16.");
      }
      if (((drawnCard.value() + secondDrawnCard.value()) > 16)
              || ((drawnCard.value() + secondDrawnCard.value()) < 12)) {
         throw new RuntimeException("Hand total wrong on Shoe.HARD_TOTAL_12_16: "
                 + ((drawnCard.value() + secondDrawnCard.value())));
      }
      myShoe.addCard(drawnCard);
      myShoe.addCard(secondDrawnCard);


      drawnCard = myShoe.drawAppropriate(DrawMode.ALL_SOFT, true, theRules);
      secondDrawnCard = myShoe.drawAppropriate(DrawMode.ALL_SOFT, drawnCard);
      if ((drawnCard.getCardValue() != CardValue.ACE)
              && (secondDrawnCard.getCardValue() != CardValue.ACE)) {
         throw new RuntimeException("Ace not drawn to soft hand in drawAppropriate.");
      }
      myShoe.addCard(secondDrawnCard);
      myShoe.addCard(drawnCard);

      drawnCard = myShoe.drawAppropriate(DrawMode.ALL_SOFT_AND_HARD, true, theRules);
      secondDrawnCard = myShoe.drawAppropriate(DrawMode.ALL_SOFT_AND_HARD, drawnCard);
      if (drawnCard.value() == secondDrawnCard.value()) {
         throw new RuntimeException(
                 "The same two CVs were drawn in drawAppropiate to DrawMode.ALL_SOFT_AND_HARD:"
                 + drawnCard.toString() + " and " + secondDrawnCard.toString());
      }
      myShoe.addCard(secondDrawnCard);
      myShoe.addCard(drawnCard);

      drawnCard = myShoe.drawAppropriate(DrawMode.ALL_SPLITS, true, theRules);
      secondDrawnCard = myShoe.drawAppropriate(DrawMode.ALL_SPLITS, drawnCard);
      if (drawnCard.value() != secondDrawnCard.value()) {
         throw new RuntimeException("Split cards not of identical value in drawAppropriate:"
                 + drawnCard.toString() + " and " + secondDrawnCard.toString());
      }
      myShoe.addCard(secondDrawnCard);
      myShoe.addCard(drawnCard);


      drawnCard = myShoe.drawAppropriate(DrawMode.SOFT_OVER_16, true, theRules);
      secondDrawnCard = myShoe.drawAppropriate(DrawMode.SOFT_OVER_16, drawnCard);
      if ((drawnCard.getCardValue() != CardValue.ACE) && (secondDrawnCard.getCardValue() != CardValue.ACE)) {
         throw new RuntimeException("SOFT_OVER_16 did not return an Ace in drawAppropriates:"
                 + drawnCard.toString() + " and " + secondDrawnCard.toString());
      }
      scratch.add(drawnCard);
      scratch.add(secondDrawnCard);
      if ((Utilities.handTotal(scratch) < 16) || (Utilities.handTotal(scratch) > 21)) {
         throw new RuntimeException("SOFT_OVER_16 did not give the correct hand total in drawAppropriates:"
                 + drawnCard.toString() + " and " + secondDrawnCard.toString());
      }
      scratch.clear();
      myShoe.addCard(secondDrawnCard);
      myShoe.addCard(drawnCard);

      drawnCard = myShoe.drawAppropriate(DrawMode.SOFT_UNDER_16, true, theRules);
      secondDrawnCard = myShoe.drawAppropriate(DrawMode.SOFT_UNDER_16, drawnCard);
      if ((drawnCard.getCardValue() != CardValue.ACE) && (secondDrawnCard.getCardValue() != CardValue.ACE)) {
         throw new RuntimeException("SOFT_UNDER_16 did not return an Ace in drawAppropriates:"
                 + drawnCard.toString() + " and " + secondDrawnCard.toString());
      }
      scratch.add(drawnCard);
      scratch.add(secondDrawnCard);
      if (Utilities.handTotal(scratch) > 16) {
         throw new RuntimeException("SOFT_UNDER_16 did not give the correct hand total in drawAppropriates:"
                 + drawnCard.toString() + " and " + secondDrawnCard.toString());
      }
      scratch.clear();
      myShoe.addCard(secondDrawnCard);
      myShoe.addCard(drawnCard);

      /* MOTHBALLED

       drawnCard = myShoe.drawAppropriate(Shoe.HAND_TOTAL_UNDER_9, true,theRules );
       if ( (drawnCard.value() < 2) || (drawnCard.value() > 6) )
       throw new RuntimeException("drawAppropriate draw a " + drawnCard.toString() + ", not 2-7.");

       secondDrawnCard = myShoe.drawAppropriate(Shoe.HAND_TOTAL_UNDER_9, drawnCard);
       if ( (secondDrawnCard.value() < 2) || (secondDrawnCard.value() > 6) )
       throw new RuntimeException("drawAppropriate(2-args) draw a " + drawnCard.toString() + ", not 2-7.");

       if  (  (drawnCard.value() + secondDrawnCard.value() ) > 8)
       throw new RuntimeException("Hand total wrong on Shoe.HAND_TOTAL_UNDER_9: " +
       ((drawnCard.value() + secondDrawnCard.value() )) );

       myShoe.addCard(secondDrawnCard);
       myShoe.addCard(drawnCard);

       */

      drawnCard = myShoe.drawAppropriate(DrawMode.DEALER_2_6, false, theRules);
      if ((drawnCard.value() < 2) || (drawnCard.value() > 6)) {
         throw new RuntimeException("drawAppropriate drew a " + drawnCard.toString() + ", not 2-6.");
      }
      myShoe.addCard(drawnCard);



      drawnCard = myShoe.drawAppropriate(DrawMode.DEALER_7_A, false, theRules);
      if ((drawnCard.value() >= 2) && (drawnCard.value() <= 6)) {
         throw new RuntimeException("drawAppropriate draw a " + drawnCard.toString() + ", not 7-A.");
      }
      myShoe.addCard(drawnCard);



      /*


       drawnCard = myShoe.drawAppropriate(Shoe.HAND_TOTAL_UNDER_12, true,theRules );
       if ( (drawnCard.value() < 2) || (drawnCard.value() > 9) )
       throw new RuntimeException("drawAppropriate draw a " + drawnCard.toString() + ", not 2-9.");
       secondDrawnCard = myShoe.drawAppropriate(Shoe.HAND_TOTAL_UNDER_12, drawnCard);
       if ( (secondDrawnCard.value() < 2) || (secondDrawnCard.value() > 9) )
       {
       throw new RuntimeException("drawAppropriate(2-args) draw a " + secondDrawnCard.toString() + ", of value "
       + secondDrawnCard.value() + "; not of value 2-9.");
       }
       if  (  (drawnCard.value() + secondDrawnCard.value() ) >= 12)
       throw new RuntimeException("Hand total wrong on Shoe.HAND_TOTAL_UNDER_12: " +
       ((drawnCard.value() + secondDrawnCard.value() )) );
       myShoe.addCard(drawnCard);
       myShoe.addCard(secondDrawnCard);


       */
   }

}

/**
 * testDrawAppropriateFunctions -- some parts are mothballed (2-6, 7-A, + ?1
 * more)
 *
 *
 * Tests fastProbabilityOf and drawAppropriates, nothing else.
 *
 */
public static void testShoe() {

   Shoe Deck = new Shoe(8);
   final int cardsInDeck = 8 * 52;
   assert (cardsInDeck == Deck.numberOfCards());
   final int[] internalCVC = Deck.getCardValueCache();
   for (CardValue i : CardValue.values()) {
      assert ((Deck.fastProbabilityOf(i)
              < ((double) internalCVC[i.value()] / (double) cardsInDeck) + Blackjack.EPSILON)
              && (Deck.fastProbabilityOf(i)
              > ((double) internalCVC[i.value()] / (double) cardsInDeck - Blackjack.EPSILON)));
   }

   Shoe myShoe = new Shoe(1);
   try {
      testDrawAppropriateFunctions(myShoe);
      for (int i = 1; i < 10; i++) {
         shoeCopyConstructorTest(i);
      }
   }
   catch (ShuffleNeededException notShuffled) {
      System.out.println("testDrawAppropriate(int cardPullCode) or shoeCopyConstructor(int) threw an exception.");
      notShuffled.printStackTrace();
      assert false;
   }




}

/**
 * Tests the fast shoe key used in the dealer probability cache.
 * This could use vast improving. Specifically, a recursive solution
 * to go to an arbitrary depth, and a permutation formula to solve
 * for the total number of expected keys at a given depth.
 */
public static void testFastShoeKey() {
   int number;
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
            /*
             for (int cv3 =0; cv3<10; cv3++)
             {
             myShoe.fasterDrawSpecific(cv3);
             allTheKeys.add(myStringKey());
             myShoe.addCard(cv3);
             }  */
            myShoe.addCard(cv2);
         }
         myShoe.addCard(cv1);

      }
   }
   final int twoRemovedKeys = 55 * (DECK_CHOICES - 1);
   final int oneRemovedKeys = 10 * (DECK_CHOICES - 1);
   assert (allTheKeys.size() == (oneRemovedKeys + twoRemovedKeys)) :
           "There are this many keys: " + allTheKeys.size();
   //55 permutations while removing 2 cards

}

/**
 * This calls testFastShoeKey.
 * Functions that haven't been tested here:

 */
public static void testOverLoadFastShoe(int numDecks) {
   testFastShoeKey();
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

      assert (value > (1 + Blackjack.FIVE_PERCENT_ERROR) * -0.12446) : value + " for best action: "
              + myStrategy.findBestAction(theRules, aState);

      assert (value < (1 - Blackjack.FIVE_PERCENT_ERROR) * -0.12446) : value;

      aState = new State(new Card(Suit.CLUBS, CardValue.EIGHT),
              new Card(Suit.CLUBS, CardValue.EIGHT),
              new Card(Suit.CLUBS, CardValue.TWO));
      assert (myStrategy.findBestEV(theRules, aState) < (1 + Blackjack.FIVE_PERCENT_ERROR) * 0.097472);
      assert (myStrategy.findBestEV(theRules, aState) > (1 - Blackjack.FIVE_PERCENT_ERROR) * 0.097472);

      aState = new State(new Card(Suit.CLUBS, CardValue.FOUR),
              new Card(Suit.CLUBS, CardValue.FOUR),
              new Card(Suit.CLUBS, CardValue.SIX));
      assert (myStrategy.findBestEV(theRules, aState) < (1 + Blackjack.FIVE_PERCENT_ERROR) * 0.199142);
      assert (myStrategy.findBestEV(theRules, aState) > (1 - Blackjack.FIVE_PERCENT_ERROR) * 0.199142);

      aState = new State(new Card(Suit.CLUBS, CardValue.NINE),
              new Card(Suit.CLUBS, CardValue.NINE),
              new Card(Suit.CLUBS, CardValue.SIX));
      assert (myStrategy.findBestEV(theRules, aState) < (1 + Blackjack.FIVE_PERCENT_ERROR) * 0.44897);
      assert (myStrategy.findBestEV(theRules, aState) > (1 - Blackjack.FIVE_PERCENT_ERROR) * 0.44897);

      //Now change the rules.
      theRules.myDoubleRules.setNotPostSplit(true);

      aState = new State(new Card(Suit.CLUBS, CardValue.SEVEN),
              new Card(Suit.CLUBS, CardValue.SEVEN),
              new Card(Suit.CLUBS, CardValue.TWO));
      assert (myStrategy.findBestEV(theRules, aState) > (1 + Blackjack.FIVE_PERCENT_ERROR) * -0.179005) : myStrategy.findBestEV(theRules, aState);
      assert (myStrategy.findBestEV(theRules, aState) < (1 - Blackjack.FIVE_PERCENT_ERROR) * -0.179005);

      aState = new State(new Card(Suit.CLUBS, CardValue.EIGHT),
              new Card(Suit.CLUBS, CardValue.EIGHT),
              new Card(Suit.CLUBS, CardValue.TWO));
      value = myStrategy.findBestEV(theRules, aState);

      assert (value < (1 + Blackjack.FIVE_PERCENT_ERROR) * 0.01726) :
              value + " for best action: " + myStrategy.findBestAction(theRules, aState);

      assert (value > (1 - Blackjack.FIVE_PERCENT_ERROR) * 0.01726) : value + " for best action: "
              + myStrategy.findBestAction(theRules, aState);
      // These two numbers are from http://www.bjstrat.net/cgi-bin/cdca.cgi
      //


      aState = new State(new Card(Suit.CLUBS, CardValue.THREE),
              new Card(Suit.CLUBS, CardValue.THREE),
              new Card(Suit.CLUBS, CardValue.SIX));
      assert (myStrategy.findBestEV(theRules, aState) < (1 + Blackjack.FIVE_PERCENT_ERROR) * 0.0905487);
      assert (myStrategy.findBestEV(theRules, aState) > (1 - Blackjack.FIVE_PERCENT_ERROR) * 0.0905487);

      aState = new State(new Card(Suit.CLUBS, CardValue.NINE),
              new Card(Suit.CLUBS, CardValue.NINE),
              new Card(Suit.CLUBS, CardValue.SIX));
      assert (myStrategy.findBestEV(theRules, aState) < (1 + Blackjack.FIVE_PERCENT_ERROR) * 0.389554);
      assert (myStrategy.findBestEV(theRules, aState) > (1 - Blackjack.FIVE_PERCENT_ERROR) * 0.389554);



   }
   catch (NoRecommendationException nre) {
      throw new RuntimeException(nre);
   }
   catch (IOException io) {
      throw new RuntimeException(io);
   }

}

static public void viewRawResplits() {
   try {


      ArrayList<ArrayList<State>> solvedHard = new ArrayList<ArrayList<State>>();
      ArrayList<ArrayList<State>> solvedSoft = new ArrayList<ArrayList<State>>();
      Rules theRules = new Rules(4);
      theRules.setNumResplitAces(1); //Aces be split, but not resplit
      theRules.setMaxNumberSplitHands(2); //But other things can be resplit
      theRules.setHitSplitAces(false);
      theRules.setAccuracy(Rules.CACHE_ACCURACY);

      //Assuming the dealer does NOT have blackjack.
      final String rulesHash = theRules.toString();
      System.out.println(rulesHash);
      solvedSoft = Blackjack.solveSoftPlayersRecursive(theRules, false);
      solvedHard = Blackjack.solveHardPlayersRecursive(theRules, false);

      Testers.printStrategy(solvedSoft, "Assuming the dealer doesn't have blackjack.", false);
      System.out.println("------------------------------------");
      Testers.printStrategy(solvedHard, "Assuming the dealer doesn't have blackjack.", false);

      Testers.viewRawSplitEV(theRules, solvedHard, solvedSoft, false);
      if (!rulesHash.equals(theRules.toString())) {
         System.out.println("Rules were corrupted.");
      }
      //double q = -7.96;
      //System.out.format("%+.2f  " ,  q);

   }
   catch (NoRecommendationException e) {
      e.printStackTrace();
      throw new RuntimeException(e);

   }


}

/**
 * Provides simple testing of split answers. Does no testing on early surrender
 * cases.
 *
 * Add assert back in after mystery bug found on 8 8 A that thinks splitting is
 * a bad
 * idea.
 *
 * I should be using validateSolvedStrategy instead.
 *
 * @param stupidAnswers
 * @param theRules
 */
@Deprecated
static public void simplePrintAnswer(ArrayList<Answer> stupidAnswers,
        Rules theRules, final boolean verbosity) {
   CardValue firstCard;
   CardValue secondCard, dealerCard;
   Action bestAction;
   for (int i = 0; i < stupidAnswers.size(); i++) {
      firstCard = stupidAnswers.get(i).getFirstPlayerCard();
      secondCard = stupidAnswers.get(i).getSecondPlayerCard();
      dealerCard = stupidAnswers.get(i).getDealerCard();
      bestAction = stupidAnswers.get(i).getBestAction();

      if ((firstCard == secondCard)
              && (firstCard
              == CardValue.ACE) || (firstCard == CardValue.EIGHT)
              && (theRules.getEarlySurrender() == false)) {
         if ((theRules.dealerHoleCard() == false)
                 && (theRules.getNumberOfDecks() >= 4)) {
            if ((dealerCard == CardValue.ACE)
                    || (dealerCard == CardValue.TEN)) {
               assert (bestAction == Action.HIT);
            }
            else //Unsure of where I came up with these tests
            {
               assert (bestAction == Action.SPLIT);
            }
         }
         else if ((theRules.getNumberOfDecks() >= 2)
                 && (firstCard == CardValue.EIGHT)) {
            assert (bestAction == Action.SURRENDER);
         }
         else if (theRules.dealerHoleCard()) {
            assert (bestAction == Action.SPLIT);
         }
      }

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
      myState = Blackjack.PlayerRecursive(myShoe, myState, theRules);

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
   assert (solvedState.getPreferredAction() == Action.DOUBLE) : "Correct action not picked";
   assert ((solvedState.getExpectedValue() < 0.18192 + Blackjack.EPSILON)
           && (solvedState.getExpectedValue() > 0.18192 - Blackjack.EPSILON)) :
           "Expected value not calculated correctly for player 9-2 vs. dealer Ace:"
           + solvedState.getExpectedValue();




}

/**
 * Scratch
 *
 * This implies that enums do NOT need to be deep clones; they are more
 * like primitives. So I've changed CardValue and Card
 *
 *
 */
@Deprecated
static public void testEnumEncapsulation() {
   Card aCard = new Card(Suit.SPADES, CardValue.TWO);
   System.out.println(aCard);
   CardValue otherCV = CardValue.FIVE;
   CardValue myCV = aCard.getCardValue();
   myCV = CardValue.FOUR;
   System.out.println(aCard);
   myCV = otherCV;
   System.out.println(aCard);
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
      solvedSoft = Blackjack.solveSoftPlayersRecursive(theRules, false);
      Testers.printStrategy(solvedSoft, "", viewSecondBest);
      solvedHard = Blackjack.solveHardPlayersRecursive(theRules, false);

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

/**
 *
 * Should be tested: answerHash, getConsolidatedActions
 */
public static class AnswerTest {
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
   byte first, second, dealer;
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
               System.err.println("Answer hash error on bytes: First card = " + first
                       + ", second card = " + second + ", dealer card = " + dealer + ".");
               System.err.println("Splitting not recommended.");
               System.err.println("My answer hash is: " + Answer.answerHash(first, second, dealer, false));
               assert false;
            }
            if (splitPossible
                    && (!answerHashes.add(Answer.answerHash(first, second, dealer, true)))) {
               System.err.println("Answer hash error on bytes: First card = " + first
                       + ", second card = " + second + ", dealer card = " + dealer + ".");
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
         Answer someAnswer = new Answer(Strategy.dummyByte, CardValue.TWO,
                 CardValue.JACK, CardValue.QUEEN);
         assert false : "Answer constructor failed.";
      }
      catch (IOException ioe) {
      }


      final byte answerConsolidated = anAnswer.getConsolidatedActions();
      assert (answerConsolidated == 3);
      Answer copyOfAnswer = new Answer(answerConsolidated, CardValue.TWO,
              CardValue.JACK, CardValue.QUEEN);
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

/**
 *
 * @author Watongo
 */
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

   wrapperCheckTotalEV(0.0035484, theRules, verbosity);
   //System.out.println("House edge is " + totalEV);

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
         Blackjack.printCacheStatus();
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
      percentErrorPass = ((totalEV < expectedHouseEdge * (1 + Blackjack.FIVE_PERCENT_ERROR)) && (totalEV > expectedHouseEdge * (1 - Blackjack.FIVE_PERCENT_ERROR)));
      testPass = (percentErrorPass || ((totalEV < expectedHouseEdge + Blackjack.MAXIMUM_ABSOLUTE_ERROR) && (totalEV > expectedHouseEdge - Blackjack.MAXIMUM_ABSOLUTE_ERROR)));
   }
   else {
      percentErrorPass = ((-1 * totalEV < -1 * expectedHouseEdge * (1 + Blackjack.FIVE_PERCENT_ERROR)) && (-1 * totalEV > -1 * expectedHouseEdge * (1 - Blackjack.FIVE_PERCENT_ERROR)));
      testPass = (percentErrorPass || ((-1 * totalEV < -1 * expectedHouseEdge + Blackjack.MAXIMUM_ABSOLUTE_ERROR) && (-1 * totalEV > -1 * expectedHouseEdge - Blackjack.MAXIMUM_ABSOLUTE_ERROR)));
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
      myStrats.print();
      try {
         Thread.sleep(100);
      }
      catch (Exception e) {
      }
      assert false;
   }
   if (!percentErrorPass && verbosity) {
      System.out.println("Off by more than 5 % on this test. Split strategy table: ");
      myStrats.print();
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
   Blackjack.clearCache();
   if (verbosity) {
      System.out.println("All single deck tests passed.");
   }
   Blackjack.clearCache();


   totalEVTwoDeckHit17HoleCard(verbosity);
   if (verbosity) {
      System.out.println("All double deck tests passed.");
   }
   Blackjack.clearCache();

   totalEVFourDeckStand17NoHole(verbosity);
   totalEVFourDeckStand17HoleCard(verbosity);
   if (verbosity) {
      System.out.println("All four deck tests passed.");
   }
   Blackjack.clearCache();



   totalEVEightDeckHit17HoleCard(verbosity);
   if (verbosity) {
      System.out.println("All eight deck tests passed.");
   }
   Blackjack.clearCache();

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
