package beta1.collafis.util;

import java.io.Serializable;

import java.util.Map;

public class GoogleChart implements Serializable {

    private String xAxisLabel, yAxisLabel, title = null;
    private Map<? extends Object, Integer> data;
    private boolean xString = true, yString = true;
//  private long width = 0, height = 0;

    public GoogleChart(){}

    public GoogleChart(Map<? extends Object, Integer> data, String xAxisLabel, String yAxisLabel) {
        this.data = data;
        this.xAxisLabel = xAxisLabel;
        this.yAxisLabel = yAxisLabel;
    }

    public GoogleChart xIsNum() {
        xString = false;
        return this;
    }

    public GoogleChart yIsNum() {
        yString = false;
        return this;
    }

    public GoogleChart setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getData() {

        StringBuilder ret = new StringBuilder();

        // Begin of literal
        ret.append("{");

        // add columns
        ret.append("cols: [");

        if (xString)
            ret.append("{label: '"+xAxisLabel+"', type:'string'}");
        else
            ret.append("{label: '"+xAxisLabel+"', type:'number'}");

        ret.append(",");

        if (yString)
            ret.append("{label: '"+yAxisLabel+"', type:'string'}");
        else
            ret.append("{label: '"+yAxisLabel+"', type:'number'}");

        ret.append("],");

        // add values
        ret.append("rows: [");
        boolean first = true;
        for(Map.Entry<?  extends Object, Integer> entry: data.entrySet() ){

            if (!first) {
                ret.append(",");;
            }
                
            if (xString) // the difference is in the single quotes surounding the variable
                ret.append("{c: [{v: '"+entry.getKey().toString()+"'}, {v: "+entry.getValue()+"}]}");
            
            first = false;
        }
        
        ret.append("]}");

        return ret.toString();
/*        
        return "{"
             + "cols: [{label: 'Employee Name', type: 'string'},"
             +        "{label: 'Start Date', type: 'date'}],"
             + "rows: [{c:[{v: 'Mike'}, {v: new Date(2008, 1, 28), f:'February 28, 2008'}]},"
             +        "{c:[{v: 'Bob'}, {v: new Date(2007, 5, 1)}]},"
             +        "{c:[{v: 'Alice'}, {v: new Date(2006, 7, 16)}]},"
             +        "{c:[{v: 'Frank'}, {v: new Date(2007, 11, 28)}]},"
             +        "{c:[{v: 'Floyd'}, {v: new Date(2005, 3, 13)}]},"
             +        "{c:[{v: 'Fritz'}, {v: new Date(2011, 6, 1)}]}]}";
*/
        
    }

    public String getOptions() {
        
        StringBuilder ret = new StringBuilder();
        
        ret.append("{");

//        ret.append("'legend': 'none',");
        if (title != null) 
            ret.append("'title': '"+title+"'");
        
        ret.append("}"); 
        
        return ret.toString();
/*        
        return "{"
             + "width: 800, height: 240,"
             + "}";
*/
    }

}
