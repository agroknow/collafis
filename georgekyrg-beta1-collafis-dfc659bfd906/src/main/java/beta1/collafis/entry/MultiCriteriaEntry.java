package beta1.collafis.entry;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import java.lang.Double;

import org.grouplens.lenskit.data.pref.Preference;
import org.grouplens.lenskit.data.pref.SimplePreference;

import org.springframework.roo.addon.jpa.activerecord.RooJpaActiveRecord;
import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.tostring.RooToString;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@RooJavaBean
@RooToString
@RooJpaActiveRecord
public class MultiCriteriaEntry extends Entry {

    private Double[] preferences;
     
    private static final Logger log = LoggerFactory.getLogger(MultiCriteriaEntry.class);

/*
    public MultiCriteriaEntry(long user, long item, Double...preferences) {
        super(user, item, preferences.length);
    
        log.info("* MultiCriteriaEntry: "+ user +" "+ item);

        this.preferences = preferences;


    }
*/

    public Preference getRating(int dimension) {
//        log.debug("* getRating({})", dimension);
        try {
            return new SimplePreference(getUserId(), getItemId(), this.preferences[dimension]);
        } catch ( IndexOutOfBoundsException ex ) {
            throw new RuntimeException("Unsupported dimension:"+dimension);
        }
    }

    public Preference getPreference() {
        log.debug("* getPreference", this.preferences[getDefaultDim()] );
        return new SimplePreference(getUserId(), getItemId(), this.preferences[ getDefaultDim() ]);
    }


    public MultiCriteriaEntry copy() {
        MultiCriteriaEntry dup = new MultiCriteriaEntry();        
        dup.setPreferences(Arrays.copyOf(this.preferences, preferences.length) );
        dup.setEventId( getEventId() );
        dup.setUserId( getUserId() );
        dup.setItemId( getItemId() );
        dup.setDimensions( getDimensions() );
        dup.setDefaultDim( getDefaultDim() );
        dup.setDataset( getDataset() );
        return dup;
    }
/*
    public long getId() {
        return this.eventId;
    }
/*    
    public void setRating(int dimension, double rating) {
        try {
            this.preferences[dimension] = rating;
            //this.preferences.set(dimension, rating);
        } catch ( IndexOutOfBoundsException ex ) {
            throw new RuntimeException("Unsupported dimension:"+dimension);
        }
    }
*/
}
