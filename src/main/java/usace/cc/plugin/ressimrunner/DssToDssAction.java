package usace.cc.plugin.ressimrunner;

import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;

import hec.heclib.dss.DSSErrorMessage;
import hec.heclib.dss.HecTimeSeries;
import hec.io.TimeSeriesContainer;
import usace.cc.plugin.api.Action;
import usace.cc.plugin.api.DataSource;

public class DssToDssAction {
    private Action action;
    public DssToDssAction(Action a) {
        action = a;
    }
    public void computeAction(){

        //find source 
        Optional<DataSource> opSource = action.getInputDataSource("source");
        if(!opSource.isPresent()){
            System.out.println("could not find input datasource named source in the dss to dss action");
            System.exit(-1);
        }
        DataSource source = opSource.get();

        //find destination parameter
        Optional<DataSource> opDestination = action.getOutputDataSource("destination");
        if(!opDestination.isPresent()){
            System.out.println("could not find output datasource named destination in the dss to dss action");
            System.exit(-1);
        }
        DataSource destination = opDestination.get();
        Optional<Boolean> opFill_Null = action.getAttributes().get("fill_empty_values");
        if (!opFill_Null.isPresent()){
            System.out.println("could not find boolean attribute fill_nulls in dss to dss action");
            System.exit(-1);
        }
        
        boolean fill = opFill_Null.get();
        System.out.println("fill_empty_values " + fill);
        //datapath

        //create dss reader
        //open up the dss file. reference: https://www.hec.usace.army.mil/confluence/display/dssJavaprogrammer/General+Example
        HecTimeSeries reader = new HecTimeSeries();
        int readerstatus = reader.setDSSFileName(source.getPaths().get("default"));//assumes one path and assumes it is dss.
        if (readerstatus <0){
            //panic?
            DSSErrorMessage error = reader.getLastError();
            error.printMessage();
            return;
        }
        //create dss writer
        HecTimeSeries writer = new HecTimeSeries();
        int writerstatus = writer.setDSSFileName(destination.getPaths().get("default"));//assumes one path and assumes it is dss.
        if (writerstatus <0){
            //panic?
            DSSErrorMessage error = reader.getLastError();
            error.printMessage();
            return;
        }
        Optional<Map<String,String>> opSourceDataPaths = source.getDataPaths();
        if(!opSourceDataPaths.isPresent()){
            System.out.println("source datapaths was not present");
            System.exit(-1);
        }
        Map<String, String> sourceDataPaths = opSourceDataPaths.get();
        Optional<Map<String,String>> opDestinationDataPaths = destination.getDataPaths();
        if(!opDestinationDataPaths.isPresent()){
            System.out.println("destination datapaths was not present");
            System.exit(-1);
        }
        Map<String, String> destinationDataPaths = opDestinationDataPaths.get();
        //read time series from source
        for(Entry<String,String> p : sourceDataPaths.entrySet()){//assumes datapaths for source and dest are named the same keys.
            Optional<Float> opMultiplier = action.getAttributes().get(p.getValue() + "- multiplier");
            float multiplier = 1.0f;
            if (opMultiplier.isPresent()){
                float mult = opMultiplier.get();
                multiplier = mult;
            }
            TimeSeriesContainer tsc = new TimeSeriesContainer();
            tsc.fullName = p.getValue();

            readerstatus = reader.read(tsc,true);
            if (readerstatus <0){
                //panic?
                DSSErrorMessage error = reader.getLastError();
                error.printMessage();
                break;
            }
            double[] values = tsc.values;
            double[] outvalues = new double[values.length];

            int i = 0;
            for(double f : values){
                double fillvalue = f;
                outvalues[i] =fillvalue;
                if (fill){
                    if(fillvalue<= -900){
                        if (i==0){
                            fillvalue = tsc.minimumValue();
                            outvalues[i]=fillvalue;
                            System.out.println("modifying timestep 1");
                        }else{
                            fillvalue = outvalues[i-1];
                            outvalues[i] = fillvalue;
                            System.out.println("modifying a timestep");
                        }
                    }
                }
                outvalues[i] = fillvalue*multiplier;

                i++;
            }
            //write time series to destination
            TimeSeriesContainer desttsc = new TimeSeriesContainer();
            desttsc.fullName = destinationDataPaths.get(p.getKey());
            desttsc.setValues(outvalues);
            desttsc.setTimes(tsc.getTimes());
            tsc.setValues(outvalues);
            try {
                writer.write(desttsc);
                reader.write(tsc);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
                                        
        }
        //close reader
        reader.close();
        //close writer
        writer.close();
    }
}
