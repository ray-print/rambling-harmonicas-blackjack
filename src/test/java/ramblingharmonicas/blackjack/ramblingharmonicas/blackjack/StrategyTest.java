package ramblingharmonicas.blackjack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class StrategyTest {

private Rules theRules;
private Strategy myStrategy;
private Strategy.Skill skillLevel;

@Parameters
public static Collection<Object[]> data() {
    List data = new ArrayList<Object []>();
    Object [] dataSet;
    Rules theRules;

    theRules = new Rules(1);
    theRules.setAccuracy(Rules.CACHE_ACCURACY);
    dataSet = new Object[]{theRules, Strategy.Skill.COMP_DEP};
    data.add(dataSet);
    
    return data;
}

public StrategyTest(Rules theRules, Strategy.Skill skillLevel) {
    this.theRules = theRules;
    this.skillLevel = skillLevel;
    this.myStrategy = new Strategy(theRules, skillLevel);
}


@Test
public void testGetStrategyType() {
   assertEquals(skillLevel, myStrategy.getStrategyType());
}

}


