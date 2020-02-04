package examples.common;

import java.io.File;

import language.exceptions.XMDPException;
import language.mdp.XMDP;

public interface IXMDPLoader {

	public XMDP loadXMDP(File problemFile) throws DSMException, XMDPException;
}
