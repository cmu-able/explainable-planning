This top level README is out of date. You should look at [XPlanning](XPlanning/README.md) for the latest information on building and running the demos.

This repository contains code for various parts of the Explainable Planning research at the ABLE group at Carnegie Mellon University. The code herein is in various stages of maturity. The repository is organized as follows (but is likely to undergo significant reorganization in the future):

- **XPlanning**: Contains the source code and examples for the core work on explainable planning as detailed in [Towards Explainable Multi-Objective Probabilistic Planning](http://acme.able.cs.cmu.edu/pubs/show.php?id=572). This is the most reusable by third parties.
- **XPlanningEvaluation**: Contains code for implementing the human studies experiment to show efficacy of contrastive explanations, as detailed in [Tradeoff-Focused Contrastive Explanation for MDP Planning](http://acme.able.cs.cmu.edu/pubs/show.php?id=629).
- **IRB-docs**: Material related to IRB approval for the study in **XPlanningEvaluation**.
- **XPlanningIterative**: Contains code for implementing "Why?" and "Why not?" questions about plans, as described in [Interactive Explanation for Planning-Based Systems](http://acme.able.cs.cmu.edu/pubs/show.php?id=596)

Other folders here contain random presentations, scripts used to process data for evalutation, etc.

# explainable-planning

To run MobileRobotDemo:

- Compile MobileRobotDemo.java under /XPlanning/src/examples/mobilerobot/demo/.

- Execute MobileRobotDemo with an argument {missionName}.json, where missionName can be: mission0, mission1, or mission2.

- The mission files are located under /XPlanning/data/mobilerobot/missions/. Each missionX.json references a corresponding mapX.json under /XPlanning/data/mobilerobot/maps/.

- Once the MobileRobot's execution completes, it will generate several output files under /XPlanning/tmpdata/. All output files generated here should be removed (manually as of now) before the next demo run.

Output files:

- You can ignore any output file under /XPlanning/tmpdata/prism/.

- /XPlanning/tmpdata/policies/ is where the .json files of the solution policy and the alternative policies are located.

- /XPlanning/tmpdata/explanations/ is where the explanation file is located. The explanation file references the solution policy and alternative policies under /XPlanning/tmpdata/policies/.
