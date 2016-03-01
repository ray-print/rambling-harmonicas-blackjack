package ramblingharmonicas.blackjack;

import java.io.IOException;
import java.util.*;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import ramblingharmonicas.blackjack.cards.*;
import ramblingharmonicas.blackjack.rules.Surrender;

//TODO: Refactor to move some functionality to Utilities or Strategy or some new class(es).
public class Blackjack {
static public long dealerIterations = 0;
static public long fastDealerIterations = 0;
static public long playerIterations = 0;
static public long holeCardCheck = 0;
private static boolean DEBUGGING = true;

public static void main(String[] args) throws NoRecommendationException, IOException {
    Strategy myStrategy = parseArguments(args);
    if (myStrategy == null) {
        return;
    }

    myStrategy.solve();
    myStrategy.print(false);
}

private static Strategy parseArguments(String[] args) {
    OptionParser parser = createOptionsParser();
    try {
        Rules theRules = createRules(parser, args);
        //TODO: The command line can pass on info about whether
        //to print the best/second best EV, force calculations or load them, use total-dep strategy,
        //etc., add that functionality and make a createStrategy function similar to createRules
        Strategy someStrat = new Strategy(theRules, Strategy.Skill.COMP_DEP);
        return someStrat;
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
static final String[] DOUBLE_AFTER_SPLIT = {"double-after-split", "das"};
static final String[] HELP = {"help", "?"};
static final String HIT_SOFT_17 = "hit-soft-17";
static final String[] HIT_SPLIT_ACES = {"hsa", "hit-split-aces"};
static final String NO_HOLE_CARD = "no-hole-card";
static final String RESPLIT_ALLOWED = "resplit-allowed";
static final String SURRENDER = "surrender";

static enum Double {
ANY_TWO_CARDS, TEN_ELEVEN, NINE_TEN_ELEVEN, HARD_ONLY
};
//TODO: Investigate if there's any good reason why this isn't already an enum
};

private static Rules createRules(OptionParser parser, String[] args) throws OptionException,
        ClassCastException {
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
    Rules theRules = new Rules(decks);
    theRules.setHoleCard(!((Boolean) options.valueOf(Options.NO_HOLE_CARD)));

    theRules.setHitOn17((Boolean) options.valueOf(Options.HIT_SOFT_17));
    theRules.setHitSplitAces(
            (Boolean) options.valueOf(Options.HIT_SPLIT_ACES[0]));

    String surrenderOption = ((String) options.valueOf(Options.SURRENDER)).toUpperCase();
    Surrender surrender = Surrender.valueOf(surrenderOption);
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

    final boolean resplitAllowed = ((Boolean) options.valueOf(
            Options.RESPLIT_ALLOWED));
    if (resplitAllowed) {
        theRules.setMaxNumberSplitHands(2);
    }
    else {
        theRules.setMaxNumberSplitHands(1);
    }

    String doubleSetting = (String) options.valueOf(Options.DOUBLE_RULES);
    Options.Double doubleRule = Options.Double.valueOf(doubleSetting);
    switch (doubleRule) {
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

    final boolean doubleAfterSplit = (Boolean) options.valueOf(
            Options.DOUBLE_AFTER_SPLIT[0]);
    theRules.myDoubleRules.setNotPostSplit(!doubleAfterSplit);

    return theRules;
}

private static OptionParser createOptionsParser() {
    OptionParser parser = new OptionParser();
    parser.acceptsAll(Arrays.asList(Options.HELP), "Show help").forHelp();
    //TODO: Add option for forcing calculations and for skill level (Total-dep, comp-dep)
    parser.accepts(Options.DECKS, "Number of decks (1-8)").withRequiredArg().ofType(
            Integer.class)
            .defaultsTo(8).describedAs("decks");
    parser.accepts(Options.HIT_SOFT_17, "Dealer hits on soft 17").withOptionalArg()
            .ofType(Boolean.class).defaultsTo(true);
    parser.acceptsAll(Arrays.asList(Options.HIT_SPLIT_ACES),
            "The player can hit on split aces")
            .withOptionalArg().ofType(Boolean.class).defaultsTo(true);
    parser.accepts(Options.NO_HOLE_CARD,
            "The dealer does not draw a hole card until after "
            + "the player's hand is done (European style).").withOptionalArg()
            .ofType(Boolean.class).defaultsTo(false);
    parser.accepts(Options.SURRENDER,
            "The player can surrender his hand. Late surrender occurs "
            + "after the dealer has checked for blackjack. Early surrender, an extremely rare "
            + "rule, allows the player to surrender before the dealer has checked for blackjack.")
            .withOptionalArg().defaultsTo("LATE")
            .describedAs("early, early_excluding_aces, late, or none");
    //.ofType(Surrender.class); (JOptSimple would auto-parses this and mess up the case)
    parser.accepts(Options.RESPLIT_ALLOWED,
            "The player is allowed to split one more time after"
            + " her first split. (Sorry, multiple resplits are not supported by the engine.)")
            .withOptionalArg().ofType(Boolean.class).defaultsTo(true);
    parser.accepts(Options.DOUBLE_RULES, "any_two_cards means the player can"
            + " double with any two cards in hand. nine_ten_eleven and ten_eleven restrict doubling"
            + " to those hand totals only (also with only two cards in hand). Hard only means the"
            + " player can't double if she has an ace in hand.").withOptionalArg()
            .defaultsTo("ANY_TWO_CARDS").describedAs(
            "any_two_cards, nine_ten, nine_ten_eleven,"
            + " hard_only");
    //Same comment re: enum parsing as Surrender option
    parser.acceptsAll(Arrays.asList(Options.DOUBLE_AFTER_SPLIT),
            "The player is allowed to double"
            + " after splitting.").withOptionalArg().ofType(Boolean.class).defaultsTo(
            true);
    return parser;
}

/**
 *
 * @return True if debugging is enabled, false otherwise
 * @deprecated Use asserts for doing assertions; for logging, a separate
 * set-up is to be created; or throw exceptions if it's called for. Or split up functions
 * to allow for better testing
 */
public static boolean debug() {
    if (DEBUGGING) {
        return true;
    }
    else {
        return false;
    }
}

}
