package usace.wat.plugin.ressimrunner;

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
        String workspaceFilePath = action.getParameters().get("wksp_file").getPaths()[0];
        System.out.println("opening workspace " + workspaceFilePath);
        String[] Args = new String[]{SCRIPT,workspaceFilePath,simulationName,alternativeName};
        hec.rss.server.RssRMIServer.main(Args);
        System.out.println("simulation completed for " + simulationName);
    }
}
