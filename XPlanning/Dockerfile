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
  

# Install PRISM
#RUN wget http://acme.able.cs.cmu.edu/public/BRASS/p2/prism-${PRISM_VER}-linux64.tar.gz && \
#    tar zxf prism-${PRISM_VER}-linux64.tar.gz && \
#    cd prism-${PRISM_VER}-linux64 && \
#    ./install.sh && \
#    mv /xplanning/prism-${PRISM_VER}-linux64/lib /xplanning/lib && \
#    echo "export LD_LIBRARY_PATH=/xplanning/prism-${PRISM_VER}-linux64/lib:\$LD_LIBRARY_PATH" >> ~/.bashrc && \
#    cd .. && \
#    rm -f prism-${PRISM_VER}-linux64.tar.gz

ADD src src
ADD data data
# ADD test test
ADD pom.xml pom.xml
ADD .classpath .classpath
 
RUN mkdir -p /xplanning/tmpdata/prism/models \
 && mkdir -p /xplanning/tmpdata/prism/advs \
 && mkdir -p /xplanning/tmpdata/policies \
 && mkdir -p /explanations \
 && mkdir -p /data
   
ADD data/clinicscheduling /data/clinicscheduling
ADD data/dart /data/dart
ADD data/mobilerobot /data/mobilerobot
ADD data/docker-globals.properties /data/globals.properties

RUN mvn install

ADD gurobi9.1.0_linux64.tar.gz /gurobi9.1.0_linux64
ADD gurobi.lic /opt/gurobi/
RUN echo "export LD_LIBRARY_PATH=/gurobi9.1.0_linux64/gurobi910/linux64/lib/:\$LD_LIBRARY_PATH" >> ~/.bashrc
 


