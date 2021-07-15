FROM ubuntu:focal

RUN apt-get update \
        && apt-get -y install software-properties-common wget \
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

ENV PRISM_VER=4.3.1

# Install PRISM
RUN wget http://acme.able.cs.cmu.edu/public/BRASS/p2/prism-${PRISM_VER}-linux64.tar.gz && \
    tar zxf prism-${PRISM_VER}-linux64.tar.gz && \
    cd prism-${PRISM_VER}-linux64 && \
    ./install.sh && \
    mv /xplanning/prism-${PRISM_VER}-linux64/lib /xplanning/lib && \
    echo "export LD_LIBRARY_PATH=/xplanning/prism-${PRISM_VER}-linux64/lib:\$LD_LIBRARY_PATH" >> ~/.bashrc && \
    cd .. && \
    rm -f prism-${PRISM_VER}-linux64.tar.gz

 ADD src src
 ADD data data
 ADD test test
 ADD pom.xml pom.xml
 ADD .classpath .classpath

 RUN mvn compile
