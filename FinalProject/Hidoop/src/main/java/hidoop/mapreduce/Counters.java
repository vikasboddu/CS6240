package hidoop.mapreduce;

import hidoop.mapreduce.counters.CounterGroup;

/**
 * Created by jon on 4/12/16.
 */
public class Counters {
    private CounterGroup cg;
    public Counters(){
        this.cg = new CounterGroup();
    }
    public CounterGroup getGroup(String groupName) {
        return this.cg;
    }
}