package usace.cc.plugin.ressimrunner;

import java.util.ArrayList;
import java.util.Optional;

import hec.heclib.dss.DSSErrorMessage;
import hec.heclib.dss.HecTimeSeries;
import hec.io.TimeSeriesContainer;
import usace.cc.plugin.api.Action;
import usace.cc.plugin.api.DataSource;

public class PostPeaksAction {
    private Action action;
    public PostPeaksAction(Action a) {
        action = a;
    }
    public void computeAction(){
        String event_identifier = System.getenv("CC_EVENT_IDENTIFIER");
        //find source 
        Optional<DataSource> opSource = action.getInputDataSource("source");
        if(!opSource.isPresent()){
            System.out.println("could not find input datasource named source in the post peaks action");
            System.exit(-1);
        }
        DataSource source = opSource.get();

        //find destination parameter
        Optional<DataSource> opDestination = action.getOutputDataSource("destination");
        if(!opDestination.isPresent()){
            System.out.println("could not find output datasource named destination in the post peaks action");
            System.exit(-1);
        }
        DataSource destination = opDestination.get();
        Optional<ArrayList<String>> opPathNames = action.getAttributes().get("exported-peak-paths");
        if(!opPathNames.isPresent()){
            System.out.println("could not find action attribute named exported-peak-paths");
            return;
        }
        ArrayList<String> pathNames = opPathNames.get();
        Optional<ArrayList<Integer>> opDurations = action.getAttributes().get("exported-peak-durations");
        if(!opDurations.isPresent()){
            System.out.println("could not find action attribute named exported-peak-durations");
            return;
        }
        ArrayList<Integer> durations = opDurations.get();
        //dsspath
        String dsspath = source.getPaths().get("default");
        double[][] peaks = extractPeaksFromDSS(dsspath, durations, pathNames);
        //create header row //
        String header = "eventNumber,";
        for(String name : pathNames){
            header += name + ",";
        }
        header = header.substring(0,header.length()-1);
        header += "\n";
        for(Integer duration : durations){
            //create a path//
            //add header row.
            StringBuilder sb = new StringBuilder();
            sb.append(header);
                double[] vals = peaks[durations.indexOf(duration)];
                sb.append(event_identifier + ",");
                for(double value : vals){
                    sb.append(Double.toString(value) + ",");
                }
                sb.append("\n");
            try{
                action.put(sb.toString().getBytes(), destination.getName(), Integer.toString(duration), ""); 
            }catch(Exception ex){
                System.out.println("failed writing duration " + Integer.toString(duration));
            }
                      
        }

    }
    private double[][] extractPeaksFromDSS(String path, ArrayList<Integer> timesteps, ArrayList<String> datapaths){
        HecTimeSeries reader = new HecTimeSeries();
        int status = reader.setDSSFileName(path);
        double[][] result = new double[timesteps.size()][];
        if (status <0){
            //panic?
            DSSErrorMessage error = reader.getLastError();
            error.printMessage();
            return result;
        }

        for (int timestep = 0; timestep < timesteps.size(); timestep++){
            result[timestep] = new double[datapaths.size()];
        }
        int datapathindex = 0;
        for(String datapath : datapaths){
            //get the data.
            TimeSeriesContainer tsc = new TimeSeriesContainer();
            tsc.fullName = datapath;
            status = reader.read(tsc,true);
            if (status <0){
                //panic?
                DSSErrorMessage error = reader.getLastError();
                error.printMessage();
                reader.closeAndClear();
                return result;
            }
            int durationIndex = 0;
            double[] data = tsc.values;
            for (int duration : timesteps){
                //find duration peak.
                double maxval = 0.0;
                double runningVal = 0.0;
                for (int timestep = 0; timestep < data.length; timestep++)
                {
                    runningVal += data[timestep];
                    if (timestep < duration)
                    {
                        maxval = runningVal;
                    }
                    else
                    {
                        runningVal -= data[timestep - duration];
                        if (runningVal > maxval)
                        {
                            maxval = runningVal;
                        }
                    }
                }
                result[durationIndex][datapathindex] = maxval/(double)duration;
                durationIndex ++;
            }
            datapathindex ++;
        }  
        reader.closeAndClear(); //why so many close options? seems like close should do what it needs to do.
        return result;
    }
}
