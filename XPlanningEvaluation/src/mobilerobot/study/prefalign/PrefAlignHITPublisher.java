package mobilerobot.study.prefalign;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.json.simple.parser.ParseException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import mobilerobot.study.mturk.HITInfo;
import mobilerobot.study.mturk.HITPublisher;
import mobilerobot.study.mturk.MTurkAPIUtils;
import mobilerobot.utilities.FileIOUtils;
import software.amazon.awssdk.services.mturk.MTurkClient;
import software.amazon.awssdk.services.mturk.model.ReviewPolicy;

public class PrefAlignHITPublisher {

	private final HITPublisher mHITPublisher;
	private final File mHITInfoCSVFile;

	public PrefAlignHITPublisher(MTurkClient client) throws IOException {
		mHITPublisher = new HITPublisher(client);
		mHITInfoCSVFile = createHITInfoCSVFile();
	}

	public void publishAllHITs(boolean controlGroup, Set<String> validationQuestionDocNames) throws URISyntaxException,
			IOException, ClassNotFoundException, ParseException, ParserConfigurationException, TransformerException {
		File serializedLinkedQuestionsDir = FileIOUtils.getResourceDir(getClass(), "serialized-linked-questions");

		for (File file : serializedLinkedQuestionsDir.listFiles()) {
			try (FileInputStream fileIn = new FileInputStream(file)) {
				try (ObjectInputStream objectIn = new ObjectInputStream(fileIn)) {
					LinkedPrefAlignQuestions linkedPrefAlignQuestions = (LinkedPrefAlignQuestions) objectIn
							.readObject();

					boolean withExplanation = !controlGroup;
					// All PrefAlign question document names in this HIT will be written to hitInfo.csv
					String[] linkedQuestionDocNames = linkedPrefAlignQuestions
							.getLinkedQuestionDocumentNames(withExplanation);

					// ExternalURL contains a parameter that points to the first PrefAlign question HTML document in the link
					String headQuestionDocName = linkedQuestionDocNames[0];
					File questionXMLFile = createQuestionXMLFile(headQuestionDocName);

					ReviewPolicy assignmentReviewPolicy = MTurkAPIUtils
							.getAssignmentReviewPolicy(linkedPrefAlignQuestions, validationQuestionDocNames);
					HITInfo hitInfo = mHITPublisher.publishHIT(questionXMLFile, controlGroup, assignmentReviewPolicy);

					writeHITInfoToCSVFile(hitInfo, linkedQuestionDocNames);
				}
			}
		}
	}

	private File createQuestionXMLFile(String headQuestionDocName)
			throws ParserConfigurationException, IOException, TransformerException {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();

		// Disable external entities
		docFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		docFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
		docFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		Document doc = docBuilder.newDocument();

		// <ExternalQuestion xmlns="[the ExternalQuestion schema URL]">
		Element externalQuestionElement = doc.createElement("ExternalQuestion");
		externalQuestionElement.setAttribute("xmlns",
				"http://mechanicalturk.amazonaws.com/AWSMechanicalTurkDataSchemas/2006-07-14/ExternalQuestion.xsd");
		doc.appendChild(externalQuestionElement);

		// <ExternalURL>...</ExternalURL>
		Element externalURLElement = doc.createElement("ExternalURL");
		String externalURL = createExternalURL(headQuestionDocName);
		Text externalURLText = doc.createTextNode(externalURL);
		externalURLElement.appendChild(externalURLText);
		externalQuestionElement.appendChild(externalURLElement);

		// <FrameHeight>0</FrameHeight>
		Element frameHeightElement = doc.createElement("FrameHeight");
		Text frameHeightText = doc.createTextNode("0");
		frameHeightElement.appendChild(frameHeightText);
		externalQuestionElement.appendChild(frameHeightElement);

		File questionXMLFile = FileIOUtils.createOutputFile(headQuestionDocName + ".xml");
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(questionXMLFile);
		transformer.transform(source, result);

		return questionXMLFile;
	}

	private String createExternalURL(String headQuestionDocName) {
		// TODO
		return null;
	}

	private File createHITInfoCSVFile() throws IOException {
		File hitInfoCSVFile = FileIOUtils.createOutputFile("hitInfo.csv");
		try (BufferedWriter writer = Files.newBufferedWriter(hitInfoCSVFile.toPath())) {
			writer.write("HIT ID, HITType ID, Document Names\n");
		}
		return hitInfoCSVFile;
	}

	private void writeHITInfoToCSVFile(HITInfo hitInfo, String[] linkedQuestionDocNames) throws IOException {
		try (BufferedWriter writer = Files.newBufferedWriter(mHITInfoCSVFile.toPath())) {
			writer.write(hitInfo.getHITId());
			writer.write(", ");
			writer.write(hitInfo.getHITTypeId());
			for (String questionDocName : linkedQuestionDocNames) {
				writer.write(", ");
				writer.write(questionDocName);
			}
			writer.write("\n");
		}
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
