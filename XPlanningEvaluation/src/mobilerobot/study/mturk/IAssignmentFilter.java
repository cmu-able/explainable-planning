package mobilerobot.study.mturk;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.json.simple.parser.ParseException;
import org.xml.sax.SAXException;

import software.amazon.awssdk.services.mturk.model.Assignment;

public interface IAssignmentFilter {

	public boolean accept(Assignment assignment)
			throws ParserConfigurationException, SAXException, IOException, ParseException;

	public String getRejectFeedback();
}
