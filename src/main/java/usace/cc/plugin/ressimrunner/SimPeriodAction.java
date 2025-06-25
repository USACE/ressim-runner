package usace.cc.plugin.ressimrunner;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

import hec.heclib.util.HecTime;
import usace.cc.plugin.api.Action;
import usace.cc.plugin.api.DataSource;
import usace.cc.plugin.api.DataStore.DataStoreException;
import usace.cc.plugin.api.IOManager.InvalidDataSourceException;
public class SimPeriodAction {
    private Action action;
    private final String STARTDATE = "  FLD=_startDate";
    private final String ENDDATE = "  FLD=_endDate";
    private final String LOOKBACKDATE = "  FLD=_lookbackDate";
    public SimPeriodAction(Action a) {
        this.action = a;

    }

    public void computeAction(){
        Optional<String> oplookback_adjustment = action.getAttributes().get("lookback_adjustment");//switch to attributes with SDK update.
        Optional<String> opstart_time_adjustment = action.getAttributes().get("start_time_adjustment");
        if(!oplookback_adjustment.isPresent()){
            System.out.println("could not find attribute lookback_adjustment in action simperiod_action");
            System.exit(-1);
        }
        if(!opstart_time_adjustment.isPresent()){
            System.out.println("could not find attribute start_time_adjustment in action simperiod_action");
            System.exit(-1);
        }
        String lookback_adjustment = oplookback_adjustment.get();
        String start_time_adjustment = opstart_time_adjustment.get();
        //convert lookback adjustment (in hours to int)
        int lb_adjustment = Integer.parseInt(lookback_adjustment);
        //convert lookbackwithin timewindow to boolean
        int st_adjustment = Integer.parseInt(start_time_adjustment);
        byte[] controlbytes = null; 
        try {
            controlbytes = action.get("control_file","default","");
        } catch (IOException | InvalidDataSourceException | DataStoreException e) {
            System.out.println("could not get bytes for control_file");
            System.exit(-1);
        }
        HmsControlFile hcf = new HmsControlFile(controlbytes);
        //System.out.println(new String(controlbytes));
        //set start end and lookback times.
        HecTime lookback = hcf.getStartDateTime();
        HecTime startTime = (HecTime)hcf.getStartDateTime().clone();
        HecTime endTime = hcf.getEndDateTime();

        
            //calculate the starttime by adjusting HMS control start time to reflect when the start time starts
            startTime.addHours(st_adjustment);

            //calculate the lookback by adjusting HMS control start time to reflect when the lookbackstarts
            lookback.addHours(lb_adjustment);

        //System.out.println(lookback.dateAndTime(104));
        //System.out.println(startTime.dateAndTime(104));

        //parse simperiod file

        byte[] simperiodbytes = null;
        try {
            simperiodbytes = action.get("simperiod_file", "default", "");
        } catch (IOException | InvalidDataSourceException | DataStoreException e) {
            System.out.println("could not get bytes for simperiod_file");
            System.exit(-1);
        }
        //byte[] simperiodbytes = pm.getFile(simperiodFile, 0); // does not support local file types
        String simperiodcontent = new String(simperiodbytes);
        //System.out.println(simperiodcontent);
        String[] simperiodlines = simperiodcontent.split("\\r?\\n");

        //update start end and lookback lines
        for(int i = 0; i<simperiodlines.length;i++){
            if (simperiodlines[i].contains(STARTDATE)){
                simperiodlines[i+1] = "  STR=" + startTime.dateAndTime(104).replace(", ", ",").replace(":","");//.toString();
            }
            if (simperiodlines[i].contains(ENDDATE)){
                simperiodlines[i+1] = "  STR=" + endTime.dateAndTime(104).replace(", ", ",").replace(":","");
            }
            if (simperiodlines[i].contains(LOOKBACKDATE)){
                simperiodlines[i+1] = "  STR=" + lookback.dateAndTime(104).replace(", ", ",").replace(":","");
            }
        }
        StringBuilder sb = new StringBuilder();
        for(String line : simperiodlines){
           sb.append(line + "\r\n");
           System.out.println(line);
        }

        //write out sim period lines to the sim period file.
        Optional<DataSource> opSimperiodFile = action.getInputDataSource("simperiod_file");
        simperiodbytes = sb.toString().getBytes();
        try {
            //getting the path without checking the optional is present is safe because it is the same path we used to get the datafile from aws.
            Files.write(Paths.get(opSimperiodFile.get().getPaths().get("default")),simperiodbytes);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return;
        }
        System.out.println("simperiod file updated");
    } 
}
