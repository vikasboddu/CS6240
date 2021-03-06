package hidoop.conf;

import hidoop.io.Text;
import hidoop.mapreduce.Mapper;
import hidoop.mapreduce.Partitioner;
import hidoop.mapreduce.Reducer;
import hidoop.util.Consts;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

// Author: Jun Cai
// Reference: github.com/apache/hadoop
public class Configuration implements Serializable {
    public boolean isLocalMode;
    public String jobName;
    public Class mapperClass;
    public Class combinerClass;
    public Class reducerClass;
    public Class partitionerClass;
    public Class outputKeyClass;
    public Class outputValueClass;
    public Class mapOutputKeyClass;
    public Class mapOutputValueClass;
    public Class mapInputKeyClass;
    public Class mapInputValueClass;
    public String inputPath;
    public String outputPath;
    public int reducerNumber;
    public List<String> slaveIpList;
    public String masterIp;
    public int masterPort;
    public int slavePort;
    public int slaveNum;


    public Configuration() throws IOException {
        isLocalMode = true;
        reducerNumber = 4;
        partitionerClass = Partitioner.class;
        // input key value type
        mapInputKeyClass = Object.class;
        mapInputValueClass = Text.class;

        // TODO load local config file
        // mode: local/ec2
        // master port
        // slave port
        BufferedReader br = new BufferedReader(new FileReader(Consts.CONFIG_PATH));
        String line;
        line = br.readLine();
        isLocalMode = line.equals(Consts.LOCAL_MODE);
        if (!isLocalMode) {
            masterPort = Integer.parseInt(br.readLine());
            slavePort = Integer.parseInt(br.readLine());
            System.out.println("Master port: " + masterPort);
            System.out.println("Slave port: " + slavePort);
            // slave IPs
            br = new BufferedReader(new FileReader(Consts.IP_LIST_PATH));

            slaveIpList = new ArrayList<String>();
            masterIp = null;
            while ((line = br.readLine()) != null) {
                if (masterIp == null) {
                    masterIp = line;
                    System.out.println("Master ip: " + masterIp); // the master ip will be the first one in the ips
                }
                slaveIpList.add(line);
                System.out.println("adding ip to slave iplist: " + line);
            }
            br.close();
            slaveNum = slaveIpList.size();
            reducerNumber = slaveNum;
        }
    }

    public void setJobName(String jname) {
        this.jobName = jname;
    }

    public void setMapperClass(Class<? extends Mapper> cls) {
        this.mapperClass = cls;
    }

    public void setCombinerClass(Class<? extends Reducer> cls) {
        this.combinerClass = cls;
    }

    public void setReducerClass(Class<? extends Reducer> cls) {
        this.reducerClass = cls;
    }

    public void setPartitionerClass(Class<? extends Partitioner> cls) {
        this.partitionerClass = cls;
    }

    public void setOutputKeyClass(Class<?> cls) {
        if (this.mapOutputKeyClass == null) {
            setMapOutputKeyClass(cls);
        }
        this.outputKeyClass = cls;
    }

    public void setOutputValueClass(Class<?> cls) {
        if (this.mapOutputValueClass == null) {
            setMapOutputValueClass(cls);
        }
        this.outputValueClass = cls;
    }

    public void setMapOutputKeyClass(Class<?> cls) {
        this.mapOutputKeyClass = cls;
    }

    public void setMapOutputValueClass(Class<?> cls) {
        this.mapOutputValueClass = cls;
    }

    public void setInputPath(String p) {
        this.inputPath = p;
    }

    public void setOutputPath(String p) {
        this.outputPath = p;
    }

    public void setNumReduceTasks(int num) {
        reducerNumber = num;
    }

    public void set(String name, String value) {
        // do nothing;
    }
}
