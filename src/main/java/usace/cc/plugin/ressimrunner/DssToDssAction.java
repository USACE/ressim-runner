package usace.cc.plugin.ressimrunner;

import hec.heclib.dss.DSSErrorMessage;
import hec.heclib.dss.HecTimeSeries;
import hec.io.TimeSeriesContainer;
import usace.cc.plugin.Action;
import usace.cc.plugin.DataSource;
import usace.cc.plugin.Payload;
import usace.cc.plugin.PluginManager;

public class DssToDssAction {
    private Action action;
    public DssToDssAction(Action a) {
        action = a;
    }
    public void computeAction(){
        PluginManager pm = PluginManager.getInstance();
        Payload payload = pm.getPayload();
        //find source 
        DataSource source = action.getParameters().get("source");
        //create dss reader
        //open up the dss file. reference: https://www.hec.usace.army.mil/confluence/display/dssJavaprogrammer/General+Example
        HecTimeSeries reader = new HecTimeSeries();
        int readerstatus = reader.setDSSFileName(source.getPaths()[0]);//assumes one path and assumes it is dss.
        if (readerstatus <0){
            //panic?
            DSSErrorMessage error = reader.getLastError();
            error.printMessage();
            return;
        }
        //find destination parameter
        DataSource destination = action.getParameters().get("destination");
        //create hdf writer
        HecTimeSeries writer = new HecTimeSeries();
        int writerstatus = writer.setDSSFileName(destination.getPaths()[0]);//assumes one path and assumes it is dss.
        if (writerstatus <0){
            //panic?
            DSSErrorMessage error = reader.getLastError();
            error.printMessage();
            return;
        }
        String fill_null = action.getParameters().get("fill_empty_values").getPaths()[0];
        System.out.println("fill_empty_values " + fill_null);
        //convert to bool
        boolean fill = Boolean.parseBoolean(fill_null);
        //read time series from source
        int datasetPathIndex = 0;
        for(String p : source.getDataPaths()){//assumes datapaths for source and dest are ordered the same.
            boolean hasMultiplier = payload.getAttributes().containsKey(p + "- multiplier");
            float multiplier = 1.0f;
            if (hasMultiplier){
                float mult = Float.parseFloat((String) payload.getAttributes().get(p + " - multiplier"));
                multiplier = mult;
            }
            TimeSeriesContainer tsc = new TimeSeriesContainer();
            tsc.fullName = p;

            readerstatus = reader.read(tsc,true);
            if (readerstatus <0){
                //panic?
                DSSErrorMessage error = reader.getLastError();
                error.printMessage();
                break;
            }
            double[] values = tsc.values;
            double[] outvalues = new double[values.length];
            /*System.out.println(p);
            for(double f : values){
                System.out.print(f);
            }
            System.out.println("");*/
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
            /*System.out.println(p);
            for(double f : outvalues){
                System.out.print(f);
            }
            System.out.println("");*/
            //write time series to destination
            TimeSeriesContainer desttsc = new TimeSeriesContainer();
            desttsc.fullName = destination.getDataPaths()[datasetPathIndex];
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
            datasetPathIndex++;
                                        
        }
        //close reader
        reader.close();
        //close writer
        writer.close();
    }
}
