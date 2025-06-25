package usace.cc.plugin.ressimrunner;

import java.io.File;
import java.util.Optional;

import usace.cc.plugin.api.Action;

public class ComputeAction {
    private Action action;
    private String simulationName;
    private String alternativeName;
    private final String SCRIPT = "/HEC-ResSim-3.5.0.280/SimpleServer.py";
    public ComputeAction(Action a, String simname, String altname) {
        this.action = a;
        this.simulationName = simname;
        this.alternativeName = altname;
    }
    public void computeAction(){
        Optional<String> opWorkspaceFilePath = action.getAttributes().get("project_file");
        if(!opWorkspaceFilePath.isPresent()){
            System.out.println("could not find compute-action attribute project_file");
            System.exit(-1);
        }
        String workspaceFilePath = opWorkspaceFilePath.get();
        //printFileNames("/model");
        System.out.println("opening workspace " + workspaceFilePath);
        String[] Args = new String[]{SCRIPT,workspaceFilePath,simulationName,alternativeName};
        hec.rss.server.RssRMIServer.main(Args);
        System.out.println("simulation completed for " + simulationName);
    }
    /* 
    private void printFileNames(String path){
        File[] files = new File(path).listFiles();
        for (File f : files) {
            if (f.isDirectory()) {
                printFileNames(f.getAbsolutePath());
            }else{
                System.out.println(f.getAbsolutePath());
            }
        }        
    }
        */
}
