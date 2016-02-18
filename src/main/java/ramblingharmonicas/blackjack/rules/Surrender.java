package blackjack.rules;

public enum Surrender { EARLY, LATE, NONE, EARLY_EXCLUDING_ACES;} 
//TODO: Use this in theRules and everywhere else; the performance implications are very negligible.
//TODO: Stick Rules.java and DoubleRules.java in this package