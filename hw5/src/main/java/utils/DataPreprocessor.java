package utils;

import org.apache.commons.math3.linear.*;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.io.Text;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.lang.*;



public class DataPreprocessor {

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
            String airTime = values[OTPConsts.AIR_TIME];
            double airTimeVal = Double.parseDouble(airTime);
            double distance = Double.parseDouble(values[OTPConsts.DISTANCE]);

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

    public static void updateConnectionInfo(Map<Integer, ConnectionInfo> acMap, String s) {
        // parse the value string
        String[] vals = s.split(" ");
        int airportID = Integer.parseInt(vals[0]);
        if (!acMap.containsKey(airportID)) {
            acMap.put(airportID, new ConnectionInfo());
        }

        if (vals.length == 2) { // this is a arrTime record
            acMap.get(airportID).updateArr(new Date(Long.parseLong(vals[1])));
        } else if (vals.length == 3) {  // this is a cover range
            Date coverStart = new Date(Long.parseLong(vals[1]));
            Date coverEnd = new Date(Long.parseLong(vals[2]));
            Date[] cover = {coverStart, coverEnd};
            acMap.get(airportID).updateCoverRanges(cover);
        }
    }

    public static void updateConnectionInfoMap(Map<String, Map<Integer, ConnectionInfo>> ciMap, FlightInfo flight) {
        String carrier = flight.getCarrier();
        updateConnectionInfoMap(ciMap, carrier, flight.getOriginalAirportId(), flight.getDepTime(), false);
        updateConnectionInfoMap(ciMap, carrier, flight.getDestAirportId(), flight.getArrTime(), true);
    }

    public static void updateConnectionInfoMap(Map<String, Map<Integer, ConnectionInfo>> ciMap, String carrier,
                                               int airPortID, Date ts, boolean isArr) {
        Map<Integer, ConnectionInfo> acMap;
        Calendar c = Calendar.getInstance();
        c.setTime(ts);
        int year = c.get(Calendar.YEAR);
        String carrierYear = carrier + " " + year;

        if (!ciMap.containsKey(carrierYear)) {
            acMap = new HashMap<Integer, ConnectionInfo>();
            ciMap.put(carrierYear, acMap);
        }

        acMap = ciMap.get(carrierYear);

        if (!acMap.containsKey(airPortID)) {
            acMap.put(airPortID, new ConnectionInfo());
        }

        if (isArr) {
            acMap.get(airPortID).updateArr(ts);
        } else {
            acMap.get(airPortID).updateDep(ts);
        }
    }

}
