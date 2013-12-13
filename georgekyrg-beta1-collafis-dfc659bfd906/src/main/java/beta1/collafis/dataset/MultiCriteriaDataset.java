package beta1.collafis.dataset;

import java.io.*;

import java.util.Date;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.HashMap;
import java.util.Comparator;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Arrays;

import javax.persistence.EntityTransaction;

import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.jpa.activerecord.RooJpaActiveRecord;
import org.springframework.roo.addon.tostring.RooToString;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import beta1.collafis.entry.MultiCriteriaEntry;
import beta1.collafis.entry.Entry;
import beta1.collafis.util.Grouping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RooJavaBean
@RooToString
@RooJpaActiveRecord
public class MultiCriteriaDataset extends Dataset {

    private static final Logger log = LoggerFactory.getLogger(MultiCriteriaDataset.class);
/*
    public MultiCriteriaDataset() {
        super();
    }   
*/

    public void addFromTokens(String userId, String itemId, String[] tokens, String timestamp, Dataset dataset) {
        MultiCriteriaEntry entry = new MultiCriteriaEntry();
        entry.setUserId( Long.parseLong(userId) );
        entry.setItemId( Long.parseLong(itemId) );
        entry.setDataset( this );
        entry.setDimensions(tokens.length);

        Double[] ratings = new Double[tokens.length];
        for(int i = 0; i < tokens.length; i++) 
            ratings[i] = Double.valueOf(tokens[i]); 
        entry.setPreferences(ratings);

        entry.setTimestamp( Long.parseLong(itemId) );    
//        entry.persist();
    
        this.add(entry);
    }
 
    public void addFromTokens(String[] tokens) {
        MultiCriteriaEntry entry = new MultiCriteriaEntry();
        entry.setUserId( Long.parseLong(tokens[0]) );
        entry.setItemId( Long.parseLong(tokens[1]) );
        entry.setDataset( this );
        entry.setDimensions(tokens.length-2);

        Double[] ratings = new Double[tokens.length-2];
        for(int i = 2, j = 0; i < tokens.length; i++, j++) {
            ratings[j] = Double.valueOf(tokens[i]);
        }
        entry.setPreferences(ratings);

        Date now = new Date();
        entry.setTimestamp( now.getTime() );    
//        entry.persist();

        this.add(entry);
    }
 
    public void populateFromDatabase(ResultSet rs) throws Exception {

       // Get result set meta data
        ResultSetMetaData rsmd = rs.getMetaData();
        int numColumns = rsmd.getColumnCount();
       
        while (rs.next()) {
            MultiCriteriaEntry entry = new MultiCriteriaEntry();
            entry.setUserId( rs.getLong("userID") );
            entry.setItemId( rs.getLong("itemID") ); 
        
            Double[] ratings = new Double[numColumns-2];
            for(int i = 3, j = 0; i < numColumns+1; i++, j++) {
                ratings[j] = rs.getDouble(i);
            }
            entry.setPreferences(ratings);

            Date now = new Date();
            entry.setTimestamp( now.getTime() );    
    
            add(entry);
        } //end while
    }


    public static MultiCriteriaDataset LenskitFromCsvFile(String name, File file) throws FileNotFoundException {
        log.info("* LenskitFromCsvFile");
        
        MultiCriteriaDataset dataset = new MultiCriteriaDataset();
        dataset.setName(name);
        dataset.setDimensions(new String[] {"preference1"});
        dataset.setGroups(new String[]{"0-20", "21-40", "41-60","61-80", "81-80", "81-105"});        
        

        dataset.persist();

        BufferedReader in = new BufferedReader(new FileReader(file));
        String sCurrentLine;
        
        String delimiter = "::";
        try { 
            while ((sCurrentLine = in.readLine()) != null) {
                String[] tokens = sCurrentLine.split(delimiter);

                //System.out.println(tokens[0]);

                dataset.addFromTokens(tokens[0], tokens[1], 
                                      Arrays.copyOfRange(tokens, 3, tokens.length-1), tokens[tokens.length-1], dataset );
            } //while
        }  catch (IOException ex) {
            ex.printStackTrace();
        }

        dataset.persist();
        return dataset;
    }

    public static MultiCriteriaDataset MendeleyFromCsvFile(String name, File file) throws FileNotFoundException {
        log.info("* MendeleyFromCsvFile");
       
       
        MultiCriteriaDataset dataset = new MultiCriteriaDataset();
        dataset.setName(name);
        dataset.setDimensions(new String[] {"preference1", "preference2", "preference3"});
        dataset.setGroups(new String[]{"0-20", "21-40", "41-60","61-80", "81-80", "81-105"});        
    
        Grouping<Integer> grouping = new Grouping<Integer>();
        grouping.addGroup(0, 20, "0-20").addGroup(20, 40, "20-40")
                .addGroup(40, 60, "40-60").addGroup(60, 80, "60-80")
                .addGroup(80, 105, "80-105");

        dataset.setGrouping(grouping);
        dataset.persist();

//        EntityTransaction etx = dataset.entityManager.getTransaction();
//        etx.begin();
  

        BufferedReader in = new BufferedReader(new FileReader(file));
        String sCurrentLine;
        
        String delimiter = ",";
        int i = 1;
        try { 
            while ((sCurrentLine = in.readLine()) != null) {
                String[] tokens = sCurrentLine.split(delimiter);
                dataset.addFromTokens(tokens);     
                
//                if ( i%100 == 0 ) {
//                    dataset.entityManager.flush();
//                    dataset.entityManager.clear();
//                }
//                i++;
            
            } //while
        }  catch (IOException ex) {
            ex.printStackTrace();
        }

        for (Entry e: dataset.getRatings() ){
            System.out.println( e.getEventId() );
        }

//        dataset.storeRatings();

        dataset.persist();
//        etx.commit();
//        session.close();

        return dataset;
    }



/*
    public void addFromCsvFile(File file) throws FileNotFoundException {
        System.out.println("* populateFromCsvFile");
        
        // Populate from csv file, where the delimiter is , 
        String delimiter = ",";   

        BufferedReader in = new BufferedReader(new FileReader(file));
        String sCurrentLine;
        
        try { 
            while ((sCurrentLine = in.readLine()) != null) {
                String[] tokens = sCurrentLine.split(delimiter);
                
                Double[] ratings = new Double[tokens.length-2];
                
                // The first two elements are the userID and the ItemID
                for (int i = 2, j = 0; i < tokens.length; i++, j++) 
                    ratings[j] = Double.valueOf(tokens[i]) ;
                
                MultiCriteriaEntry  entry;
                try {
                    entry = new MultiCriteriaEntry(Long.parseLong(tokens[0]), Long.parseLong(tokens[1]), ratings);
                } catch (NumberFormatException ex) {
                    ex.printStackTrace();
                    continue;
                }

                //entry.setDataset(this);
                //entry.persist();
                add(entry);
            } //while
        }  catch (IOException ex) {
            ex.printStackTrace();
            
        }               
    }
*/

/* 
    protected TreeMap<?, Integer> linearGrouping( HashMap<Long, Integer> dist ) {

        final String[] groups = getGroups();
        TreeMap<String, Integer> ret = new TreeMap<String, Integer>(
                                        new Comparator<String>(){
                                            public int compare(String s1, String s2) {
                                                if ( s1.equals(s2) ) return 0;
                                                for (int i = 0; i < groups.length; i++) {
                                                    if ( s1.equals(groups[i]) ) return -1;
                                                    if ( s2.equals(groups[i]) )return 1;
                                                }
                                                throw new RuntimeException("We should never have reached this place");
                                            }
                                        });
        
        for ( Integer ratings: dist.values() ) {
            int count = 0;
            String key = "";

            if ( ratings >= 0 && ratings <= 20 ) {
                key = groups[0];
            } else if ( ratings <= 40 ) {
                key = groups[1];
            } else if ( ratings <= 60 ) {
                key = groups[2];
            } else if ( ratings <= 80 ) {
                key = groups[3];
            } else if ( ratings <= 100 ) {
                key = groups[4];
            } else {
                throw new RuntimeException("Value out of range");
            }    

            if ( ret.containsKey(key) )
                count = ret.remove(key);
            ret.put(key, count+1);
        }

        return ret;
    }
*/
/* 
    public Entry findEntryById(int ratingId){    
        for (Entry entry : trace) {
            if ( entry.get("ratingId") == ratingId ) {
//                System.err.println("Found:"+ratingId);
                return entry;
            }
        }
        return null; 
    }
*/
   
    public String toString() {
        return this.getName();
    }

}
