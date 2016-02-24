package ramblingharmonicas.blackjack;
import ramblingharmonicas.blackjack.calculation.Validation;
import ramblingharmonicas.blackjack.cards.*;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.StreamCorruptedException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

/**
 * This class is used as an interface to hide the strategy implementation
 * details. It is used to perform calculations, save them to disk, load them,
 * and retrieve them.
 *
 * Functions that can be overriden under different file systems:
 *
 * FileInputStream getFileStream(String fileName)
 * FileOutputStream getFileStream(String fileName)
 * protected void makeDataDirectory()
 * protected static File getStrategyFile(String filename)
 *
 *
 * Internal workings -- Whenever Strategy is
 * passed a Rule set, findAllAnswers should be called, which will update Strategy's
 * copy of the Rules and loadedRuleSet.
 *
 * TODO: Factor out all file-related code, or better yet, factor out all code that isn't
 * file-related, since most of this code is related to saving/loading/doing bulk calculations.
 */
public class Strategy {
/**
 *
 * @return true if this Strategy is loaded and ready to go (If true,
 * any calls to Strategy which don't involve a change in the Rules or Skill will
 * be fast.)
 */
public boolean isAllSolved() {
   return allSolved;
}

public StringBuilder getHardTable(final boolean displaySecondBest) {
	Skill strategyType = getStrategyType();
	final boolean hardTable = true, compDep;
	if (strategyType == Skill.COMP_DEP || strategyType == Skill.PERFECT) {
		compDep = true;
	}
	else {
		compDep = false;
	}
	CardValue firstPlayerCard, secondPlayerCard;
	State myState;
	ArrayList<ArrayList<State>> solvedStates = new ArrayList<ArrayList<State>>();
	ArrayList<State> currentStates = new ArrayList<State>() ;

	for (int i = 2; i < 11; i++) {
		  for (int j = i + 1; j < 11; j++) {
			  currentStates = new ArrayList<State>() ;
			  for (CardValue dealerCard : CardValue.twoToAce) {
				  firstPlayerCard = CardValue.cardValueFromInt(i);
				  secondPlayerCard = CardValue.cardValueFromInt(j);
				  myState = new State(firstPlayerCard, secondPlayerCard, dealerCard);
				  myState.setDealerBlackjack(false); 
				  currentStates.add(myState);
			  }
			  solvedStates.add(currentStates);
		  }
	}
	//Order by hand total
	Collections.sort(solvedStates, new Comparator<ArrayList<State>>() {
	      @Override
	      public int compare(ArrayList<State> q, ArrayList<State> p) {
	         return q.get(0).handTotal() - p.get(0).handTotal();
	      }
	});

	if (!compDep) {
		//For total-dependent strategy, remove all rows with the same hand total
		int rowHandTotal, nextRowHandTotal;
		for (int i = 0; i < solvedStates.size() - 1; i++) {
			rowHandTotal = solvedStates.get(i).get(0).handTotal();
			nextRowHandTotal = solvedStates.get(i+1).get(0).handTotal();
			if (rowHandTotal == nextRowHandTotal) {
				solvedStates.remove(i);
				i--;
			}
		}
	}

	return getTable(solvedStates, hardTable, displaySecondBest, "HARD HANDS");
}

/**
 * Displays the post-blackjack EV for split hands.
 * @param displaySecondBest Show second best action and EV
 */
public StringBuilder getSplitTable(final boolean displaySecondBest) {
   State aState;
   final boolean hardTable = false;
   ArrayList<ArrayList <State>> splitStates = new ArrayList<ArrayList<State>>();
   ArrayList<State> currentStates;
   for (CardValue playerCard: CardValue.twoToAce) {
	   currentStates = new ArrayList<State>();
	   for (CardValue dealerCard : CardValue.twoToAce) {
		   aState = new State(playerCard, playerCard, dealerCard);
		   aState.setDealerBlackjack(false);
		   currentStates.add(aState);
	   }
	   splitStates.add(currentStates);
   }
   return getTable(splitStates, hardTable, displaySecondBest, "SPLIT HANDS");
}

/**
 * Prints soft table of the given Strategy to screen.
 *
 * @param displaySecondBest Shows the second best action and EV
 */
public StringBuilder getSoftTable(final boolean displaySecondBest) {
   State aState;
   final boolean hardTable = false;
   ArrayList<ArrayList <State>> softStates = new ArrayList<ArrayList<State>>();
   ArrayList<State> currentStates;
   for (CardValue playerCard : CardValue.twoToTen) {
	   currentStates = new ArrayList<State>();
	   for (CardValue dealerCard : CardValue.twoToAce) {  //prints out best action
		   aState = new State(CardValue.ACE, playerCard, dealerCard);
		   aState.setDealerBlackjack(false);
		   currentStates.add(aState);
	   }
	   softStates.add(currentStates);
   }
   return getTable(softStates, hardTable, displaySecondBest, "SOFT HANDS");
}

/**
 * The loaded Rule Set. Used along with loadedRuleSet (the Rules key)
 */
private Rules theRules;
/**
 * What game mode is this -- which cards do the dealer and player draw?
 * Used to allow the player to practice, say, soft totals only or splits
 * only.
 *
 */
private DrawMode playerDrawMode;
/**
 * What card the dealer is allowed to draw first (for practice purposes).
 */
private DrawMode dealerDrawMode;
private double houseEdge;
public static final String filePrefix = "bj";
private final static String directory = "data";
final public static byte dummyByte = -127;
/**
 * The hashmap key of the current rules.
 *
 */
private long loadedRuleSet;
/**
 * Stores the answers of the current Rule Set. Note that this
 * stores values by individual player cards, regardless of the
 * selected Skill. Also note that the map contains double entries for hands 
 * that can be split.
 */
private Map<Integer, Answer> allAnswers;

public final static int NUMBER_ANSWERS = 650;
/**
 * Set to true when everything is solved for this rule set. For
 * perfect play, this doesn't count, since I have to recalculate at
 * every step. Set to false if I am passed a new set of rules or a
 * new strategyType.
 */
private boolean allSolved = false;

/**
 * You can only get insurance on an Ace. For any other card this
 * will throw an exception.
 *
 * TODO: Verify that this sometimes produces a weird warning, and if it does, fix it.
 *
 * @param myShoe Current shoe.
 * @param theRules Current rule set.
 * @param myState Current State.
 * @return If you should or shouldn't get insurance.
 * @exception IllegalStateException if insurance is not a legal action, like
 * if an ace is not face up.
 */
public static boolean insuranceGoodIdea(VagueShoe myShoe, Rules theRules,
        State myState) {
   if (!theRules.isPossible(Action.INSURANCE, myState)) {
      System.err.println(theRules);
      System.err.println(myState);
      assert false: "The Rules starts that insurance is impossible.";
      return false;
   }
   double probability;
   Card dealerCard = myState.getDealerUpCard();
   if (dealerCard.value() == CardValue.ACE.value()) {
      probability = myShoe.probabilityOf(CardValue.TEN);
   }
   else {
      throw new IllegalStateException("Insurance with non-ace dealer up cards unsupported.");
   }
   double getIt = probability * 1.0 + (1 - probability) * -0.5;
   if (getIt > 0) {
      myState.setInsuranceAdvised(true);
      return true;
   }

   myState.setInsuranceAdvised(false);
   return false;
}

/**
 * Strategy constructors do not load the total EV map or the Strategy from file,
 * nor do they solve a given Rules set. Other public functions may do those, as
 * necessary, as part of the task they are being asked to perform.
 *
 *
 * @param theRules The game rules
 * @param skillLevel The skill level used to judge correct strategy decisions
 * @param aGameMode The player draw mode (used to decide what cards a player
 * draws)
 */
public Strategy(Rules theRules, Skill skillLevel, DrawMode aGameMode) {
   this(theRules, skillLevel);
   this.playerDrawMode = aGameMode;
}

/**
 * This is the base constructor. All constructors should chain to this one.
 * It sets default values for several variables.
 *
 * @param theRules
 * @param skillLevel
 */
public Strategy(Rules theRules, Skill skillLevel) {
   this.loadedRuleSet = theRules.myHashKey();
   this.theRules = new Rules(theRules);
   this.strategyType = skillLevel;
   this.allSolved = false;
   this.allAnswers = new HashMap<Integer, Answer>(NUMBER_ANSWERS);
   this.houseEdge = -1000000;
   this.fullAnswers = false;
   this.forceSolve = false;
   this.totalEVMapSkill = skillLevel;
   this.playerDrawMode = DrawMode.FREE_PLAY;
   this.dealerDrawMode = DrawMode.FREE_PLAY;
   this.mapLoadDeactivated = false;
}

private boolean mapLoadDeactivated;

/**
 *
 * @param someValue True if the Strategy should ignore any precalculated
 * strategies
 * and recalculate a strategy from scratch. False to load any precalculated
 * strategies
 * and the EV map from file.
 * Untested. Primarily of use for debugging.
 */
public void setForceSolve(boolean someValue) {
   this.forceSolve = someValue;
}

/**
 * This is always calculated as if the shoe was full and as if the skill was
 * advanced.
 * It's called by print().
 * TODO: modify this so that it works with other skill levels.
 *
 * @param theRules
 * @return the house edge ( = EV * -1), or a large negative number (far more
 * negative than -10)
 * if the mapLoadDeactivated is true.
 * @throws NoRecommendationException if calculations are disabled and I'd need
 * to calculate in
 * order to find the house edge.
 * @throws IOException Caused by either a FileNotFoundException or
 * StreamCorruptedException.
 */
public double getHouseEdge() throws NoRecommendationException, IOException {
   /*
    * if (strategyType != Skill.ADVANCED)
    /*   throw new NoRecommendationException(null, theRules, "Unsupported Operation:"
    + "You can't calculate the house edge if the " +
    " skill level is not set to advanced (composition-dependent.)");
    *
    */
   findAllAnswers(theRules);
   return this.houseEdge;
}

public DrawMode getPlayerDrawMode() {
   return playerDrawMode;
}

public DrawMode getDealerDrawMode() {
   return dealerDrawMode;
}

public void setPlayerDrawMode(DrawMode myGM) {
   this.playerDrawMode = myGM;
}

public void setDealerDrawMode(DrawMode aDrawMode) {
   dealerDrawMode = aDrawMode;
}

/**
 * Returns 1 strategy's worth of raw byte data from a DIS. Returns the checksum.
 * If fileRetrieval mode is set to MANY_SMALL_ANSWERS, this checks the checksum.
 * Otherwise, it returns the summed checksum.
 * It does NOT close the DIS.
 *
 * @param fileFormat What kind of file I'm pulling this data from
 * @param data The byte array which will store the returned data
 * @param byteSource The DataInputStream which has the raw Strategy data
 * @return Null if any problem has arisen
 */
private short retrieveRawByteData(DataInputStream byteSource, byte[] data,
        final int fileFormat) throws IOException {
   short checkSum = 0;
   byte currentByte;
   int index = 0;
   try {
      while (index < NUMBER_ANSWERS) {
         currentByte = byteSource.readByte();
         data[index++] = currentByte;
         if ((fileFormat == Strategy.CONSOLIDATED_BY_DECKS_FILES)
                 || (currentByte != Strategy.dummyByte)) {
            checkSum += currentByte;
         }
      }
      if (fileFormat == Strategy.MANY_SMALL_FILES) {
         final short actualCheck = byteSource.readShort();

         if (checkSum != actualCheck) {
            System.err.println("Actual checkSum is " + actualCheck + 
                    " vs. calculated value " + checkSum);
            throw new IOException("Checksum error. Abandon ship.");
         }
         if (byteSource.read() != -1) {
            throw new IOException("Extra data detected in input stream.");
         }
      }
      return checkSum;
   }
   catch (IOException ioe) {
      Utilities.attemptClosure(byteSource);
      throw ioe;
   }
}

public boolean isMapLoadDeactivated() {
   return mapLoadDeactivated;
}

public void setMapLoadDeactivated(boolean mapLoadDeactivated) {
   this.mapLoadDeactivated = mapLoadDeactivated;
}

/**
 * This class is used solely as an argument to toggleBaseRulesBooleans.
 * Its purpose is to allow the rules toggling framework to be used for varied
 * purposes,
 * such as validating all rule sets, calculating all rule sets, consolidating
 * strategy files, etc.
 *
 */
abstract class ToggleSettings {
ToggleSettings(boolean verb) {
   verbosity = verb;
   assert !mapLoadDeactivated;
}

boolean verbosity;

abstract void insideAction(Rules someRules);

void initialSetup(Rules someRules) {
}

void halfDone() {
}

void doneWithACall(Rules someRules) {
}

/**
 * If certain rule sets should not calculated or recorded, that
 * information should be stored here. This is done primarily to
 * save disc space, but secondarily so that data which has not been
 * tested is not released. I'm not sure if this has been properly
 * implemented on the ToggleSettings subclasses.
 *
 * @param someRules
 * @return true if someRules contains rules that should not be
 * calculated or recorded.
 *
 */
boolean isDisabled(Rules someRules) {
   if (someRules.myDoubleRules.alwaysPossible()) {
      return true;
   }
   if (someRules.myDoubleRules.notSplitAces()) {
      return true;
   }
   return false;
}

abstract void allTogglesDone();

}

/**
 * The string that forms the middle part of the consolidated file name.
 * 1 -First consolidated files. These include the no-double-on-aces post-split
 * rule,
 * but include the Auto Toggles bug which incorrectly set certain doubles rules.
 * Hence,
 * certain rule sets are not included in this set.
 * 2 -Second set of consolidated files. These do not include the
 * no-double-on-aces-post-split rule
 *
 */
public static final String fileConsolidationSuffix = "-CONS1";

/** TODO: Facer out this massive inner class 
 * Used to consolidate Strategy files together to save disk space
 * (some file systems allocate space in 4KB blocks which makes it
 * inefficient to store thousands of files that are much smaller than 4 KB)
 * and reduce the number of files used.
 *
 * Each file contains:
 * two bytes, representing the number of non-duplicate strategies
 * (3 if 1, 2, 4 are unique) for hit on 17 [first byte], and the number
 * of non-duplicate strategies (3 if 1, 2, 4 are unique) for stand on 17
 * [second byte].
 * all the non-duplicate deck strategies for the hit on 17 strategies
 * all the non-duplicate deck strategies for the stand on 17 strategies
 *
 * For example, for a given rule set, the file may contain:
 * 3 3 [Strat for 1 deck hit 17] [Strat for 2 deck hit 17] [Strat for 4 deck hit
 * 17]
 * [Strat for 1 deck stand 17] [Strat for 2 deck stand 17] [Strat for 4 deck
 * stand 17]
 * assuming that the strategies for 6-deck hit-17 and 8-deck hit 17 are exactly
 * the same as for
 * 4 deck hit 17.
 *
 * To get the name of the file, call fileNameForAnswers (Doesn't matter what
 * deck number or hiton17 rules there are, it'll automatically fix that.)
 *
 * This also validates each data set upon completion.
 *
 * It does nothing if the file already exists.
 */
private class FileConsolidation extends ToggleSettings {
FileConsolidation(boolean verb) {
   super(verb);
   finishedRuleSets = new HashSet<Long>();
}

Set finishedRuleSets;

private void testEliminateDuplicateData() {
   byte[][] testData = {{(byte) 1, (byte) 2, (byte) 3},
      {(byte) 2, (byte) 3, (byte) 4},
      {(byte) 1, (byte) 2, (byte) 3},
      {(byte) 1, (byte) 2, (byte) 3}};
   testData = eliminateDuplicateData(testData);
   assert (testData.length == 3);
   assert (testData[2][1] == 2) : "testData[2][1] is " + testData[2][1];
   testData = new byte[][]{{(byte) 1, (byte) 2, (byte) 3},
      {(byte) 2, (byte) 3, (byte) 4},
      {(byte) 2, (byte) 3, (byte) 4},
      {(byte) 1, (byte) 2, (byte) 3}};

   testData = eliminateDuplicateData(testData);
   assert (testData.length == 4);
   assert (testData[3][2] == 3);
   testData = new byte[][]{{(byte) 1, (byte) 2, (byte) 3},
      {(byte) 2, (byte) 3, (byte) 4},
      {(byte) 2, (byte) 3, (byte) 4},
      {(byte) 2, (byte) 3, (byte) 4}};
   testData = eliminateDuplicateData(testData);
   assert (testData.length == 2);
   assert (testData[1][1] == 3);
}

/**
 * Used to test eliminateDuplicateData
 *
 * @param someRules
 */
@Override
void initialSetup(Rules someRules) {
   testEliminateDuplicateData();


}

@Override
void insideAction(Rules someRules) {
   boolean viewPostConversion = false;
   Rules theRulesAfterToggles = new Rules(someRules);
   theRulesAfterToggles.doAutoToggles();
   if (isDisabled(theRulesAfterToggles)) {
      return;
   }
   theRulesAfterToggles.setNumberDecks(1);
   theRulesAfterToggles.setHitOn17(true);

   theRulesAfterToggles.setBlackjackPayback(1.5D);
   /**
    * Strategy files are only stored if the payback is 1.5.
    * All decks are being consolidated to 1 file.
    * I may need this so that the finishedRuleSets Set
    * works properly
    */
   if (!finishedRuleSets.add(theRulesAfterToggles.myHashKey())) {
      return; //Return if I've already solved for this; otherwise add it.
   }
   final boolean fullAnswers = false;
   if (strategyFileExists(fullAnswers, theRulesAfterToggles, strategyType,
           Strategy.CONSOLIDATED_BY_DECKS_FILES)) {
      return;
   }

   byte[][] hit17Data = getByteData(theRulesAfterToggles);

   theRulesAfterToggles.setHitOn17(false);
   byte[][] stand17Data = getByteData(theRulesAfterToggles);

   writeConsolidatedFiles(hit17Data, stand17Data, theRulesAfterToggles);

   Strategy thisStrat = new Strategy(theRulesAfterToggles, strategyType);
   thisStrat.setFileFormat(Strategy.CONSOLIDATED_BY_DECKS_FILES);
   try {
      Validation.validateSolvedStrategy(thisStrat);
      //Validate this rule set right now.
   }
   catch (NoRecommendationException nre) {
      throw new RuntimeException(nre);
   }
   catch (IOException ioe) {
      throw new RuntimeException(ioe);
   }

}

/**
 * Writes consolidated data to a file. Throws RuntimeExceptions upon
 * failure.
 *
 * @param hit17Data
 * @param stand17Data
 * @param theRules
 *
 */
private void writeConsolidatedFiles(byte[][] hit17Data, byte[][] stand17Data,
        Rules theRules) {

   final boolean fullAnswers = false;
   final int fileFormat = Strategy.CONSOLIDATED_BY_DECKS_FILES;
   final String filename = fileNameForAnswers(fullAnswers, theRules,
           strategyType, fileFormat);

   File aFile = new File(directory);
   aFile.mkdir();

   OutputStream fileOut = null;

   try {
      fileOut = getFileOutputStream(filename);

      writeOneConsolidatedFile(fileOut, hit17Data, stand17Data);

      fileOut.close();
   }
   catch (FileNotFoundException exceptional) {

      exceptional.printStackTrace();
      System.err.println("File " + directory + "/" + filename + " is a directory rather than "
              + "a regular file,\n"
              + " does not exist but cannot be created, or cannot be opened for some other reason.");
//This is only thrown by the FileOutputStream constructor, so no need to close objectstream.
      Utilities.attemptClosure(fileOut);
      throw new RuntimeException(exceptional);
   }
   catch (IOException closingFailure) {
      Utilities.attemptClosure(fileOut);
      throw new RuntimeException(closingFailure);
   }


}

/**
 * Creates one consolidated file.
 *
 * @param fileOut
 * @param hit17Data
 * @param stand17Data
 * @throws IOException
 */
private void writeOneConsolidatedFile(OutputStream fileOut, byte[][] hit17Data,
        byte[][] stand17Data) throws IOException {
   byte toWrite;
   DataOutputStream outStream = null;
   int checkSum = 0;
   try {
      outStream = new DataOutputStream(fileOut);
      checkSum += toWrite = (byte) hit17Data.length;
      outStream.writeByte(toWrite);

      checkSum += toWrite = (byte) stand17Data.length;
      outStream.writeByte(toWrite);

      for (int i = 0; i < hit17Data.length; i++) {
         for (int j = 0; j < hit17Data[i].length; j++) {
            checkSum += toWrite = hit17Data[i][j];
            outStream.writeByte(toWrite);
         }
      }

      for (int i = 0; i < stand17Data.length; i++) {
         for (int j = 0; j < stand17Data[i].length; j++) {
            checkSum += toWrite = stand17Data[i][j];
            outStream.writeByte(toWrite);
         }
      }

      outStream.writeInt(checkSum);

      outStream.close();
   }
   catch (IOException io) {
      Utilities.attemptClosure(outStream);
      throw io;
   }
}

/**
 * Retrieves, in the form of a 2-D byte array, the strategies
 * for this rule set, varying only the number of decks.
 *
 * @param someRules
 * @return
 *
 */
private byte[][] getByteData(Rules someRules) {
   final int origDeckNumber = someRules.getNumberOfDecks();
   byte[][] rawStrategies = new byte[solvedNumberOfDecks.length][];

   for (int i = 0; i < solvedNumberOfDecks.length; i++) {
      someRules.setNumberDecks(solvedNumberOfDecks[i]);
      rawStrategies[i] = pullByteData(someRules);
   }


   rawStrategies = eliminateDuplicateData(rawStrategies);

   someRules.setNumberDecks(origDeckNumber);
   return rawStrategies;
}

private byte[] pullByteData(Rules someRules) {
   InputStream strategyData = null;
   DataInputStream byteSource = null;
   byte[] rawStrat = new byte[NUMBER_ANSWERS];
   try {
      strategyData = getFileInputStream(fileNameForAnswers(fullAnswers, someRules, strategyType, Strategy.MANY_SMALL_FILES));

      byteSource = new DataInputStream(strategyData);
      retrieveRawByteData(byteSource, rawStrat, Strategy.MANY_SMALL_FILES);
      if (byteSource.read() != -1) {
         throw new IOException("Too much data retrieved from DataInputStream.");
      }
      strategyData.close();
      return rawStrat;
   }
   catch (IOException ioe) {
      Utilities.attemptClosure(strategyData);
      System.err.println("Working on this rule set: " + someRules);
      throw new RuntimeException(ioe);
   }

}

/**
 * Untested
 *
 * @param rawStrategies
 * @return
 */
private byte[][] eliminateDuplicateData(byte[][] rawStrategies) {
   int numDuplicates = 0;
   int i;
   for (i = rawStrategies.length - 1; i > 1; i--) {
      if (Arrays.equals(rawStrategies[i], rawStrategies[i - 1])) {
         numDuplicates++;
      }
      else {
         break; //Only find consecutive duplicates from the back.
      }
   }
   byte[][] returnedBytes = new byte[(rawStrategies.length - numDuplicates)][];
   for (i = 0; i < (rawStrategies.length - numDuplicates); i++) {
      returnedBytes[i] = rawStrategies[i];
   }
   return returnedBytes;
//https://stackoverflow.com/questions/16362872/how-to-get-2d-subarray-from-2d-array-in-java

}

@Override
void allTogglesDone() {
   if (verbosity) {
      System.out.println("A round of file consolidation has been completed.");
   }
}
}
/**
 * This class loads and validates all rule sets. The test fails if a rule set
 * has not been calculated or if it fails validations.
 * It does not currently validate the totalEVMap, only the strategy files.
 * Furthermore, it only validates the files when in their original form.
 */
class ValidateCalculations extends ToggleSettings {
final private int fileFormat;
private int numRuleSetsValidated;
private long numDuplicateRuleSets;

/**
 *
 * @param verb Verbosity
 * @param fileFormat one of Strategy.MANY_SMALL_FILES or
 * Strategy.CONSOLIDATED_BY_DECKS_FILES
 *
 */
ValidateCalculations(boolean verb, final int fileFormat) {
   super(verb);
   this.fileFormat = fileFormat;
   numRuleSetsValidated = 0;
   rulesKeys = new ArrayList<Long>(18000);
   numDuplicateRuleSets = 0;
}

@Override
void initialSetup(Rules someRules) {
   /*
    try {
    loadTotalEVMap(false); //haven't tried it with this arg.
    }
    catch (IOException ioe)
    {   throw new RuntimeException(ioe);
    }
    */
}

/**
 * If I've already loaded a rule set, I don't need to load it again.
 *
 */
private List<Long> rulesKeys;

@Override
void insideAction(Rules someRules) {
   Rules theRulesAfterToggles = new Rules(someRules);
   theRulesAfterToggles.doAutoToggles();
   if (isDisabled(theRulesAfterToggles)) {
      return;
   }
   if (rulesKeys.contains(theRulesAfterToggles.myHashKey())) {
      numDuplicateRuleSets++;
      return;
   }
   //I assume that the Long .equals will behave correctly

   if (!strategyFileExists(fullAnswers, theRulesAfterToggles, strategyType, fileFormat)) {
      System.err.println("Validation failed. No file exists for these rules, with strategy type "
              + strategyType.toString() + ": " + theRulesAfterToggles);
      assert false;
   }

   try {
      solve(theRulesAfterToggles);
      //System.out.println("I've solved for this strategy: " + theRulesAfterToggles);
      Validation.validateSolvedStrategy(Strategy.this); //Weird, does this work?
      numRuleSetsValidated++;
      //if ( ((numRuleSetsValidated % 5000) == 0) && (verbosity) )
      //   System.out.println(numRuleSetsValidated + " rule sets have been validated.");
      rulesKeys.add(theRulesAfterToggles.myHashKey());
   }
   catch (IOException ioe) {
      throw new RuntimeException(ioe);
   }
   catch (NoRecommendationException nre) {
      throw new RuntimeException(nre);
   }

}

@Override
void allTogglesDone() {
   if (verbosity) {
      System.out.println("This validation set is complete; a total of "
              + numRuleSetsValidated + " rule sets have been validated. A total of"
              + numDuplicateRuleSets + " rule sets were duplicates of previously loaded"
              + " rule sets.");
   }
}

}

/**
 *
 * This tests the uniqueness of the Rules keys.
 */
void testRulesKeys(boolean verbosity) {
   ToggleSettings rulesTest = new TestRulesKeys(verbosity);

   int[] deckArray = {1, 2, 4, 6, 8};
   for (int i = 0; i < deckArray.length; i++) {
      toggleBaseRulesBooleans(deckArray[i], rulesTest);
   }

}

class TestRulesKeys extends ToggleSettings {
private long numberOfAddedKeys = 0;
private Set keysForAllRules = new TreeSet();

TestRulesKeys(boolean verb) {
   super(verb);
}

@Override
void insideAction(Rules someRules) {
   /*
    if (isDisabled(someRules))
    return;
    If I disable certain rule sets, it has the inadvertent
    side effect of sending that same Rule set to this function
    * multiple times.
    * */
   assert (!someRules.getAutoToggles()) : "This test requires auto toggles to be disabled.";
   keysForAllRules.add(someRules.myHashKey());
   numberOfAddedKeys++;
   final long testHash = -1;
   if (someRules.myHashKey() == testHash) {
      System.err.println("On iteration " + numberOfAddedKeys + ", I get this rule set: " + someRules);
   }

   if (keysForAllRules.size() != numberOfAddedKeys) {
      System.err.println("Rules hash error. Rule set:" + someRules);
      System.err.println(numberOfAddedKeys + " = numberOfAddedKeys.");
      assert false;
   }
}

@Override
void allTogglesDone() {
   if (verbosity) {
      System.out.println(keysForAllRules.size() + " number of unique rules keys created.");

   }
}

}

/**
 * This function is currently unused. It's a diagnostic function
 * used to tell how many rule sets will be solved for and how many
 * duplicates there are.
 */
public void testToggles() {
   final boolean verbosity = true;
   ToggleSettings rulesTest = new TestToggles(verbosity);

   int[] deckArray = {1, 2, 4, 6, 8};
   for (int i = 0; i < deckArray.length; i++) {
      toggleBaseRulesBooleans(deckArray[i], rulesTest);
   }

}

class TestToggles extends ToggleSettings {
TestToggles(boolean verb) {
   super(verb);
}

@Override
void insideAction(Rules someRules) {

   Rules theRulesAfterToggles = new Rules(someRules);
   theRulesAfterToggles.doAutoToggles();
   if (isDisabled(theRulesAfterToggles)) {
      return;
   }
   Testers.StrategyTest.testToggles(theRulesAfterToggles);
}

@Override
void allTogglesDone() {
   Testers.StrategyTest.printTestToggleReport();
}

}

/**
 * Used to do all the calculations
 *
 */
class RunCalculations extends ToggleSettings {
private void printStatus(Rules theRulesAfterToggles) {
   long current = System.currentTimeMillis();
   long elapsed = (current - initialTime) / 60000;
   if (elapsed < 4) {
      return;
   }

   if ((wrapperSolveEntries % 500) == 0) {
      System.out.println("-----------------------------------");
      System.out.println(elapsed + " minutes elapsed; currently working on this rule set:");
      System.out.println(theRulesAfterToggles.toString());
      System.out.println("There are currently " + totalEVMap.size() + " entries in the total EV map.");
   }

   if ((numberSolves % 500) == 0) {
      System.out.println(numberSolves + " strategies solved." + "wrapperSolveAndStore entered "
              + wrapperSolveEntries + " times. " + numberSavedFiles + " files saved to disk.");
      System.out.println(elapsed + " minutes elapsed.");
      DealerCache.printCacheStatus();
      System.out.println("------------------------------");
      if (elapsed > 300) { //
         saveTotalEVMap();
         System.out.println("5 hours have elapsed on one run. I need a break.");
         System.out.println("This is my final rule set: " + theRulesAfterToggles);
         System.exit(0);
      }
   }
}

private int numberSolves = 0;
private long initialTime;
private int wrapperSolveEntries = 0;
private int numberSavedFiles = 0;
final boolean fullAnswers = false;
final boolean actingSolo = false;

RunCalculations(boolean verb) {
   super(verb);
}

@Override
void halfDone() {
   if (verbosity) {
      System.out.println("Halfway done.");
   }
}

@Override
void initialSetup(Rules someRules) {
   initialTime = System.currentTimeMillis();
}

/**
 * This function:
 * Assumes that everything in the total EV Map is solved at skill level
 * advanced.
 * Assumes that the total EV map has been loaded.
 * Stores the solved strategy if the blackjack payback is 1.5.
 * Stores the house edge if the skill level is advanced
 * Otherwise, it returns.
 *
 * Stores the house edge in the map.
 * Attempts to save the given rule set in a file.
 * Exits immediately upon failure.
 *
 * @param theRules
 */
@Override
void insideAction(Rules someRules) {

   wrapperSolveEntries++;
   final long ruleKeyToTest = -1;
   final boolean testActivated;

   if (ruleKeyToTest == someRules.myHashKey()) {
      testActivated = true;
   }
   else {
      testActivated = false;
   }
   if (testActivated) {
      System.out.println("RunCalc.insideAction: Rule set " + ruleKeyToTest + " found: " + someRules);
   }

   Rules theRulesAfterToggles = new Rules(someRules);
   theRulesAfterToggles.doAutoToggles();
   if (isDisabled(theRulesAfterToggles)) {
      return;
   }
//If I do the auto toggles any earlier than this, they'll hide certain rule sets
//For example, once you set a surrender to being true, it instantly sets all others
//to false, and you can't reactivate the original surrender to true instantly.

   if (testActivated) {
      System.out.println("Rule set after toggles: " + theRulesAfterToggles);
   }

   /*The Rules key does not take into account whether or not the strategy solved for
    * is basic or advanced.

    If the EV map already has the key, then I've already solved for it, but it
    didn't need to be stored in a file. Why not?
    Because it's BJ payback was not 1.5.
    */

   boolean success;
   try {
	   
      final boolean fileExists = strategyFileExists(fullAnswers, theRulesAfterToggles, strategyType,
              Strategy.MANY_SMALL_FILES);

      if (testActivated && fileExists) {
         System.out.println("Rule file exists for this rule set.");
      }

      if (fileExists && totalEVMap.containsKey(theRulesAfterToggles.myHashKey())) {
         return;
      }

      if (testActivated && fileExists) {
         System.out.println("However, " + theRulesAfterToggles.myHashKey() + " was not found in the map.");
      }

      printStatus(theRulesAfterToggles);

      if ((theRulesAfterToggles.getBlackJackPayback() < 1.52)
              && (theRulesAfterToggles.getBlackJackPayback() > 1.48)) {  //Store it if it pays 3:2
         success = solveAndStore(theRulesAfterToggles, actingSolo); //this calls testSolvedStrategy
         if (!success) {
            System.err.println("I did not solve and store successfully. Throwing exception.");
            System.err.println("Here is the failed Rule set: " + theRulesAfterToggles.toString());
            System.err.println("Here is my strategy currently: ");
            print(true);
            saveTotalEVMap();
            throw new RuntimeException();
         }
         numberSavedFiles++;
      }
      else //Otherwise, load and solve, but don't store.
      //This will put the value in the hashmap, which will be stored after everything is done.
      {
         solve(theRulesAfterToggles);
         if (strategyType == Skill.COMP_DEP) {
            totalEVMap.put(loadedRuleSet, (float) houseEdge);
         }
         //Don't store the house edge for Skill.BASIC because it hasn't been calculated correctly yet.

         //Solve and store stores both the strategy and the house edge.
         //In this case, I only need to store the house edge.

      }
      numberSolves++;
      printStatus(theRulesAfterToggles);

   }
   catch (NoRecommendationException problem) {
      saveTotalEVMap();
      throw new RuntimeException(problem);
   }
   catch (IOException problem) {
      saveTotalEVMap();
      throw new RuntimeException(problem);
   }

}

@Override
void allTogglesDone() {
   saveTotalEVMap();
   System.out.println("This run has been completed.");
}

}

public enum Skill {
VERY_EASY(0), SIMPLE(10),
/**
 * Basic total-dependent strategy, independent of the Shoe
 * contents.
 */
TOTAL_DEP(20),
/**
 * Basic composition-dependent strategy, independent of the Shoe
 * contents.
 */
COMP_DEP(30),
/**
 * Perfect strategy, dependent on the Shoe contents and the
 * cards in your hand.
 */
PERFECT(40);

private int mySkill; 

/**
 * @return null if val doesn't correspond to a valid skill level
 */
static public Skill getSkill(final int val) {
   for (Skill s : values()) {
      if (s.value() == val) {
         return s;
      }
   }
   return null;
}

public int value() {
   return mySkill;
}

Skill(int level) {
   this.mySkill = level;
}

/**
 * This is used to make the file names of strategy and total EV files.
 * Changing it will change what files Strategy saves and tried to load.
 *
 * @return
 */
char abbrev() {
   switch (mySkill) {
      case 0:
         return 'v';
      case 10:
         return 's';
      case 20:
         return 'b';
      case 30:
         return 'a';
      case 40:
         return 'p';
      default:
         return 'x';

   }


}

}

private Skill strategyType;

public Skill getStrategyType() {
   return strategyType;
}

public void setStrategyType(Skill strategyType) {
   this.strategyType = strategyType;
   allSolved = false;
}

public void solve() throws NoRecommendationException, IOException {
   solve(theRules);
}   
/**
 * UNTESTED
 * This function should load the Rules into the strategy.
 * It's a wrapper function for findAllAnswers.
 *
 * @param someRules The rule set to load into this strategy
 * @throws NoRecommendationException An internal error
 * @throws IOException An error while loading data from a file
 *
 */
public void solve(Rules someRules) throws NoRecommendationException, IOException {
   try {
      findAllAnswers(someRules);
   }
   catch (FileNotFoundException fnf) {
      throw new IOException(Utilities.stackTraceToString(fnf));
   }
   catch (StreamCorruptedException sce) {
      throw new IOException(Utilities.stackTraceToString(sce));
   }

}

/**
 * This is an internal function used every time a new Rule set is passed; it
 * loads the correct strategy or calculates it.
 *
 * This is the keystone function, call it early and often.
 *
 * @param theRules
 * @throws NoRecommendationException
 *
 */
private void findAllAnswers(Rules someRules) throws NoRecommendationException,
        FileNotFoundException, StreamCorruptedException, IOException {
   if (strategyType == Skill.PERFECT) {
      throw new IllegalStateException("Perfect strategy can't be solved in the general sense.");
   }
   //Load current rule set.
   if (someRules.myHashKey() != loadedRuleSet) {
      allSolved = false;
      allAnswers.clear();
      loadedRuleSet = someRules.myHashKey();
      this.theRules = new Rules(someRules);
   }
   if (!allSolved) {
      if ((strategyType == Skill.SIMPLE) || (strategyType == Skill.VERY_EASY)) {
         loadAnEasyStrategy();
      }
      else {
         if (!mapLoadDeactivated && !forceSolve) {
            loadTotalEVMap(false); //This call does nothing if map has already been loaded
         }
         final boolean isPrecal = isPrecalculated();
         if (isPrecal) {
            loadAnswersFromFile();
         }
         else {
            if (calculationDeactivated) {
               System.err.println("Calculations are currently deactivated.");
               System.err.println("isPrecalculated returns: " + isPrecal);
               System.err.println("The file name I was looking for is: " + fileNameForAnswers());
               throw new NoRecommendationException(null, someRules, "I have to calculate "
                       + "this rule set.");

            }
            calculateBasicStrategy();
         }
      }
   }
   allSolved = true;
}

/**
 * Convenience function for findBestEV(rules, State) which goes back to
 * findBestAnswer
 *
 *
 * @param myState
 * @return
 * @throws NoRecommendationException
 * @throws IOException
 * @throws ClassNotFoundException
 * @throws IllegalStateException
 */
public double findBestEV(State myState) throws NoRecommendationException, IOException,
        IllegalStateException {
   return findBestEV(theRules, myState);
}

/**
 * As long as the top recommended action is possible, this gives the EV for that
 * answer.
 * However, if it's not possible, there'll be a silent error here, because this
 * always
 * gives the EV for the best action possible, regardless of whether or not it's
 * possible.
 * Note that findBestAction should always return a possible action, so there
 * might be
 * a discrepancy between this EV and the EV of the suggested action. However, if
 * I fix that,
 * then I can't view the correct EV s for when the dealer has an Ace up and has
 * not
 * checked for blackjack, because a late surrender is ruled impossible in that
 * case, but
 * I still want to be able to see the EV for that situation.
 *
 * Encapsulates findBestAnswer. Do NOT use with Perfect accuracy
 * because it doesn't take the shoe into account.
 *
 * @param theRules
 * @param myState
 * @return
 * @throws NoRecommendationException
 * @throws IOException
 * @throws ClassNotFoundException
 * @throws IllegalStateException
 */
public double findBestEV(Rules theRules, State myState)
        throws NoRecommendationException, IOException, IllegalStateException {

   Shoe aShoe = new Shoe(theRules.getNumberOfDecks());
   Answer myAnswer = findBestAnswer(aShoe, theRules, myState);
   if (!myAnswer.isComplete()) {
      throw new NoRecommendationException(myState, theRules, "Can't call findBestEV on"
              + "an incomplete Answer.");
   }
   return myAnswer.getBestEV();
   /* I'm not using this code because it means that there might be a discrepancy
    * between the chosen action and the best EV in the case of a dealer ace up card.
    * Basically
    if (theRules.isPossible(myAnswer.getBestAction(), myState))
    return myAnswer.getBestEV();
    else if (theRules.isPossible(myAnswer.getSecondBestAction(), myState))
    return myAnswer.getSecondBestEV();
    else throw new NoRecommendationException("Neither recommended action is possible.");
    */

}

/**
 * Encapsulates findBestAnswer.
 * Do NOT use with Perfect accuracy
 * because it doesn't take the shoe into account.
 *
 * @param theRules
 * @param myState
 * @return
 * @throws NoRecommendationException
 * @throws IOException
 * @throws ClassNotFoundException
 * @throws IllegalStateException
 */
public double findSecondBestEV(Rules theRules, State myState)
        throws NoRecommendationException, IOException, IllegalStateException {
   Shoe aShoe = new Shoe(theRules.getNumberOfDecks());
   Answer myAnswer = findBestAnswer(aShoe, theRules, myState);
   if (!myAnswer.isComplete()) {
      throw new NoRecommendationException(myState, theRules, "Can't call findSecondBestEV on"
              + "an incomplete Answer.");
   }
   return myAnswer.getSecondBestEV();
}

public double findSecondBestEV(State myState)
        throws NoRecommendationException, IOException, IllegalStateException {
   return findSecondBestEV(theRules, myState);

}

public Action findSecondBestAction(State myState)
        throws NoRecommendationException, IOException, IllegalStateException {
   return findSecondBestAction(theRules, myState);


}

/**
 * Wrapper function for findBestAnswer
 *
 * @param theRules
 * @param myState
 * @return
 * @throws NoRecommendationException
 * @throws IOException
 * @throws ClassNotFoundException
 * @throws IllegalStateException
 */
public Action findSecondBestAction(Rules theRules, State myState)
        throws NoRecommendationException, IOException, IllegalStateException {
   Shoe aShoe = new Shoe(theRules.getNumberOfDecks());
   Answer myAnswer = findBestAnswer(aShoe, theRules, myState);
   //System.out.println("Strategy.findSecondBestAction: Here is the Answer I have: ");
   //System.out.println(myAnswer);
   //if (true) throw new RuntimeException();
   if (Blackjack.debug()) {
      Action recommendedBest = myAnswer.getBestAction();
      if ((myAnswer.getSecondBestAction() == recommendedBest)
              && (!myState.playerBJ()) && theRules.isPossible(recommendedBest, myState)) {
         System.err.println(theRules.toString() + myState.toString() + myAnswer.toString());
         throw new NoRecommendationException(myState, theRules, "Second best action is the same as first best action.");
      }
      /*  I'm removing this error check. The reason is that
       * during late surrender, you can't surrender until the dealer has checked
       * for blackjack. So surrender isn't possible. It's only after the dealer checks
       * that this becomes possible. I'm going to leave out that logic since it would
       * be solely for the sake of this test.
       *
       recommendedBest =this.findBestAction(theRules, myState);
       if (  (recommendedBest == myAnswer.getSecondBestAction() )
       &&
       (!myState.playerBJ() ) && (theRules.isPossible(recommendedBest, myState))
       )
       {
       System.err.println(theRules.toString() + myState.toString() + myAnswer.toString());
       System.err.println("this.findBestAction said that I should: " + this.findBestAction(theRules, myState) );
       throw new NoRecommendationException(myState, theRules, "Second best action is the same as first best action.");
       }
       */
   }
   return myAnswer.getSecondBestAction();
}

/**
 *
 * Not tested.
 *
 *
 * @param myShoe The Shoe is only used if the skill is set to
 * Perfect. All computation is done anew for every Perfect hand, so
 * no hashes are used.
 * @param theRules The current rule set.
 * @param myState The current state.
 * @return The answer.
 *
 *
 * @throws NoRecommendationException for Perfect calculations
 * on split hands. Insurance is not covered
 * by this. Simple and very easy strategy are NOT YET IMPLEMENTED.
 * @throw NoRecommendationException if the actions I want to recommend
 * are all impossible or there's an error of some kind.
 * @throw IOException, ClassNotFoundException for its I/O functions
 *
 */
Answer findBestAnswer(Shoe myShoe, Rules someRules, State myState)
        throws NoRecommendationException, IOException {

   if ((someRules.numPossibleActions(myState, false) < 2) && (!myState.playerBJ())) {
      throw new NoRecommendationException(myState, someRules, 
    		  "findBestAnswer was called when there is only only one possible action.");
   }

   if (strategyType != Skill.PERFECT) {
      findAllAnswers(someRules);
      return retrieveAnswerAdvOrBasic(myState);
   }

   if (someRules.isPossible(Action.SPLIT, myState)) {
      throw new NoRecommendationException("No perfect solve for split states");    
   }
   State solvedState = Blackjack.PlayerRecursive(new FastShoe(myShoe), myState, someRules);
   return new Answer(solvedState);
}

//Hmm. Should I load this into memory?
private void loadAnEasyStrategy() throws NoRecommendationException {
   ArrayList<Card> myHand = new ArrayList<Card>(2);
   CardValue secondPlayer;
   for (CardValue firstPlayer : CardValue.oneToTen) {
      myHand.add(new Card(Suit.CLUBS, firstPlayer));
      for (int i = firstPlayer.value(); i < 11; i++) {
         secondPlayer = CardValue.cardValueFromInt(i);
         myHand.add(new Card(Suit.CLUBS, secondPlayer));
         for (CardValue dealerCard : CardValue.oneToTen) {
            if (strategyType == Skill.VERY_EASY) {
               loadVeryEasy(Utilities.handTotal(myHand), dealerCard, firstPlayer, secondPlayer);
            }
            else if (strategyType == Skill.SIMPLE) {
               loadSimple(Utilities.handTotal(myHand), dealerCard, firstPlayer, secondPlayer);
            }
            else {
               throw new NoRecommendationException("Illegal State: Strategy type wrong in loadAnEasyStrategy");
            }
         }
         myHand.remove(1);
      }
      myHand.clear();
   }
}

private void loadSimple(final int handTotal, CardValue dealerCard,
        CardValue firstPlayerCard,
        CardValue secondPlayerCard) {
   throw new UnsupportedOperationException();
}

/**
 * Helper function for loadAnEasyStrategy
 * Rules theRules currently not used. Eventually change it based on dealer hole
 * card. (can use loadedRules)
 */
private void loadVeryEasy(final int handTotal, CardValue dealerCard,
        CardValue firstPlayer,
        CardValue secondPlayer) throws NoRecommendationException {
   Action bestAction = null, secondBestAction = null;
   Answer solvedAnswer;
   final boolean isSoft, splitPossible;
   if ((firstPlayer == CardValue.ACE) || (secondPlayer == CardValue.ACE)) {
      isSoft = true;
   }
   else {
      isSoft = false;
   }
   if (firstPlayer.value() == secondPlayer.value()) {
      splitPossible = true;
   }
   else {
      splitPossible = false;
   }



   if ((splitPossible) && ((firstPlayer == CardValue.ACE) || (firstPlayer == CardValue.EIGHT))) { //Only recommend splits on 8 and A. This is supposed to be butt easy.
      bestAction = Action.SPLIT;
      if (CardValue.EIGHT == firstPlayer) {
         if ((dealerCard.value() >= 2) && (dealerCard.value() <= 6)) {
            secondBestAction = Action.STAND; //stand on dealer 2-6 with a 16.
         }
         else {
            secondBestAction = Action.HIT; // hit on dealer 7-A with a 16.
         }
      }
      else {
         secondBestAction = Action.HIT; //hit with two aces
      }
   }
   else if (handTotal > 16) {
      bestAction = Action.STAND; //Off if you're soft. Way off on soft 17.
      if (handTotal == 21) {
         secondBestAction = Action.STAND;
      }
      else if (splitPossible) {
         secondBestAction = Action.SPLIT;
      }
      else if (isSoft) {
         secondBestAction = Action.DOUBLE;
      }
      else {
         secondBestAction = Action.SURRENDER;
      }

   }
   else if (handTotal < 10) {
      bestAction = Action.HIT;
      secondBestAction = Action.STAND; // surrender for 7-A

      //GO BAKC HERE
   }
   else if ((handTotal == 11) || (handTotal == 10)) {
      bestAction = Action.DOUBLE; //On 10 only double on 2-9 actually.
      secondBestAction = Action.HIT;
   }
   //Keep else-iffing. 12-16 inclusive are left
   else {
      if (!isSoft) {
         if ((dealerCard == CardValue.ACE) || (dealerCard.value() >= 7)) { 
        	 //Hard 12-16 vs. dealer 7-A
            bestAction = Action.HIT;
            secondBestAction = Action.SURRENDER;
         }
         else //Hard 12-16 vs. dealer 2-6
         {
            bestAction = Action.STAND;
            secondBestAction = Action.HIT;
         }
      }
      else {
         assert (isSoft);
         assert (handTotal >= 12);
         assert (handTotal <= 16);
         //Let's make it easy.
         bestAction = Action.HIT;
         secondBestAction = Action.DOUBLE;
      }
   }

   try {
      //at last, do all this

      if ((bestAction == secondBestAction) && (handTotal != 21)) {
         System.err.println("Strategy.loadVeryEasy: I recommend the same action, viz., "
      + bestAction + " for cards " + firstPlayer + " and " + secondPlayer +
      " vs. a dealer " + dealerCard);
         throw new IllegalStateException();
      }
      solvedAnswer = new Answer(bestAction, secondBestAction, firstPlayer, secondPlayer, dealerCard);
      //The new Answer constructor can now call the static State functions

      allAnswers.put(solvedAnswer.myHashKey(), solvedAnswer);

      boolean splitFlag = false;

      //OK. For split hands I have to make a dummy answer so that there is always a hard answer in the map.
      //Otherwise my TwoAnswersInMap function will freak. Heck, maybe I should have changed that one instead.
      //These values should never actually be used because the split answers always take precedence.
      if (bestAction == Action.SPLIT) // 8 and A.
      {
         splitFlag = true;
         if (secondBestAction != Action.HIT) {
            bestAction = Action.HIT; //Making this up.
         }
         else {
            bestAction = Action.STAND;
         }
      }
      if (secondBestAction == Action.SPLIT) {
         splitFlag = true;
         if (bestAction != Action.STAND) {
            secondBestAction = Action.STAND; //Making this up.
         }
         else {
            secondBestAction = Action.HIT;
         }
      }
      if (splitFlag) {
         Answer dummyAnswer = new Answer(bestAction, secondBestAction, firstPlayer, secondPlayer, dealerCard);
         allAnswers.put(dummyAnswer.myHashKey(), dummyAnswer);
      }

   }
   catch (IOException io) {
      NoRecommendationException nor = new NoRecommendationException(io);
      nor.setRules(theRules);
      throw nor;
   }
}

/**
 * Convenience function to get to findBestAnswer
 *
 * @param theRules
 * @param myState
 * @return
 * @throws NoRecommendationException
 * @throws IOException
 * @throws ClassNotFoundException
 *
 */
public Action findBestAction(Rules theRules,
        State myState) throws NoRecommendationException, IOException {
   if (strategyType == Skill.PERFECT) {
      throw new NoRecommendationException("Convenience function findbestAction(Rules,State) should"
              + "not be used with perfect skill, since that means you take the current shoe into account.");
   }

   Shoe aShoe = new Shoe(theRules.getNumberOfDecks());
   return findBestAction(aShoe, theRules, myState);
}

/**
 * Convenience function to get to findBestAnswer
 *
 * @param myState
 * @return
 * @throws NoRecommendationException
 * @throws IOException
 * @throws ClassNotFoundException
 *
 */
public Action findBestAction(State myState) throws NoRecommendationException, IOException {
   return findBestAction(theRules, myState);

}

/**
 * Not tested.
 *
 * @param myShoe The Shoe is only used if the skill is set to
 * Perfect. All computation is done anew for every Perfect hand, so
 * no hashes are used.
 * @param theRules The current rule set.
 * @param myState The current state.
 * @return The best action. Null if there are
 *
 * @throws IllegalStateException if there are less than 2 possible
 * actions
 * @throws UnsupportedOperationException for Perfect calculations
 * on split hands. Insurance is not covered
 * by this. Simple and very easy strategy are NOT YET IMPLEMENTED.
 * @throw NoRecommendationException if the actions I want to recommend
 * are all impossible or there's an error of some kind.
 *
 */
public Action findBestAction(Shoe myShoe, Rules theRules,
        State myState) throws NoRecommendationException, IOException {
   Answer theAnswer = findBestAnswer(myShoe, theRules, myState);
   //OK. Do I have the weird surrender/blackjack issue here?
   if (theRules.isPossible(theAnswer.getBestAction(), myState)) {
      return theAnswer.getBestAction();
   }
   else if (theRules.isPossible(theAnswer.getSecondBestAction(),
           myState)) {
      return theAnswer.getSecondBestAction();
   }
   throw new NoRecommendationException();
}

/**
 * Returns false if this.forceSolve is true.
 * Returns true if the answer for this rule
 * set has already been calculated in a file. If Strategy.fullAnswers is false,
 * this will first convert the blackjack payback to 1.5 before looking for the
 * file,
 * since for small files I am not storing rule sets that vary only in BJ payback
 * amount
 * in different files. (The strategy does not change at all when the BJ payback
 * changes.)
 *
 * If the totalEVMap has already been loaded,
 * then this checks to see if the current rule set is in the map. If not, it
 * will return false, even if
 * the file exists. If mapLoadDeactivated is true, it does not check the total
 * EV map at all, just
 * whether or not the file exists.
 * Loading and validating can be done by the next function??
 *
 * @param theRules
 * @return In general, true if a file of the right name exists and false
 * otherwise. But for special cases
 * see the Javadocs or the code.
 */
private boolean isPrecalculated() {
   if (forceSolve) {
      System.out.println("Force solve is on -- all calculations will be done fresh without using stored data.");
      return false;
   }

   if (!mapLoadDeactivated) {
      if (totalEVMap == null) {
         System.err.println("isPrecalculated: Total EV Map not loaded yet.");
      }

      if (totalEVMap != null) {
         if (!totalEVMap.containsKey(loadedRuleSet)) { //System.err.println("The total EV Map does not contain the key " + loadedRuleSet );
            //System.err.println("The map contains this many elements: "+ totalEVMap.size() );

            return false;

         }
      }
   }
   final boolean fileExists = strategyFileExists(fullAnswers, theRules, strategyType, fileRetrievalFormat);
   return fileExists;
}

boolean strategyFileExists(boolean fullAnswers, Rules theRules, Skill mySkill,
        final int fileRetrievalFormat) {
   String filename;
   Rules someRules;
   if (!fullAnswers) {
      someRules = new Rules(theRules);
      someRules.setBlackjackPayback(1.5);
      filename = fileNameForAnswers(fullAnswers, someRules, mySkill, fileRetrievalFormat);
   }
   else {
      throw new UnsupportedOperationException();
      //filename = fileNameForAnswers(fullAnswers, theRules, mySkill, fileRetrievalFormat);

   }
   return strategyFileExists(filename);
}

protected boolean strategyFileExists(String filename) {
   return getStrategyFile(filename).exists();
}

/**
 * Java does not allow you to override static methods.
 * Android provides no mechanism for retrieving a File object from the Assets.
 *
 *
 * @param filename
 * @return
 */
private File getStrategyFile(String filename) {
   return new File(directory + File.separator + filename);
}

/**
 * Override me.
 *
 * @param fileNameForAnswers
 * @return FileInputStream of the correct strategy file
 * @throws FileNotFoundException
 *
 */
protected InputStream getFileInputStream(String fileName) throws IOException
//FileNotFoundException
{
   return new FileInputStream(directory + File.separator + fileName);
}

/**
 * Override me.
 * Note that this is NOT used in all the places it should be used in. I need to
 * change that.
 *
 * @param fileName
 * @return
 * @throws FileNotFoundException
 */
protected OutputStream getFileOutputStream(String fileName) throws IOException 
//FileNotFoundException
{
   return new FileOutputStream(directory + File.separator + fileName);
}

public String getDirectoryName() {
   return directory;
}

/**
 * This loads the correct answers from a file. If this.fullAnswers is false,
 * it will load the file corresponding to a BJ payback of 1.5, regardless of the
 * actual blackjack payback, because the blackjack payback has no effect on
 * strategy.
 * if (this.fileRetrievalFormat == Strategy.CONSOLIDATED_BY_DECKS_FILES)
 * it sets the number of decks to 1, since that will be the name of the
 * consolidated file.
 * Call this function only after theRules have been loaded into loadedRuleSet.
 *
 * Not tested.
 *
 *
 * @param theRules
 */
private void loadAnswersFromFile() throws IOException {
   InputStream answerFile;
   if (!fullAnswers) {
      Rules someRules = new Rules(theRules);
      someRules.setBlackjackPayback(1.5);
      if (this.fileRetrievalFormat == Strategy.CONSOLIDATED_BY_DECKS_FILES) {
         someRules.setNumberDecks(1);
      }
      answerFile = getFileInputStream(fileNameForAnswers(fullAnswers, someRules, strategyType, fileRetrievalFormat));
   }
   else {
      throw new UnsupportedOperationException();
   }
   //System.out.println("Attempting to load file " + fileNameForAnswers(fullAnswers) );
   try {
      loadSmallFile(answerFile);
      answerFile.close();
      //Praise the lord here.
      //At some point earlier than this, the map has already been loaded, if desired.

      if (!mapLoadDeactivated) {
         if (!totalEVMap.containsKey(loadedRuleSet)) {
            System.err.println("Rule set not found in map: " + theRules);
            System.err.println("The map contains " + totalEVMap.size() + " elements.");
         }
         this.houseEdge = totalEVMap.get(loadedRuleSet);
      }
      //Have to change this line if I ever solve for total-dependent

      allSolved = true;
      //System.out.println("File " + fileNameForAnswers(fullAnswers) + " has apparently "
      // 		+ "been loaded.");
      //System.out.println("Size of my map: "+ allAnswers.size() );
   }
   catch (StreamCorruptedException corruption) {
     try {
         answerFile.close();
      }
      catch (IOException ioproblem) {
         ioproblem.initCause(corruption);
         throw ioproblem;
      }
      throw new IOException(Utilities.stackTraceToString(corruption));
   }
   catch (IOException iotrouble) {
     try {
         answerFile.close();
      }
      catch (IOException ioproblem) {
         ioproblem.initCause(iotrouble);
         throw ioproblem;
      }
      throw iotrouble;
   }
}

/**
 * Helper function for loadSmallFile.
 * Uses the FIS, theRules, and fileRetrievalFormat to get the raw byte
 * stream correlating to the desired Rule set.
 * Assumes you will only retrieve data for a deck number stored in
 * Strategy.solvedNumberOfDecks
 *
 * @param answerFile
 */
private byte[] retrieveRawStrategyData(InputStream answerFile,
        final boolean validateData)
        throws IOException {
   DataInputStream byteSource = null; //Too slow...
   byte[] theRawStrat = new byte[Strategy.NUMBER_ANSWERS];
   byte[] scratchData = new byte[NUMBER_ANSWERS];

   try {
      byteSource = new DataInputStream(answerFile);
      if (fileRetrievalFormat == Strategy.MANY_SMALL_FILES) {
         retrieveRawByteData(byteSource, theRawStrat, Strategy.MANY_SMALL_FILES);
         if (!Utilities.attemptClosure(byteSource)) {
            throw new IOException("Stream closure failed.");
         }

         return theRawStrat;
      }
      else if (fileRetrievalFormat == Strategy.CONSOLIDATED_BY_DECKS_FILES) {
         int checkSum = 0;
         final int numHit17Strats = byteSource.read();

         final int numStand17Strats = byteSource.read();
         checkSum += numHit17Strats;
         checkSum += numStand17Strats;
         //Find deck index position
         int indexPosition = -1;
         final int desiredNumberDecks = theRules.getNumberOfDecks();
         for (int i = 0; i < solvedNumberOfDecks.length; i++) {
            if (solvedNumberOfDecks[i] == desiredNumberDecks) {
               indexPosition = i;
               break;
            }
         }

         if (indexPosition == -1) {
            throw new IllegalStateException(
                    "Strategies for " + desiredNumberDecks + " decks have not been stored.");
         }

// - 1 b/c numHit17Strats is always at least 1, and I need an index position

//Read hit 17 data. Return immediately when found if validataData is false.
         if (theRules.hitOn17() && (indexPosition > numHit17Strats - 1)) {
            indexPosition = numHit17Strats - 1;
         }
         for (int i = 0; i < numHit17Strats; i++) {
            checkSum += retrieveRawByteData(byteSource, scratchData, Strategy.CONSOLIDATED_BY_DECKS_FILES);

            if ((i == indexPosition) && theRules.hitOn17()) {
               System.arraycopy(scratchData, 0, theRawStrat, 0, scratchData.length);


               if (!validateData) {
                  if (!Utilities.attemptClosure(byteSource)) {
                     throw new IOException("Stream closure failed.");
                  }
                  return scratchData;
               }
            }
         }


         if (!theRules.hitOn17() && (indexPosition > numStand17Strats - 1)) {
            indexPosition = numStand17Strats - 1;
         }
         for (int i = 0; i < numStand17Strats; i++) {
            checkSum += retrieveRawByteData(byteSource, scratchData, Strategy.CONSOLIDATED_BY_DECKS_FILES);

            if ((i == indexPosition) && !theRules.hitOn17()) {
               System.arraycopy(scratchData, 0, theRawStrat, 0, scratchData.length);

               if (!validateData) {
                  if (!Utilities.attemptClosure(byteSource)) {
                     throw new IOException("Stream closure failed.");
                  }
                  return scratchData;
               }
            }
         }
         if (theRawStrat == null) {
            throw new IOException("NPE: Raw Strategy data never found.");
         }

         final int actualCheckSum;
         byte[] intData = new byte[4];
         byteSource.read(intData, 0, 4);
         actualCheckSum = (Utilities.convertBytesToInteger(intData))[0];

         if (actualCheckSum != checkSum) {
            System.err.println("Calculated checksum is " + checkSum + "; recorded checksum is " + actualCheckSum);
            throw new IOException("Checksum mismatch.");
         }

         if (!Utilities.attemptClosure(byteSource)) {
            throw new IOException("Stream closure failed.");
         }

         return theRawStrat;

      }
      else {
         Utilities.attemptClosure(byteSource);
         throw new IllegalArgumentException("Invalid valid of this.fileRetrievalFormat: " + fileRetrievalFormat);
      }
   }//End of try block
   catch (IOException ioe) {
      Utilities.attemptClosure(byteSource);
      throw ioe;
   }

}

/**
 * The workhorse function that actually converts the FIS into a loaded Strategy.
 * The FIS should be closed by the CALLING function.
 *
 * @param answerFile
 * @throws IOException
 *
 */
private void loadSmallFile(InputStream answerFile) throws IOException {
   final byte[] rawData;
   int index = 0;

   rawData = retrieveRawStrategyData(answerFile, Blackjack.debug());
   Answer anAnswer;
   int myKey;
   byte consolidated;
   CardValue secondPlayer;
   assert (allAnswers.isEmpty());

   for (CardValue firstPlayer : CardValue.oneToTen) {
      for (int i = firstPlayer.value(); i < 11; i++) {
         secondPlayer = CardValue.cardValueFromInt(i);
         for (CardValue dealerCard : CardValue.oneToTen) {
            consolidated = rawData[index++];
            //consolidated, first second, dealer
            anAnswer = new Answer(consolidated, firstPlayer, secondPlayer, dealerCard);
            allAnswers.put(anAnswer.myHashKey(), anAnswer);

            //B) Load split answer if it exists. Should be the next byte.
            if (firstPlayer == secondPlayer) {
               consolidated = rawData[index++];
               if (consolidated == Strategy.dummyByte) ; //Dummy value, no split answer
               //Do not add to map
               else { //There is a split answer
                  anAnswer = new Answer(consolidated, firstPlayer, secondPlayer, dealerCard);
                  //System.out.println("Adding this answer to map: " + anAnswer);
                  allAnswers.put(anAnswer.myHashKey(), anAnswer);
               }

            }
         }
      }
   }



}

/**
 * Should only be called from the linchpin function findAllAnswers.
 * Call this to calculate a complete strategy from
 * scratch with a pristine Shoe. It assumes that you have already
 * checked that the strategy has not already been calculated. At
 * the end it sets allSolved to true.
 *
 *
 * @param theRules
 * @throws IllegalStateException, NoRecommendationException
 */
private void calculateBasicStrategy() throws NoRecommendationException {
   assert (strategyType != Skill.PERFECT); //Unsupported
   boolean splittingAllowed = true;
   if ((strategyType == Skill.SIMPLE) || (strategyType == Skill.VERY_EASY) || (strategyType == Skill.PERFECT)) {
      throw new IllegalStateException("No need to calculate basic strategy because the skill is set to "
              + strategyType);
   }
   //Those are predefined; no calculations are necessary.

   ArrayList<Answer> splitAnswers = null;
   ArrayList<ArrayList<State>> hardAnswers = Blackjack.solveHardPlayersRecursive(
           theRules, true);
   ArrayList<ArrayList<State>> softAnswers = Blackjack.solveSoftPlayersRecursive(
           theRules, true);
   if (theRules.getMaxNumberSplitHands() > 0) {
      splitAnswers = Blackjack.calculateAllSplitValues(theRules, hardAnswers,
              softAnswers, true);
   }
   else {
      splittingAllowed = false;
   }

   //Everything is solved.


   if ((strategyType == Skill.TOTAL_DEP)) {
      Blackjack.consolidateIntoTotalDependent(hardAnswers, theRules);
   }
   //DEBUGGING
   //Testers.printStrategy(softAnswers, "Soft answers for " + theRules, false);
   //Testers.printStrategy(hardAnswers, "Hard answers for the same rule set.", false);
   //Blackjack.printCacheStatus();

   fillHashMap(hardAnswers, softAnswers, splitAnswers);
   calculateHouseEdge();

   allSolved = true;
}

/**
 * This takes information from calculateBasicStrategy and converts in into
 * Answers which go in the hashmap.
 * For each hand that can be split, two versions of the answer may exist in the
 * map,
 * a splitting-possible version and a version that doesn't consider splitting at
 * all.
 * If splitting is neither the first nor second best action, then the split
 * answer is
 * not put into the map at all.
 *
 * @param hardAnswers
 * @param softAnswers
 * @param splitAnswers
 */
private void fillHashMap(ArrayList<ArrayList<State>> hardAnswers,
        ArrayList<ArrayList<State>> softAnswers,
        ArrayList<Answer> splitAnswers) {

   int i, j;
   Answer anAnswer;
   for (i = 0; i < hardAnswers.size(); i++) {
      for (j = 0; j < hardAnswers.get(0).size(); j++) {
         final boolean isConsolidated = (strategyType == Strategy.Skill.TOTAL_DEP) ? true : false;
         anAnswer = new Answer(hardAnswers.get(i).get(j),
                 isConsolidated);
         allAnswers.put(anAnswer.myHashKey(), anAnswer);
      }
   }

   for (i = 0; i < softAnswers.size(); i++) {
      for (j = 0; j < softAnswers.get(0).size(); j++) {
         anAnswer = new Answer(softAnswers.get(i).get(j));
         allAnswers.put(anAnswer.myHashKey(), anAnswer);
      }
   }


   if (splitAnswers == null) ;
   else {
      for (i = 0; i < splitAnswers.size(); i++) {
         if ((splitAnswers.get(i).getBestAction() == Action.SPLIT)
                 || (splitAnswers.get(i).getSecondBestAction() == Action.SPLIT)) {
            allAnswers.put(splitAnswers.get(i).myHashKey(), splitAnswers.get(i));
         }
      }
   }


}

/**
 * Convenience function for solveAndStore. Assumes you just want one rule set.
 *
 *
 * @return true if the save was successful, false otherwise
 * @throws NoRecommendationException
 * @throws ClassNotFoundException
 * @throws IOException
 */
public boolean store() throws NoRecommendationException, IOException {
   final boolean actingSolo = true;
   return solveAndStore(theRules, actingSolo);
}

/**
 * Saves the answers to the current rules to a file using HASH
 * accuracy if the skill level is advanced or basic.
 * Returns true if the save was successful, false otherwise.
 * Throws exception if the skill level is not advanced or basic.
 *
 * This will only save the house edge if the strategy type is ADVANCED.
 *
 *
 * @param actingSolo True if this is a one-off function call. False if it's
 * being done
 * as part of a long series of calculations.
 * The difference is that, if done in a one-off,
 * it will load the totalEVMap from file and save it to file.
 * If not (actingSolo is false), it will assume the map is already loaded and it
 * will also
 * not save it to file. The reason is that it would cost too much time to open
 * and close the file
 * every time, if I do this function thousands of times.
 * @return False if the save was not successful, or if the mapLoadDeactivated is
 * true (since in that
 * case it wouldn't be able to save to a file)
 */
boolean solveAndStore(Rules someRules, boolean actingSolo)
		throws NoRecommendationException, IOException 
		//,ClassNotFoundException, IOException
{
   if (mapLoadDeactivated) {
      System.err.println("Strategy.solveAndStore: I cannot solve and store a data set when the total EV map "
              + " loading has been deactivated. Set mapLoadDeactivated to false before calling solveAndStore.");
      return false;
   }
   final int actualAccuracy = someRules.getAccuracy();
   someRules.setAccuracy(Rules.CACHE_ACCURACY);
   if (actingSolo) {
      loadTotalEVMap(true);
      //Will load map if map is empty. Unless the file is empty, in which case,
   }  //it'll load nothing.

   findAllAnswers(someRules);
   //This should also correctly initialize Strategy variables

   if ((strategyType != Skill.COMP_DEP) && (strategyType != Skill.TOTAL_DEP)) {
      throw new NoRecommendationException(null, someRules, "Can't store results of this skill level:" + strategyType);
   }


   if (Blackjack.debug()) {
      Validation.validateSolvedStrategy(this);
   }

   if (someRules.myHashKey() != loadedRuleSet) {
      System.err.println("Strategy.solveAndStore: Rules discrepancy. The rules I think are loaded are "
              + theRules + ", with hash key " + loadedRuleSet);
      System.err.println("However, I should have this rule set loaded, with hash key " + someRules.myHashKey()
              + someRules);
      System.err.println("I am returning without saving this rule set.");
      throw new NoRecommendationException();
      //saveTotalEVMap();
      //throw new IllegalStateException();
   }

   final boolean success = saveToFile();
   someRules.setAccuracy(actualAccuracy);


   if (strategyType == Skill.COMP_DEP) {
      totalEVMap.put(loadedRuleSet, (float) houseEdge);
   }
   //If I ever solve this for total-consolidate, I need to change this
   if (actingSolo) {
      saveTotalEVMap();
// Then save the map to the file.
   }
   return success;


}

/**
 * False to load the shortened version of the strategy. This will also convert
 * the BJ payback to 1.5 before loading any strategy file, since the strategy
 * does not change at all when the BJ payback does change.
 *
 * True to load the full Answers. True has not been tested at all and is unused.
 *
 */
private boolean fullAnswers;
public final static String completeStore = "c";
public final static String smallStore = "s";
/**
 * When correctly implemented, this will force a Strategy to recalculate every
 * time
 * instead of looking in a stored file. Currently, it just forces
 * isPrecalculated to return false, which may be just as good.
 */
private boolean forceSolve;
/**
 * A possible value of fileRetrievalFormat
 *
 */
static final public int MANY_SMALL_FILES = 1;
/**
 * A possible value of fileRetrievalFormat
 *
 */
static final public int CONSOLIDATED_BY_DECKS_FILES = 2;
/**
 * When set to true, CALCULATION_DEACTIVATED causes Strategy to throw a
 * NoRecommendationException when it cannot find a saved strategy that it's
 * trying
 * to solve for. When set to false, there is no effect.
 *
 */
private boolean calculationDeactivated = false;

public boolean setCalculationDeactivated(boolean calcDeactivated) {
   final boolean currentCalcStatus = calculationDeactivated;
   calculationDeactivated = calcDeactivated;
   return currentCalcStatus;
}

/**
 * What format the files are stored in: either Strategy.MANY_SMALL_FILES or
 * Strategy.CONSOLIDATED_BY_DECKS_FILES
 *
 */
static private int fileRetrievalFormat = CONSOLIDATED_BY_DECKS_FILES;

/**
 * Sets the static field fileRetrievalFormat to one of its valid values,
 * currently either
 * CONSOLIDATED_BY_DECKS_FILES or
 * MANY_SMALL_FILES
 *
 * @param fileFormatCode
 *
 */
static public void setFileFormat(final int fileFormatCode) {
   switch (fileFormatCode) {
      case MANY_SMALL_FILES:
      case CONSOLIDATED_BY_DECKS_FILES:
         fileRetrievalFormat = fileFormatCode;
         break;
      default:
         throw new IllegalArgumentException();
   }

}

/**
 * Files are only stored for the blackjack payback amount of 1.5.
 *
 * @param fullStore Whether or not the file is in the full store format --
 * whether object
 * serialization is used or not. If not, the data is stored directly as bytes.
 * @param someRules The Rule set being used to find the answer.
 * @return The file name corresponding to the specified rule set
 * fullStore has not been tested.
 */
private static String fileNameForAnswers(boolean fullStore, Rules someRules,
        Skill mySkill,
        final int fileRetrievalFormat) {
   Rules effectiveRules = new Rules(someRules);
   effectiveRules.setBlackjackPayback(1.5); //Added
   StringBuilder fileName = new StringBuilder();
   if (fullStore) {
      fileName.append(filePrefix).append(completeStore);
   }
   else {
      fileName.append(filePrefix).append(smallStore);
   }


   if (fileRetrievalFormat == Strategy.CONSOLIDATED_BY_DECKS_FILES) {
      //All hit/stand 17 for all deck values is stored in the same file. Hence:
      effectiveRules.setNumberDecks(1);
      effectiveRules.setHitOn17(true);
      fileName.append(fileConsolidationSuffix);
   }
   fileName.append(mySkill.abbrev()).append(String.valueOf(effectiveRules.myHashKey())).append(".ser");
   return fileName.toString();
}

private String fileNameForAnswers() {
   return fileNameForAnswers(fullAnswers, theRules, strategyType, fileRetrievalFormat);
}

/**
 * Helper function for saveToFile
 *
 */
boolean completeAnswerStore(OutputStream fileOut) {
   ObjectOutputStream hashStream = null;
   try {

      hashStream = new ObjectOutputStream(fileOut);

      //Saves space to not write the entire map, just the individual answers
      hashStream.writeShort((short) allAnswers.size()); // First: record the size
      for (Object a : allAnswers.values()) {
         hashStream.writeObject((Answer) a);
      }

      hashStream.writeDouble(houseEdge);

      hashStream.close();
      return true;
   }
   catch (IOException io) {
      io.printStackTrace();
      try {
         if (hashStream != null) {
            hashStream.close();
         }
         return false;
      }
      catch (IOException finalProb) {
         finalProb.printStackTrace();
         return false;
      }

   }

}

protected void makeDataDirectory() {
   File aFile = new File(directory);
   aFile.mkdir();
}

/**
 * Do NOT call this function from anywhere except
 * inside solveAndStore. Not sure if it'll freak if I give it incomplete
 * Answers.
 * Upon errors, it'll print the stack trace and return false.
 *
 * Dangit. "DataOutput" was the interface I was looking for. Ah well, I already
 * separated the functions.
 *
 * If the file already exists, this will just return true instantly.
 */
private boolean saveToFile() {
   if (isPrecalculated()) {
      return true;
   }
   if (fileRetrievalFormat != Strategy.MANY_SMALL_FILES) {
      System.err.println("I can't currently save files in any format except Strategy.MANY_SMALL_FILES.");
      return false;
   }
   String filename;
   if (fullAnswers) //never used
   {
      filename = fileNameForAnswers(fullAnswers, theRules, strategyType, fileRetrievalFormat);
   }
   else {
      Rules someRules = new Rules(theRules);
      someRules.setBlackjackPayback(1.5);
      filename = fileNameForAnswers(fullAnswers, someRules, strategyType, fileRetrievalFormat);
   }

   boolean success = true;

   makeDataDirectory();

   OutputStream fileOut = null;
   try {
      fileOut = getFileOutputStream(filename);

      //FORMAT: One short representing the size of the map.
      // The data, either all Answers or all bytes depending on the selection
      //One double representing the house edge
      if (fullAnswers) {
         if (!completeAnswerStore(fileOut)) {
            success = false;
         }
      }
      else {
         if (!smallFileMaker(fileOut)) {
            success = false;
         }
      }
      fileOut.close();
      return success;
   }
   catch (FileNotFoundException exceptional) {
      exceptional.printStackTrace();
      System.err.println("File " + directory + "/" + filename + " is a directory rather than "
              + "a regular file,\n"
              + " does not exist but cannot be created, or cannot be opened for some other reason.");
//This is only thrown by the FileOutputStream constructor, so no need to close objectstream.
      if (fileOut != null) {
         try {
            System.err.println("File Not Found error, but fileOut is non-null.");
            fileOut.close();
         }
         catch (IOException closingFailure) {
            closingFailure.initCause(exceptional);
            closingFailure.printStackTrace();
            return false;
         }
      }
      return false;
   }
   catch (IOException myBad) {
      myBad.printStackTrace();

      if (fileOut != null) {
         try {
            fileOut.close();
         }
         catch (IOException closingFailure) {
            closingFailure.initCause(myBad);
            closingFailure.printStackTrace();
         }
      }
      return false;
   }

}

/**
 * Map which stores all total EV numbers. The skill level of the map
 * is stored in totalEVMapSkill. Currently, only Skill.COMP_DEP is
 * supported.
 *
 */
private static Map<Long, Float> totalEVMap = new HashMap<Long, Float>();

/**
 * Helper function for saveToFile. This is the deep function which actually
 * puts the Answers into a file.
 *
 * @param hashStream
 *
 */
private boolean smallFileMaker(OutputStream fileOut) {
   Answer anAnswer;
   int myKey;
   byte first, second, dealer;
   DataOutputStream dataStream = null;
   short checkSum = 0;
   byte toWrite;
   try {
      dataStream = new DataOutputStream(fileOut);
      CardValue secondPlayer;
      //OK. I don't want to double-record these.
      for (CardValue firstPlayer : CardValue.oneToTen) {
         for (int i = firstPlayer.value(); i < 11; i++) {
            secondPlayer = CardValue.cardValueFromInt(i);
            //iterate over the array, starting at firstPlayer
            for (CardValue dealerCard : CardValue.oneToTen) {
               first = Answer.cardValueToByte(firstPlayer);
               second = Answer.cardValueToByte(secondPlayer);
               dealer = Answer.cardValueToByte(dealerCard);
               myKey = Answer.answerHash(first, second, dealer, false); //Non-split key
               //first second, dealer, all byte.
               anAnswer = allAnswers.get(myKey);
               assert (anAnswer != null);



               toWrite = anAnswer.getConsolidatedActions();
               dataStream.writeByte(toWrite);
               checkSum += toWrite;
               //B) Store split answer, or dummy answer.
               if (firstPlayer == secondPlayer) {
                  myKey = Answer.answerHash(first, second, dealer, true); //Split key
                  if (allAnswers.containsKey(myKey)) {
                     anAnswer = (Answer) allAnswers.get(myKey);
                     assert (anAnswer != null);
                     toWrite = anAnswer.getConsolidatedActions();
                     dataStream.writeByte(toWrite);
                     //System.out.println("Saving this answer: " + anAnswer);
                     checkSum += toWrite;
                  }
                  else {
                     dataStream.writeByte(Strategy.dummyByte); //-> Don't add this back
                  }                  //to the map or include in checksums


               }
            }
         }
      }
      dataStream.writeShort(checkSum);

      dataStream.close();
      return true;
   }
   catch (IOException io) {
      io.printStackTrace();
      if (dataStream != null) {
         try {
            System.err.println("Data stream is not null, but there was a problem after that.");
            dataStream.close(); 
            return false;

         }
         catch (IOException lastProblem) {
            System.err.println("Data stream was non-null, but could not be successfully closed.");
            lastProblem.printStackTrace();
            return false;

         }
      }
      System.err.println("Data stream was null.");
      return false; //Data stream could not be opened
   }
}

/**
 * Should only be called after the hashmap has already been made.
 * This throws an exception if a hard answer isn't in the map, so I've had to
 * make
 * dummy hard answers for the very easy/easy strategies. Or I could just not
 * throw
 * the exception and instead load the split answer.
 *
 * @param theRules
 * @param myState
 * @return
 * @throws NoRecommendationException
 */
private Answer twoAnswersInMap(State myState) throws NoRecommendationException {
   float bestEV = -5000;
   Answer splitAnswer = null;
   Answer normalAnswer = allAnswers.get(myState.getAnswerHash(false));
   if (normalAnswer == null) {
      NoRecommendationException empty = new NoRecommendationException(myState, theRules, "Can't find"
              + " answer in loaded map (code: " + myState.getAnswerHash(false));
      throw empty;
   }
   boolean isComplete = normalAnswer.isComplete();

   if ((theRules.isPossible(Action.SPLIT, myState))
           && (allAnswers.containsKey(myState.getAnswerHash(true)))) {
      //If I can split, and there is an answer that involves splitting.

      //I cannot compare EVs on incomplete answers. Hence, just return the split answer.
      splitAnswer = (Answer) allAnswers.get(myState.getAnswerHash(true));
      if (!isComplete) {
         return splitAnswer;
      }

      if (theRules.isPossible(splitAnswer.getBestAction(), myState)) {

         bestEV = splitAnswer.getBestEV();
      }
      else if (theRules.isPossible(splitAnswer.getSecondBestAction(), myState)) {
         bestEV = splitAnswer.getSecondBestEV();


      }
      else {
         assert false; //I can't be here -- I know that splitting is possible and
      }      //that one of these actions is splitting.
   }

   if (!isComplete) {
      return normalAnswer; //Splitting isn't in the answer set.
   }
   //Can I do what the normalAnswer says? Is it better than the split answer?
   if (theRules.isPossible(normalAnswer.getBestAction(), myState)) {
      if (normalAnswer.getBestEV() > bestEV) {
         return normalAnswer;
      }
   }
   else if (theRules.isPossible(normalAnswer.getSecondBestAction(), myState)) {
      if (normalAnswer.getSecondBestEV() > bestEV) {
         return normalAnswer;

      }
   }
   // if (nothing was possible) || (I made some mistake)
   if (bestEV < -400) {
      throw new NoRecommendationException(myState, theRules,
              "All recommended actions aren't legal.");
   }
   return splitAnswer;
}

/**
 * Throws NoSuchElementException if key is not in map
 * FOR DEBUGGING PURPOSES
 *
 * @param key
 * @return
 *
 */
Answer pullAnswer(int key) throws NoSuchElementException {
   Answer response = allAnswers.get(key);
   if (response == null) {
      System.out.println("No match for key: + " + key + " in map.");
      throw new NoSuchElementException();
   }

   return response;
}

/**
 * This stores the skill of the currently loaded EV map.
 */
private Skill totalEVMapSkill;

/**
 * This function is called naturally from within the other public Strategy
 * functions; but it is a time-consuming operation and so to prevent lag
 * the operation should be done beforehand in another thread.
 *
 * @throws IOException
 *
 */
public void loadTotalEVMap() throws IOException {
   loadTotalEVMap(false);

}

/**
 * Loads the totalEVMap from the file corresponding to the
 * current strategy type. If the file is already loaded, this does nothing.
 * If the wrong map is loaded (total-dep and not comp-dep or vice versa), this
 * should
 * clear the map and load the right one; however, that's untested since I
 * haven't
 * developed a total-dep total EV algorithm yet.
 * In the mean time, I'm just going to arbitrarily load the comp-dependent map.
 * .
 * If the file does not exist, this prints an error message and returns. In that
 * case, I assume that there is no error, but that I haven't done calculations
 * yet.
 *
 * @throws IOException
 * @param saveCurrentMap true to save the currently loaded map into a file
 *
 */
private void loadTotalEVMap(final boolean saveCurrentMap) throws IOException {


   if (mapLoadDeactivated) {
      String error = "Strategy.loadTotalEVMap: I am being called when the mapLoadDeactivated is true.";
      assert false : error;
      throw new IOException(error);

   }
   if (!totalEVMap.isEmpty()) {
      return;
      /* From HERE
       if (strategyType == totalEVMapSkill)
       return;
       else
       {//totalEVMapSkill is null here. I don't know how. When did the totalEVMap get loaded??
       if (saveCurrentMap)
       saveTotalEVMap(totalEVMapSkill);
       totalEVMap.clear();
       totalEVMapSkill = this.strategyType;
       } to HERE commented out. If it's loaded, then I'm happy. */
   }
   
   //The map is empty. I'm going to either load it or return if
   //the map file doesn't exist. In either case, I need to indicate what type
   //of Skill the map is.
   totalEVMapSkill = this.strategyType;
   
   //System.out.println("Past the empty check in loadTotalEVMap");
   //assert (totalEVMap.size() == 0): "Strategy.loadTotalEVMap called when the existing map was non-zero.";
   InputStream myFIS = null;

   ObjectInputStream myOIS = null;
   try {
      try {
         myFIS = getFileInputStream(fileNameForMap(strategyType)); //you pass it just the file name.
      }
      catch (FileNotFoundException fnf) {
         System.err.println("Total EV Map file doesn't exist. ");
         fnf.printStackTrace();
         return; //OK, I haven't created this yet.
      }
      myOIS = new ObjectInputStream(myFIS);
      totalEVMap = (HashMap<Long, Float>) myOIS.readObject();
      //System.out.println("Past object creation in loadTotalEVMap: the map has " + totalEVMap.size() + " elements.");
      myOIS.close();
      myFIS.close();
   }
   catch (IOException problemOne) {
      Utilities.attemptClosure(myOIS);
      Utilities.attemptClosure(myFIS);
      throw problemOne;
   }
   catch (ClassNotFoundException problemOne) {
      Utilities.attemptClosure(myOIS);
      Utilities.attemptClosure(myFIS);
      throw new IOException(Utilities.stackTraceToString(problemOne));
   }

}

/**
 * The range of decks whose strategies will be precalculated and
 * saved to disk
 *
 */
static public final int[] solvedNumberOfDecks = {1, 2, 4, 6, 8};

public void validateCalculations(boolean verbosity, final int fileType) {

   ToggleSettings validation = new ValidateCalculations(verbosity, fileType);
   for (int i = 0; i < solvedNumberOfDecks.length; i++) {
      toggleBaseRulesBooleans(solvedNumberOfDecks[i], validation);
   }

}

/**
 * Validates all solved strategies in the set skill level.
 *
 */
public void validateCalculations(boolean verbosity) {
   validateCalculations(verbosity, Strategy.MANY_SMALL_FILES);
   validateCalculations(verbosity, Strategy.CONSOLIDATED_BY_DECKS_FILES);

}

public void solveStoreEverything(final int NumberOfDecks, boolean verbosity) {
   ToggleSettings calculations = new RunCalculations(verbosity);
   toggleBaseRulesBooleans(NumberOfDecks, calculations);
}

public void consolidateAllFiles(boolean verbosity) {
   ToggleSettings consolidate = new FileConsolidation(verbosity);
   for (int i = 0; i < solvedNumberOfDecks.length; i++) {
      toggleBaseRulesBooleans(solvedNumberOfDecks[i], consolidate);
   }
   //Actually, I really only need to do this for decks = 1, but just to check.
}

/**
 * TODO: Fix this, it is gross. Refactor to:
 * -Create a Rules constructor which takes as arguments each different Rule setting
 * (it'd take an array)
 * -Create a Rules function which validates the currently passed rule set
 * -Then generate a array (linearly, not nested like this) for each different
 * combination.
 * -For the final combination, validate it using the new rules function, then
 * if it passes create that Rule set.
 * (This would be far far easier in Javascript :( )
 * 
 * 
 * Used to cycle through all rule possibilities for the given number of decks.
 * Chains calls to toggleRulesInts and then toggleDoubleBooleans. Uses hash
 * accuracy.
 *
 *
 * aStrategy.toggleBaseRulesBooleans(1, false, true) gives:
 * Total Rule set possibilities: 81920 (for each choice of numberOfDecks),
 * without
 * checking for duplication. With duplication, there are 5120 rule sets per
 * deck.
 *
 * Total rule sets, using the double toggles: 409600
 * After I comment out the doubleAlwaysPossible line, and only store the total
 * EV
 * numbers from the BJ payback, I get 10 times fewer sets.
 *
 *
 * @param NumberOfDecks number of decks
 * @param testRulesHash Used to test the rules key function
 * @param testToggles used to test the toggling functionality.
 */
void toggleBaseRulesBooleans(final int NumberOfDecks, ToggleSettings settings) {
   Rules someRules = new Rules(NumberOfDecks);

   someRules.setRulesAutoToggles(false);
//Disables the doubles rules changing each other. This is so I can verify that every different rule set
//produces a unique key. If I enabled toggling, this would output rule sets that
//are the same, so of course they would produce the same key.
//In addition, auto toggles also break my toggling system here, since they never undo.


   settings.initialSetup(someRules);

   someRules.setAccuracy(Rules.CACHE_ACCURACY);
   if ((this.strategyType != Skill.COMP_DEP) && (this.strategyType != Skill.TOTAL_DEP)) {
      throw new IllegalStateException("The other strategies are either perfect (shoe-dependent) or hand-made.");
   }

   int i = 0;
   int[] flags = new int[6];
   Utilities.zero(flags);
   for (someRules.setHitOn17(true); flags[0] < 2; flags[0] += 1, someRules.setHitOn17(false)) {// System.out.println(flags[0] + " is flags[0]");
      for (someRules.setEarlySurrender(true); flags[1] < 2; flags[1] += 1, someRules.setEarlySurrender(false)) { //System.out.println(flags[1] + " is flags[1]");
         for (someRules.setHitSplitAces(true); flags[2] < 2; flags[2] += 1, someRules.setHitSplitAces(false)) {//System.out.println(flags[2] + " is flags[2]");


            for (someRules.setLateSurrender(true); flags[3] < 2; flags[3] += 1, someRules.setLateSurrender(false)) { //System.out.println(flags[3] + " is flags[3]");


               for (someRules.setHoleCard(true); flags[4] < 2; flags[4] += 1, someRules.setHoleCard(false)) { //System.out.println(flags[4] + " is flags[4]");


                  for (someRules.setEarlySurrenderNotOnAces(true); flags[5] < 2; flags[5] += 1, someRules.setEarlySurrenderNotOnAces(false)) {


                     toggleRulesInts(someRules, settings);
                     settings.doneWithACall(someRules);
                  }
                  flags[5] = 0;

               }
               flags[4] = 0;



            }
            flags[3] = 0;


         }
         flags[2] = 0;
      }
      flags[1] = 0;
      settings.halfDone();
      DealerCache.clearCache(); //After I switch hit/stand on 17.
   }

   //The final step -- save map into file. Unless I'm testing.
   settings.allTogglesDone();
}

/**
 * The name of the file which holds the total EV map, for composition-dependent
 * answers.
 *
 */
public static final String totalEVMapName = "42";

/**
 * CONVERTS ALL MAP REQUESTS TO COMP_DEP
 *
 * @param mySkill
 * @return
 *
 */
protected String fileNameForMap(Skill mySkill) {
   /*
    if (mySkill != Skill.COMP_DEP)
    { System.err.println(theRules);
    NoRecommendationException nre =
    new NoRecommendationException("Only the composition-dependent map is currently supported.");
    nre.printStackTrace();
    }
    */
   return filePrefix + Skill.COMP_DEP.abbrev() + totalEVMapName + ".ser";

}

private boolean saveTotalEVMap() {
   return saveTotalEVMap(strategyType);
}

/**
 * UNTESTED
 * This should take the total EV map in memory and save it to a file.
 *
 * @return true if the save was successful, false otherwise (like if
 * mapLoadDeactivated is true)
 */
private boolean saveTotalEVMap(Skill mySkill) {
   if (mapLoadDeactivated) {
      System.err.println("Strategy.saveTotalEVMap:mapLoadDeactivated is true and I am trying to save the total EV map.");
      return false;
   }
   File aFile;
   OutputStream fileOut = null;
   aFile = new File(directory);
   aFile.mkdir();
   //I assume here that this will write over whatever I had before.

   ObjectOutputStream mapStream = null;
   try {
      fileOut = getFileOutputStream(fileNameForMap(mySkill));
      //Can throw FileNotFoundException

      mapStream = new ObjectOutputStream(fileOut);
      mapStream.writeObject(totalEVMap);
      /*System.out.println("Strategy.saveTotalEVMap: I've saved totalEVMap, containing " +
       totalEVMap.size() + " elements, to a file."); */
      mapStream.close();
      fileOut.close();
      return true;
   }
   catch (IOException ioe) {
      System.err.println(ioe);
      if (mapStream != null) {
         try {
            mapStream.close();
            return false;
         }
         catch (IOException ioe1) {
            System.err.println(ioe1);
            Utilities.attemptClosure(fileOut);
            return false;
         }

      }
      else {
         Utilities.attemptClosure(fileOut);
         return false;
      }

   }

}

/**
 *
 * This changes the blackjack payback array based on the strategy type.
 * If the strategy is basic, this will only compute the strategy for a
 * payback of 1.5. The reason is that the payback does not alter the strategy,
 * so my results would be the same no matter what I set the payback to.
 *
 * This function toggles the integers, and the blackjack payback amounts, in the
 * Rules.
 *
 * @param theRules
 * @param testRulesHash
 */
private void toggleRulesInts(Rules someRules, ToggleSettings settings) {
   double[] bjPaybackArray;
//= {1, 6D/5D, 7D/5D,
   // 1.5D, 2D};
   if (strategyType == Skill.COMP_DEP) {
      bjPaybackArray = new double[]{1, 6D / 5D, 7D / 5D, 1.5D, 2D};
   }
   else if (strategyType == Skill.TOTAL_DEP) {
      bjPaybackArray = new double[]{1.5D};
   }
   else {
      throw new IllegalStateException("Invalid skill level in toggleRulesInts");
   }

   for (int i = 0; i < bjPaybackArray.length; i++) {
      someRules.setBlackjackPayback(bjPaybackArray[i]);
      for (int j = 0; j < 2; j++) {
         someRules.setNumResplitAces(j);
         for (int k = 1; k < 3; k++) { //You can split once or twice
            someRules.setMaxNumberSplitHands(k);

            toggleDoubleBooleans(someRules, settings);
         }
      }
   }

}

private void toggleDoubleBooleans(Rules someRules, ToggleSettings settings) {
   int[] flags = new int[7];
   Utilities.zero(flags);
   //assert (!someRules.getAutoToggles()): someRules;
   for (someRules.myDoubleRules.setAlwaysPossible(true); flags[0] < 2;
           flags[0]++, someRules.myDoubleRules.setAlwaysPossible(false)) {
      for (someRules.myDoubleRules.setAnyTwoCards(true); flags[1] < 2;
              flags[1]++, someRules.myDoubleRules.setAnyTwoCards(false)) {
         for (someRules.myDoubleRules.setNotOnAces(true); flags[2] < 2;
                 flags[2]++, someRules.myDoubleRules.setNotOnAces(false)) {
            for (someRules.myDoubleRules.setNotPostSplit(true); flags[3] < 2;
                    flags[3]++, someRules.myDoubleRules.setNotPostSplit(false)) {  
            	//assert (!someRules.getAutoToggles()): someRules;
               for (someRules.myDoubleRules.setNotSplitAces(true); flags[4] < 2;
                       flags[4]++, someRules.myDoubleRules.setNotSplitAces(false)) {
                  for (someRules.myDoubleRules.setOnlyNineTenEleven(true); flags[5] < 2;
                          flags[5]++, someRules.myDoubleRules.setOnlyNineTenEleven(false)) {
                     for (someRules.myDoubleRules.setOnlyTenAndEleven(true); flags[6] < 2;
                             flags[6]++, someRules.myDoubleRules.setOnlyTenAndEleven(false)) {

                        settings.insideAction(someRules);

                     }
                     flags[6] = 0;
                  }
                  flags[5] = 0;
               }
               flags[4] = 0;
            }
            flags[3] = 0;
         }
         flags[2] = 0;
      }
      flags[1] = 0;
   }
}

/**
 * ONLY CALL ME FROM calculateBasicStrategy. That means all answers should be
 * complete.
 * Use the getter otherwise
 *
 */
private void calculateHouseEdge() throws NoRecommendationException {

   if (allAnswers.isEmpty()) {
      throw new IllegalStateException("Illegal State: the hash map is currently empty, so I can't "
              + "solve for the house ");
   }
   Answer currentAnswer;
   State scratch;
   Suit q = Suit.CLUBS;
   double totalEV = 0;
   FastShoe myShoe = new FastShoe(theRules.getNumberOfDecks());
   double probSum = 0;
   double probability;

   for (CardValue dealerCard : CardValue.oneToTen) {
      for (CardValue firstPlayerCard : CardValue.oneToTen) {
         for (CardValue secondPlayerCard : CardValue.oneToTen) {

            probability = myShoe.probTheseThreeInOrder(firstPlayerCard, secondPlayerCard, dealerCard);
            if (probability > 0) {
               scratch = new State(new Card(q, firstPlayerCard), new Card(q, secondPlayerCard),
                       new Card(q, dealerCard));

               currentAnswer = retrieveAnswerAdvOrBasic(scratch);
               assert (currentAnswer.isComplete());
               totalEV += probability * currentAnswer.getBestEV();
               assert ( (probSum += probability) > 0);
            }
         }
      }
   }

   assert ((probSum < 1 + Constants.EPSILON) && (probSum > 1 - Constants.EPSILON));
   this.houseEdge = -1 * totalEV;
}

/**
 * This function assumes that tens, jacks, and queens are interchangable.
 * ONLY CALL ME FROM inside calculateHouseEdge or calculateBasicStrategy or
 * findBestAnswer...
 * wait, maybe this is a generic function for getting an Answer from a solved
 * rule state given
 * that your skill level is Advanced or Basic, without any error checking.
 * Doesn't it work on the easier skill levels too?
 */
private Answer retrieveAnswerAdvOrBasic(State myState) throws NoRecommendationException {

   Answer theAnswer;
   if (myState.getFirstCardValue().value() == myState.getSecondCardValue().value()) //firstCardIs(myState.getSecondCard().getCardValue())) <-wrong, jacks and queens are the same
   {
      //the hashmap may have two answers in this case.
      theAnswer = twoAnswersInMap(myState);
      if (theAnswer == null) {
         throw new NoRecommendationException("NPE -- no answer in map for " + myState);
      }
   }
   else //Now I know that splitting is impossible, since the player cards are different.
   {
      theAnswer = (Answer) allAnswers.get(myState.getAnswerHash(false));
      if (theAnswer == null) {
         State.printStateStatus(myState, "In retrieveAnswerAdvOrBasic");
          throw new NoRecommendationException("NullPointerException");
      }
   }
   return theAnswer;
}

/**
 * Prints the split, soft, and hard strategy tables to screen.
 * Future: Should just return StringBuilder.
 * TODO: Standardize and note whether dealer A up card expected values include the possibility of
 * dealer blackjack or not.
 */
public void print(final boolean showSecondBest) {
   if (!isAllSolved()) {
      System.err.println("This strategy has not been solved yet.");
      return;
   }
   if (getStrategyType() == Skill.PERFECT) {
      System.err.println("The split table cannot be viewed on perfect strategy because it"
              + " always depends on the shoe contents. To view perfect composition-dependent"
              + " play with a pristine shoe, set the strategy type to Skill.ADVANCED.");
      return;
   }
   System.out.println(theRules); //TODO: Create a prettier version of toString and use that

   StringBuilder sb = new StringBuilder();
   sb.append(getHardTable(showSecondBest));
   sb.append(getSoftTable(showSecondBest));
   sb.append(getSplitTable(showSecondBest));
   System.out.println(sb);
   if (strategyType != Skill.COMP_DEP) {
	  // TODO: Figure out how far the program is from solving for total-dependent strategy and arbitrary strategies
	  // There might be coding lurking somewhere that already does this. 
	  System.out.println("The house edge is currently only available for composition-dependent strategy.");
	  return;
   }
   try {
      System.out.format("The house edge is %.4f%% %n", (100 * getHouseEdge()));
   }
   catch (NoRecommendationException nre) {
      System.err.println(nre);
   }
   catch (IOException ioe) {
      System.err.println(ioe);

   }

}

public Rules getMyRules() {
   return new Rules(theRules);

}

/**
 * Untested
 * Returns true if allSolved is true (Something has been solved),
 * and what has been solved is complete. Otherwise, returns false.
 *
 * @return
 */
public boolean isCompleteLoaded() {
   Card someCard = new Card(Suit.CLUBS, CardValue.TWO);
   //Is anything loaded?
   if (allSolved) {
      State someState = new State(someCard, someCard, someCard);
      Answer anAnswer = allAnswers.get(someState.getAnswerHash(false));
      if (anAnswer.isComplete()) {
         return true;
      }
   }
   return false;

}

/**
 * Creates the hard, soft, and split strategy tables. Requires a width of 92 characters.
 * @param states Solved states used to draw table. Each row should have the same player cards and 
 * an ascending dealer up card, going from 2 to A. 
 * @param hardTable True if the table being printed is for hard hand totals
 * @param displaySecondBest True to display the second best action/EV for the rule set
 */
private StringBuilder getTable(ArrayList<ArrayList<State>> states, boolean hardTable, boolean displaySecondBest,
		String tableTitle) {
	String leftHandHeading = "Player Cards";
	final boolean showHandTotal; 
	StringBuilder sb = new StringBuilder();
	Formatter formatter = new Formatter(sb, Locale.US);
	formatter.format("%40s%12s%40s%n", "", tableTitle, "");
	if (hardTable && ( strategyType != Skill.COMP_DEP) && strategyType != Skill.PERFECT ) {
		leftHandHeading = "Hand Total";
		showHandTotal = true;
	}		
	else {
		showHandTotal = false;
	}
	formatter.format("%41s%14s%41s%n", "","Dealer Up Card",""); //center text. 92 characters console
	formatter.format("%-16s%8s%8s%8s%8s%8s%8s%8s%8s%8s%8s%n",
			   leftHandHeading,"2", "3","4","5","6","7","8","9","10","A");
	Action bestAction, secondBestAction;
	ArrayList<State> currentStates;
	State firstState;
	double ev;
	String handString;
	try {
	for (int i = 0; i < states.size(); i++) {
		currentStates = states.get(i);
		firstState = currentStates.get(0);
		if (showHandTotal) {
		    handString = Integer.toString(firstState.handTotal());
		}
		else {
			handString = firstState.getFirstCardValue().abbrev() + ", " + 
						 firstState.getSecondCardValue().abbrev();
		}
		formatter.format("%-16s", handString);
		for (State aState : currentStates) {
			bestAction = findBestAction(aState);
			formatter.format("%8s",bestAction.abbrev() );
		}
		formatter.format("%n");
		if (isCompleteLoaded()) {
			formatter.format("%16s", "");
			for (State aState : currentStates) {
				ev = findBestEV(theRules, aState);
				formatter.format("%+8.4f", ev);
			}
			formatter.format("%n");
		}
		if (!displaySecondBest) {
			continue;
		}
		
		formatter.format("%16s", "");
		for (State aState : currentStates) {
			secondBestAction = findSecondBestAction(theRules, aState);
			formatter.format("%8s",secondBestAction.abbrev());
		}
		formatter.format("%n");
		if (isCompleteLoaded()) {
			formatter.format("%16s", "");
			for (State aState : currentStates) {
				ev = findSecondBestEV(theRules, aState);
				formatter.format("%+8.4f", ev);
			}
			formatter.format("%n");
		}
	}
	formatter.format("%n");
	formatter.close();
	return sb;
	}
	catch (NoRecommendationException e) {
	      e.printStackTrace();
	      System.err.println(e);
		  formatter.close();
		  return new StringBuilder("");
	}
	catch (IOException e) {
	      e.printStackTrace();  
	      System.err.println(e);
		  formatter.close();
		  return new StringBuilder("");
	}
}



}