package beta1.collafis.entry;

import beta1.collafis.dataset.Dataset;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Id;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.roo.addon.serializable.RooSerializable;
import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.jpa.activerecord.RooJpaActiveRecord;
import org.springframework.roo.addon.tostring.RooToString;

import org.grouplens.lenskit.data.pref.Preference;
import org.grouplens.lenskit.data.event.Rating;

import org.grouplens.lenskit.data.Event;
import org.grouplens.lenskit.data.event.Rating;


@RooSerializable
@RooJavaBean
@RooToString
@RooJpaActiveRecord(inheritanceType = "TABLE_PER_CLASS")
public abstract class Entry implements Rating {

    private static long currentId = 0;

//    @GeneratedValue(strategy = GenerationType.AUTO)
//    @Column(unique = true)
    @Id 
    @GeneratedValue(strategy = GenerationType.TABLE)
    @Column(name = "id")
    private Long eventId;

    private long timestamp;

    private long userId;

    private long itemId;

    private int dimensions;

    private static final Logger log = LoggerFactory.getLogger(Entry.class);

    private int defaultDim = 0;

    @ManyToOne
    private Dataset dataset;

    public Entry() {
        this.eventId = currentId;
        currentId++;
    }

    public long getId() {
        return eventId;    
    }

    public Preference getRating(int dimension) {
        throw new UnsupportedOperationException();
    }

    public abstract Preference getPreference();
    public abstract Rating copy(); 

    public Preference getRating(String dim) {
        throw new UnsupportedOperationException();
    }

        

/*
    public Entry merge() {
        System.out.println("* merge:"+this.eventId);        
        if (this.entityManager == null) this.entityManager = entityManager();
        Entry merged = this.entityManager.merge(this);
        return merged;
    }
*/

}
