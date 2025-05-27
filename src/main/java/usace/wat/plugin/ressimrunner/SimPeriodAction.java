package usace.wat.plugin.ressimrunner;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import hec.heclib.util.HecTime;
import usace.cc.plugin.Action;
import usace.cc.plugin.DataSource;
import usace.cc.plugin.PluginManager;
public class SimPeriodAction {
    private Action action;
    private final String STARTDATE = "  FLD=_startDate";
    private final String ENDDATE = "  FLD=_endDate";
    private final String LOOKBACKDATE = "  FLD=_lookbackDate";
    public SimPeriodAction(Action a) {
        this.action = a;

    }

    public void computeAction(){
        PluginManager pm = PluginManager.getInstance();
        String lookback_adjustment = action.getParameters().get("lookback_adjustment").getPaths()[0];//switch to attributes with SDK update.
        String start_time_adjustment = action.getParameters().get("start_time_adjustment").getPaths()[0];

        //convert lookback adjustment (in hours to int)
        int lb_adjustment = Integer.parseInt(lookback_adjustment);
        //convert lookbackwithin timewindow to boolean
        int st_adjustment = Integer.parseInt(start_time_adjustment); 

        //parse an HMS control file to find the time range of the compute.
        DataSource controlFile = action.getParameters().get("control_file");
        //byte[] controlbytes = pm.getFile(controlFile, 0);//does not support local file operations
        byte[] controlbytes;
        try {
            controlbytes = Files.readAllBytes(Paths.get(controlFile.getPaths()[0]));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return;
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
        DataSource simperiodFile = action.getParameters().get("simperiod_file");
        byte[] simperiodbytes;
        try {
            simperiodbytes = Files.readAllBytes(Paths.get(simperiodFile.getPaths()[0]));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return;
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
        //pm.putFile(sb.toString().getBytes(), simperiodFile, 0);//does not support local write operations.
        simperiodbytes = sb.toString().getBytes();
        try {
            Files.write(Paths.get(simperiodFile.getPaths()[0]),simperiodbytes);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return;
        }
        System.out.println("simperiod file updated");
    } 
}
