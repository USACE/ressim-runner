package usace.cc.plugin.ressimrunner;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import hec.heclib.dss.HecDss;
import hec.hecmath.HecMath;
import usace.cc.plugin.api.Action;
import usace.cc.plugin.api.DataSource;

public class DsstoDssAppendAction {
        private Action action;
    public DsstoDssAppendAction(Action a) {
        action = a;
    }
    public void computeAction(){
        try{
            //find source 
            Optional<DataSource> opSourceDs = action.getInputDataSource("source");
            if(!opSourceDs.isPresent()){
                System.out.println("could not find input datasource named source in action dss to dss append");
                System.exit(-1);
            }
            DataSource sourceDs = opSourceDs.get();
            //create dss reader
            //open up the dss file. reference: https://www.hec.usace.army.mil/confluence/display/dssvuedocs/Working+with+DataContainers
            HecDss source = HecDss.open(sourceDs.getPaths().get("default"));
            //find destination parameter
            Optional<DataSource> opDestinationDs = action.getOutputDataSource("destination");
            if(!opDestinationDs.isPresent()){
                System.out.println("could not find output datasource named destination in action dss to dss append");
                System.exit(-1);
            }
            DataSource destinationDs = opDestinationDs.get();
            //create dss file
            HecDss destination = HecDss.open(destinationDs.getPaths().get("default"));
            //read time series from source
            Optional<Map<String,String>> opSourceDataPaths = sourceDs.getDataPaths();
            if(!opSourceDataPaths.isPresent()){
                System.out.println("source datapaths was not present");
                System.exit(-1);
            }
            Map<String, String> sourceDataPaths = opSourceDataPaths.get();
            Optional<Map<String,String>> opDestinationDataPaths = destinationDs.getDataPaths();
            if(!opDestinationDataPaths.isPresent()){
                System.out.println("destination datapaths was not present");
                System.exit(-1);
            }
            Map<String, String> destinationDataPaths = opDestinationDataPaths.get();
            for(Entry<String,String> p : sourceDataPaths.entrySet()){//assumes datapaths for source and dest are named the same keys.
                HecMath stsm = source.read(p.getValue());
                HecMath dtsm = destination.read(destinationDataPaths.get(p.getKey()));
                HecMath merged = stsm.mergeTimeSeries(dtsm);
                int result = destination.write(merged);
                if (result <0){
                    //panic?
                    System.out.println("merge write failed");
                    return;
                }
            }
            //close reader
            source.close();
            //close writer
            destination.close();
        }catch(Exception e){
            System.out.println(e);
            return;
        }
    }
}
