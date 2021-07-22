# DART UAV

The DART (Distributed Adaptive Real-Time) Systems project at the Carnegie Mellon Software
Engineering Institute implements a simulated team of unmanned aerial vehicles (UAVs) performing 
a reconnaissance mission in a hostile environment. The mission of the team is to fly a planned
route at constant speed across an area to detect as many targets on the ground as possible. At the
same time, the team must try to avoid being shot down by threats in the area, which would result
in the failure of the mission. As the team flies during the mission, it discovers the environment
(i.e., the location of targets and threats) with some uncertainty, and plans to adapt its altitude,
formation, and other configurations for a finite horizon, accordingly. The team continues the
cycle of planning and execution until it has reached the end of the route, or it is shot down by
threats.

The team has two long-range forward-looking sensors to monitor the state of the environment for 
a fixed horizon ahead of the team: one for sensing targets and the other for sensing threats. 
The route is divided into segments of equal length. For each route segment in front of
the sensor, it reports whether it detects a target/threat. Due to sensing errors, these reports can
be false positive or false negative. The team therefore would get multiple observations to construct 
a probability distribution of target or threat presence in each route segment ahead. These
probability distributions will be used in mission planning.

To detect targets on the ground, the team uses a downward-looking sensor as it flies over.
The closer the UAVs fly to the ground, the more likely they are to detect the targets, but also
the higher probability of being destroyed by a threat. The team can be in either loose or tight
formation. Flying in tight formation reduces the probability of being shot down; however, it also
reduces the target detection probability due to sensor occlusion or overlap. The team can use
electronic countermeasures (ECM) to reduce the probability of being destroyed by threats, but
using ECM also reduce the probability of target detection.

Using the probability distributions of targets and threats ahead of the team constructed from
observations, the team plans for any necessary adaptation to change the team's configuration or
altitude to balance the objectives of maximizing the number of targets detected and minimizing
the probability of being destroyed by threats.

To generate a contrastive explanation for a plan (as well as a plan itself) requires modeling the following
in the XPlanning framework:

- The XMDP definition of the domain for the UAV, which is done by providing a series of classes that 
extend and use the XMDP APIs, which include state variables, actions, transition functions, quality models,
and costs.
- Providing translators that translate from a file representation of the model into instances of the above
classes to instantiate the model.
- Domain-specific classes to represent plans in a human readable format.
- Implementing a class that brings these all together to invoke the planner and generate the explanations.

## Modeling the problem domain with XMDP XPlanner

### State Variables

The state space for the team of UAVs is defined by five variables: 

- Altitude, 
- Team formation: tight or loose, 
- ECM status: on or off, 
- Route segment: the segment of the map that the team is currently above,
- Team destroyed: whether the team has been destroyed by threats

Each of these state variables is modeled in XMDP by implementing the `language.domain.models.IStateVarValue` 
interface or one of its primitive derivatives: `IStateVarInt`, `IStateVarDouble` or `IStateVarBoolean`. For example, 
Altitude is implemented in the [TeamAltitude](models/TeamAltitude.java) class and provides the integer altitude level of the UAV
team.

XMDP allows us to define attributes associated with state variables that can be used in planning to provide additional 
information about the state variable. For the UAVs, we define the Route Segment variable to have the following attributes:

- [TargetDistribution](models/TargetDistribution.java) which represents the probability of targets of interest within the segment 
- [ThreatDistribution](models/ThreatDistribution.java) which represents the probability of threats within the segment.

Each of these attributes in XMDP is modeled by implementing the class `language.domain.models.IStateVarAttribute` 
and passed to the constructor for [RoutSegment](models/RouteSegment.java). The method `getAttributeValue` can be used to 
retrieve these attributes.

### Actions

The action space of the team of UAVs is defined by the following types of actions:
`IncAlt` [IncAltAction](models/IncAltAction.java), `DecAlt` [DecAltAction.java](models/DecAltAction.java), 
and `Fly` [FlyAction](models/FlyAction.java) denote the actions to increase altitude level, decrease altitude level,
and fly in the same altitude level, respectively. All of these actions fly the team forward by 1 route
segment. The `IncAlt` and `DecAlt` action types are parameterized by a `TeamAltitude` argument
denoting the change in altitude level, denoted in the forms `IncAlt(achange : TeamAltitude)` and
`DecAlt(achange : TeamAltitude)`, respectively. The altitude change is passed in to the constructor of these
actions.

For specification convenience, we incorporate type hierarchy for action types to allow reuse
of factored PSOs describing the actions' preconditions and effects on individual or groups of
variables. For instance, `IncAlt`, `DecAlt` and `Fly` actions all have an effect of advancing the team
forward by 1 route segment. We create a supertype [DurativeAction](models/DurativeAction.java) with the factored PSO 
specifying the effect of advancing t's value by 1. When defining `IncAlt`, `DecAlt` and `Fly` as subtypes
of `DurativeAction`, they automatically inherit its factored PSO of the effect on `t`. Although, 
subtypes are allowed to have additional preconditions, that is, the subtypes' preconditions can be
stronger than (and subsume) its supertype's precondition.

We define a helper action type `Tick`, also as a subtype of `DurativeAction`, to represent the
passage of time after the team has been destroyed by threats prior to reaching the end of the
route. This helper action allows the cost computation in planning to accrue the expected number
of targets missed throughout the entire route after the team has been destroyed.

`ChangeForm` [ChangeForm.java](models/ChangeForm.java) denotes the action to change formation of the team. It is parameterized by a
`TeamFormation` argument denoting the target formation: `ChangeForm(φtarget : TeamFormation)`. 

`SwitchECM` [SwitchECM.java](models/SwithECM.java) denotes the action to switch on and off the team's ECM. It is parameterized
by a `TeamECM` argument denoting the target ECM status: `SwitchECM(Etarget : TeamECM)`.

Both of these action types are instantaneous relative to the others.

### Transition model




