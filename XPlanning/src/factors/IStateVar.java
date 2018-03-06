package factors;

/**
 * {@link IStateVar} is an interface to a state variable. A state variable has a unique name and a value of a specific
 * type.
 * 
 * @author rsukkerd
 *
 */
public interface IStateVar {

	public String getName();

	public IStateVarValue getValue();
}
