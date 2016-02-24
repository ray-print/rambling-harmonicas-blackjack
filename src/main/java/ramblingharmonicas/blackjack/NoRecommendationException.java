package ramblingharmonicas.blackjack;

/**
 * Indicates a problem when solving a rule set or validate a solved rule set.
 */
public class NoRecommendationException extends Exception {
private static final long serialVersionUID = 1L;
private State myState;
private Rules theRules;
private Strategy myStrategy;

public NoRecommendationException() {}

public NoRecommendationException(Throwable t) {
	super(t);
}

public NoRecommendationException(String msg) {
	super(msg);
} 
public final void setRules(Rules theRules) {
    if (theRules != null) {
        this.theRules = new Rules(theRules);
    }
}

public NoRecommendationException(State aState, Rules theRules, Strategy myStrategy, String msg) {
   super(msg);
    if (aState != null) {
        this.myState = new State(aState);
    }
    this.myStrategy = myStrategy;
    setRules(theRules);
}

@Override
public String toString() {
   StringBuilder s = new StringBuilder(super.toString());
   if (theRules != null) {
      s.append("\n").append(theRules);
   }
   if (myState != null) {
      s.append("\n").append(myState);
   }
   if (myStrategy != null) {
	   s.append("\n").append(myStrategy);
   }
   return s.toString();

}

}
