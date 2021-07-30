# Contrastive Explanation of MDP planning

This repository contains API code and example usages for generating contrastive explanations about plans that result from multi-objective planning as described in:


Roykrong Sukkerd, Reid Simmons and David Garlan. 
[Towards Explainable Multi-Objective Probabilistic Planning.](http://acme.able.cs.cmu.edu/pubs/uploads/pdf/ICSE-WS-SEsCPS-13.pdf)
In Proceedings of the 4th International Workshop on Software Engineering for Smart Cyber-Physical Systems (SEsCPS'18), Gothenburg, Sweden, 27 May 2018. 


The code generates explanations that justify decision actions yielded from a multi-
objective probabilistic planning problem, in terms of the
underlying domain semantics of the optimization objectives,
and how specifically the competing objectives are reconciled. Specifically, the explanation will
justify the plan in terms of the qualities used to optimize that plan, and will contrast that with an
alternative plan that was close to optimal but not chosen.

The repository contains three examples. The links below point to a README describing how the framework is customized for each example:

- [Dart UAV](src/examples/dart/README.md): An example of Unmanned Aerial Vehicle Team planning.
- [Mobile Robot](src/examples/mobilerobot/README.md): An example of a service robot navigating a building.
- [Clinic Scheduling](src/examples/clinicscheduling/README.md): An example of scheduling tasks in a clinical setting.

# Running the demo

## Building the demo
For simplicity, the examples and code are provided in a docker container. 

## Prerequisites

### Docker

Before you can use the images provided by the repo, make sure that [Docker](https://www.docker.com/) 17.05 or higher is installed on your machine.

See the following for platform-specific instructions for installing Docker:

- [Installing Docker Engine on Ubuntu](https://docs.docker.com/engine/install/ubuntu)
- [Installing Docker Engine on Fedora](https://docs.docker.com/engine/install/fedora)
- [Install Docker Desktop on Mac](https://docs.docker.com/docker-for-mac/install)
- [Install Docker Desktop on Windows](https://docs.docker.com/docker-for-windows/install)

If using Linux, make sure to follow the
(post-installation instructions)[https://docs.docker.com/engine/install/linux-postinstall>]
(e.g., adding your user account to the `docker` group) to avoid common
issues (e.g., requiring `sudo` to run `docker` commands).

## Building the Docker image
To build the docker image, after installing docker, change the the root of this repository in the command line, and issue the command:

```
$ docker build -t cmuable/xplanner .
```

## Running examples

To run the example, you need to pass the example kind (`dart`, `mobilerobot`, or `clinic`) and the mission to explain when running a container. For example, to run the `dart` example on `mission0`, run:

```
$ docker run cmuable/xplanning dart mission0.txt
```

To find what missions are available, you can omit the last parameter and the list of available missions will be presented.

```
$ docker run cmuable/xplanning dart 
Must specify a mission file as the first argument
One of:
  mission0.txt
  mission1.txt
  mission2.txt
  mission3.txt
  mission4.txt
  mission5.txt
  mission6.txt
  mission7.txt
  mission8.txt
  mission9.txt
```

