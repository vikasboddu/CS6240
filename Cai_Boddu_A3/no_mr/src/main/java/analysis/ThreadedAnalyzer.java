package analysis;

import java.io.File;
import java.util.*;

public class ThreadedAnalyzer extends SequentialAnalyzer {

    public ThreadedAnalyzer(boolean isMean, boolean fastMedian) {
        this.isMean = isMean;
        this.fastMedian = fastMedian;
    }
    /**
     * Analyze all the gzip files in the given directory.
     * @param dir directory which contain all the compressed csv files.
     * @throws InterruptedException
     */
    public void analyze(String dir) throws InterruptedException {

        File[] files  = new File(dir).listFiles();
        List<String> fileList1 = new ArrayList<String>();
        List<String> fileList2 = new ArrayList<String>();

        int flag = 0;

        for (File fp : files) {
            if (flag++ % 2 == 0) {
                fileList1.add(fp.getAbsolutePath());
            } else {
                fileList2.add(fp.getAbsolutePath());
            }
        }

        WorkingThread t1 = new WorkingThread(fileList1);
        WorkingThread t2 = new WorkingThread(fileList2);

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        combineCarrierMap(carrierMap, t1.carrierDataMap);
        combineCarrierMap(carrierMap, t2.carrierDataMap);

        this.f += t1.f + t2.f;
        this.k += t1.k + t2.k;
    }

    /**
     * Merge price lists for each carrier from a thread to a major price map.
     * @param mainMap the major price map, all the data will finally be merged to this map.
     * @param newMap price map from a thread
     */
    private void combineCarrierMap(Map<String, CarrierData> mainMap, Map<String, CarrierData> newMap) {
        for (String key : newMap.keySet()) {
            if (!mainMap.containsKey(key)) {
                mainMap.put(key, newMap.get(key));
            } else {
                mainMap.get(key).combine(newMap.get(key));
            }
        }
    }

    /**
     * Merge eligible carrier set from a thread to the major list.
     * @param c1 the major eligible carrier set
     * @param c2 eligible carrier set from a thread
     */
    private void combineEligibleCarriers(Set<String> c1, Set<String> c2) {
        c1.addAll(c2);
    }

    /**
     * Thread to complete part of the workload.
     */
    class WorkingThread extends Thread {
        List<String> lof;
        public long k;
        public long f;
        public Map<String, CarrierData> carrierDataMap;

        public WorkingThread(List<String> lof) {
            this.lof = lof;
            this.k= 0;
            this.f = 0;
        }

        public void run() {
            SequentialAnalyzer sa = new SequentialAnalyzer();
            sa.batchAnalyze(this.lof);
            this.carrierDataMap = sa.getCarrierMap();
            this.k = sa.getK();
            this.f = sa.getF();
        }

    }
}


