# Dockerfile for creating self-contained planning explanation examples.
# Note, you must have a Gurobi license to successfully run this,
# and the Gurobi license file must be mounted when running the container
# See: https://www.gurobi.com/
# e.g., docker run -v /full/path/on/host:/opt/gurobi/gurobi.lic -it cmuable/xplanning -l 120 mobilerobot mission64.json
FROM ubuntu:focal

RUN apt-get update \
        && apt-get -y install software-properties-common wget make gcc g++\
        && apt-get clean \
        && rm -rf /var/lib/apt/lists/*

RUN add-apt-repository -y ppa:openjdk-r/ppa \
    && apt-get update \
    && apt-get -y install openjdk-8-jdk \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

RUN apt-get update \
        && apt-get -y install maven \
        && apt-get clean \
        && rm -rf /var/lib/apt/lists/*


RUN mkdir /xplanning

WORKDIR /xplanning

ENV PRISM_VER=4.4

RUN  wget https://github.com/prismmodelchecker/prism/archive/refs/tags/v${PRISM_VER}.tar.gz \
  && tar zxf v${PRISM_VER}.tar.gz \
  && cd prism-${PRISM_VER}/prism \
  && make \
  && ./install.sh \
  && cd classes \
  && jar cf ../prism.jar * \
  && cd .. \
  && jar uf prism.jar images dtds \
  && mv lib/ /xplanning/lib \
  && mv prism.jar /xplanning/lib/ \
  && echo "export LD_LIBRARY_PATH=/xplanning/lib/:\$LD_LIBRARY_PATH" >> ~/.bashrc \
  && cd ../.. \
  && rm -f v${PRISM_VER}.tar.gz
  
RUN wget https://packages.gurobi.com/9.1/gurobi9.1.0_linux64.tar.gz \
  && tar zxf gurobi9.1.0_linux64.tar.gz \
  && mv gurobi910 /gurobi910 \
  && echo "export LD_LIBRARY_PATH=/gurobi910/linux64/lib/:\$LD_LIBRARY_PATH" >> ~/.bashrc \
  && rm -f gurobi9.1.0_linux64.tar.gz

ADD src src
ADD test test
ADD pom.xml pom.xml
ADD .classpath .classpath


ENV LD_LIBRARY_PATH /xplanning/lib:/gurobi9.1.0_linux64/gurobi910/linux64/lib/
RUN mvn install -DskipTests
 
RUN mkdir -p /xplanning/tmpdata/prism/models \
 && mkdir -p /xplanning/tmpdata/prism/advs \
 && mkdir -p /xplanning/tmpdata/policies \
 && mkdir -p /explanations \
 && mkdir -p /data
   
ADD data data
ADD data/clinicscheduling /data/clinicscheduling
ADD data/dart /data/dart
ADD data/mobilerobot /data/mobilerobot
ADD data/docker-globals.properties /data/globals.properties


#ADD gurobi9.1.0_linux64.tar.gz /gurobi9.1.0_linux64
#RUN echo "export LD_LIBRARY_PATH=/gurobi9.1.0_linux64/gurobi910/linux64/lib/:\$LD_LIBRARY_PATH" >> ~/.bashrc


ADD run-demo.sh run-demo.sh
RUN chmod +x run-demo.sh
SHELL ["/bin/bash", "-c"]

ENTRYPOINT ["/xplanning/run-demo.sh"]

# docker run -v /mnt/d/ONR/explainable-planning/XPlanning/gurobi.lic:/opt/gurobi/gurobi.lic -it cmuable/xplanning -l 120 mobilerobot mission64.json
