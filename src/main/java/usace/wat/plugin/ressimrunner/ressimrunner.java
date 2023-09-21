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
import usace.cc.plugin.Payload;
import usace.cc.plugin.PluginManager;


public class ressimrunner  {
    public static final String PluginName = "ressimrunner";
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        System.out.println(PluginName + " says hello.");
        String wkspFile = "/HEC-ResSim-3.5.0.280/Examples/ExampleWatersheds/base/BaldEagle_V3.1/BaldEagle_V3.1.wksp";//modelOutputDestination;
        String simFilePath = "/HEC-ResSim-3.5.0.280/Examples/ExampleWatersheds/base/BaldEagle_V3.1/rss/1993.11.27-1400.simperiod";//modelOutputDestination;

        if (true){
            try {
                RssRMIServer server = new hec.rss.server.RssRMIServer();
                
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            RmiAppImpl rmiapp = RmiAppImpl.getApp();
            RmiFileManager filemanager = rmiapp.getFileManager();
            Identifier wkspid = new Identifier(wkspFile); 
            try {
                wkspid = filemanager.openFile("ressimrunner", wkspid);
                RmiWorkspace rmiwksp = rmiapp.openWorkspace("ressimrunner",wkspid);
                RmiWorkspace rssrmiwksp = rmiwksp.getChildWorkspace("rss");
                Identifier simid = new Identifier(simFilePath);
                SimulationPeriod sim = (SimulationPeriod)rssrmiwksp.getManager(SimulationPeriod.class.getName(),simid);
                sim.loadWorkspace(null, "/HEC-ResSim-3.5.0.280/Examples/ExampleWatersheds/base/BaldEagle_V3.1/");//modelOutputDestination);
                SimulationRun simrun = sim.getSimulationRun("NoDSOps");
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
}