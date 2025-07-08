# RunHeadless.py
# This is a ResSim (server-side) command script to:
# open a watershed, then compute an alternative in a simulation
#	original: M.Ackerman & J.Klipsch, Nov2008
#	stripped down to its simplest form: J.Klipsch Sep 2023

#	Revise the following three strings to match your watershed, simulation, and alternative names.
#	The wkspFile must be a fully qualified path to the .wksp file in your watershed.
#	The simulationName is only the name of the simulation, not the path to it.
#	The alternativeName is only the name of the alternative, not the run name (i.e., no dashes).
#		However, you can enter -All- to compute all alts in the simulation.

#	Note from Joan.
#		I'm not sure if you can specify a trial to run.  
#		If it is possible, then it will be computed as part of -All-...
#		To see how to identify a trial, compute -All- for a simulation that contains a trial,
#		then check the console log for the altName of each "run" computed for -All-.
#		You should be able to identify your trial in the list of runs, IF it ran.

LogLevel = 3

from hec.server import RmiAppImpl
from hec.io import Identifier
from java.lang import System
from hec.rss.model import SimulationExtractModel
from hec.script import Constants
from rma.util import RMAIO
from hec.heclib.dss import HecDSSFileAccess
import os

def openWatershed(workspaceFile, user):
	rmiApp = RmiAppImpl.getApp()

	workspaceFile = workspaceFile.replace(os.sep, "/")	#ResSim doesn't like backslashes in paths
	assert os.path.isfile(workspaceFile), "####SCRIPT### - Watershed file does exist"

	id = Identifier(workspaceFile)
	rmiWksp = rmiApp.openWorkspace(user, id)
	if rmiWksp == None:
		print("ERROR: Failed to open Watershed "+workspaceFile)
		return rmiWksp
	rssRmiWksp = rmiWksp.getChildWorkspace("rss")
	return rssRmiWksp

def openSimulation(simulationName, rssWksp):
	wtrshdPath= rssWksp.getWorkspacePath()
	simulationPath = wtrshdPath+"/rss/"+simulationName+".simperiod"
	assert os.path.isfile(simulationPath), "####SCRIPT### - Simulation's simperiod file does exist"

	simId = Identifier(simulationPath)
	print(simId)
	simMgr = rssWksp.getManager("hec.model.SimulationPeriod", simId)
	if simMgr == None:
		print("ERROR: Failed to getManager for simulation "+simulationName)
		return simMgr
	simMgr.loadWorkspace(None,wtrshdPath)
	return simMgr

def computeAll(simMgr):
	runs = simMgr.getSimulationRuns()
	for run in runs:
		altname = run.getUserName()
		print("\n####SCRIPT### ----------------------------------------------------------------------------------------------")
		print("####SCRIPT### Computing alternative: \t" +altName)
		print("####SCRIPT### ----------------------------------------------------------------------------------------------\n")
		simRun = simMgr.getSimulationRun(altname)
		if simRun != None:
			simRun.getRssAlt().setLogLevel(LogLevel)		#log level controls how much messaging is sent to the console and log
			simMgr.setComputeAll(Constants.TRUE)
			if simMgr.computeRun(simRun,-1):
				print("####SCRIPT### Computed "+altName+" successfully")
			else:
				print("ERROR: "+altName+" failed to compute\n")
	return 0
	
def computeRun(altName, simMgr):
	simRun = simMgr.getSimulationRun(altName)
	if simRun == None:
		print("ERROR: Failed to find SimulationRun "+altName)
		return 1
	print("\n####SCRIPT### ----------------------------------------------------------------------------------------------")
	print("####SCRIPT### Computing alternative: \t" +altName)
	print("####SCRIPT### ----------------------------------------------------------------------------------------------\n")
	simRun.getRssAlt().setLogLevel(LogLevel)		#log level controls how much messaging is sent to the console and log
	simMgr.setComputeAll(Constants.TRUE)	#ComputeAll=True forces the compute even if already computed
	if simMgr.computeRun(simRun, -1):
		# do something
		print("####SCRIPT### Computed "+altName+" successfully")
	else:
		print("ERROR:"+altName+" failed to compute\n")
	return 0

#
#	Main()
#
print "\n####SCRIPT### - commandline args", sys.argv[0:]
programName  = "BatchRunControl"
wkspfile = ""
simulationName = ""
alternativeName = ""
argcnt = len(sys.argv)
if argcnt < 2 :
	# the required arguments are not on the command line
	print("The following arguments must follow this script's name on the HEC-ResSim command line.")
	print("Watershed(wksp)_file simulation_name [alternative_name]")
	System.exit(2)
else :
	# HEC-ResSim driver only passes the command line entries that follow the script name as arguments to the script
	wkspFile = os.path.realpath(sys.argv[0])
	simulationName = sys.argv[1]
	if argcnt > 2:
		alternativeName = sys.argv[2]
	else:
		alternativeName = "-All-"

LogLevel = 3
user = System.getProperty("user.name")

print("\n####SCRIPT### ----------------------------------------------------------------------------------------------")
print("####SCRIPT### - Workspace file:\t"+wkspFile)
print("####SCRIPT### - Simulation:\t"+simulationName)
print("####SCRIPT### - Alternative:\t"+alternativeName)
print("####SCRIPT### - User:\t\t"+user)
print("####SCRIPT### ----------------------------------------------------------------------------------------------\n")

wksp = openWatershed(wkspFile, user)
if wksp == None: sys.exit("####SCRIPT### - openWatershed failed")
watershedDir = wksp.getWorkspacePath()

# This next command turns down the HEC-DSS logging level to the lowest level. (default level is 4)
from hec.heclib.dss import HecDSSFileAccess
HecDSSFileAccess.setMessageLevel(1)

sim = openSimulation(simulationName, wksp)
if sim == None: sys.exit("####SCRIPT### - openSimulation failed")

if alternativeName == "-All-":
	status = computeAll(sim)
else:
	status = computeRun(alternativeName, sim)

wksp.closeWorkspace(user)

if status == 0 :
	print("####SCRIPT### Script Complete - Compute Completed Successfully")
else:
	print("####SCRIPT### Script Complete - Compute Failed")

