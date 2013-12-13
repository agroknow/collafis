package beta1.collafis.dataset;

import java.util.*;

import java.io.Serializable;
import java.io.Writer;
import java.io.IOException;
import java.io.OutputStream;

import org.springframework.binding.message.MessageBuilder;

import org.springframework.webflow.execution.RequestContext;
import org.springframework.webflow.action.MultiAction;
import org.springframework.webflow.execution.Event;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.webflow.context.servlet.ServletExternalContext;
import org.springframework.webflow.context.ExternalContext;

import beta1.collafis.entry.Entry;
import beta1.collafis.util.GoogleChart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatasetManager extends MultiAction implements Serializable {
    
    private static final Logger log = LoggerFactory.getLogger(DatasetManager.class);

    private List<String> chosenDimensions = new ArrayList<String>();

    private Dataset dataset;

    public Dataset getDataset() {
        return dataset;
    }

    public List<String> getChosenDimensions() {
        return chosenDimensions;
    }

    private void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }

    private void addChosenDimension(String dimension) {
        chosenDimensions.add(dimension);
    }
  
    public Event getDataset(RequestContext context) {
        log.info("* getDataset");

        //Clean up: remove the dataset list from the context
        context.getFlowScope().remove("datasets");
                
        try {
            Long id = (Long)context.getViewScope().get("chosenDatasets");
            Dataset dataset = Dataset.findDataset(id);
        
            dataset.getRatings().size();

            setDataset(dataset);
//          log.info("+ Dataset toString "+ dataset.toString() );
            dataset.clear();
            
        } catch (Exception e) {
            context.getMessageContext().addMessage(
                    new MessageBuilder().error().defaultText("Error loading chosen dataset:"+ e.getMessage() ).build());
            return error();
        }

        return success();
    }
    
    public Event findAllDatasets(RequestContext context) {
        log.info("* findAllDataset");
        try {
            context.getFlowScope().put("datasets", Dataset.findAllDatasets() );
        } catch (Exception e) {
            context.getMessageContext().addMessage(
                    new MessageBuilder().error().defaultText("Error for chosen dataset:"+ e.getMessage() ).build());
            return error();
        }
        return success();
    }

 
    public Event findAllDimensions(RequestContext context) {
        log.info("* findAllDataset");
        try {
            context.getViewScope().put("dimensions", dataset.getDimensions() );
            for (String dim : dataset.getDimensions() ) 
                log.info("+"+ dim);
         } catch (Exception e) {
            context.getMessageContext().addMessage(
                    new MessageBuilder().error().defaultText("Error for finding dimensions:"+ e.getMessage() ).build());
            return error();
        } 
        return success();
    }


    public Event generateStatistics(RequestContext context) {
        log.info("* generateStatistics");
    
        String dimensions_str = (String)context.getFlowScope().remove("chosenDimensions");
        log.info("+ dimensions "+dimensions_str);
       
        // The key is the dimension, the values are pairs of metric:value
        //LinkedHashMap<String, Map<String, Number>> results = new LinkedHashMap<String, Map<String, Number>>();
        
        // The key is the metric, the values are the measurments for the each dimension
        LinkedHashMap<String, List<Number>> results= new LinkedHashMap<String, List<Number>>();
        
        String[] dimensions = dimensions_str.split(",");
        for( String dim : dimensions ) {
           addChosenDimension(dim);

          for(Map.Entry<String, Number> entry: dataset.getStatistics(dim).entrySet() ) {
          
            if( results.containsKey(entry.getKey()) ) {
                results.get( entry.getKey() ).add( entry.getValue() );
            } else {
                List measurments =  new ArrayList<Number>();
                measurments.add( entry.getValue() );
                
                results.put( entry.getKey(), measurments );
            }
          }
        }

        context.getViewScope().put("dimensions", dimensions );
        context.getViewScope().put("results", results );

/*
        Map<String, Number> data = new LinkedHashMap<String,Number>();
        data.put("Mike1", 10);
        data.put("Mike2", 20);
        data.put("Mike3", 30);
        data.put("Mike4", 40);
*/
        List<GoogleChart> charts = new ArrayList<GoogleChart>();

        try {
            for (String dimension : dimensions) {
                GoogleChart aGraph1 = new GoogleChart(dataset.userRatingFrequency(dimension), "#users", "#ratings");
                aGraph1.setTitle(dimension+": User-Rating Frequency Distribution");
                aGraph1.yIsNum();
                charts.add(aGraph1);
    
                GoogleChart aGraph2 = new GoogleChart(dataset.itemRatingFrequency(dimension), "#items", "#ratings");
                aGraph2.setTitle(dimension+": Item-Rating Frequency Distribution");
                aGraph2.yIsNum();
                charts.add(aGraph2);
    
                GoogleChart aGraph3 = new GoogleChart(dataset.ratingUserFrequency(dimension), "#ratings", "#users");
                aGraph3.setTitle(dimension+": Rating-User Frequency Distribution"); 
                aGraph3.yIsNum();
                charts.add(aGraph3);
    
                GoogleChart aGraph4 = new GoogleChart(dataset.ratingItemFrequency(dimension), "#ratings", "#items");
                aGraph4.setTitle(dimension+": Rating-Item Frequency Distribution"); 
                aGraph4.yIsNum();
                charts.add(aGraph4);
                
                GoogleChart aGraph5 = new GoogleChart(dataset.ratingUserFrequencyGrouped(dimension), "#ratings", "#users" );
                aGraph5.setTitle(dimension+": Rating-User Frequency Distribution Grouped"); 
                aGraph5.yIsNum();
                charts.add(aGraph5);
            
                GoogleChart aGraph6 = new GoogleChart(dataset.ratingItemFrequencyGrouped(dimension), "#ratings", "#items" );
                aGraph6.setTitle(dimension+": Rating-Item Frequency Distribution Grouped"); 
                aGraph6.yIsNum();
                charts.add(aGraph6); 
            
            }
        } catch (Exception ex) { 
            context.getMessageContext().addMessage(
                    new MessageBuilder().error().defaultText("Error in graph construction:"+ ex.getMessage() ).build());
            return error();
        }
        
        context.getViewScope().put("charts", charts); 
 
        return success();
    }

    public Event downloadStatistics(RequestContext context) {
        log.info("* downloadStatistics");
        
        ServletExternalContext externalContext = (ServletExternalContext) context.getExternalContext();
        //HttpServletRequest request = (HttpServletRequest) externalContext.getRequest();
       //HttpServletRequest request = ((ServletExternalContext)context.getExternalContext()).getRequest();
       
        HttpServletResponse response = (HttpServletResponse) context
                                       .getExternalContext().getNativeResponse();
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"dataset-statistics.csv\"");

        Writer stream = externalContext.getResponseWriter();
        //OutputStream out = stream.getOutputStream()

        LinkedHashMap<String, List<Number>> results = (LinkedHashMap<String, List<Number>>)context.getFlowScope().get("results");
        String[] dimensions = (String[])context.getFlowScope().get("dimensions");

        try {
            stream.append("dimension");
      
            for ( String metric : results.keySet() ) {
                stream.append(","+metric);
            }
            stream.append("\n");
            
            for(int i = 0; i < dimensions.length; i++) {
            
                stream.append(dimensions[i]);
                for (List<Number> measurment: results.values() ) {
                    stream.append( ","+measurment.get(i) );
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

}
