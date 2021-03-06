import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class OTPReducer extends Reducer<Text, Text, Text, DoubleWritable> {

    @Override
    public void reduce(Text key, Iterable<Text> values, Context context)
        throws IOException, InterruptedException {
        if (key.toString().equals(OTPConsts.INVALID)) {
            int sum = 0;
            for (Text value : values) {
                sum += 1;
            }
            context.write(key, new DoubleWritable((double)sum));
        } else {
            Map<String, List<Double>> monthPrices = new HashMap<String, List<Double>>();
            String month;
            Double price;
            List<Double> prices;
            Text carrierMonth = new Text();
            DoubleWritable mean = new DoubleWritable();

            for (Text value : values) {
                month = DataPreprocessor.getMonth(value);
                price = DataPreprocessor.getPrice(value);
                if (!monthPrices.containsKey(month)) {
                    prices = new ArrayList<Double>();
                    prices.add(price);
                    monthPrices.put(month, prices);
                } else {
                    monthPrices.get(month).add(price);
                }
            }

            for (String mk : monthPrices.keySet()) {
                carrierMonth.set(key.toString() + " " + mk);
                mean.set(DataPreprocessor.getMean(monthPrices.get(mk)));
                context.write(carrierMonth, mean);
            }
        }
    }
}

