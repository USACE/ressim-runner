#! /bin/bash

# Determine the directory the HEC-ResSim.sh script is in (relative to pwd) so that we can correctly reference jars, libraries, and configs.
BASEDIR=$RESSIM_HOME
RUNNERDIR=$RESSIM_RUNNER_HOME

JAVAPATH=/usr/bin/java

# Ensure $XDG_DATA_HOME is a valid path.
if [ -z "$XDG_DATA_HOME" ]
then
    XDG_DATA_HOME="$HOME/.local/share"
fi

# Ensure $XDG_CACHE_HOME is a valid path.
if [ -z "$XDG_CACHE_HOME" ]
then
    XDG_CACHE_HOME="$HOME/.cache"
fi

#:------------------------------------------------------:
#: Memory parameters                                    :
#:------------------------------------------------------:
HEC_JVMPARAMS="-mx4000M"

#:------------------------------------------------------:
#: Disables VolitileImages in the MapPanel solving      :
#: some bad graphical glitches.                         :
#:------------------------------------------------------:
HEC_JVMPARAMS="$HEC_JVMPARAMS \
-DMapPanel.NoVolatileImage=true"

#:------------------------------------------------------:
#: Log Definitions                                      :
#:------------------------------------------------------:
HEC_JVMPARAMS="$HEC_JVMPARAMS \
-Dlogfile.directory=\"$XDG_CACHE_HOME/HEC/HEC-ResSim/3.5/logs/\" \
-DLOGFILE=\"$XDG_CACHE_HOME/HEC/HEC-ResSim/3.5/logs/HEC-ResSim.log\" \
-DDSSLOGFILE=\"$XDG_CACHE_HOME/HEC/HEC-ResSim/3.5/logs/HEC-ResSim_DSS.log\" \
-DCACHE_DIR=\"$XDG_CACHE_HOME/HEC/HEC-ResSim/3.5/cache/ \"
-Dpython.path=\"$BASEDIR/jar/sys/jythonLib.jar/lib\":\"$BASEDIR/jar/sys/jythonutils.jar\" \
-Dpython.home=\"$XDG_CACHE_HOME/HEC/HEC-ResSim/3.5/pyhome\" \
-DDataSetValidParameterListFile=\"$BASEDIR/config/parameters_units.def\" \
-DUNIT_FILE=\"$BASEDIR/config/unitConversions.def\" \
-DAsciiSerializer.formatFile=true \
-Dlogin.properties.path=\"$XDG_DATA_HOME/HEC/HEC-ResSim/3.5/properties/login.properties\" \
-Dproperties.path=\"$BASEDIR/config\" \
-DCWMS_HOME=\"$XDG_DATA_HOME/HEC/HEC-ResSim/3.5\" \
-DCWMS_EXE=\"$BASEDIR\" \
-Djava.security.policy=\"$BASEDIR/config/java.policy\" \
-DstatePlane.directory=\"$BASEDIR/config\" \
-Djava.library.path=\"$BASEDIR/lib\" \
-DNO_PREDEFINED_WKSP=true \
-DHasGlobalVariables=false \
-DUseDefaultSecurityManager=true"

# For any arguments passed in
for ARGUMENT in $@
do
    # If it is a -D flag
    if [[ $ARGUMENT = -D* ]]
    then
        # Append it to our list of -D flags
        HEC_JVMPARAMS="$HEC_JVMPARAMS $ARGUMENT"
    else
        # Otherwise, treat it as a program argument
        ARGUMENTS="$ARGUMENTS $ARGUMENT"
    fi
done

# +-----------------------------------------------------+
# |                                                     |
# | Specify the class path.                             |
# |                                                     |
# +-----------------------------------------------------+
if [ -z "$HEC_CLASSPATH" ]
then
    HEC_CLASSPATH="\"$BASEDIR/jar/*\":\"$BASEDIR/jar/sys/*\":\"$BASEDIR/jar/ext/*\""
else
    HEC_CLASSPATH="\"$BASEDIR/jar/sys/*\":\"$BASEDIR/jar/*\":\"$BASEDIR/jar/ext/*\":$HEC_CLASSPATH"
fi

# Execute java passing the classpath, vm parameters, main class, and any program arguments passed into the script.
eval "$JAVAPATH" -cp $HEC_CLASSPATH $HEC_JVMPARAMS -jar $RUNNERDIR/ressim-runner-0.0.1.jar
