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

	public PrefAlignAssignmentsCollector(MTurkClient client) {
		mAssignmentsCollector = new AssignmentsCollector(client);
	}

	public HITProgress collectHITProgress(int hitIndex, File hitInfoCSVFile)
			throws ParserConfigurationException, SAXException, IOException, ParseException {
		// Set appropriate assignment filter(s) here
		IAssignmentFilter[] assignmentFilters = {};
		return mAssignmentsCollector.collectHITProgress(hitIndex, hitInfoCSVFile, assignmentFilters);
	}

	public void writeAssignmentsToCSVFile(int hitIndex, HITProgress hitProgress, File currentAssignmentsCSVFile,
			String[] dataTypes, int numQuestions)
			throws ParserConfigurationException, SAXException, IOException, ParseException {
		mAssignmentsCollector.writeAssignmentsToCSVFile(hitIndex, hitProgress, currentAssignmentsCSVFile, dataTypes,
				numQuestions);
	}

	public void approveAssignments(int hitIndex, File assignmentsCSVFile) throws IOException {
		mAssignmentsCollector.approveAssignments(hitIndex, assignmentsCSVFile);
	}

	public static void main(String[] args)
			throws URISyntaxException, IOException, ParserConfigurationException, SAXException, ParseException {
		String option = args[0]; // args[0]: option
		String clientType = args[1]; // args[1]: -prod or -sandbox

		MTurkClient client;
		if (clientType.equals("-prod")) {
			client = MTurkAPIUtils.getProductionClient();
		} else if (clientType.equals("-sandbox")) {
			client = MTurkAPIUtils.getSandboxClient();
		} else {
			throw new IllegalArgumentException("Need MTurk client type argument");
		}

		PrefAlignAssignmentsCollector assignmentsCollector = new PrefAlignAssignmentsCollector(client);

		if (option.equals("collectAssignments")) {
			String hitInfoCSVFilename = args[2]; // args[2]: hitInfo.csv filename
			int hitIndex = Integer.parseInt(args[3]); // args[3]: HIT index to collect assignments
			// args[4]: current assignments.csv filename if exists
			String currentAssignmentsCSVFilename = args.length > 4 ? args[4] : null;

			File hitInfoCSVFile = FileIOUtils.getFile(PrefAlignAssignmentsCollector.class, "hit-info",
					hitInfoCSVFilename);
			File currentAssignmentsCSVFile = currentAssignmentsCSVFilename != null
					? FileIOUtils.getFile(PrefAlignAssignmentsCollector.class, "assignments",
							currentAssignmentsCSVFilename)
					: null;

			String[] dataTypes = { "ref", "total-cost", "answer", "confidence", "elapsedTime" };
			int numQuestions = 4;

			HITProgress hitProgress = assignmentsCollector.collectHITProgress(hitIndex, hitInfoCSVFile);
			assignmentsCollector.writeAssignmentsToCSVFile(hitIndex, hitProgress, currentAssignmentsCSVFile, dataTypes,
					numQuestions);
		} else if (option.equals("approveAssignments")) {
			String assignmentsCSVFilename = args[2]; // args[2]: assignments.csv filename
			int hitIndex = Integer.parseInt(args[3]); // args[3]: HIT index to approve assignments

			File assignmentsCSVFile = FileIOUtils.getFile(PrefAlignAssignmentsCollector.class, "assignments",
					assignmentsCSVFilename);

			assignmentsCollector.approveAssignments(hitIndex, assignmentsCSVFile);
		}

	}

}
