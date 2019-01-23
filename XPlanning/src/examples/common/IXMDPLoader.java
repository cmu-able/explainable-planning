package examples.common;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.json.simple.parser.ParseException;

import language.exceptions.XMDPException;
import language.mdp.XMDP;

public interface IXMDPLoader {
	public Set<XMDP> loadAllXMDPs() throws IOException, ParseException, DSMException, XMDPException;

	public XMDP loadXMDP(File missionJsonFile) throws IOException, ParseException, DSMException, XMDPException;
}
