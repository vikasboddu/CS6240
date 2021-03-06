package utils;

import org.apache.hadoop.io.Text;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.lang.*;



public class DataPreprocessor {

    public static void processLine(String line, Text carrier, Text dateAndPrice) throws IOException {
        String price;
        String date;

        String[] values;
        values = parseCSVLine(line);
        if (!sanityCheck(values)) {
            carrier.set(OTPConsts.INVALID);
            dateAndPrice.set(OTPConsts.INVALID);
        } else {
            carrier.set(values[OTPConsts.UNIQUE_CARRIER]);
            price = values[OTPConsts.AVG_TICKET_PRICE];
            date = values[OTPConsts.FL_DATE];
            dateAndPrice.set(date + " " + price);
        }
    }

    /**
     * Parse a line in CSV format
     * @param line a string in CSV format
     * @return list of values as a string array
     */
    static String[] parseCSVLine(String line) {
        List<String> values = new ArrayList<String>();
        StringBuffer sb = new StringBuffer();
        boolean inQuote = false;
        char curChar;
        for (int i = 0; i < line.length(); i++) {
            curChar = line.charAt(i);
            if (inQuote) {
                if (curChar == '"') {
                    inQuote = false;
                } else {
                    sb.append(curChar);
                }

            } else {
                if (curChar == '"') {
                    inQuote = true;
                } else if (curChar == ',') {
                    values.add(sb.toString());
                    sb = new StringBuffer();
                } else {
                    sb.append(curChar);
                }
            }
        }
        values.add(sb.toString());  // last field

        return values.toArray(new String[1]);
    }

    /**
     * Check the number of fields in the record and the logic between some fields
     * @param values a String array contains the value of each field in a record
     * @return true if the input is a valid record, false otherwise
     */
    static boolean sanityCheck(String[] values) {
        if (values.length != 110) return false;
        try {
            // check not 0
            for (int i : OTPConsts.NOTZERO) {
                if (Double.parseDouble(values[i]) == 0) return false;
            }

            double timeZone = getMinDiff(values[OTPConsts.CRS_ARR_TIME], values[OTPConsts.CRS_DEP_TIME])
                    - Double.parseDouble(values[OTPConsts.CRS_ELAPSED_TIME]);
            double residue = timeZone % 60;
            if (residue != 0) return false;

            // check larger than 0
            for (int i : OTPConsts.LARGERTHANZERO) {
                if (Double.parseDouble(values[i]) <= 0) return false;
            }

            // check not empty
            for (int i : OTPConsts.NOTEMPTY) {
                if (values[i].isEmpty()) return false;
            }

            // for flights not canceled
            boolean isCanceled = (Double.parseDouble(values[OTPConsts.CANCELLED]) == 1);

            // ArrTime -  DepTime - ActualElapsedTime - timeZone should be zero
            if (!isCanceled) {
                double timeDiff = getMinDiff(values[OTPConsts.ARR_TIME], values[OTPConsts.DEP_TIME])
                        - Double.parseDouble(values[OTPConsts.ACTUAL_ELAPSED_TIME]) - timeZone;
                if (timeDiff != 0) return false;

                double arrDelay = Double.parseDouble(values[OTPConsts.ARR_DELAY]);
                double arrDelayNew = Double.parseDouble(values[OTPConsts.ARR_DELAY_NEW]);
                // if ArrDelay > 0 then ArrDelay should equal to ArrDelayMinutes
                if (arrDelay > 0) {
                    if (arrDelay != arrDelayNew) return false;
                }

                // if ArrDelay < 0 then ArrDelayMinutes???? should be zero
                if (arrDelay < 0) {
                    if (arrDelayNew != 0) return false;
                }
                // if ArrDelayMinutes >= 15 then ArrDel15 should be false
                boolean arrDel15 = (Double.parseDouble(values[OTPConsts.ARR_DEL15]) == 1);
                if (arrDelayNew >= 15 && !arrDel15) return false;
            }

            // finally, check the carrier field and price field
            String carrier = values[OTPConsts.UNIQUE_CARRIER];
            if (carrier.isEmpty()) return false;
            double avgTicketPrice = Double.parseDouble(values[OTPConsts.AVG_TICKET_PRICE]);

        } catch (Exception ex) {
            // ex.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Calculate the difference between two time stamp in minutes.
     * Note that the first timestamp is always later than the second one.
     * @param t1 first timestamp in "HHmm" format
     * @param t2 second timestamp in "HHmm" format
     * @return difference between t1 and t2 in minutes
     */
    static int getMinDiff(String t1, String t2) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("HHmm");
            Date date1 = format.parse(t1);
            Date date2 = format.parse(t2);
            long timeDiff = (date1.getTime() - date2.getTime()) / 60000;
            if (timeDiff <= 0) {
                timeDiff += 24 * 60;
            }

            return (int) timeDiff;
        } catch (ParseException ex) {
            return -1;
        }
    }

    /**
     * Get mean of ticket price for each eligible carrier.
     * @return a map contains carrier mean price pairs.
     */
    public static double getMean(List<Double> prices) {
        double sum = sumOfDoubleList(prices);
        return sum / prices.size();
    }

    /**
     * Compute sum of all the elements in a double list.
     * @param lod a list of double instances
     * @return sum of all the doubles in the list
     */
    public static double sumOfDoubleList(List<Double> lod) {
        double sum = 0;
        for (double d : lod){
            sum += d;
        }
        return sum;
    }

    /**
     * Get median from a sorted list of doubles
     * @param lod a sorted list of doubles
     * @return the median
     */
    public static double getMedian(List<Double> lod) {
        Collections.sort(lod);
        int len = lod.size();
        if (len % 2 == 0) {
            return (lod.get(len / 2) + lod.get((len / 2) - 1)) / 2;
        } else {
            return lod.get(len / 2);
        }
    }

    public static boolean isActiveCarrier(Iterable<Text> values) {
        for (Text value : values) {
            if (value.toString().startsWith(OTPConsts.ACTIVE_YEAR)) {
                return true;
            }
        }
        return false;
    }

    public static String getMonth(Text value) {
        String[] dates =  value.toString().split(" ")[0].split("-");
        return dates[0] + "-" + dates[1];
    }

    public static double getPrice(Text value) {
        return Double.parseDouble(value.toString().split(" ")[1]);
    }

    public static double fastMedianTwo(List<Double> lod) {
        // implement the median of medians
        // Pseudo code reference: https://en.wikipedia.org/wiki/Median_of_medians
        int medianIndex = MOMSelect(lod, 0, lod.size() - 1, lod.size() / 2);
        return lod.get(medianIndex);
    }

    public static int MOMSelect(List<Double> list, int left, int right, int n) {
        if (left == right) return left;
        int pIndex;

        while (true) {
            pIndex = MOMPivot(list, left, right);
            pIndex = MOMPartition(list, left, right, pIndex);
            if (n == pIndex) {
                return n;
            } else if (n < pIndex) {
                right = pIndex - 1;
            } else {
                left = pIndex + 1;
            }
        }
    }

    public static int MOMPivot(List<Double> list, int left, int right) {
        if (right - left < 5) {
            return partition5(list, left, right);
        }

        for (int i = left; i <= right; i += 5) {
            int subRight = i + 4;
            subRight = (subRight > right) ? right : subRight;
            int median5 = partition5(list, i, subRight);
            swap(list, median5, left + (i - left) / 5);
        }
        return MOMSelect(list, left, left + (int)Math.ceil(1.0 * (right - left) / 5), left + (right - left) / 10);
    }

    public static int partition5(List<Double> list, int left, int right) {
        List<Double> subList = new ArrayList<Double>();
        // TODO < or <= right?
        for (int i = left; i <= right; i++) {
            subList.add(list.get(i));
        }
        Collections.sort(subList);
        for (int i = 0; i < subList.size(); i++) {
            list.set(left + i, subList.get(i));
        }
        return subList.size() / 2;
    }

    public static int MOMPartition(List<Double> list, int left, int right, int pIndex) {
        double pValue = list.get(pIndex);
        swap(list, left, right);
        int storeIndex = left;
        for (int i = left; i < right; i++) {
            if (list.get(i) < pValue) {
                swap(list, storeIndex, i);
                storeIndex++;
            }
        }
        swap(list, right, storeIndex);
        return storeIndex;
    }

    public static void swap(List<Double> list, int i1, int i2) {
        double tmp = list.get(i1);
        list.set(i1, list.get(i2));
        list.set(i2, tmp);
    }

    public static double fastMedian(List<Double> lod) {
        // A naive approach which only uses 10 percent of the data to find the median
        int len = lod.size();
        List<Double> subLod = lod;

        if (len > 50) {
            subLod = new ArrayList<Double>();

            Random random = new Random();
            int rIndex;

            for (int i = 0; i < len / 10; i++) {
                rIndex = random.nextInt(len);
                subLod.add(lod.get(rIndex));
            }
        }
        return getMedian(subLod);
    }


}
