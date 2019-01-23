package examples.utils;

import java.io.File;

import examples.common.DSMException;
import examples.common.IXMDPLoader;
import language.exceptions.XMDPException;
import language.mdp.XMDP;

public class XMDPDataProvider {

	public static Object[][] loadXMDPs(String problemsPath, IXMDPLoader xmdpLoader) throws DSMException, XMDPException {
		File problemsDir = new File(problemsPath);
		File[] problemFiles = problemsDir.listFiles();
		Object[][] data = new Object[problemFiles.length][2];

		int i = 0;
		for (File problemFile : problemFiles) {
			XMDP xmdp = xmdpLoader.loadXMDP(problemFile);
			data[i] = new Object[] { problemFile, xmdp };
			i++;
		}
		return data;
	}
}
