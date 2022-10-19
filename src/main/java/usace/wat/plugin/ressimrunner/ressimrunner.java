package usace.wat.plugin.ressimrunner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import hec.csinterface.RmiFileManager;
import hec.io.Identifier;
import hec.model.SimulationPeriod;
import hec.model.SimulationRun;
import hec.rmi.csinterface.RmiWorkspace;
import hec.rss.server.RssRMIServer;
import hec.server.RmiAppImpl;
import rma.util.RMAIO;

import usace.wat.plugin.Message;
import usace.wat.plugin.ModelPayload;
import usace.wat.plugin.ResourcedFileData;
import usace.wat.plugin.Utilities;


public class ressimrunner  {
    public static final String PluginName = "ressimrunner";
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        System.out.println(PluginName + " says hello.");
        //check the args are greater than 1
        

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

        /*Utilities.InitalizeFromEnv();
        //load payload. 
        ModelPayload mp = Utilities.LoadPayload(filepath);
        */
        ModelPayload mp = LoadLocalPayload(filepath);
        String wkspName = "BaldEagle_V3.1";
        String simName = RMAIO.userNameToFileName(mp.getModel().getName());
        String altName = mp.getModel().getAlternative();
        String altFileName = RMAIO.userNameToFileName(mp.getModel().getAlternative());

        //copy the model to local if not localcompute
        //hard coded outputdestination is fine in a container
        //String modelOutputDestination = "/model/"+mp.getModel().getName()+"/";
        try {
            RssRMIServer app = new RssRMIServer();
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        boolean wkspFound = false;
        String wkspFile = "/workspaces/ressim-runner/rss/hec-ressim-3.5.0.280-linux-x86_64.tar/ressim-280-wat/HEC-ResSim-3.5.0.280/Examples/ExampleWatersheds/base/BaldEagle_V3.1/BaldEagle_V3.1.wksp";//modelOutputDestination;
        boolean altFound = false;
        boolean simFound = false;
        String simFilePath = "/workspaces/ressim-runner/rss/hec-ressim-3.5.0.280-linux-x86_64.tar/ressim-280-wat/HEC-ResSim-3.5.0.280/Examples/ExampleWatersheds/base/BaldEagle_V3.1/rss/1993.11.27-1400.simperiod";//modelOutputDestination;
        //download the payload to list all input files
        //Utilities.CopyPayloadInputsLocally(mp, modelOutputDestination);
        for(ResourcedFileData input : mp.getInputs()){
            if (input.getFileName().contains(wkspName + ".wksp")){
                wkspFound = true;
                wkspFile += input.getFileName();
            }
            if (input.getFileName().contains(altFileName +".malt")){
                altFound = true;
            }
            if (input.getFileName().contains(simName + ".simperiod")){
                simFound = true;
                simFilePath += "rss/" + input.getFileName();
            }
        }
        if (wkspFound && altFound && simFound){
            RmiAppImpl rmiapp = RmiAppImpl.getApp();
            RmiFileManager filemanager = rmiapp.getFileManager();
            Identifier wkspid = new Identifier(wkspFile); 
            try {
                wkspid = filemanager.openFile("ressimrunner", wkspid);
                RmiWorkspace rmiwksp = rmiapp.openWorkspace("ressimrunner",wkspid);
                RmiWorkspace rssrmiwksp = rmiwksp.getChildWorkspace("rss");
                Identifier simid = new Identifier(simFilePath);
                SimulationPeriod sim = (SimulationPeriod)rssrmiwksp.getManager(SimulationPeriod.class.getName(),simid);
                sim.loadWorkspace(null, "/workspaces/ressim-runner/rss/hec-ressim-3.5.0.280-linux-x86_64.tar/ressim-280-wat/HEC-ResSim-3.5.0.280/Examples/ExampleWatersheds/base/BaldEagle_V3.1/");//modelOutputDestination);
                SimulationRun simrun = sim.getSimulationRun(altName);
                sim.setComputeAll(true);
                sim.computeRun(simrun, -1);
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            System.out.println("run completed for " + wkspFile);
        }
        //push results to s3.
        /*
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
        */
    }
    private static ModelPayload LoadLocalPayload(String filepath) {
        File file = new File(filepath);
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory()); // jackson databind
        try {
            return mapper.readValue(file, ModelPayload.class);
        } catch (Exception e) {
            Message message = Message.BuildMessage()
            .withMessage("Error Parsing Payload Contents: " + e.getMessage())
            .fromSender("Plugin Services")
            .build();
            Utilities.Log(message);
        }
        return new ModelPayload();
    }

}