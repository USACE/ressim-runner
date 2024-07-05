package usace.wat.plugin.ressimrunner;

import hec.heclib.dss.HecDss;
import hec.hecmath.HecMath;
import usace.cc.plugin.Action;
import usace.cc.plugin.DataSource;

public class DsstoDssAppendAction {
        private Action action;
    public DsstoDssAppendAction(Action a) {
        action = a;
    }
    public void computeAction(){
        try{
            //find source 
            DataSource sourceDs = action.getParameters().get("source");
            //create dss reader
            //open up the dss file. reference: https://www.hec.usace.army.mil/confluence/display/dssvuedocs/Working+with+DataContainers
            HecDss source = HecDss.open(sourceDs.getPaths()[0]);
            //find destination parameter
            DataSource destinationDs = action.getParameters().get("destination");
            //create dss file
            HecDss destination = HecDss.open(destinationDs.getPaths()[0]);
            //read time series from source
            int dsIndex = 0;
            for(String p : sourceDs.getDataPaths()){//assumes datapaths for source and dest are ordered the same.
                HecMath stsm = source.read(p);
                HecMath dtsm = destination.read(destinationDs.getDataPaths()[dsIndex]);
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
