FROM ubuntu:20.04 as dev

#need to get the jdk.
RUN apt update
RUN apt upgrade -y
RUN apt-cache search openjdk-11
RUN apt -y install openjdk-11-jre
RUN apt -y install openjdk-11-jdk

RUN apt -y install git
RUN apt -y install libgfortran5