package analysis;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import utils.FlightInfo;
import utils.OTPConsts;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// Authors: Jun Cai and Vikas Boddu
public class TrainingMapper extends Mapper<LongWritable, Text, Text, Text> {
    List<String> infoStrList;
    String rInput = "./OTP_prediction_training_" + UUID.randomUUID().toString() + ".csv";
    long recordCount;

    @Override
    protected void setup(Context context) throws IOException {
        recordCount = 0;
        infoStrList = new ArrayList<String>();
        writeRecordsToFile(true);
    }

    @Override
    public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
        String line = value.toString();

        // skip the header
        if (line.startsWith(OTPConsts.HEADER_START)) return;

        FlightInfo flight = new FlightInfo(line, false);

        if (flight.isValid()) {
            infoStrList.add(flight.toString());
            recordCount++;
        }
        
        if (infoStrList.size() > 100000) {
            writeRecordsToFile(false);
        }
    }

    private void writeRecordsToFile(boolean init) throws IOException {
        File f = new File(rInput);
        if (init) {
            f.createNewFile();
        }
        FileWriter fw = new FileWriter(f, true);
        if (init) {
            fw.write(OTPConsts.CSV_HEADER);
        }
        for (String s : infoStrList) {
            fw.write(s);
        }
        fw.flush();
        fw.close();
        infoStrList.clear();
    }

    @Override
    protected void cleanup(Context context) throws IOException, InterruptedException {
        if (recordCount == 0) {
            Files.deleteIfExists(Paths.get(rInput));
            return;
        }
        if (infoStrList.size() > 0) {
            writeRecordsToFile(false);
        }

        String path = "./OTP_prediction_" + UUID.randomUUID().toString() + ".rf";
        String comm = "Rscript ./rf.R " + rInput + " " + path;
        Process p = Runtime.getRuntime().exec(comm);
        int ret = p.waitFor();

        // read Random Forest as string and write it to the context
        byte[] b = Files.readAllBytes(Paths.get(path));
        String rfString = new String(b, Charset.defaultCharset());
        context.write(new Text("RandomForest"), new Text(rfString));

        // Remove used file from tmp folder
        Files.deleteIfExists(Paths.get(rInput));
        Files.deleteIfExists(Paths.get(path));
    }
}
