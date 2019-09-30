package mobilerobot.study.prefalign.mturk;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import javax.xml.parsers.ParserConfigurationException;

import org.json.simple.parser.ParseException;
import org.xml.sax.SAXException;

import mobilerobot.study.mturk.AssignmentsCollector;
import mobilerobot.study.mturk.HITProgress;
import mobilerobot.study.mturk.IAssignmentFilter;
import mobilerobot.study.mturk.MTurkAPIUtils;
import mobilerobot.utilities.FileIOUtils;
import software.amazon.awssdk.services.mturk.MTurkClient;

public class PrefAlignAssignmentsCollector {

	private final AssignmentsCollector mAssignmentsCollector;

	public PrefAlignAssignmentsCollector(MTurkClient client, File hitInfoCSVFile, String[] dataTypes, int numQuestions)
			throws IOException {
		mAssignmentsCollector = new AssignmentsCollector(client, hitInfoCSVFile, dataTypes, numQuestions);
	}

	public HITProgress collectHITProgress(int hitIndex)
			throws ParserConfigurationException, SAXException, IOException, ParseException {
		// Set appropriate assignment filter(s) here
		IAssignmentFilter[] assignmentFilters = {};
		return mAssignmentsCollector.collectHITProgress(hitIndex, assignmentFilters);
	}

	public void writeAssignmentsToCSVFile(int hitIndex, HITProgress hitProgress, File currentAssignmentsCSVFile)
			throws ParserConfigurationException, SAXException, IOException, ParseException {
		mAssignmentsCollector.writeAssignmentsToCSVFile(hitIndex, hitProgress, currentAssignmentsCSVFile);
	}

	public static void main(String[] args)
			throws URISyntaxException, IOException, ParserConfigurationException, SAXException, ParseException {
		String hitInfoCSVFilename = args[0]; // hitInfo.csv filename
		int hitIndex = Integer.parseInt(args[1]); // HIT index to collect assignments
		String clientType = args[2]; // -prod or -sandbox
		String currentAssignmentsCSVFilename = args.length > 3 ? args[3] : null; // current assignments.csv filename if exists

		File hitInfoCSVFile = FileIOUtils.getFile(PrefAlignAssignmentsCollector.class, "hit-info", hitInfoCSVFilename);
		File currentAssignmentsCSVFile = currentAssignmentsCSVFilename != null
				? FileIOUtils.getFile(PrefAlignAssignmentsCollector.class, "assignments", currentAssignmentsCSVFilename)
				: null;

		MTurkClient client;
		if (clientType.equals("-prod")) {
			client = MTurkAPIUtils.getProductionClient();
		} else if (clientType.equals("-sandbox")) {
			client = MTurkAPIUtils.getSandboxClient();
		} else {
			throw new IllegalArgumentException("Need MTurk client type argument");
		}

		String[] dataTypes = { "ref", "total-cost", "answer", "confidence", "elapsedTime" };
		int numQuestions = 4;
		PrefAlignAssignmentsCollector assignmentsCollector = new PrefAlignAssignmentsCollector(client, hitInfoCSVFile,
				dataTypes, numQuestions);
		HITProgress hitProgress = assignmentsCollector.collectHITProgress(hitIndex);
		assignmentsCollector.writeAssignmentsToCSVFile(hitIndex, hitProgress, currentAssignmentsCSVFile);
	}

}
