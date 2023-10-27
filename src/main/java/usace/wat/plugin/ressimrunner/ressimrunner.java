package usace.wat.plugin.ressimrunner;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import usace.cc.plugin.DataSource;
import usace.cc.plugin.Payload;
import usace.cc.plugin.PluginManager;


public class ressimrunner  {
    public static final String PluginName = "ressimrunner";
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        System.out.println(PluginName + " says hello.");
        //check the args are greater than 1
        PluginManager pm = PluginManager.getInstance();
        //load payload. 
        Payload mp = pm.getPayload();
        //get Watershed name
        String watershedName = (String) mp.getAttributes().get("watershed_name");
        //get Alternative name
        String alternativeName = (String) mp.getAttributes().get("alternative_name");
        //get Simulation name?
        String simulationName = (String) mp.getAttributes().get("simulation_name");
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
            byte[] bytes = pm.getFile(i, 0);
            //write bytes locally.
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
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            try(FileOutputStream outputStream = new FileOutputStream(f)){
                outputStream.write(bytes);
            }catch(Exception e){
                e.printStackTrace();
                return;
            }
        }    
        String[] blankArgs = new String[]{"/HEC-ResSim-3.5.0.280/SimpleServer.py",wkspFilePath,simulationName,alternativeName};
        hec.rss.server.RssRMIServer.main(blankArgs);
        System.out.println("simulation completed for " + simulationName);
        //push results to s3.
        for (DataSource output : mp.getOutputs()) { 
            Path path = Paths.get(modelOutputDestination + output.getName());
            byte[] data;
            try {
                data = Files.readAllBytes(path);
                pm.putFile(data, output,0);
            } catch (IOException e) {
                e.printStackTrace();
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