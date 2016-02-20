package ramblingharmonicas.blackjack;

public class ShuffleNeededException extends RuntimeException {
   private static final long serialVersionUID = -3825323107980632817L;
public ShuffleNeededException() {}

public ShuffleNeededException(String msg) {
   super(msg);
}

}
