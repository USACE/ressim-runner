FROM ubuntu:20.04 as dev

#need to get the jdk.
RUN apt update &&\
    apt upgrade -y &&\
    apt-cache search openjdk-8 &&\
    apt -y install openjdk-8-jre &&\
    apt -y install openjdk-8-jdk &&\
    apt -y install git &&\
    apt -y install libgfortran5 &&\
    apt -y install unzip &&\
    apt -y install wget &&\
    wget https://services.gradle.org/distributions/gradle-7.3.1-bin.zip -P /tmp &&\
    unzip -d /opt/gradle /tmp/gradle-7.3.1-bin.zip &&\
    ln -s /opt/gradle/gradle-7.3.1 /opt/gradle/latest
    
#replace this with a dynamic call to a repo ultimately
COPY cloud-compute/jar/cc-java-sdk-0.0.51-JR8.jar /cloud-compute/jar/cc-java-sdk-0.0.51-JR8.jar
COPY rss/HEC-ResSim-3.5.0.280 /HEC-ResSim-3.5.0.280
COPY rss/SimpleServer.py /HEC-ResSim-3.5.0.280

