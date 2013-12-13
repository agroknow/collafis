package beta1.collafis.util;

import java.io.Serializable;

import java.util.List;
import java.util.ArrayList;

public class Grouping<T extends Comparable<T>> implements Serializable {

    List<Group<T>> groups = new ArrayList<Group<T>>(); 

    private class Group<T extends Comparable<T>> implements Serializable {
        private T start, end;
        private String label;

        Group (T start, T end, String label) {
            this.start = start;
            this.end = end;
            this.label = label;
        }

        boolean contains(T value) {
            if ( start.compareTo(value) <= 0 && end.compareTo(value) > 0 )
                return true;
            return false;
        }

        String getLabel() {
            return label;
        }

        public String toString(){
            return start.toString()+","+end.toString()+","+label;
        }
    }

    public Grouping addGroup(T start, T end, String label){
        groups.add( this.new Group(start, end, label));        
        return this;
    }

    public String getGroup(T value) {
        for(Group group: groups) {
            if(group.contains(value))
                return group.getLabel();
        }

        return null;
    }

    public List<String> getGroups() {
        List<String> ret = new ArrayList<String>();

        for (Group group: groups)
            ret.add(group.getLabel());
        
        return ret;
    }

    public String toString() {
        StringBuilder ret = new StringBuilder();
        for (Group group: groups) 
            ret.append(group.toString()+"\n");
        return ret.toString();
    }

}
