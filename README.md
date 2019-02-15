# explainable-planning

To run MobileRobotDemo:

- Compile MobileRobotDemo.java under /XPlanning/src/examples/mobilerobot/demo/.

- Execute MobileRobotDemo with an argument {missionName}.json, where missionName can be: mission0, mission1, or mission2.

- The mission files are located under /XPlanning/data/mobilerobot/missions/. Each missionX.json references a corresponding mapX.json under /XPlanning/data/mobilerobot/maps/.

- Once the MobileRobot's execution completes, it will generate several output files under /XPlanning/tmpdata/. All output files generated here should be removed (manually as of now) before the next demo run.
