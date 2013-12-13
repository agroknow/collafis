package beta1.collafis.util;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;


import beta1.collafis.util.Grouping;

public class GroupingTest {
    private Grouping<Integer> grouping;

    @Before
    public void setUp() {
        grouping = new Grouping<Integer>();
        grouping.addGroup(0,2,"0-2");
        grouping.addGroup(2,4,"2-4");
        grouping.addGroup(8,16,"8-16");

    }

    @Test public void init() throws IOException {
      
        for(int i = 0; i < 30; i++) {
            int rand = (int) (20*Math.random());
        
            if (rand >= 0 && rand < 2)
                assertTrue( grouping.getGroup(rand).equals("0-2"));
            else if (rand >= 2 && rand < 4) 
                assertTrue( grouping.getGroup(rand).equals("2-4"));
            else if (rand >= 8 && rand < 16) 
                assertTrue(grouping.getGroup(rand).equals("8-16"));
            else
                assertNull( grouping.getGroup(rand)); 
        }
    }

/*
    @Test public void filter() {
        preds = Predictions.fromFile(sampleData); 
    
        Predictions preds1 = preds.filter(1);
        
        for(Object pred: preds1) {
            assertTrue(((Prediction)pred).getPartition() == 1L);
        }
    }
*/
}
