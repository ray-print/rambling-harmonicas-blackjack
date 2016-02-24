package ramblingharmonicas.blackjack;

import org.junit.Test;

public class LegacyTest {

    @Test
    public void testAll() {
        //Runs all legacy tests. This can be destroyed after all tests have
        //been moved over to JUnit.
        Testers.callTests();
    }
}
