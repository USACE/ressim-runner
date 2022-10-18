FROM ubuntu:20.04 as dev

#need to get the jdk.
RUN apt update
RUN apt upgrade -y
RUN apt-cache search openjdk-11
RUN apt -y install openjdk-11-jre
RUN apt -y install openjdk-11-jdk

RUN apt -y install wget
RUN wget https://www.hec.usace.army.mil/nexus/repository/ressim-releases/mil/army/usace/hec/mil/army/usace/hec-ressim/hec-ressim-3.5.0.280-linux-x86_64.tar.gz -P /
RUN tar -xvzf /hec-ressim-3.5.0.280-linux-x86_64.tar.gz -C /

RUN apt -y install git
RUN apt -y install libgfortran5