package beta1.collafis.recommend;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Writer;
import java.io.IOException;

import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;

import org.grouplens.lenskit.data.dao.DAOFactory;
import org.grouplens.lenskit.data.dao.EventCollectionDAO;
import org.grouplens.lenskit.data.dao.SimpleFileRatingDAO;

import org.grouplens.lenskit.RatingPredictor;
import org.grouplens.lenskit.core.LenskitRecommenderEngineFactory;

import org.grouplens.lenskit.baseline.BaselinePredictor;
import org.grouplens.lenskit.baseline.ItemMeanPredictor;

import org.grouplens.lenskit.knn.item.ItemItemRatingPredictor;
import org.grouplens.lenskit.knn.params.NeighborhoodSize;
import org.grouplens.lenskit.knn.user.UserUserRatingPredictor;

import org.grouplens.lenskit.transform.normalize.BaselineSubtractingUserVectorNormalizer;
import org.grouplens.lenskit.transform.normalize.MeanVarianceNormalizer;
import org.grouplens.lenskit.transform.normalize.UserVectorNormalizer;
import org.grouplens.lenskit.transform.normalize.VectorNormalizer;

import org.grouplens.lenskit.baseline.BaselinePredictor;
import org.grouplens.lenskit.baseline.ItemUserMeanPredictor;
import org.grouplens.lenskit.baseline.BaselineRatingPredictor;

import org.grouplens.lenskit.params.Damping;

import org.grouplens.lenskit.knn.user.SimpleNeighborhoodFinder;
import org.grouplens.lenskit.knn.user.UserUserRecommender;
import org.grouplens.lenskit.knn.user.UserUserRatingPredictor;

import org.grouplens.lenskit.RecommenderEngine;
import org.grouplens.lenskit.Recommender;
import org.grouplens.lenskit.ItemRecommender;

import org.grouplens.lenskit.collections.ScoredLongList;

import org.grouplens.lenskit.vectors.similarity.VectorSimilarity;
import org.grouplens.lenskit.vectors.similarity.CosineVectorSimilarity;
import org.grouplens.lenskit.vectors.similarity.PearsonCorrelation;
import org.grouplens.lenskit.vectors.similarity.SignificanceWeightedVectorSimilarity;
import org.grouplens.lenskit.vectors.similarity.SpearmanRankCorrelation;




import beta1.collafis.dataset.Dataset;
import beta1.collafis.dataset.DatasetManager;

import org.grouplens.lenskit.vectors.SparseVector;
import org.grouplens.lenskit.data.event.Rating;

import org.springframework.webflow.execution.RequestContext;
import org.springframework.webflow.action.MultiAction;
import org.springframework.webflow.execution.Event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Imports for analyze
import org.grouplens.lenskit.eval.data.crossfold.CrossfoldCommand;
import org.grouplens.lenskit.eval.data.GenericDataSource;
import org.grouplens.lenskit.data.pref.PreferenceDomain;
import org.grouplens.lenskit.eval.data.crossfold.RandomOrder;
import org.grouplens.lenskit.eval.traintest.TrainTestEvalCommand;
import org.grouplens.lenskit.eval.data.traintest.TTDataSet;

import org.grouplens.lenskit.eval.metrics.predict.CoveragePredictMetric;
import org.grouplens.lenskit.eval.metrics.predict.MAEPredictMetric;
import org.grouplens.lenskit.eval.metrics.predict.RMSEPredictMetric;
import org.grouplens.lenskit.eval.metrics.predict.NDCGPredictMetric;
import org.grouplens.lenskit.eval.metrics.predict.HLUtilityPredictMetric;

import org.grouplens.lenskit.eval.AlgorithmInstance;
import org.grouplens.lenskit.eval.util.table.TableImpl;

import org.springframework.binding.message.MessageBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.webflow.context.servlet.ServletExternalContext;
import org.springframework.webflow.context.ExternalContext;

public class RecommenderManager extends MultiAction implements Serializable {

//    private static final File GROUPLENS_DATAFILE = new File("src/main/resources/grouplens/ratings.dat");
    
    private static final Logger log = LoggerFactory.getLogger(Recommender.class);

    private static final String EVAL_DIR = "/home/gchinis/Development/roo/beta1-collafis/src/main/resources/lenskit_eval/";

    public void evaluate(Dataset dataset) throws Exception {
     
        log.info("* evaluate");
        log.info("Dataset name: "+dataset.getName());
     
        CrossfoldCommand crco = new CrossfoldCommand();

        DAOFactory daoFactory;
        daoFactory = new EventCollectionDAO.Factory( dataset.getRatings(0) );
         
        crco.setSource(new GenericDataSource("lenkit", daoFactory, new PreferenceDomain(1,5,1) ) );
        
        crco.setTest(EVAL_DIR+"test.%d.csv");
        crco.setTrain(EVAL_DIR+"train.%d.csv");
   
        crco.setOrder(new RandomOrder<Rating>() );
        crco.setHoldout(0.2);
        crco.setPartitions(3);
    
//        crco.call();
 
        TrainTestEvalCommand ttec = new TrainTestEvalCommand();    
        ttec.setOutput(new File(EVAL_DIR+"eval-results.csv"));
        ttec.setPredictOutput(new File(EVAL_DIR+"eval-preds.csv"));
        ttec.setUserOutput(new File(EVAL_DIR+"eval-user.csv"));

        ttec.addMetric(new CoveragePredictMetric());
        ttec.addMetric(new HLUtilityPredictMetric());
        ttec.addMetric(new MAEPredictMetric());
        ttec.addMetric(new RMSEPredictMetric());
        ttec.addMetric(new NDCGPredictMetric());

       
        for ( TTDataSet tt: crco.call() ) { 
            ttec.addDataset( tt ); 
        }
        
        LenskitRecommenderEngineFactory factory = new LenskitRecommenderEngineFactory();
        factory.bind(ItemRecommender.class).to(UserUserRecommender.class);
        factory.bind(RatingPredictor.class).to(UserUserRatingPredictor.class);
        // let's use item mean rating as the baseline predictor
        factory.bind(BaselinePredictor.class).to(ItemMeanPredictor.class);
        
        ttec.addAlgorithm(new AlgorithmInstance("UserUserCF", factory) );
            
        ttec.call();
        
        log.info("# evaluate");
    }

    public void recommend(Dataset dataset) throws Exception {
        log.info("* recommend Dataset name: "+dataset.getName());

        DAOFactory daoFactory;
/*
        DAOFactory base = new SimpleFileRatingDAO.Factory(GROUPLENS_DATAFILE, "::");
        // Reading directly from CSV files is slow, so we'll cache it in memory.
        // You can use SoftFactory here to allow ratings to be expunged and re-read
        // as memory limits demand. If you're using a database, just use it directly.
        daoFactory = EventCollectionDAO.Factory.wrap(base);
*/
        for(int dimension = 0; dimension < dataset.getDimensions().length; dimension++) {
            System.out.println("Dimension:"+dimension);
                   
            daoFactory = new EventCollectionDAO.Factory( dataset.getRatings(dimension) );

            LenskitRecommenderEngineFactory factory = new LenskitRecommenderEngineFactory(daoFactory);

            factory.bind(ItemRecommender.class).to(UserUserRecommender.class);
            factory.bind(RatingPredictor.class).to(UserUserRatingPredictor.class);

            // let's use item mean rating as the baseline predictor
            factory.bind(BaselinePredictor.class).to(ItemMeanPredictor.class);

            factory.set(NeighborhoodSize.class).to(2);
            RecommenderEngine engine = factory.create();
            /* get the and use the recommender */
            Recommender rec = engine.open();
            try {
                /* get ratings, predictions */
                ItemRecommender recommender = rec.getItemRecommender();
                assert recommender != null; // not null because we configured one
                // for users
                for (long user = 1; user < 20; user++) { 
                    // get 10 recommendation for the user
                    ScoredLongList recs = recommender.recommend(user, 10);
                    System.out.format("Recommendations for %d:\n", user);
                    for (long item: recs) {
                        System.out.format("\t%d\n", item);
                    }
                }
            } finally {
            rec.close();
            }
        }
    }

    public Event configureRecommender(RequestContext context) {
        log.info("* configureRecommender");
        
        ///TODO move the map in another position, maybe config file
        Map<String, String> vectorSimilarity = new HashMap<String, String>();
        vectorSimilarity.put("Cosine Similarity", "org.grouplens.lenskit.vectors.similarity.CosineVectorSimilarity");
        vectorSimilarity.put("Pearson Correlation", "org.grouplens.lenskit.vectors.similarity.PearsonCorrelation");
        vectorSimilarity.put("Significance Weight Similarity", "org.grouplens.lenskit.vectors.similarity.SignificanceWeightedVectorSimilarity");
        vectorSimilarity.put("Spearman Rank Correlation", "org.grouplens.lenskit.vectors.similarity.SpearmanRankCorrelation");

        try {
            //Dataset dataset = (Dataset)context.getFlowScope().get("dataset");
            context.getFlowScope().put("similarityFunction", vectorSimilarity );
         } catch (Exception e) {
            context.getMessageContext().addMessage(
                    new MessageBuilder().error().
                        defaultText("Error configuring recommender:"+ e.getMessage() ).build());
            return error();
        } 
        return success();
    }

    public Event evaluate(RequestContext context) {
        log.info("* evaluate");
        Integer neighborhoodSize =  (Integer)context.getFlowScope().get("neighborhoodSize");
        Integer partitions =  (Integer)context.getFlowScope().get("partitions");
        Double holdout =  (Double)context.getFlowScope().get("holdout");
        String similarityFunction =  (String)context.getFlowScope().get("similarityFunction");
 
        DatasetManager datasetManager = (DatasetManager)context.getFlowScope().get("datasetManager");
        Dataset dataset = datasetManager.getDataset();
        List<String> dimensions = datasetManager.getChosenDimensions();
   
        log.info("+"+neighborhoodSize);
        log.info("+"+partitions);
        log.info("+"+holdout);
        log.info("+"+similarityFunction);

        // The List<List<String> is the list of the lines
        // with the results. The List<String> is the list of tokens in a line.
        List<List<String>> output = new ArrayList<List<String>>();

        boolean metrics_line = false;
        for(String dim: dimensions) {
/*    
            System.out.println("Dimension:"+dim);
            for (Rating rating: dataset.getRatings(dim) ) {
                System.out.println(rating.getPreference().getUserId()+":"+ rating.getPreference().getItemId()+":"+rating.getPreference().getValue());
            }
*/            
            File results = null;
            BufferedReader br = null;
            try {

                if ( similarityFunction.indexOf("CosineVectorSimilarity") != -1 )
                    results = evaluate(neighborhoodSize, partitions, holdout, CosineVectorSimilarity.class, dataset.getRatings(dim), dim);
                else if ( similarityFunction.indexOf("PearsonCorrelation") != -1) 
                    results = evaluate(neighborhoodSize, partitions, holdout, PearsonCorrelation.class, dataset.getRatings(dim), dim);
                else if ( similarityFunction.indexOf("SignificanceWeightedVectorSimilarity") != -1 )
                    results =evaluate(neighborhoodSize, partitions, holdout, SignificanceWeightedVectorSimilarity.class, dataset.getRatings(dim), dim);
                else if ( similarityFunction.indexOf("SpearmanRankCorrelation") != -1 )
                    results = evaluate(neighborhoodSize, partitions, holdout, SpearmanRankCorrelation.class, dataset.getRatings(dim), dim);
            
            
                String sCurrentLine;
                List<List<String>> result_table = new ArrayList<List<String>>();

                br = new BufferedReader(new FileReader(results) );

                boolean  first_line = true;
                while ((sCurrentLine = br.readLine()) != null) {
                    //System.out.println(sCurrentLine);
                    // Parse each line into token with ',' delimeter
                    
                    List<String> result_line = new ArrayList<String>();
                    
                    // The first line of all results, so we print the names of the columns
                    if (first_line && !metrics_line) {
                        result_line.add("dimension");
                        result_line.addAll( Arrays.asList( sCurrentLine.split(",") ) );        
                        result_table.add( result_line );                              
                        first_line = false;
                        metrics_line = true;
                    
                    // The first line of consecutive results. Don't print names of columns
                    } else if (!first_line) {
                        result_line.add(dim);
                        result_line.addAll( Arrays.asList( sCurrentLine.split(",") ) );        
                        result_table.add( result_line );                              
                    } else if (first_line) {
                        first_line = false;
                    }
                    
                }
                output.addAll(result_table);

            } catch (Exception ex) {
                ex.printStackTrace();
                return error();
            }
      
        } //end for(String dim: dimensions) 

        context.getFlowScope().put( "evaluationResults", output );
 
    return success(); 
    }


   public File evaluate(Integer neighborhoodSize, Integer partitions, Double holdout, Class<? extends VectorSimilarity> similarityFunction, Collection<? extends Rating> dataset, String prefix) throws Exception {
     
        log.info("* evaluate nSize:"+ neighborhoodSize +" part:"+ partitions +" holdout:"+ holdout +" prefix:"+prefix);
     
        CrossfoldCommand crco = new CrossfoldCommand();

        DAOFactory daoFactory;
        daoFactory = new EventCollectionDAO.Factory( dataset );
         
        crco.setSource(new GenericDataSource("lenkit", daoFactory, new PreferenceDomain(1,5,1) ) );
        
        crco.setTest(EVAL_DIR+prefix+"_test.%d.csv");
        crco.setTrain(EVAL_DIR+prefix+"_train.%d.csv");
   
        crco.setOrder(new RandomOrder<Rating>() );
        crco.setHoldout(holdout.doubleValue());
        crco.setPartitions(partitions);
    
//        crco.call();
 
        TrainTestEvalCommand ttec = new TrainTestEvalCommand();    
        ttec.setOutput(new File(EVAL_DIR+prefix+"_eval-results.csv"));
        ttec.setPredictOutput(new File(EVAL_DIR+prefix+"_eval-preds.csv"));
        ttec.setUserOutput(new File(EVAL_DIR+prefix+"_eval-user.csv"));

        ttec.addMetric(new CoveragePredictMetric());
        ttec.addMetric(new HLUtilityPredictMetric());
        ttec.addMetric(new MAEPredictMetric());
        ttec.addMetric(new RMSEPredictMetric());
        ttec.addMetric(new NDCGPredictMetric());

       
        for ( TTDataSet tt: crco.call() ) { 
            ttec.addDataset( tt ); 
        }
        
        LenskitRecommenderEngineFactory factory = new LenskitRecommenderEngineFactory();
        factory.bind(ItemRecommender.class).to(UserUserRecommender.class);
        factory.bind(RatingPredictor.class).to(UserUserRatingPredictor.class);
        factory.bind(VectorSimilarity.class).to( similarityFunction );
        // let's use item mean rating as the baseline predictor
        factory.bind(BaselinePredictor.class).to(ItemMeanPredictor.class);
        
        ttec.addAlgorithm(new AlgorithmInstance("UserUserCF", factory) );
            
        ttec.call();
        
        log.info("# evaluate");
    
        return new File(EVAL_DIR+prefix+"_eval-results.csv");
    }

    public Event downloadEvaluation(RequestContext context) {
        log.info("* downloadEvaluation");
        
        List<List<String>> evaluationResults =  (List<List<String>>)context.getFlowScope().get("evaluationResults");

        ServletExternalContext externalContext = (ServletExternalContext) context.getExternalContext();
       
        HttpServletResponse response = (HttpServletResponse) context
                                       .getExternalContext().getNativeResponse();
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"recommender-evaluation.csv\"");

        Writer stream = externalContext.getResponseWriter();
        //OutputStream out = stream.getOutputStream()

        try {
        
            for(List<String> line: evaluationResults) {
                
                boolean first_token = true;
                for(String token: line) {
                    if (first_token){
                         stream.append(token);
                         first_token = false;
                    } else
                        stream.append(","+token);
                }
                stream.append("\n");
            }   

            stream.flush();
            stream.close();

            context.getExternalContext().recordResponseComplete();
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return error();
        }
        
        return success(); 
    }

    /**
     * Check if we are going to execute single or serial execution.
     */
    public Event isSingleExecution(RequestContext context) {
        String mode = (String)context.getFlowScope().get("executionMode");
        
        log.info("Mode: "+mode);
        switch(mode){
            case "single":
                log.info("Single Execution");
                return yes();
            case "serial":
                log.info("Serial Execution");
                return no();
            default:
                return error();
        }
    }
    
}
