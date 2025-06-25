package usace.cc.plugin.ressimrunner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import usace.cc.plugin.api.PluginManager;
import usace.cc.plugin.api.Action;
import usace.cc.plugin.api.DataSource;
import usace.cc.plugin.api.DataStore.DataStoreException;
import usace.cc.plugin.api.IOManager.InvalidDataSourceException;
import usace.cc.plugin.api.IOManager.InvalidDataStoreException;
import usace.cc.plugin.api.Payload;



public class RessimRunner  {
    public static final String PluginName = "ressimrunner";
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        System.out.println(PluginName + " says hello.");
        //check the args are greater than 1
        PluginManager pm = null;
        try {
            pm = PluginManager.getInstance();
        } catch (InvalidDataStoreException e) {
            // TODO Auto-generated catch block
            System.out.println("could not initialize plugin manager");
            System.exit(-1);
        }
        //load payload. 
        Payload mp = pm.getPayload();
        //get Watershed name
        Optional<String> opWatershedName = mp.getAttributes().get("watershed_name");
        if(!opWatershedName.isPresent()){
            System.out.println("could not find payload attribute watershed_name");
            System.exit(-1);
        }
        String watershedName = opWatershedName.get();
        //get Alternative name
        Optional<String> opAlternativeName = mp.getAttributes().get("alternative_name");
        if(!opAlternativeName.isPresent()){
            System.out.println("could not find payload attribute alternative_name");
            System.exit(-1);
        }
        String alternativeName = opAlternativeName.get();
        //get Simulation name?
        Optional<String> opSimulationName = mp.getAttributes().get("simulation_name");
        if(!opSimulationName.isPresent()){
            System.out.println("could not find payload attribute simulation_name");
            System.exit(-1);
        }
        String simulationName = opSimulationName.get();
        //copy the model to local if not local
        //hard coded workingdirectory is fine in a container
        String modelOutputDestination = "/model/"+watershedName+"/";
        File dest = new File(modelOutputDestination);
        deleteDirectory(dest);
        //download the payload to list all input files
        String wkspFilePath = "";
        Boolean foundWkspFile = false;
        for(DataSource i : mp.getInputs()){
            if (i.getName().contains(".wksp")){
                //compute passing in the event config portion of the model payload
                
                if (foundWkspFile){
                    String tmpFile = modelOutputDestination + i.getName();
                    if (wkspFilePath.length()<tmpFile.length()){
                        //skip?
                    }else{
                        wkspFilePath = tmpFile;
                    }
                }else{
                    wkspFilePath = modelOutputDestination + i.getName();
                }
                foundWkspFile = true;
            }
            
            File f = new File(modelOutputDestination, i.getName());
            try {
                if (!f.getParentFile().exists()){
                    f.getParentFile().mkdirs();
                }
                if (!f.createNewFile()){
                    f.delete();
                    if(!f.createNewFile()){
                        System.out.println(f.getPath() + " cant create or delete this location");
                        return;
                    }
                }
                mp.copyFileToLocal(i.getName(), "default", f.getAbsolutePath());
            } catch (IOException | InvalidDataSourceException | DataStoreException e) {
                e.printStackTrace();
                return;
            }
            
        }
        //perform all actions
        for (Action a : mp.getActions()){
            System.out.println(a.getDescription());
            
            switch(a.getType()){
                case "update_timewindow_from_dss":
                    UpdateTimeWindowFromDssAction utwfda = new UpdateTimeWindowFromDssAction(a);
                    utwfda.computeAction();
                    break;
                case "compute_simulation":
                    ComputeAction cs = new ComputeAction(a,simulationName,alternativeName);
                    cs.computeAction();
                    break;
                case "dss_to_dss": 
                    DssToDssAction da = new DssToDssAction(a);
                    da.computeAction();
                    break;
                case "dss_to_dss_merge":
                    DsstoDssAppendAction dtda = new DsstoDssAppendAction(a);
                    dtda.computeAction();
                    break;
                case "hms_control_to_simperiod":
                    SimPeriodAction spa = new SimPeriodAction(a);
                    spa.computeAction();
                    break;
                default:
                break;
            }

        }
        
        for (DataSource output : mp.getOutputs()) { 
            Path path = Paths.get(modelOutputDestination + output.getName());
            try {
                mp.copyFileToRemote(output.getName(), "default", path.toString());
            } catch (Exception e) {
                System.out.println("could not post " + path.toString() + " to " + output.getName() + " at " + output.getPaths().get("default"));
                System.exit(-1);
                return;
            } 
        }
    }
    private static boolean deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }
}
