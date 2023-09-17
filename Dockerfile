FROM ubuntu:20.04 as dev

#need to get the jdk.
RUN apt update
RUN apt upgrade -y
RUN apt-cache search openjdk-8
RUN apt -y install openjdk-8-jre
RUN apt -y install openjdk-8-jdk
#replace this with a dynamic call to a repo ultimately
COPY cloud-compute/jar/cc-java-sdk-0.0.51-JR8.jar /cloud-compute/jar/cc-java-sdk-0.0.51-JR8.jar
COPY rss/HEC-ResSim-3.5.0.280 /HEC-ResSim-3.5.0.280

RUN apt -y install git
RUN apt -y install libgfortran5