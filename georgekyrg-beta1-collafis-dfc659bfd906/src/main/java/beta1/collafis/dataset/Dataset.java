package beta1.collafis.dataset;

import java.lang.StringBuilder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.TreeMap;
import java.util.Comparator;

import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.DriverManager;

import org.grouplens.lenskit.data.pref.Preference;
import org.grouplens.lenskit.data.event.Rating;

import javax.persistence.CascadeType;
import javax.persistence.FetchType;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Column;
import javax.validation.constraints.NotNull;

import org.springframework.stereotype.Component;

import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.jpa.activerecord.RooJpaActiveRecord;
import org.springframework.roo.addon.tostring.RooToString;
import org.springframework.roo.addon.serializable.RooSerializable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;

import beta1.collafis.entry.Entry;
import beta1.collafis.util.Grouping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@RooSerializable
@RooJavaBean
@RooToString
@RooJpaActiveRecord(inheritanceType = "TABLE_PER_CLASS")
public abstract class Dataset {

    @NotNull
    private String name;

    private long mUsers;

    private long mItems;

    private long mRatings;

    @Column(columnDefinition = "LONGBLOB") 
    private Grouping grouping;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "dataset")
    private Set<Entry> ratings = new HashSet<Entry>();

    private String[] groups;
    private String[] dimensions; 

	private enum Principal { USER, ITEM };
	private enum GroupMethod { SINGLE, LINEAR, EXPONENTIAL };
    private enum GroupBy { PRINCIPAL, RATING };

	private HashMap<Integer, Double> mMean         = new HashMap<Integer, Double>();
	private HashMap<Integer, Double> mMedian       = new HashMap<Integer, Double>();
	private HashMap<Integer, Double> mVariance     = new HashMap<Integer, Double>();
	private HashMap<Integer, Double> mItemGini     = new HashMap<Integer, Double>();
	private HashMap<Integer, Double> mUserGini     = new HashMap<Integer, Double>();
	private HashMap<Integer, Double> mItemSkewness = new HashMap<Integer, Double>();
	private HashMap<Integer, Double> mUserSkewness = new HashMap<Integer, Double>();

	private static final int UNINITIALIZED = -1;
    private static final Logger log = LoggerFactory.getLogger(MultiCriteriaDataset.class);
 
    public void add(Entry entry) {
        
        assert entry != null;
        assert this.ratings != null; 

        this.ratings.add(entry);
    }
    
    private Integer dimensionToInteger(String dimension) {
        Integer index = 0;
        while (index < dimensions.length) {
            if (dimension.equals(dimensions[index]))
                break;
            index++;
        }
        return index;
    }
   
   public Map<String, Number> getStatistics(String dimension) {
        System.out.println("* getStatistics("+dimension+"), size:"+ ratings.size());
        
        Integer index = 0;
        while (index < dimensions.length) {
            if (dimension.equals(dimensions[index]))
                break;
            index++;
        }
        return getStatistics(index);
    }
   
    protected Map<String, Number> getStatistics(Integer dim) {
        System.out.println("* getStatistics("+dim+")");
        
        Map<String, Number> results = new LinkedHashMap<String, Number>();

        results.put("users",    users().size());
        results.put("items",    items().size());
        results.put("ratings",  ratings() );
        results.put("mean",     arithmeticMean(dim) );
        results.put("variance", variance(dim) );
        results.put("user Skewness", userSkewness(dim) );
        results.put("item Skewness", itemSkewness(dim) );
        results.put("user Gini",     userGini(dim) );
        results.put("item Gini",     itemSkewness(dim) );
        results.put("min items rated by user", minItemsRatedByUser(dim));
        results.put("max items rated by user", maxItemsRatedByUser(dim));
        results.put("average items rated by user", avgItemsRatedByUser(dim));
        results.put("min rating of item", minRatingsOfItem(dim));
        results.put("max rating of item", maxRatingsOfItem(dim));
        results.put("average rating of item", avgRatingsOfItem(dim));

        return results;
    }
 
   public Collection<? extends Rating> getRatings(String dimension) {
        System.out.println("* getRatings("+dimension+"), size:"+ ratings.size());
        
        Integer index = 0;
        while (index < dimensions.length) {
            if (dimension.equals(dimensions[index]))
                break;
            index++;
        }
        return getRatings(index);
    }
   

//    @Cachable("getRatings")
    public Collection<? extends Rating> getRatings(int dimension) {
        // Get a Collection of the ratings of one dimension
        List<Entry> single_rating = new ArrayList<Entry>();
        for (Entry entry: ratings) { 
            entry.setDefaultDim(dimension); 
            single_rating.add(entry);
        }
        return single_rating;
    }
   
    /********************************
     * METHODS COMPUTING STATISTICS *
     ********************************/
    
 	public Set<Long> items() {
		Set<Long> items = new HashSet<Long>();
		
		for (Entry entry: ratings) 
			items.add(entry.getItemId());

		mItems = new Long(items.size());
		return items;
	}

	public Set<Long> users() {
		Set<Long> users = new HashSet<Long>();
		
		for (Entry entry: ratings) 
			users.add(entry.getUserId());

		mUsers = new Long(users.size());
		return users;
	}

	public Long ratings() {
		mRatings = new Long(ratings.size());
		return mRatings;
	}

    public double arithmeticMean(Integer dim) {
		double sum = 0, arMean = 0; 
		for (Entry entry: ratings) 
			sum += entry.getRating(dim).getValue();
		
		arMean = sum/ratings.size();
		mMean.put(dim, arMean);
		return arMean;
	}

	public double variance(Integer dim) {
		double arMean, sum = 0.0, variance;
		
		if ( mMean.containsKey(dim) == false )
			arithmeticMean(dim);
		arMean = mMean.get(dim);

		for (Entry entry: ratings) {
			double value = entry.getRating(dim).getValue();
			sum += (value - arMean)*(value - arMean);
		}
		
		variance = sum / ratings.size();
		mVariance.put(dim, variance);
		return variance;

	}

/*
	public Hashtable<String, Double> arithmeticMeans() {
		for (String dim: dimensions)
			arithmeticMean(dim);
		return mMean;
	}
*/

	public double skewness(Integer dim, Principal principal) {
		
		double sum = 0, sumA = 0, sumB = 0, value, skew, n;
		double arithmetic_mean = 0.0;

		/* compute user/item popularity */
		HashMap<Long, Integer> elementPopularity = new HashMap<Long, Integer>();
		for (Entry entry: ratings) {
			int count = 0;
			int increment = (entry.getRating(dim).getValue() == 0) ? 0 : 1;
	
			Long key = (principal == Principal.USER) ? entry.getUserId() : entry.getItemId();

			/* choose whether we count item popularity or user popularity */
			if ( elementPopularity.containsKey(key) )
				count = elementPopularity.remove(key);
			elementPopularity.put(key, count + increment);	
			
		}

		/* compute arithmetic mean item popularity */
		for ( Integer votes: elementPopularity.values() )
			sum += votes;

		n = elementPopularity.size();
		arithmetic_mean = sum/n;

		for ( Integer v : elementPopularity.values() ){
			sumA += Math.pow(v - arithmetic_mean, 3);
			sumB += Math.pow(v - arithmetic_mean, 2);
		}

		skew = (sumA / n) / Math.pow(sumB/n, 3/2);	
		return skew;			
	}

	public double itemSkewness(Integer dim) {
		double skew = skewness(dim, Principal.ITEM);
		mItemSkewness.put(dim, skew);
		return skew;
	}

	public double userSkewness(Integer dim) {
		double skew = skewness(dim, Principal.USER);
		mUserSkewness.put(dim, skew);
		return skew;
	}

	public Double gini(Integer dim, Principal principal) {
		/** Compute the Gini coefficient for a specific dimension */
		Double arithmetic_mean, n, total = 0.0, sum = 0.0;

		if ( mItems == UNINITIALIZED ) 
			items();
		
		if ( mRatings == UNINITIALIZED )
			ratings();
		
		HashMap<Long, Integer> elementPopularity = new HashMap<Long, Integer>();

		for (Entry entry: ratings) {
			int count = 0, increment;

			increment = (entry.getRating(dim).getValue() == 0) ? 0 : 1;

			/* choose whether we count item popularity or user popularity */
			Long key = (principal == Principal.USER) ? entry.getUserId() : entry.getItemId();

			if ( elementPopularity.containsKey(key) )
				count = elementPopularity.remove(key);
			elementPopularity.put(key, count + increment);	
		}

		

       //Transfer as List and sort it
       ArrayList<Map.Entry<Long, Integer>> l = new ArrayList<Map.Entry<Long, Integer>>(elementPopularity.entrySet());
       Collections.sort(l, new Comparator<Map.Entry<Long, Integer>>(){

         public int compare(Map.Entry<Long, Integer> o1, Map.Entry<Long, Integer> o2) {
            return o1.getValue().compareTo(o2.getValue());
        }});

		

		int i = 1;
		double sumA = 0.0, sumB = 0.0;
		n = (double)l.size();	
		for ( Map.Entry<Long, Integer> entry: l ) {
			sumA += (n + 1 -i) * entry.getValue();
			sumB += entry.getValue(); 
			i++;		
		}

		return (1/(n-1)) * (n + 1 - 2*(sumA/sumB) );
	}

	public Double userGini(Integer dim) {
		double g = gini(dim, Principal.USER);
		mItemGini.put(dim, g);
		return g;
	}

	public Double itemGini(Integer dim) {
		double g = gini(dim, Principal.ITEM);
		mItemGini.put(dim, g);
		return g;
	}


	public Integer maxItemsRatedByUser(Integer dim) {
		HashMap<Long, Integer> ratings_per_user = new HashMap<Long, Integer>();
		for (Entry entry: ratings) {
			Long key = entry.getUserId();
			int increment = (entry.getRating(dim).getValue() == 0) ? 0 : 1;
			int count = 0;

			if ( ratings_per_user.containsKey(key) ) 
				count = ratings_per_user.remove(key);
			ratings_per_user.put(key, count + increment);	
		}

		return Collections.max( ratings_per_user.values() );
	}

	public Integer minItemsRatedByUser(Integer dim) {

		HashMap<Long, Integer> ratings_per_user = new HashMap<Long, Integer>();
		for (Entry entry: ratings) {
			Long key = entry.getUserId();
			int increment = (entry.getRating(dim).getValue() == 0) ? 0 : 1;
			int count = 0;

			if ( ratings_per_user.containsKey(key) ) 
				count = ratings_per_user.remove(key);
			ratings_per_user.put(key, count + increment);	
		}
		return Collections.min( ratings_per_user.values() );
	}

	public Double avgItemsRatedByUser(Integer dim) {

		HashMap<Long, Integer> ratings_per_user = new HashMap<Long, Integer>();
		for (Entry entry: ratings) {
			Long key = entry.getUserId();
			int increment = (entry.getRating(dim).getValue() == 0) ? 0 : 1;
			int count = 0;

			if ( ratings_per_user.containsKey(key) ) 
				count = ratings_per_user.remove(key);
			ratings_per_user.put(key, count + increment);	
		}

		return average( ratings_per_user.values() );
	}

	public Integer maxRatingsOfItem(Integer dim) {
		HashMap<Long, Integer> ratings_per_item = new HashMap<Long, Integer>();
		for (Entry entry: ratings) {
			Long key = entry.getItemId();
			int increment = (entry.getRating(dim).getValue() == 0L) ? 0 : 1;
			int count = 0;

			if ( ratings_per_item.containsKey(key) ) 
				count = ratings_per_item.remove(key);
			ratings_per_item.put(key, count + increment);	
		}
		return Collections.max( ratings_per_item.values() );
	}

	public Integer minRatingsOfItem(Integer dim) {

		HashMap<Long, Integer> ratings_per_item = new HashMap<Long, Integer>();
		for (Entry entry: ratings) {
			Long key = entry.getItemId();
			int increment = (entry.getRating(dim).getValue() == 0) ? 0 : 1;
			int count = 0;

			if ( ratings_per_item.containsKey(key) ) 
				count = ratings_per_item.remove(key);
			ratings_per_item.put(key, count + increment);	
		}
		return Collections.min( ratings_per_item.values() );
	}

	public Double avgRatingsOfItem(Integer dim) {

		HashMap<Long, Integer> ratings_per_item = new HashMap<Long, Integer>();
		for (Entry entry: ratings) {
			Long key = entry.getItemId();
			int increment = (entry.getRating(dim).getValue() == 0) ? 0 : 1;
			int count = 0;

			if ( ratings_per_item.containsKey(key) ) 
				count = ratings_per_item.remove(key);
			ratings_per_item.put(key, count + increment);	
		}
		
		return average( ratings_per_item.values() );
	}

	public Double average(Collection<Integer> values) {
		double sum = 0.0;
		for (Integer value : values) 
 			sum += value;
		
		return sum/values.size();
	}

	public Map<?, Integer> ratingFrequency(Integer dim, Principal principal, GroupBy groupBy) {
		/** Compute the distibution of the user/resource ratings for one dimension */	
		log.info("* ratingFrequency("+dim+","+principal+")");
        
        // Keep a hash where the key is the user and the value is the number of
		// starred items.
		HashMap<Long, Integer> principal_ratings = new HashMap<Long, Integer>();
		Map<Integer, Integer> temp = new TreeMap<Integer, Integer>();
		
		// Keep a hash table where the key is the number of ratings and the
		// value is the number of users who have given so many ratings.
		Map<Integer, Integer> rating_distribution = new TreeMap<Integer, Integer>();


		for (Entry entry: ratings) {
			int count = 0, increment;
            long key;

			increment = (entry.getRating(dim).getValue() == 0) ? 0 : 1;

			/* choose whether we count item popularity or user popularity */
			key = (principal == Principal.USER) ? entry.getUserId() : entry.getItemId();

			if ( principal_ratings.containsKey(key) )
				count = principal_ratings.remove(key);
			principal_ratings.put(key, count + increment);	
		}

		
		for ( Integer ratings: principal_ratings.values() ) {
			int count = 0;
			if ( temp.containsKey(ratings) )
				count = temp.remove(ratings);
			temp.put(ratings, count + 1);
		}
	
        log.info("temp: {}", temp);	

        if (groupBy == GroupBy.RATING)
            return temp;

//		for ( Map.Entry<Integer, Integer> entry: temp.entrySet() ) 
//			System.err.println(entry.getKey() +" "+ entry.getValue() );
		
		
		int value=0;
		for ( Map.Entry<Integer, Integer> entry: temp.entrySet() ) {
			
			int key = entry.getValue(), new_value = entry.getKey();
			int old_value;
			if ( rating_distribution.containsKey(key) ) {
				old_value = rating_distribution.get(key);
			
				if (old_value >= new_value) continue;
			}

			rating_distribution.put(key, new_value);
		}
        log.info("rating_distribution: {}", rating_distribution);
		return rating_distribution;

	}	

	public Map<?, Integer> userRatingFrequency(String dim) {
		return ratingFrequency(dimensionToInteger(dim), Principal.USER, GroupBy.PRINCIPAL);
	}

	public Map<?, Integer> itemRatingFrequency(String dim) {
		return ratingFrequency(dimensionToInteger(dim), Principal.ITEM, GroupBy.PRINCIPAL);
	}

	public Map<?, Integer> ratingUserFrequency(String dim) {
		return ratingFrequency(dimensionToInteger(dim), Principal.USER, GroupBy.RATING);
	}

	public Map<?, Integer> ratingItemFrequency(String dim) {
		return ratingFrequency(dimensionToInteger(dim), Principal.ITEM, GroupBy.RATING);
	}


	public Map<?, Integer> principalDistributionGrouped(Integer dim, Principal principal) throws Exception {
		/** Compute the number of users/resources having a number of ratings */	
		log.info("* principalDistributionGrouped("+dim+","+principal+")");
        
        // Keep a hash where the key is the user and the value is the number of
		// starred items. 
        HashMap<Long, Integer> principal_ratings = new HashMap<Long, Integer>();

        if (this.grouping == null){
            log.info("Illegal state exception: Undefined grouping ");
            throw new IllegalStateException("Undefined grouping");
		}
		// Keep a hash table where the key is the number of ratings and the
		// value is the number of users who have given so many ratings.

        int increment, count;
        long key;
		for (Entry entry: ratings) {
			count = 0;

			increment = (entry.getRating(dim).getValue() == 0) ? 0 : 1;

			/* choose whether we count item popularity or user popularity */
			key = (principal == Principal.USER) ? entry.getUserId() : entry.getItemId();;

			if ( principal_ratings.containsKey(key) )
				count = principal_ratings.remove(key);
			principal_ratings.put(key, count + increment);	
		}

        log.info("Principal rating populated");
        LinkedHashMap<String, Integer> ret = new LinkedHashMap<String, Integer>();
         
        // Initialize the hash map to set the iteration order 
        // and default value for all the groups
        for (Object group : grouping.getGroups()) 
            ret.put((String)group,0);
       
        String group;
        for (Map.Entry<Long, Integer> entry: principal_ratings.entrySet() ) {
            group = grouping.getGroup(entry.getValue());

            if (group == null) {
                log.error("No group for Entry:"+entry.getKey()+":"+entry.getValue() );
//                throw new Exception("No group for Entry:"+entry.getKey()+":"+entry.getValue() );
                continue;
            }

            count = 0;
            increment = 1;
            if ( ret.containsKey(group) )
				count = ret.get(group);
            ret.put(group, count+increment);
        }
        
        log.info("ret: {}", ret);

        return ret;
/*		
		switch ( grouping ) {
			case SINGLE:
				return singleGrouping(principal_ratings);
			case LINEAR:
				return linearGrouping(principal_ratings);
			default:
				throw new RuntimeException("Unsupported grouping "+ grouping);
		}
*/
	}	

    public Map<?, Integer> ratingUserFrequencyGrouped(String dim) throws Exception {
		return principalDistributionGrouped(dimensionToInteger(dim), Principal.USER);
	}

    public Map<?, Integer> ratingItemFrequencyGrouped(String dim) throws Exception {
		return principalDistributionGrouped(dimensionToInteger(dim), Principal.ITEM);
	}
    
    @Deprecated
	public Map<?, Integer> userRatingDistributionSingle(Integer dim) throws Exception {
		return principalDistributionGrouped(dim, Principal.USER);
	}
    
    @Deprecated
	public Map<?, Integer> itemRatingDistributionSingle(Integer dim) throws Exception {
		return principalDistributionGrouped(dim, Principal.ITEM);
	}

    @Deprecated
	public Map<?, Integer> userRatingDistributionLinear(Integer dim) throws Exception {
		return principalDistributionGrouped(dim, Principal.USER); 
	}

    @Deprecated
	public Map<?, Integer> itemRatingDistributionLinear(Integer dim) throws Exception {
		return principalDistributionGrouped(dim, Principal.ITEM);
	}

}
