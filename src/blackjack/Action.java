package blackjack;

/**
 * Possible player actions.
 * While INSURANCE is listed here, it is fundamentally different than other
 * actions and treated differently. For example, it is never listed in
 * the State's action list.
 * Action.ERROR indicates that that Action has not been initialized; it's
 * usually not used as an error condition.
 */
public enum Action {
HIT, STAND, DOUBLE, SURRENDER, INSURANCE, SPLIT, ERROR;

/**
 * (I wrote this function before I realized that Java enums automatically
 * disallow shallow clone behavior.)
 *
 * @param myAction
 * @return
 * @deprecated
 */
@Deprecated
static Action deepClone(Action myAction) {
   switch (myAction) {
      case HIT:
         return Action.HIT;
      case STAND:
         return Action.STAND;
      case DOUBLE:
         return Action.DOUBLE;
      case SURRENDER:
         return Action.SURRENDER;
      case INSURANCE:
         return Action.INSURANCE;
      case SPLIT:
         return Action.SPLIT;
      case ERROR:
         return Action.ERROR;
   }
   throw new RuntimeException(
           "Error in Action.deepclone(Action). Have a nice day.");
}

public char abbrev() {
   if (this == Action.SPLIT) {
      return 'Y';
   }
   if (this == Action.SURRENDER) {
      return 'R';
   }
   char q = toString().charAt(0);
   return q;
}

@Override
public String toString() {
   //only capitalize the first letter
   String s = super.toString();
   return s.substring(0, 1) + s.substring(1).toLowerCase();
}

}
