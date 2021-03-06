package hidoop.mapreduce.lib.map;

import hidoop.mapreduce.MapContext;
import hidoop.mapreduce.Mapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;

// Author: Jun Cai
// Reference: github.com/apache/hadoop
public class WrappedMapper<KEYIN, VALUEIN, KEYOUT, VALUEOUT>
        extends Mapper<KEYIN, VALUEIN, KEYOUT, VALUEOUT> {
	
    public Mapper<KEYIN, VALUEIN, KEYOUT, VALUEOUT>.Context
    getMapContext(MapContext<KEYIN, VALUEIN, KEYOUT, VALUEOUT> mapContext) {
        return new Context(mapContext);
    }

    public class Context
            extends Mapper<KEYIN, VALUEIN, KEYOUT, VALUEOUT>.Context {

        protected MapContext<KEYIN, VALUEIN, KEYOUT, VALUEOUT> mapContext;

        public Context(MapContext<KEYIN, VALUEIN, KEYOUT, VALUEOUT> mapContext) {
            this.mapContext = mapContext;
        }

        @Override
        public KEYIN getCurrentKey() throws IOException, InterruptedException {
            return mapContext.getCurrentKey();
        }

        @Override
        public VALUEIN getCurrentValue() throws IOException, InterruptedException {
            return mapContext.getCurrentValue();
        }

        @Override
        public boolean nextKeyValue() throws IOException, InterruptedException {
            return mapContext.nextKeyValue();
        }

        @Override
        public void write(KEYOUT key, VALUEOUT value) throws IOException,
                InterruptedException {
            mapContext.write(key, value);
        }

        @Override
        public void close() {
            mapContext.close();
        }
        @Override
        public long getCounterValue(){
            return mapContext.getCounterValue();
        }

        @Override
        public Map<Integer, List<String>> getOutputBuffer() {
            return mapContext.getOutputBuffer();
        }
    }
}
