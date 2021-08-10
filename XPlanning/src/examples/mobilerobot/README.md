# Indoor Robot Navigation Example

![Indoor robot navigation](../../../resources/mobilerobot/example.png)

The figure above shows a mobile robot
whose task is to drive from its current location to a goal location in a building. The robot
has to arrive at the goal as soon as possible, while trying to avoid collisions for its own safety
and to avoid driving intrusively through human-occupied areas. The robot has access to the
building’s map (locations, and connections and distances between them), placement and density
of obstacles (sparse or dense obstacles), and the kinds of areas in the environment (public, private,
and semi-private areas). The robot can determine its current location, knows its current driving-
speed setting, and can detect when it bumps into obstacles. The robot can also navigate between
two adjacent locations, and change its driving speed between two settings: full- and half-speed.

When the robot drives at its full speed through a path segment that has obstacles, it has some
probability of colliding, depending on the density of the obstacles. Furthermore, the intrusiveness
of the robot is based on the kinds of areas the robot travels through. The robot is non-intrusive,
somewhat intrusive, or very intrusive when it is in a public, semi-private, or private area, respectively.

The figure shows an example of a tradeoff reasoning the robot might have to make. Suppose
the robot computes the dashed path as its solution. The robot could provide its rationale by
showing the alternative dotted path that is more direct, and explaining that while the dashed path
takes longer travel time than the direct dotted path, it has lower intrusiveness and lower chance
of collision.

## Modeling the planning domain with XMDP XPlanner APIs

To generate a contrastive explanation for a plan (as well as a plan itself) requires modeling the planning problem 
in the XPlanning framework, which involves defining (1) the state space for the problem (e.g., the altitude and formation 
of the UAV team); (2) the actions that can be applied in states to transition the MDP to new states; (3) a factored representation
of states X actions -> new states that capture the conditions in states where actions can be applied, and the probability
distribution of new states that will be transitioned to after applying an action; (4) the qualities that will be used to determine
whether plans are good (called rewards in MDP) and the costs, which are negative rewards; and (5) the initial state of the system and goal
for the planner to achieve.

To bring this all together into a planning problem requires defining an `XMDPBuilder` object that defines these actions, either directly
(hardcoded in the class), or (more commonly) by taking some textual representation of a mission and using that to generate the XMDP planning 
description.

### State Variables

State variables represent information about states in the MDP, and are implemented by implementing state interfaces provided 
in the package:
`language.domain.models`.  Each of these state variables is modeled in XMDP by implementing the 
`language.domain.models.IStateVarValue` 
interface or one of its primitive derivatives: `IStateVarInt`, `IStateVarDouble` or `IStateVarBoolean`, which 
provide state variables 
represented by integers, doubles, or booleans respectively. Furthermore, any state variable may have attributes that provide 
more detail about 
the state variable. Such information might, for example, describe the units of the state variable, or the probability 
distribution of the value.

The state space for the indoor service robot is defined by the following two variables:

- [Robot Location](models/Location.java), which is the current location of the robot, which will be drawn from a set of 
waypoints on a map, and
- [Robot Speed](models/RobotSpeed.java), which is the current speed setting for the robot, as an implementation of 
`IStateVarDouble`. Although the robot may have discrete levels of
speed settings, for instance, full speed and half speed, we define RobotSpeed as a floating point
type to allow the *rSpeed* variable to be used directly in computation of QA values.

Note that in this example domain, we choose not to include the state of the robot’s bump
sensor as a state variable for simplicity. However, the collision concern of the robot can still be
captured via other XMDP constructs, as we will discuss.

As mentioned above, XMDP allows us to define attributes associated with state variables that can be used in planning
to provide additional information about a state variable. We define the following state-variable attributes 
associated with the Location type: ID representing the unique ID of a particular
location, and [Area](models/Area.java) representing the type of area in which the location is, which can be one of
the following types: public, semi-private or private. Some examples of public areas include hallway and cafeteria, 
semi-private areas include conference rooms and student lounges, and private areas include faculty offices.


## Generating an explanation

```
Explanation: (/explanations/mission0_explanation.json)
============
I'm planning to follow this policy [Solution] (below). It is expected to have 0.0 expected collision (0.0 in cost); take
165.78068230493494 seconds (165.78068230493497 in cost); and have intrusiveness-penalty of 10.0: it will be
non-intrusive at 8.0 locations (0.0-penalty), somewhat-intrusive at 1.0 location (1.0-penalty), and very-intrusive at
3.0 locations (9.0-penalty) (110.0 in cost). It has the lowest expected intrusiveness, and collision.

Alternatively, following this policy [Alternative 0 (below)] would reduce the expected travel time by 17.19041023612138
seconds (-17.190410236121437 in cost). However, I didn't choose that policy because it would increase the expected
collision by 0.20000000000007512 (+20.00000000000751 in cost). The decrease in expected travel time is not worth the
increase in expected collision.

Solution
========
                                                                           ⢀⠤⠤⠤⠤⠤⠤⠤⡄
                                                                           ⢸       ⡇
                                                                ⣀⣀⣀⣀⣀⣀⣀⡀   ⢸ ((L5))⡇
                                                               ⢸       ⣇⠤⠒⠊⢹       ⡇
                                                               ⢸ ((L4))⡇   ⠘⠒⠒⠒⢲⠒⠒⠒⠃
                                              ⢀⣀⣀⣀⣀⣀⣀⣀     ⢀⣀⠤⠒⢺       ⡇       ⢸
                                              ⡇      ⢸⣀⡠⠴⠒⠉⠁   ⠸⠤⠤⠤⡤⠤⠤⠤⠇       ⠈⡆
                                              ⡇((L3))⢹             ⡇            ⡇
                                ⢰⠉⠉⠉⠉⠉⠉⠉⡇⢀⣀[]⠒⡇      ⢸            ⢠⠃            ⡇
                         ⢀⠤⠤⠤⠤⠤⠤⢼⡄((L2))⡏⠁    ⠧⠤⠤⠤⠤⠤⠤⠼            ⢸          ⡠⠤⠤⠧⠤⠤⠤⢤
                         ⢸      (→)     ⡇                      ⡠⠤⠤⠼⠤⠤⠤⢤      ⡇      ⢸
                         ⢸*L1*  ⢸⣇⣀⣀⣀⣀⣀⣀⡇                      ⡇      ⢸      ⡇((L10))
                         ⢸       ⡇  ⡇                          ⡇L9    ⢸      ⡇      ⢸
                         ⠘⠒⠒⠒⠒⠒⠒⠒⠃ ⢀⠇                          ⡇      ⢸      ⠓⠒⠒⠒⡖⠒⠒⠚
                                   (↓)                         ⠓⠒⠒⢲⠒⠒⠒⠚          ⢱
                                   ⢸                              ⢸              ⢸
                  ⢰⠉⠉⠉⠉⠉⠉⠉⡇        ⢸                              ⢸              ⠈⡆
                  ⢸ L6    ⡇    ⢰⠉⠉⠉⠉⠉⠉⠉⡇    ⢀⠤⠤⠤⠤⠤⠤⠤⡄             ⢸               ⡇
                  ⢸       ⡗⠒⠒⠒⠢⢼ L7   ⢀⡇    ⢸       ⡇          ⡠⠤⠤⠼⠤⠤⠤⢤        ⢀⠤⠤⠵⠤⠤⠤⠤⡄
                  ⢸⣀⣀⣀⣠⣀⣀⣀⡇    ⢸       ⡏⠉⇒⠉⠉⢹ L8    ⡧⠤⠤⢄⣀(→)⣀  ⡇      ⢸        ⢸       ⡇
                      ⢸        ⢸⣀⣀⣀⣀⣀⣀⣀⡇    ⢸       ⡇        ⠉⠉⡇L11   ⢸⠒⠒⠒⠒[]⠒⠒⢺((L12))⡇
                      ⠈⡆                    ⠘⠒⠒⠒⢲⠒⠒⠒⠃          ⡇      ⢸        ⢸       ⡇
                       ⡇                         ⡇             ⠓⠒⠒⢲⠒⠒⠒⠚        ⠘⠒⠒⢲⠒⠒⠒⠒⠃
                       ⡇                         ⡇                ⢸               ⢸
                       ⢸                         ⢸                ⡜               ⡇
                       ⢸                         ⢸                ⇓               ⡇
                       ⠘⡄                         ⡇               ⡇               ⡇
                        ⡇                         ⡇               ⡇              ⢠⠃
                     ⡎⠉⠉⠉⠉⠉⠉⢹                  ⢰⠉⠉⠉⠉⠉⠉⠉⡇      ⡎⠉⠉⠉⠉⠉⠉⢹        ⡎⠉⠉⠉⠉⠉⠉⢹
                     ⡇LX1   ⢸                  ⢸[[LX2]]⡇      ⡇L13   ⢸        ⡇((LX3))
                     ⡇      ⢸                  ⢸       ⡇      ⡇      ⢸        ⡇      ⢸
                     ⣇⣀⣀⣀⣀⣀⣀⣸                  ⢸⣀⣀⣀⣀⣀⣀⣀⡇      ⣇⣀⣀⣠⣀⣀⣀⣸        ⣇⣀⣀⣀⣀⣀⣀⣸
                         ⡇                         ⡇             ⢸               ⡇
                         ⡇                         ⢸             ⢸              ⢀⠇
                         ⢣                         ⢸             ⢸              ⢸
                         ⢸                         ⢸             ⇓              ()
                         ⢸                         ⠈⡆            ⢸              ⡜
                          ⡇                         ⡇            ⢸              ⡇
                          ⡇                         ⡇            ⢸              ⡇
                       ⡠⠤⠤⠧⠤⠤⠤⢤    ⡠⠤⠤⠤⠤⠤⠤⢤      ⡠⠤⠤⠧⠤⠤⠤⢤     ⡠⠤⠤⠼⠤⠤⠤⢤      ⡠⠤⠤⠴⠥⠤⠤⢤
                       ⡇      ⢸    ⡇      ⢸      ⡇      ⢸     ⡇      ⢸      ⡇      ⢸
                       ⡇((L14))⠒⠒⠒⠒⡇((L15))⠒⠒[]⠒⠒⡇[[L16]]⠒⠒[]⠒⡇[[L17]]⠒⠒⠒⠒⠒⠒⡇((L18))
                       ⡇      ⢸    ⡇      ⢸      ⡇      ⢸     ⡇      ⢸      ⡇      ⢸
                       ⠓⠒⠒⠒⠒⠒⠒⠚    ⠓⡞⠒⡖⠒⠒⠒⠚      ⠓⠒⠒⠒⠒⠒⠒⠚     ⠓⠒⡲⠒⠒⠒⠒⠚      ⠓⠒⡺⠒⠒⠒⠒⠚
                                  ⢀⡜  ⡇                        ⢠⠃         ⣀⣀⣀⣀()⣀⡀
                                 ⢀()  ⡇                       ⢀⇙         ⢸       ⡇
                                ⢀⠎    ()                      ⡜          ⢸L22    ⡇
                               ⢀⠎     ⡇                  ⡠⠤⠤⠤⠴⠥⠤⢤        ⢸       ⡇
                          ⡎⠉⠉⠉⠉⠉⠉⢹   ⢠⠃                  ⡇      ⢸        ⠸⠤⠤⡦⠤⠤⠤⠤⠇
                          ⡇((L19))   ⢸                   ⡇L21   ⢸          ⢸
                          ⡇      ⢸   ⢸                   ⡇      ⢸          ⡎
                          ⣇⣀⣀⣀⣀⣀⣀⣺⡎⠉⠉⠉⠉⠉⠉⢹               ⠓⠒⠒⡖⠒⠒⠒⠚         ⢰⠁
                              ⠈⡆  ⡇[[L20]]                  ⇓         ⡠⠤⠤⠤⠮⠤⠤⢤
+==========================+   ⢸  ⡇      ⢸              ⢀⣀⣀⣀⣇⣀⣀⣀      ⡇      ⢸
| LEGEND                   |    ⢇ ⣇⣀⣠⣀⣀⣀⣀⣸              ⡇      ⢸   ⇒⣀⠤⡇L26   ⢸
+--------------------------+    ⠈⡆  ⢸                   ⡇L25   ⢸⠊⠉⠉   ⡇      ⢸
| Locations:               |     ⢸  ⡇                   ⡇      ⢸      ⠓⠒⢺⠒⠒⠒⠒⠚
|  *X* : Start Node        |   ⢠⠒⠒⠓⠒⠓⠒⠒⡆                ⠧⠤⢤⠤⠤⠤⠤⠼        ⡇
|  .X. : End Node          |   ⢸       ⡇⢀⠤⠤⠤⠤⠤⠤⠤⡄        ⢠⠇            ⢸
|  (X) : Semi-Private      |   ⢸ [[L23]]⣸       ⡇   ⢀⠤⠤⠤⠤⠮⠤⠤⡄          ⡎
| ((X)): Private           |   ⢸       ⡇⢸ L24   ⣇⣀⣀⡀⢸       ⡇         ⢰⠁
+--------------------------+   ⠈⠉⠉⠉⡏⠉⠉⠉⠁⢸       ⡇  ⠈⢹L27    ⡇         ⡜
| Edges:                   |       ⡇    ⠘⠒⠒⠒⠒⠒⠒⠒⠃   ⢸       ⡇        ⢀(↙)
|  ()  : Sparse occlusion  |       ⢸                ⠘⠒⢒⠗⠒⠒⠒⠒⠃        ⡸
|  []  : Dense occlusion   |       ⢸                  ⡸              ⡇
|  ⇒   : 0.7m/s traversal  |       ⢸                 ⢀⠇             ⢸
|  →   : 0.35m/s traversal |       ⢸                 ⡸              ⡎
+--------------------------+       []               ⢠⠃             ⢠⠃
                                   ⢸                ⡜              ⡜
                                   ⢸               ⢀⠇         ⢰⠉⠉⠉⠉⠉⠉⠉⡇
                                   ⢸           ⢀⣀⣀⣀⣸⣀⣀⣀       ⢸ LX5   ⡇
                                   ⢸           ⡇      ⢸       ⢸       ⡇
                                   ⢸           ⡇L28   ⢸       ⢸⣀⣀⣀⣀⣀⣀⣀⡇
                                 ⣀⣀⣸⣀⣀⣀⣀⡀      ⡇      ⢸          ⡎
                                ⢸       ⡇      ⠧⠤⠤⡤⠤⠤⠤⠼         ⢠⠃
                                ⢸[[LX4]]⡇         ⡇             ⢸
                                ⢸      ⠠⣇⡀        ⡇             ⇙
                                ⠸⠤⠤⠤⠤⠤⠤⠤⠇⠈[]⣄⡀⢠⠒⠒⠒⠓⠒⠒⠒⡆        ⢰⠁
                                             ⠈⢹       ⡇        ⡜
                                              ⢸[[L29]]⡇       ⢀⠇
                                              ⢸      ⠈⡏⠒[]⣰⠉⠉⠉⠉⠉⠉⠉⡇
                                              ⠈⠉⠉⢩⠋⠉⠉⠉⠁   ⢸[[L31]]⡇
                                                 ⢸        ⢸      ⠠⣇⡀  ⢀⣀⣀⣀⣀⣀⣀⣀
                                                 ⢸        ⢸⣀⣀⣀⣀⣀⣀⣀⡇[→]⡇      ⢸
                                                 ⢸                    ⡇[[.L32.]]
                                                 ⢸                    ⡇      ⢸
                                              ⡔⠒⠒⠚⠒⠒⠒⢲                ⠧⠤⠤⠤⠤⠤⠤⠼
                                              ⡇      ⢸
                                              ⡇L30   ⢸
                                              ⡇      ⢸
                                              ⠉⠉⠉⠉⠉⠉⠉⠉
Qualities:
  +-------------------------------+
  | Quality       | Value         |
  +===============================+
  | collision     | 0.00          |
  +-------------------------------+
  | travelTime    | 165.78        |
  +-------------------------------+
  | intrusiveness | 10.00         |
  +-------------------------------+

Alternative 0
=============
                                                                           ⢀⠤⠤⠤⠤⠤⠤⠤⡄
                                                                           ⢸       ⡇
                                                                ⣀⣀⣀⣀⣀⣀⣀⡀   ⢸ ((L5))⡇
                                                               ⢸       ⣇⠤⠒⠊⢹       ⡇
                                                               ⢸ ((L4))⡇   ⠘⠒⠒⠒⢲⠒⠒⠒⠃
                                              ⢀⣀⣀⣀⣀⣀⣀⣀     ⢀⣀⠤⠒⢺       ⡇       ⢸
                                              ⡇      ⢸⣀⡠⠴⠒⠉⠁   ⠸⠤⠤⠤⡤⠤⠤⠤⠇       ⠈⡆
                                              ⡇((L3))⢹             ⡇            ⡇
                                ⢰⠉⠉⠉⠉⠉⠉⠉⡇⢀⣀[]⠒⡇      ⢸            ⢠⠃            ⡇
                         ⢀⠤⠤⠤⠤⠤⠤⢼⡄((L2))⡏⠁    ⠧⠤⠤⠤⠤⠤⠤⠼            ⢸          ⡠⠤⠤⠧⠤⠤⠤⢤
                         ⢸      (→)     ⡇                      ⡠⠤⠤⠼⠤⠤⠤⢤      ⡇      ⢸
                         ⢸*L1*  ⢸⣇⣀⣀⣀⣀⣀⣀⡇                      ⡇      ⢸      ⡇((L10))
                         ⢸       ⡇  ⡇                          ⡇L9    ⢸      ⡇      ⢸
                         ⠘⠒⠒⠒⠒⠒⠒⠒⠃ ⢀⠇                          ⡇      ⢸      ⠓⠒⠒⠒⡖⠒⠒⠚
                                   (↓)                         ⠓⠒⠒⢲⠒⠒⠒⠚          ⢱
                                   ⢸                              ⢸              ⢸
                  ⢰⠉⠉⠉⠉⠉⠉⠉⡇        ⢸                              ⢸              ⠈⡆
                  ⢸ L6    ⡇    ⢰⠉⠉⠉⠉⠉⠉⠉⡇    ⢀⠤⠤⠤⠤⠤⠤⠤⡄             ⢸               ⡇
                  ⢸       ⡗⠒⠒⠒⠢⢼ L7   ⢀⡇    ⢸       ⡇          ⡠⠤⠤⠼⠤⠤⠤⢤        ⢀⠤⠤⠵⠤⠤⠤⠤⡄
                  ⢸⣀⣀⣀⣠⣀⣀⣀⡇    ⢸       ⡏⠉⇒⠉⠉⢹ L8    ⡧⠤⠤⢄⣀(→)⣀  ⡇      ⢸        ⢸       ⡇
                      ⢸        ⢸⣀⣀⣀⣀⣀⣀⣀⡇    ⢸       ⡇        ⠉⠉⡇L11   ⢸⠒⠒⠒⠒[]⠒⠒⢺((L12))⡇
                      ⠈⡆                    ⠘⠒⠒⠒⢲⠒⠒⠒⠃          ⡇      ⢸        ⢸       ⡇
                       ⡇                         ⡇             ⠓⠒⠒⢲⠒⠒⠒⠚        ⠘⠒⠒⢲⠒⠒⠒⠒⠃
                       ⡇                         ⡇                ⢸               ⢸
                       ⢸                         ⢸                ⡜               ⡇
                       ⢸                         ⢸                ⇓               ⡇
                       ⠘⡄                         ⡇               ⡇               ⡇
                        ⡇                         ⡇               ⡇              ⢠⠃
                     ⡎⠉⠉⠉⠉⠉⠉⢹                  ⢰⠉⠉⠉⠉⠉⠉⠉⡇      ⡎⠉⠉⠉⠉⠉⠉⢹        ⡎⠉⠉⠉⠉⠉⠉⢹
                     ⡇LX1   ⢸                  ⢸[[LX2]]⡇      ⡇L13   ⢸        ⡇((LX3))
                     ⡇      ⢸                  ⢸       ⡇      ⡇      ⢸        ⡇      ⢸
                     ⣇⣀⣀⣀⣀⣀⣀⣸                  ⢸⣀⣀⣀⣀⣀⣀⣀⡇      ⣇⣀⣀⣠⣀⣀⣀⣸        ⣇⣀⣀⣀⣀⣀⣀⣸
                         ⡇                         ⡇             ⢸               ⡇
                         ⡇                         ⢸             ⢸              ⢀⠇
                         ⢣                         ⢸             ⢸              ⢸
                         ⢸                         ⢸             ⇓              ()
                         ⢸                         ⠈⡆            ⢸              ⡜
                          ⡇                         ⡇            ⢸              ⡇
                          ⡇                         ⡇            ⢸              ⡇
                       ⡠⠤⠤⠧⠤⠤⠤⢤    ⡠⠤⠤⠤⠤⠤⠤⢤      ⡠⠤⠤⠧⠤⠤⠤⢤     ⡠⠤⠤⠼⠤⠤⠤⢤      ⡠⠤⠤⠴⠥⠤⠤⢤
                       ⡇      ⢸    ⡇      ⢸      ⡇      ⢸     ⡇      ⢸      ⡇      ⢸
                       ⡇((L14))⠒⠒⠒⠒⡇((L15))⠒⠒[]⠒⠒⡇[[L16]]⠒⠒[]⠒⡇[[L17]]⠒⠒⠒⠒⠒⠒⡇((L18))
                       ⡇      ⢸    ⡇      ⢸      ⡇      ⢸     ⡇      ⢸      ⡇      ⢸
                       ⠓⠒⠒⠒⠒⠒⠒⠚    ⠓⡞⠒⡖⠒⠒⠒⠚      ⠓⠒⠒⠒⠒⠒⠒⠚     ⠓⠒⡲⠒⠒⠒⠒⠚      ⠓⠒⡺⠒⠒⠒⠒⠚
                                  ⢀⡜  ⡇                        ⢠⠃         ⣀⣀⣀⣀()⣀⡀
                                 ⢀()  ⡇                       ⢀⇙         ⢸       ⡇
                                ⢀⠎    ()                      ⡜          ⢸L22    ⡇
                               ⢀⠎     ⡇                  ⡠⠤⠤⠤⠴⠥⠤⢤        ⢸       ⡇
                          ⡎⠉⠉⠉⠉⠉⠉⢹   ⢠⠃                  ⡇      ⢸        ⠸⠤⠤⡦⠤⠤⠤⠤⠇
                          ⡇((L19))   ⢸                   ⡇L21   ⢸          ⢸
                          ⡇      ⢸   ⢸                   ⡇      ⢸          ⡎
                          ⣇⣀⣀⣀⣀⣀⣀⣺⡎⠉⠉⠉⠉⠉⠉⢹               ⠓⠒⠒⡖⠒⠒⠒⠚         ⢰⠁
                              ⠈⡆  ⡇[[L20]]                  ⇓         ⡠⠤⠤⠤⠮⠤⠤⢤
+==========================+   ⢸  ⡇      ⢸              ⢀⣀⣀⣀⣇⣀⣀⣀      ⡇      ⢸
| LEGEND                   |    ⢇ ⣇⣀⣠⣀⣀⣀⣀⣸              ⡇      ⢸   ⇒⣀⠤⡇L26   ⢸
+--------------------------+    ⠈⡆  ⢸                   ⡇L25   ⢸⠊⠉⠉   ⡇      ⢸
| Locations:               |     ⢸  ⡇                   ⡇      ⢸      ⠓⠒⢺⠒⠒⠒⠒⠚
|  *X* : Start Node        |   ⢠⠒⠒⠓⠒⠓⠒⠒⡆                ⠧⠤⢤⠤⠤⠤⠤⠼        ⡇
|  .X. : End Node          |   ⢸       ⡇⢀⠤⠤⠤⠤⠤⠤⠤⡄        ⢠⠇            ⢸
|  (X) : Semi-Private      |   ⢸ [[L23]]⣸       ⡇   ⢀⠤⠤⠤⠤⠮⠤⠤⡄          ⡎
| ((X)): Private           |   ⢸       ⡇⢸ L24   ⣇⣀⣀⡀⢸       ⡇         ⢰⠁
+--------------------------+   ⠈⠉⠉⠉⡏⠉⠉⠉⠁⢸       ⡇  ⠈⢹L27    ⡇         ⡜
| Edges:                   |       ⡇    ⠘⠒⠒⠒⠒⠒⠒⠒⠃   ⢸       ⡇        ⢀(⇙)
|  ()  : Sparse occlusion  |       ⢸                ⠘⠒⢒⠗⠒⠒⠒⠒⠃        ⡸
|  []  : Dense occlusion   |       ⢸                  ⡸              ⡇
|  ⇒   : 0.7m/s traversal  |       ⢸                 ⢀⠇             ⢸
|  →   : 0.35m/s traversal |       ⢸                 ⡸              ⡎
+--------------------------+       []               ⢠⠃             ⢠⠃
                                   ⢸                ⡜              ⡜
                                   ⢸               ⢀⠇         ⢰⠉⠉⠉⠉⠉⠉⠉⡇
                                   ⢸           ⢀⣀⣀⣀⣸⣀⣀⣀       ⢸ LX5   ⡇
                                   ⢸           ⡇      ⢸       ⢸       ⡇
                                   ⢸           ⡇L28   ⢸       ⢸⣀⣀⣀⣀⣀⣀⣀⡇
                                 ⣀⣀⣸⣀⣀⣀⣀⡀      ⡇      ⢸          ⡎
                                ⢸       ⡇      ⠧⠤⠤⡤⠤⠤⠤⠼         ⢠⠃
                                ⢸[[LX4]]⡇         ⡇             ⢸
                                ⢸      ⠠⣇⡀        ⡇             ⇙
                                ⠸⠤⠤⠤⠤⠤⠤⠤⠇⠈[]⣄⡀⢠⠒⠒⠒⠓⠒⠒⠒⡆        ⢰⠁
                                             ⠈⢹       ⡇        ⡜
                                              ⢸[[L29]]⡇       ⢀⠇
                                              ⢸      ⠈⡏⠒[]⣰⠉⠉⠉⠉⠉⠉⠉⡇
                                              ⠈⠉⠉⢩⠋⠉⠉⠉⠁   ⢸[[L31]]⡇
                                                 ⢸        ⢸      ⠠⣇⡀  ⢀⣀⣀⣀⣀⣀⣀⣀
                                                 ⢸        ⢸⣀⣀⣀⣀⣀⣀⣀⡇[→]⡇      ⢸
                                                 ⢸                    ⡇[[.L32.]]
                                                 ⢸                    ⡇      ⢸
                                              ⡔⠒⠒⠚⠒⠒⠒⢲                ⠧⠤⠤⠤⠤⠤⠤⠼
                                              ⡇      ⢸
                                              ⡇L30   ⢸
                                              ⡇      ⢸
                                              ⠉⠉⠉⠉⠉⠉⠉⠉
Qualities:
  +-------------------------------+
  | Quality       | Value         |
  +===============================+
  | collision     | 0.20          |
  +-------------------------------+
  | travelTime    | 148.59        |
  +-------------------------------+
  | intrusiveness | 10.00         |
  +-------------------------------+```
