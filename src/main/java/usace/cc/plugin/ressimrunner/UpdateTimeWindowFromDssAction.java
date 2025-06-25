package usace.cc.plugin.ressimrunner;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;

import hec.heclib.dss.DSSErrorMessage;
import hec.heclib.dss.HecTimeSeries;
import hec.heclib.util.HecTime;
import hec.io.TimeSeriesContainer;
import usace.cc.plugin.api.Action;
import usace.cc.plugin.api.DataSource;
import usace.cc.plugin.api.DataStore.DataStoreException;
import usace.cc.plugin.api.IOManager.InvalidDataSourceException;
import usace.cc.plugin.api.IOManager.InvalidDataStoreException;

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
        //find simperiod
        Optional<DataSource> opSimperiod = action.getInputDataSource("simperiod");
        if(!opSimperiod.isPresent()){
            System.out.println("could not find input datasource named simperiod");
            System.exit(-1);
        }
        DataSource simperiod = opSimperiod.get();
        //find input dss file
        Optional<DataSource> opDssFile = action.getInputDataSource("dssfile");
        if(!opDssFile.isPresent()){
            System.out.println("could not find input datasource named dssfile");
            System.exit(-1);
        }
        DataSource dssFile = opDssFile.get();
        //create dss reader
        //open up the dss file. reference: https://www.hec.usace.army.mil/confluence/display/dssJavaprogrammer/General+Example
        HecTimeSeries reader = new HecTimeSeries();
        int readerstatus = reader.setDSSFileName(dssFile.getPaths().get("default"));//assumes one path and assumes it is dss.
        if (readerstatus <0){
            //panic?
            DSSErrorMessage error = reader.getLastError();
            error.printMessage();
            return;
        }
        Optional<Map<String,String>> opDssFileDataPaths = dssFile.getDataPaths();
        if(!opDssFileDataPaths.isPresent()){
            System.out.println("dssfile input datasource did not have any datapaths");
            System.exit(-1);
        }
        String p = opDssFileDataPaths.get().get("default");
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
        Optional<Integer> opLookbackDurationSteps = action.getAttributes().get(LOOKBACKDURATION);
        if(!opLookbackDurationSteps.isPresent()){
            System.out.println("update time window from dss action did not specify " + LOOKBACKDURATION);
            System.exit(-1);
        }
        Integer lookbackDurationSteps = opLookbackDurationSteps.get();

        Optional<String> opLookbackDurationUnit = action.getAttributes().get(LOOKBACKUNITS);
        if(!opLookbackDurationUnit.isPresent()){
            if(!opLookbackDurationSteps.isPresent()){
                System.out.println("update time window from dss action did not specify " + LOOKBACKUNITS);
                System.exit(-1);
            }
        }
        String lookbackDurationUnit = opLookbackDurationUnit.get();
        String startTime = ComputeStartDateTime(lookbackDurationSteps, lookbackDurationUnit, tsc);
        byte[] sBytes;
        try {
            sBytes = Files.readAllBytes(Paths.get(simperiod.getPaths().get("default")));
            sBytes = UpdateSimPeriodFile(dssLookbackTime,startTime,dssEndTime, sBytes);
            Files.write(Paths.get(simperiod.getPaths().get("default")),sBytes);
            action.put(sBytes, simperiod.getName(),"default","");
        } catch (IOException | InvalidDataSourceException | InvalidDataStoreException | DataStoreException e) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
            System.out.println("error updating simperiod and/or pushing it to remote");
            System.exit(-1);
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
