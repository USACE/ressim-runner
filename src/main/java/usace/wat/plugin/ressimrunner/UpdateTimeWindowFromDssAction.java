package usace.wat.plugin.ressimrunner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Paths;

import hec.heclib.dss.DSSErrorMessage;
import hec.heclib.dss.HecTimeSeries;
import hec.heclib.util.HecTime;
import hec.io.TimeSeriesContainer;
import usace.cc.plugin.Action;
import usace.cc.plugin.DataSource;
import usace.cc.plugin.Payload;
import usace.cc.plugin.PluginManager;

public class UpdateTimeWindowFromDssAction {
    private Action action;
    private final String STARTDATE = "  FLD=_startDate";
    private final String ENDDATE = "  FLD=_endDate";
    private final String LOOKBACKDATE = "  FLD=_lookbackDate";
    private final String STR = "  STR=";
    private final String LOOKBACKDURATION = "lookback_duration";
    private final String LOOKBACKUNITS = "lookback_units";
    public UpdateTimeWindowFromDssAction(Action a) {
        action = a;
    }
    public void computeAction(){
        PluginManager pm = PluginManager.getInstance();
        //get Payload
        Payload payload = pm.getPayload();
        //find simperiod
        DataSource simperiod = action.getParameters().get("simperiod");
        //find input dss file
        DataSource dssFile = action.getParameters().get("dssfile");
        //create dss reader
        //open up the dss file. reference: https://www.hec.usace.army.mil/confluence/display/dssJavaprogrammer/General+Example
        HecTimeSeries reader = new HecTimeSeries();
        int readerstatus = reader.setDSSFileName(dssFile.getPaths()[0]);//assumes one path and assumes it is dss.
        if (readerstatus <0){
            //panic?
            DSSErrorMessage error = reader.getLastError();
            error.printMessage();
            return;
        }
        String p = dssFile.getDataPaths()[0];
        TimeSeriesContainer tsc = new TimeSeriesContainer();
        tsc.fullName = p;
        readerstatus = reader.read(tsc,true);
        if (readerstatus <0){
            //panic?
            DSSErrorMessage error = reader.getLastError();
            error.printMessage();
            return;
        }
        String dssLookbackTime = tsc.getStartTime().dateAndTime();
        String dssEndTime = tsc.getEndTime().dateAndTime();
        Integer lookbackDurationSteps = Integer.parseInt((String)payload.getAttributes().get(LOOKBACKDURATION));
        String lookbackDurationUnit = (String)payload.getAttributes().get(LOOKBACKUNITS);
        String startTime = ComputeStartDateTime(lookbackDurationSteps, lookbackDurationUnit, tsc);
        byte[] sBytes;
        try {
            sBytes = Files.readAllBytes(Paths.get(simperiod.getPaths()[0]));
            sBytes = UpdateSimPeriodFile(dssLookbackTime,startTime,dssEndTime, sBytes);
            Files.write(Paths.get(simperiod.getPaths()[0]),sBytes);
            pm.putFile(sBytes, simperiod, 0);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


    }
    private String ComputeStartDateTime(Integer lookbackDurationSteps, String lookbackDurationUnits, TimeSeriesContainer tsc){
        HecTime lb = tsc.getStartTime();

        switch (lookbackDurationUnits) {
            case "timesteps":
                int interval = tsc.getTimeInterval();
                lb.add(interval*lookbackDurationSteps);
                break;
            case "hours":
                lb.addHours(lookbackDurationSteps);
                break;
            case "minutes":
                lb.addMinutes(lookbackDurationSteps);
                break;
            case "days":
                lb.addDays(lookbackDurationSteps);
                break;
            default:
                lb.addDays(3);
                System.out.println("Using Default of 3 days lookback period.");
                break;
        }
        if(lb.greaterThan(tsc.getEndTime())){
            System.out.println("Start time is after end time.");
            return tsc.getStartTime().toString();
        }
        return lb.toString();
    }
    private byte[] UpdateSimPeriodFile(String LookbackDateTime, String StartDateTime, String EndDateTime, byte[] SimPeriodBytes){
        String file = new String(SimPeriodBytes);
        String[] inputlines = file.split("\r\n");
        Integer size = inputlines.length;
        StringBuilder output = new StringBuilder();
        for(int i = 0; i<size;i++){
            if(inputlines[i].contains(STARTDATE)){
                output.append(inputlines[i]+"\r\n");
                if(inputlines[i+1].contains(STR)){
                    inputlines[i+1] = STR + StartDateTime;
                    i++;
                    output.append(inputlines[i]+"\r\n");
                }
            }else if(inputlines[i].contains(LOOKBACKDATE)){
                output.append(inputlines[i]+"\r\n");
                if(inputlines[i+1].contains(STR)){
                    inputlines[i+1] = STR + LookbackDateTime;
                    i++;
                    output.append(inputlines[i]+"\r\n");
                }
            }else if(inputlines[i].contains(ENDDATE)){
                output.append(inputlines[i]+"\r\n");
                if(inputlines[i+1].contains(STR)){
                    inputlines[i+1] = STR + EndDateTime;
                    i++;
                    output.append(inputlines[i]+"\r\n");
                }
            }else{
                output.append(inputlines[i]+"\r\n");
            }

        }
        //System.out.println(output.toString());
        return output.toString().getBytes();
    }
}
