FROM ubuntu:20.04 as dev
ARG RESSIM_VERSION=3.5.1.18-linuxbeta-linux-x86_64
ARG RESSIM_HOME=HEC-ResSim-${RESSIM_VERSION}
ENV GRADLE_HOME=/opt/gradle/latest
ENV PATH=${GRADLE_HOME}/bin:$PATH
#need to get the jdk.
RUN apt update &&\
    apt upgrade -y &&\
    apt-cache search openjdk-17 &&\
    apt -y install openjdk-17-jre &&\
    apt -y install openjdk-17-jdk &&\
    apt -y install git &&\
    apt -y install libgfortran5 &&\
    apt -y install unzip &&\
    apt -y install wget &&\
    wget https://www.hec.usace.army.mil/nexus/repository/maven-public/mil/army/usace/hec/ressim/hec-ressim/${RESSIM_VERSION}/hec-ressim-${RESSIM_VERSION}.tar.gz &&\
    mkdir /${RESSIM_HOME} &&\
    tar -xvzf /hec-ressim-${RESSIM_VERSION}.tar.gz -C /${RESSIM_HOME} --strip-components=1 &&\
    wget https://services.gradle.org/distributions/gradle-7.3.1-bin.zip -P /tmp &&\
    unzip -d /opt/gradle /tmp/gradle-7.3.1-bin.zip &&\
    ln -s /opt/gradle/gradle-7.3.1 /opt/gradle/latest
    
#replace this with a dynamic call to a repo ultimately
COPY cloud-compute/jar/cc-java-sdk-1.0.0-all.jar /cloud-compute/jar/cc-java-sdk-1.0.0-all.jar
COPY rss/SimpleServer.py /${RESSIM_HOME}

