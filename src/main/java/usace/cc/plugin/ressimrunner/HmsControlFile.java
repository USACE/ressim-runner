package usace.cc.plugin.ressimrunner;

import hec.heclib.util.HecTime;

public class HmsControlFile {
    private final String STARTDATE = "     Start Date: ";
    private final String STARTTIME = "     Start Time: ";
    private final String ENDDATE = "     End Date: ";
    private final String ENDTIME = "     End Time: ";
    private HecTime StartDateTime;
    private HecTime EndDateTime;
    /*public static void main(String[] args) {
        String controlfilelines = "Control: SST\r\n     Last Modified Date: 12 June 2024\r\n     Last Modified Time: 15:35:54\r\n     Version: 4.12\r\n     Time Zone ID: America/Los_Angeles\r\n     Time Zone GMT Offset: -28800000\r\n     Start Date: 1 December 1990\r\n     Start Time: 01:00\r\n     End Date: 5 December 1990\r\n     End Time: 01:00\r\n     Time Interval: 60\r\nEnd:\r\n";
        byte[] cfbytes = controlfilelines.getBytes();
        HmsControlFile hcf = new HmsControlFile(cfbytes);
        System.out.println(hcf.EndDateTime.dateAndTime(104));
    } */
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
        //System.out.println(_startDate);
        //System.out.println(_startTime);
        //System.out.println(_endDate);
        //System.out.println(_endTime);
        StartDateTime = convert(_startDate, _startTime);
        EndDateTime = convert(_endDate, _endTime);
    }
    private HecTime convert(String date, String time){
        //System.out.println(date);
        //System.out.println(time);
        HecTime t = new HecTime(time);
        //System.out.println(t.dateAndTime());
        t.setTime(time);
        //System.out.println(t.dateAndTime());
        t.set(date);
        //System.out.println(t.dateAndTime());
        return t;
    }
    public HecTime getStartDateTime(){
        return StartDateTime;
    }
    public HecTime getEndDateTime(){
        return EndDateTime;
    }
}
