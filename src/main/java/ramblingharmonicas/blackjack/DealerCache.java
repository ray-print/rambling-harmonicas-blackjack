package ramblingharmonicas.blackjack;

import java.util.HashMap;
import java.util.Map;

class DealerCache {
	enum Status {NO_CACHE(-1), SMALL_CACHE(30000), FULL_CACHE(300000);
		final private int mySize;
		private Status(final int size) {
			mySize = size;
		}
		public int getSize() {
			return mySize;
		}
	};
	
	private static Status cacheStatus = Status.FULL_CACHE;
	
	//TODO: Make this private. This requires refactoring code in resolveHands, which
	//shouldn't be done until all the tests are refactored.
	static Map<String, float[]> dealerProbabilitiesCache;
	private static long hits = 0;
	private static long misses = 0;
	private static boolean cacheInitialized = false;
	/**
	 * Call this before trying to access the cache. If the cache has already been
	 * initialized, this function will do nothing and return.
	 */
	public static void initCache() {
	   if (cacheInitialized) {
	      return;
	   }
	   dealerProbabilitiesCache = new HashMap<String, float []>( cacheStatus.getSize() );
	   cacheInitialized = true;
	}

	public static boolean incrementHits() {
	   hits++;
	   return true;
	}

	public static boolean incrementMisses() {
	   misses++;
       final int size = dealerProbabilitiesCache.size();
       if ( (size > 0) && (size % 100000) == 0) {
         printCacheStatus();
       }
	   return true;   
	}
	

	public static long getHits() {
	   return hits;
	}

	public static long getMisses() {
	   return misses;
	}

	/**
	 * Store values in the cache which go up to this far in the shoe (12 cards
	 * drawn)
	 *
	 */
	final public static int CACHE_DEPTH = 12;
	/**
	 * TODO: use enum for this
	 * @return Current kind of cache being used.
	 */
	public static Status getCacheStatus() {
	   return cacheStatus;
	}

	public static void setCache(DealerCache.Status cacheSize) {
	   cacheStatus = cacheSize;
	}

	/**
	 * Clears the dealer probability cache and reinitializes it.
	 *
	 *
	 */
	public static void clearCache() {
	   if (dealerProbabilitiesCache != null) {
	      dealerProbabilitiesCache.clear();
	   }
	   cacheInitialized = false;
	   initCache();
	}

	/**
	 * Prints the current dealer probability cache status: Size,
	 * misses/hit ratio, and how deep into the shoe the cache will venture.
	 *
	 *
	 */
	public static void printCacheStatus() {
	   initCache();
	   System.out.println("Current cache status--------------------------------------");
	   System.out.println("Size of cache: " + dealerProbabilitiesCache.size());
	   System.out.println("Cache depth: " + CACHE_DEPTH);
	   System.out.println("Misses: " + getMisses() + ". Hits: " + getHits() + ". Ratio (hits/total): " + (double) getHits() / (double) (getMisses() + getHits()));
	}

	/** TODO: Make private, encapsulate once tests are refactored.
	 * Creates a key for the dealer probability cache based on the shoe, rules, and
	 * dealer up card.
	 *
	 * This function should create a different key if any of the following change:
	 * Dealer up card
	 * Hit/stand on 17
	 * The shoe contents
	 * The presence or absence of a dealer hole card, but only if the dealer up card
	 * is an ace or ten.
	 * Accuracy of the rules
	 *
	 * @param myShoe The current shoe
	 * @param dealerCardIndex This is in the INDEX of a standard dealer card array.
	 * 0 = Ace. That cost hours.
	 * @param theRules The current rules.
	 * @return
	 */
	static String getKeyForMap(FastShoe myShoe, final int dealerCardIndex,
	        Rules theRules) {
	   StringBuilder builder = new StringBuilder();
	   if (theRules.hitOn17() == true) {
	      builder.append("H");
	   }
	   else {
	      builder.append("S");
	   }
	   builder.append(myShoe.myStringKey());
	   assert ((dealerCardIndex >= Blackjack.ACECARD) && 
	           (dealerCardIndex <= Blackjack.TENCARD));
	   builder.append(dealerCardIndex);   // 0-9.
	
	   /*
	    if (theRules.dealerHoleCard() )
	    building.append("H");
	    else building.append("N");
	    */
	
	   if ((dealerCardIndex == Blackjack.ACECARD)
	           || (dealerCardIndex == Blackjack.TENCARD)) {
	      if (theRules.dealerHoleCard()) {
	         builder.append("H");
	      }
	      else {
	         builder.append("N");
	      }
	   }
	   else {
	      builder.append("I"); //For irrelevant
	   }
	
	   builder.append(theRules.getAccuracy());
	//This doesn't appear to have an impact.
	
	   return builder.toString();
	}
}
