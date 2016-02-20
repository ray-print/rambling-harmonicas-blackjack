package ramblingharmonicas.blackjack;
import ramblingharmonicas.blackjack.cards.*;
/**
 * Indicates an internal problem in the Strategy class.
 *
 */
public class NoRecommendationException extends Exception {
private static final long serialVersionUID = 1L;
private State myState;
private Rules theRules;
private boolean stateDefined = false, rulesDefined = false;

public NoRecommendationException() {
}

public NoRecommendationException(Throwable t) {
   super(t);
}

public final void setState(State aState) {
   if (aState != null) {
      this.myState = new State(aState);
      stateDefined = true;
   }
}

public final void setRules(Rules theRules) {
   if (theRules != null) {
      this.theRules = new Rules(theRules);
      rulesDefined = true;
   }
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
   return s.toString();

}

public NoRecommendationException(State aState, Rules theRules, String msg) {
   super(msg);
   setState(aState);
   setRules(theRules);

}

public NoRecommendationException(String msg) {
   super(msg);
}

}
