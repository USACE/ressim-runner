package usace.wat.plugin.ressimrunner;

import usace.wat.plugin.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import hec.rss.model.RssRun;


public class ressimrunner  {
    public static final String PluginName = "ressimrunner";
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        System.out.println(PluginName + " says hello.");
        //check the args are greater than 1
        Utilities.InitalizeFromEnv();

        if(args.length!=2){
            for(String s : args){
                System.out.println("arg " + s);
            }
            System.out.println("Did not detect only payload `pathtopayload` argument");
            return;
        }else{
            for(String s : args){
                System.out.println("arg " + s);
            }
        }
        //first arg should be a modelpayload check to see it is
        String filepath = args[1];
        //load payload. 
        ModelPayload mp = Utilities.LoadPayload(filepath);
        String simName = mp.getModel().getName();
        String altName = mp.getModel().getAlternative();
        //copy the model to local if not local
        //hard coded outputdestination is fine in a container
        String modelOutputDestination = "/model/"+mp.getModel().getName()+"/";
        //download the payload to list all input files
        Utilities.CopyPayloadInputsLocally(mp, modelOutputDestination);
        for(ResourcedFileData input : mp.getInputs()){
            if (input.getFileName().contains(mp.getModel().getName() + ".wksp")){
                //compute passing in the event config portion of the model payload
                String wkspFile = modelOutputDestination + input.getFileName();
                System.out.println("preparing to run " + wkspFile);
                RssRun rr = new RssRun(wkspFile);
                /*ResSim.openWatershed(wkspFile);
                ResSim.selectModule("Simulation");
                simModule = ResSim.getCurrentModule();
                if(simModule.simulationExists(simName)){
                    simModule.deleteSimulation(simName);
                }
                alts = jarray.array([altNamePadded], String);
                simulation = simModule.createSimulation(simName, "created by " + PluginName,lookBackTime,forecastTime,endTime,1,HecTime.Hour_INCREMENT, alts);
                if (os.path.exists(overridesDir + "/" + overrideName)){
                    shutil.copy(overridesDir + "/" + overrideName, watershedDir + "/rss/" + simName+ "/rss/" + overrideName);
                }
                simRun = simModule.getSimulationRun(altName);
                simModule.computeRun(simRun, -1, Constants.TRUE, Constants.TRUE);
                //save the workspace
                ClientApp.Workspace().saveWorkspace();

                ClientApp.frame().exitApplication();*/
                System.out.println("run completed for " + wkspFile);
                break;
            }
        }
        //push results to s3.
        for (ResourcedFileData output : mp.getOutputs()) {
            //ResourceInfo ri = new ResourceInfo();
            //need to set the resource info
            //Utilities.DownloadObject(info)  
            Path path = Paths.get(modelOutputDestination + output.getFileName());
            byte[] data;
            try {
                data = Files.readAllBytes(path);
                Utilities.UploadFile(output.getResourceInfo(), data);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }  
        }
    }

}