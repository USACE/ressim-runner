package usace.cc.plugin.ressimrunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

import usace.cc.plugin.api.Action;
import usace.cc.plugin.api.DataSource;
import usace.cc.plugin.api.DataStore.DataStoreException;
import usace.cc.plugin.api.IOManager.InvalidDataSourceException;

public class PeaksMergerAction {
    private Action action;
    public PeaksMergerAction(Action a) {
        action = a;
    }
    public void computeAction(){
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
        Optional<ArrayList<Integer>> opDurations = action.getAttributes().get("exported-peak-durations");
        if(!opDurations.isPresent()){
            System.out.println("could not find action attribute named exported-peak-durations");
            return;
        }
        ArrayList<Integer> durations = opDurations.get();
        //start event number
        Optional<Integer> opStartEvent = action.getAttributes().get("start-event");
        if(!opStartEvent.isPresent()){
            System.out.println("could not find action attribute named start-event");
            return;
        }
        //end event number
        Optional<Integer> opEndEvent = action.getAttributes().get("end-event");
        if(!opEndEvent.isPresent()){
            System.out.println("could not find action attribute named end-event");
            return;
        }
        //get all event data loaded into a string builder per duration.
        ArrayList<StringBuilder> data = new ArrayList<StringBuilder>();
        String defaultPath = source.getPaths().get("default");
        for(int i = opStartEvent.get(); i<=opEndEvent.get();i++){
            String eventpath = defaultPath.replace("$eventnumber",Integer.toString(i));
            if(i==opStartEvent.get()){
                //load in the header
                String header = extractHeaderFromCSV(eventpath, durations.get(0), source);
                for(Integer d : durations){
                    StringBuilder sb = new StringBuilder();
                    sb.append(header + "\n");
                    data.add(sb);
                }
            }
            String[] peaks = extractPeaksFromCSV(eventpath, durations, source);
            //load into appropriae string builder
            for(int j = 0; j < peaks.length; j++){
                StringBuilder sb = data.get(j);
                sb.append(peaks[j] + "\n");
                data.set(j, sb);
            }

        }

        for(int i = 0; i <durations.size();i++){
            //create a path//
            try{
                action.put(data.get(i).toString().getBytes(), destination.getName(), Integer.toString(durations.get(i)), ""); 
            }catch(Exception ex){
                System.out.println("failed writing duration " + Integer.toString(durations.get(i)));
                System.exit(-1);
            }
                      
        }
    }
        private String[] extractPeaksFromCSV(String path, ArrayList<Integer> durations, DataSource source){
            String[] peaks = new String[durations.size()];
            for(Integer d : durations){
                String durpath = path.replace("$duration", Integer.toString(d));
                source.getPaths().put("default", durpath);
                try {
                    byte[] data = action.get(source.getName(), "default", "");
                    String strdata = new String(data);
                    String row = strdata.split("\n")[1];
                    peaks[durations.indexOf(d)] = row;
                } catch (InvalidDataSourceException | IOException|DataStoreException e) {
                    System.out.println("could not fetch row from path " + durpath);;
                    System.exit(-1);
                }
            }
            return peaks;
        }
        private String extractHeaderFromCSV(String path, Integer duration, DataSource source){
            String durpath = path.replace("$duration", Integer.toString(duration));
            source.getPaths().put("default", durpath);
            try {
                byte[] data = action.get(source.getName(), "default", "");
                String strdata = new String(data);
                String header = strdata.split("\n")[0];
                return header;
            } catch (InvalidDataSourceException | IOException|DataStoreException e) {
                System.out.println("could not fetch header from first event at path " + durpath);;
                System.exit(-1);
            }
            return null;
        }
}
