package usace.wat.plugin.ressimrunner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;

import hec.client.ClientApp;
import hec.heclib.util.HecTime;
import hec.io.Identifier;
import hec.model.ComputeInfo;
import hec.rmi.csinterface.RmiWorkspace;
import hec.rss.AutoLoadRssRmiWorkspace;
import hec.rss.client.RSS;
import hec.rss.model.RssAlt;
import hec.rss.server.AutoLoadRssRmiWorkspaceImpl;
import rma.lang.NestingException;
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
        String lookBackTime =  "29JUN2009,12:00";
        String forecastTime = "30JUN2009,12:00";
        String endTime = "05JUL2009,12:00";
        String overridesDir = "null";
        //copy the model to local if not localcompute
        //hard coded outputdestination is fine in a container
        String modelOutputDestination = "/model/"+mp.getModel().getName()+"/";
        ClientApp.setCanExitJVM(false);
        String[] cmdArgs = new String[]{ "noframe" };
        RSS.startResSim(cmdArgs);
        RSS rss = RSS.rssApp();
        boolean wkspFound = false;
        String wkspFile = modelOutputDestination;
        boolean altFound = false;
        String altFilePath = modelOutputDestination;
        //download the payload to list all input files
        Utilities.CopyPayloadInputsLocally(mp, modelOutputDestination);
        for(ResourcedFileData input : mp.getInputs()){
            if (input.getFileName().contains(mp.getModel().getName() + ".wksp")){
                wkspFound = true;
                wkspFile += input.getFileName();
            }
            if (input.getFileName().contains(mp.getModel().getAlternative()+".alt")){
                altFound = true;
                altFilePath += input.getFileName();
            }
        }
        if (wkspFound && altFound){
            //compute using the workspace file, a provided simulation name and an alternative.
            boolean rv = rss.openWorkspace(new Identifier(wkspFile),true);
            if (!rv){
                return;
            }
            final AutoLoadRssRmiWorkspaceImpl wksp = (AutoLoadRssRmiWorkspaceImpl)RSS.Workspace().getChildWorkspace(AutoLoadRssRmiWorkspace.WKSP_TYPE);
            Identifier id = new Identifier(altFilePath); //@TODO get this from inputs before proceeding, restructure this.
            id.setName(altName);
            RssAlt alt = (RssAlt)RSS.Workspace().loadManager(RmiWorkspace.RSS,RssAlt.class.getName(),id);

            ComputeInfo ci = new ComputeInfo();
            //RemoteWrapper wrapper = new RemoteWrapper(new ComputeProgressProxy(computeListeners));
            ci.altname = altFilePath;
            ci.modelRef = new hec.lang.ModelReference(alt.getIndex(), RssAlt.class.getName(),
                    AutoLoadRssRmiWorkspace.WKSP_TYPE);
            ci.doCompute = true; // recompute all or previous model computed
            ci.forecastpath = modelOutputDestination;
            ci.modelAltComputeTime = 0;
            ci.modelAltname = altName; //was watcomputeoptions.getfpart();
            //@TODO figure out timewindow...
            //ci.runTimeWindow = new RunTimeWindow();
    
            /*
            RssComputeOptions rssCo = new RssComputeOptions();
            rssCo.
            rssCo.dynLinkages = modelAlt.getDynamicLinkages();
            rssCo.isSeededCompute = co.isFrmCompute();
            rssCo.simulationEventRandomSeed = co.getEventRandom();
            rssCo.simulationRealizationRandomSeed = co.getRealizationRandom();
            rssCo.seededIterationNumber = 1; //co.getEventNumberInLifecycle();
            rssCo.setWatComputeOptions(co);
            ci.additionalOptions = rssCo;
            */

            int timeStep = alt.getTimestep();
            int timeStepInc = alt.getTimestepIncrement();
            if ( timeStep == -1 || timeStepInc == -1 )
            {
                timeStep = 1;
                timeStepInc = HecTime.HOUR_INCREMENT;
            }
            ci.runTimeWindow.setTimeStep(timeStep, timeStepInc);
            ci.altIndex = alt.getIndex();
            ci.user = System.getProperty("user.name");
            ci.clientDisplay = null;
            ci.modelPosition = 1;
            ci.programName = "rss";
            //ci.nameMultiplier = modelAlt.getFpart().length();
            ci.inputPosition = 0;
            ci.computeType = hec.model.ComputeInfo.SIMULATION_COMPUTE;
            //ci.outputDSSFileName  = modelAlt.getDssFilename();

            try {
                int crv = wksp.computeAlternative(ci);
            } catch (RemoteException | NestingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            //save the workspace
            ClientApp.Workspace().saveWorkspace();
            //close the application
            ClientApp.frame().exitApplication();
            System.out.println("run completed for " + wkspFile);
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