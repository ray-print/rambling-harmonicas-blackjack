package ramblingharmonicas.blackjack;

import java.io.Serializable;

/*
 * Any changes to these variables must be reflected in the rules hash key function
 * This is essentially an inner class, which I made external to allow easy serialization,
 * so it should only exist in the context of a Rules object and any changes to it should
 * be reflected in the Rules (see the note at the top of the Rules file).
 * It should not be serialized alone.
 */
public class DoubleRules implements Serializable {
private static final long serialVersionUID = 1L;

/**
 * Serializable requires a no-arg constructor
 *
 */
private DoubleRules() {
   this(true);
}

private boolean autoToggles;

DoubleRules(boolean autoToggles) {
   this.autoToggles = autoToggles;
}

/**
 * This function should only be called from the enclosing Rules instance.
 */
void setAutoToggles(boolean autoToggles) {
   this.autoToggles = autoToggles;
}

boolean getAutoToggles() {
   return autoToggles;
}

/**
 * If set to true, you can double with multiple cards in hand, at any time
 * it is legal to stand.
 *
 */
boolean alwaysPossible;

//TODO: Make all these private.
boolean notOnAces;
boolean onlyTenAndEleven;
boolean onlyNineTenEleven;
boolean notSplitAces;
boolean notPostSplit;
boolean anyTwoCards;

//Package-private because no tests have been done to ensure that it works
boolean alwaysPossible() {
   return alwaysPossible;
}

/**
 * Sets whether or not doubling is always possible.
 * (If set to true, you can double with multiple cards in hand, at any time
 * it is legal to stand.)
 * If true, this also toggles all other DoubleRules variables to false.
 */
void setAlwaysPossible(boolean alwaysPossible) {
   this.alwaysPossible = alwaysPossible;
   if (alwaysPossible && autoToggles) {
      notOnAces = onlyTenAndEleven = onlyNineTenEleven = notSplitAces = notPostSplit = anyTwoCards = false;
   }
}

public boolean notOnAces() {
   return notOnAces;
}

public void setNotOnAces(boolean notOnAces) {
   this.notOnAces = notOnAces;
   if (notOnAces && autoToggles) {
      notSplitAces = true;
      anyTwoCards = false;
      alwaysPossible = false;
   }
}

public boolean onlyTenAndEleven() {
   return onlyTenAndEleven;
}

public void setOnlyTenAndEleven(boolean onlyTenAndEleven) {
   this.onlyTenAndEleven = onlyTenAndEleven;
   if (onlyTenAndEleven && autoToggles) {
      alwaysPossible = anyTwoCards = onlyNineTenEleven = false;
   }
}

public boolean onlyNineTenEleven() {
   return onlyNineTenEleven;
}

public void setOnlyNineTenEleven(boolean onlyNineTenEleven) {
   this.onlyNineTenEleven = onlyNineTenEleven;
   if (onlyNineTenEleven && autoToggles) {
      alwaysPossible = anyTwoCards = onlyTenAndEleven = false;
   }
}

/**
 * TODO: Not yet saved in strategy files. Add tests to ensure this works. Once that's
 * done, add CLI support.
 */
boolean notSplitAces() {
   return notSplitAces;
}

void setNotSplitAces(boolean notSplitAces) {
   this.notSplitAces = notSplitAces;
   if (notSplitAces && autoToggles) {
      alwaysPossible = false;
   }
}

public boolean notPostSplit() {
   return notPostSplit;
}

public void setNotPostSplit(boolean notPostSplit) {
   this.notPostSplit = notPostSplit;
   if (notPostSplit && autoToggles) {
      //notSplitAces = true; This caused problems;
      //The relationship between these two variables is not remotely
      //symmetric.
      alwaysPossible = false;
   }
}

public boolean anyTwoCards() {
   return anyTwoCards;
}

/**
 * Double on any two cards, including aces.
 *
 */
public void setAnyTwoCards(boolean anyTwoCards) {
   this.anyTwoCards = anyTwoCards;
   if (anyTwoCards && autoToggles) {
      alwaysPossible = false;
      notOnAces = onlyTenAndEleven = onlyNineTenEleven = false;
   }
}

}
