
package blackjack;

/**
 *
 * AFTER TESTING MAKE THIS SUBCLASS RUNTIME EXCEPTION
 */
public class ShuffleNeededException extends Exception
{

   public ShuffleNeededException()
   {
   }
   

   public ShuffleNeededException(String msg)
   {super(msg);
      
   }
}
