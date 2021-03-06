package analysis;

import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import utils.DataPreprocessor;
import utils.OTPConsts;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class OTPFastMedianReducer extends Reducer<Text, Text, Text, DoubleWritable> {

    @Override
    public void reduce(Text key, Iterable<Text> values, Context context)
        throws IOException, InterruptedException {
        if (key.toString().equals(OTPConsts.INVALID)) {
            int sum = 0;
            for (Text value : values) {
                sum += Integer.parseInt(value.toString());
            }
            context.write(key, new DoubleWritable((double)sum));
        } else {
            Map<String, List<Double>> monthPrices = new HashMap<String, List<Double>>();
            String month;
            Double price;
            List<Double> prices;
            Text carrierMonth = new Text();
            DoubleWritable median = new DoubleWritable();

			// check if this carrier is still active in 2015
			if (!DataPreprocessor.isActiveCarrier(values)) return;

			int totalFl = 0;
            for (Text value : values) {
				totalFl++;
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
                carrierMonth.set(mk + "," + key.toString() + "," + totalFl);
                median.set(DataPreprocessor.fastMedian(monthPrices.get(mk)));
                context.write(carrierMonth, median);
            }
        }
    }
}

