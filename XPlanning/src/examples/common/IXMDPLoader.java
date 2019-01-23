package examples.common;

import java.io.File;
import java.util.Set;

import language.exceptions.XMDPException;
import language.mdp.XMDP;

public interface IXMDPLoader {
	public Set<XMDP> loadAllXMDPs() throws DSMException, XMDPException;

	public XMDP loadXMDP(File missionJsonFile) throws DSMException, XMDPException;
}
