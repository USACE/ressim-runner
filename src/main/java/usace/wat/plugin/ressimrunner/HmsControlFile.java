package usace.wat.plugin.ressimrunner;

import hec.heclib.util.HecTime;

public class HmsControlFile {
    private final String STARTDATE = "     Start Date: ";
    private final String STARTTIME = "     Start Time: ";
    private final String ENDDATE = "     End Date: ";
    private final String ENDTIME = "     End Time: ";
    private HecTime StartDateTime;
    private HecTime EndDateTime;
    public HmsControlFile(byte[] bytes){
        String file = new String(bytes);
        String[] lines = file.split("\\r?\\n");
        String _startDate = "";
        String _endDate = "";
        String _startTime = "";
        String _endTime = "";

        for(String line : lines){
            if (line.contains(STARTDATE)){
                _startDate = line.substring(STARTDATE.length());
            }
            if (line.contains(ENDDATE)){
                _endDate = line.substring(ENDDATE.length());
            }
            if (line.contains(STARTTIME)){
                _startTime = line.substring(STARTTIME.length());
            }
            if (line.contains(ENDTIME)){
                _endTime = line.substring(ENDTIME.length());
            }
        }
        StartDateTime = convert(_startDate, _startTime);
        EndDateTime = convert(_endDate, _endTime);
    }
    private HecTime convert(String date, String time){
        HecTime t = new HecTime(time);
        t.set(date);
        return t;
    }
    public HecTime getStartDateTime(){
        return StartDateTime;
    }
    public HecTime getEndDateTime(){
        return EndDateTime;
    }
}
