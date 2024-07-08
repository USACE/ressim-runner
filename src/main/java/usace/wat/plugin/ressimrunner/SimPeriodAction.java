package usace.wat.plugin.ressimrunner;
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
        String lookbackduration = action.getParameters().get("lookback_duration").getPaths()[0];//switch to attributes with SDK update.
        String lookbackWithinTimeWindow = action.getParameters().get("lookback_within_timewindow").getPaths()[0];

        //convert lookback duration (in hours to int)
        int duration = Integer.parseInt(lookbackduration);
        //convert lookbackwithin timewindow to boolean
        Boolean lookbackwithin = Boolean.parseBoolean(lookbackWithinTimeWindow); 

        //parse an HMS control file to find the time range of the compute.
        DataSource controlFile = action.getParameters().get("control_file");
        byte[] controlbytes = pm.getFile(controlFile, 0);
        HmsControlFile hcf = new HmsControlFile(controlbytes);
        //set start end and lookback times.
        HecTime lookback = hcf.getStartDateTime();
        HecTime startTime = hcf.getStartDateTime();
        HecTime endTime = hcf.getEndDateTime();

        if (lookbackwithin){
            //calculate the ressim start time by adding the lookback duration to the start time, and setting the hms start time as the lookback time
            startTime.addHours(duration);
        }else{
            //calculate the ressim start time by subtracting the lookback duration from the start time, and setting the lookback time as a time outside of the hms compute time window
            lookback.subtractHours(duration);
        }
        
        //parse simperiod file
        DataSource simperiodFile = action.getParameters().get("simperiod_file");
        byte[] simperiodbytes = pm.getFile(simperiodFile, 0);
        String simperiodcontent = new String(simperiodbytes);
        String[] simperiodlines = simperiodcontent.split("\\r?\\n");

        //update start end and lookback lines
        for(int i = 0; i<simperiodlines.length;i++){
            if (simperiodlines[i].contains(STARTDATE)){
                simperiodlines[i+1] = "  STR=" + startTime.dateAndTime(104);//.toString();
            }
            if (simperiodlines[i].contains(ENDDATE)){
                simperiodlines[i+1] = "  STR=" + endTime.dateAndTime(104);
            }
            if (simperiodlines[i].contains(LOOKBACKDATE)){
                simperiodlines[i] = "  STR=" + lookback.dateAndTime(104);
            }
        }
        StringBuilder sb = new StringBuilder();
        for(String line : simperiodlines){
           sb.append(line + "\r\n");
        }

        //write out sim period lines to the sim period file.
        pm.putFile(sb.toString().getBytes(), simperiodFile, 0);

        System.out.println("simperiod file updated");
    } 
}
