package usace.cc.plugin.ressimrunner;

import java.io.File;

import usace.cc.plugin.Action;

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
        String workspaceFilePath = action.getParameters().get("project_file").getPaths()[0];
        printFileNames("/model");
        System.out.println("opening workspace " + workspaceFilePath);
        String[] Args = new String[]{SCRIPT,workspaceFilePath,simulationName,alternativeName};
        hec.rss.server.RssRMIServer.main(Args);
        System.out.println("simulation completed for " + simulationName);
    }
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
}
