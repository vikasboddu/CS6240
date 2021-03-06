/**
 * Created by jonca on 1/12/2016.
 *
 * #### My result with input file 323.csv.gz ######
     4082
     435940
     WN 59.50
     HP 67.06
     AS 75.47
     PI 190.54
     CO 204.75
     EA 273.33
     PA 284.25
     US 289.21
     TW 298.97
     AA 307.33
     DL 348.12
     UA 547.08
     NW 54354.39
 * ################### Note about header handling ###################
 * I didn't include the header line as a record, so my K would be one
 * smaller than the reference result (4082 instead of 4083).
 *
 * ##################################################################
 *
 * Input argument: path to gzip file
 * Output:
 *      K (number of corrupted lines)
 *      F (number of sane flights)
 *      C p (where C is the carrier code, p is the mean price of tickets)
 */

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.GZIPInputStream;


public class SequentialAnalysis {

    // constants for indices
    // CRS_ARR_TIME 40
    // CRS_DEP_TIME 29
    // CRS_ELAPSED_TIME 50
    private final static int CRS_ARR_TIME = 40;
    private final static int CRS_DEP_TIME = 29;
    private final static int CRS_ELAPSED_TIME = 50;
    private final static int[] indShouldNotBeZero = {29, 40};

    // ORIGIN_AIRPORT_ID 11
    // ORIGIN_AIRPORT_SEQ_ID 12
    // ORIGIN_CITY_MARKET_ID 13
    // ORIGIN_STATE_FIPS 17
    // ORIGIN_WAC 19
    // DEST_AIRPORT_ID 20
    // DEST_AIRPORT_SEQ_ID 21
    // DEST_CITY_MARKET_ID 22
    // DEST_STATE_FIPS 26
    // DEST_WAC 28
    private final static int[] indShouldLargerThanZero = {11, 12, 13, 17, 19, 20, 21, 22, 26, 28};

    // ORIGIN 14
    // ORIGIN_CITY_NAME 15
    // ORIGIN_STATE_ABR 16
    // ORIGIN_STATE_NM 18
    // DEST 23
    // DEST_CITY_NAME 24
    // DEST_STATE_ABR 25
    // DEST_STATE_NM 27
    private final static int[] indSholdNotBeEmpty = {14, 15, 16, 18, 23, 24, 25, 27};

    // CANCELLED 47
    private final static int CANCELLED = 47;

    // DEP_TIME 30
    // ARR_TIME 41
    // ACTUAL_ELAPSED_TIME 51
    // ARR_DELAY 42
    // ARR_DELAY_NEW 43
    // ARR_DEL15 44
    // CARRIER 8
    // AVG_TICKET_PRICE 109
    private final static int DEP_TIME = 30;
    private final static int ARR_TIME = 41;
    private final static int ACTUAL_ELAPSED_TIME = 51;
    private final static int ARR_DELAY = 42;
    private final static int ARR_DELAY_NEW = 43;
    private final static int ARR_DEL15 = 44;
    private final static int CARRIER = 8;
    private final static int AVG_TICKET_PRICE = 109;

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("The usage is $ java SequentialAnalysis GZIIPED_FILE_PATH");
            return;
        }

        String gzipFilePath = args[0];
        SequentialAnalysis sa = new SequentialAnalysis();
        sa.unzipAndAnalyze(gzipFilePath);
    }

    /**
     * Unzip the gzipped file, then parse it and calculate the mean
     * ticket price for each carrier. Print out the number of corrupted
     * lines and sane flights, and a list of mean ticket price for each
     * carrier
     * @param gzPath path to the GZIP file
     */
    public void unzipAndAnalyze(String gzPath) {
        int[] stats;
        HashMap<String, List<Double>> priceMap = new HashMap<>();

        try {
            FileInputStream fis = new FileInputStream(gzPath);
            GZIPInputStream gzis = new GZIPInputStream(fis);
            InputStreamReader isr = new InputStreamReader(gzis);

            stats = getPriceMapWithBufferedReader(isr, priceMap);

        } catch (IOException ex) {
            //  ex.printStackTrace();
            return;
        }

        // generate price list then sort it
        List<String[]> priceList = getSortedPrices(priceMap);

        System.out.println(stats[0]);
        System.out.println(stats[1]);
        printPriceList(priceList);
    }

    /**
     * Get flight count and sum of ticket price for each carrier from a reader
     * @param reader reader contains the unzipped stream
     * @param priceMap key is the airline carrier ID, value is a list of two floats where
     *                 the first one is the number of flights, the second one is the sum
     *                 of all the flight ticket price.
     * @return integer array contains number of corrupted records and sane flights
     * @throws IOException
     */
    public int[] getPriceMapWithBufferedReader(Reader reader, HashMap<String, List<Double>> priceMap) throws IOException {
        BufferedReader br = new BufferedReader(reader);
        int numOfFlights = 0;
        int k = 0;
        String carrier;
        Double price;

        br.readLine();  // get rid of the header
        String line;
        String[] values;
        while ((line = br.readLine()) != null) {
            numOfFlights++;
            values = parseCSVLine(line);
            if (!sanityCheck(values)) {
                k++;
            } else {
                carrier = values[CARRIER];
                price = Double.parseDouble(values[AVG_TICKET_PRICE]);
                if (!priceMap.containsKey(carrier)) {
                    priceMap.put(carrier, Arrays.asList(1.0, price));
                } else {
                    List<Double> curList = priceMap.get(carrier);
                    curList.set(0, curList.get(0) + 1);
                    curList.set(1, curList.get(1) + price);
                }
            }
        }

        br.close();

        return new int[]{k, numOfFlights - k};
    }

    /**
     * Parse a line in CSV format
     * @param line a string in CSV format
     * @return list of values as a string array
     */
    public String[] parseCSVLine(String line) {
        ArrayList<String> values = new ArrayList<>();
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
    public boolean sanityCheck(String[] values) {
        if (values.length != 110) return false;
        try {
            // check not 0
            for (int i : indShouldNotBeZero) {
                if (Double.parseDouble(values[i]) == 0) return false;
            }

            double timeZone = getMinDiff(values[CRS_ARR_TIME], values[CRS_DEP_TIME])
                    - Double.parseDouble(values[CRS_ELAPSED_TIME]);
            double residue = timeZone % 60;
            if (residue != 0) return false;

            // check larger than 0
            for (int i : indShouldLargerThanZero) {
                if (Double.parseDouble(values[i]) <= 0) return false;
            }

            // check not empty
            for (int i : indSholdNotBeEmpty) {
                if (values[i].isEmpty()) return false;
            }

            // for flights not canceled
            boolean isCanceled = (Double.parseDouble(values[CANCELLED]) == 1);

            // ArrTime -  DepTime - ActualElapsedTime - timeZone should be zero
            if (!isCanceled) {
                double timeDiff = getMinDiff(values[ARR_TIME], values[DEP_TIME])
                        - Double.parseDouble(values[ACTUAL_ELAPSED_TIME]) - timeZone;
                if (timeDiff != 0) return false;

                double arrDelay = Double.parseDouble(values[ARR_DELAY]);
                double arrDelayNew = Double.parseDouble(values[ARR_DELAY_NEW]);
                // if ArrDelay > 0 then ArrDelay should equal to ArrDelayMinutes
                if (arrDelay > 0) {
                    if (arrDelay != arrDelayNew) return false;
                }

                // if ArrDelay < 0 then ArrDelayMinutes???? should be zero
                if (arrDelay < 0) {
                    if (arrDelayNew != 0) return false;
                }
                // if ArrDelayMinutes >= 15 then ArrDel15 should be false
                boolean arrDel15 = (Double.parseDouble(values[ARR_DEL15]) == 1);
                if (arrDelayNew >= 15 && !arrDel15) return false;
            }

            // finally, check the carrier field and price field
            if (values[CARRIER].isEmpty()) return false;
            double avgTicketPrice = Double.parseDouble(values[AVG_TICKET_PRICE]);

        } catch (NumberFormatException ex) {
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
    public int getMinDiff(String t1, String t2) {
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
     * Get a list of carrier average price pairs in ascending order from a price map
     * @param priceMap key is the airline carrier ID, value is a list of two floats where
     *                 the first one is the number of flights, the second one is the sum
     *                 of all the flight ticket price.
     * @return a list of string array which contains the carrier ID and the average ticket
     *         price, the list is in ascending order.
     */
    public List<String[]> getSortedPrices(HashMap<String, List<Double>> priceMap) {
        List<String[]> priceList = new ArrayList<>();
        for (String key : priceMap.keySet()) {
            double avgPrice = priceMap.get(key).get(1) / priceMap.get(key).get(0);
            priceList.add(new String[]{key, String.format("%.2f", avgPrice)});
        }

        Comparator<String[]> comp = new Comparator<String[]>() {
            @Override
            public int compare(String[] o1, String[] o2) {
                if (Double.parseDouble(o1[1]) > (Double.parseDouble(o2[1]))) {
                    return 1;
                } else {
                    return -1;
                }
            }
        };
        Collections.sort(priceList, comp);
        return priceList;
    }

    /**
     * Print the price list
     * @param priceList a list of string array which contains the carrier ID and the average ticket
     *                  price, the list is in ascending order.
     */
    public void printPriceList(List<String[]> priceList) {
        for (String[] strs : priceList) {
            System.out.format("%s %s%n", strs[0], strs[1]);
        }
    }
}