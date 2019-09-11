package mobilerobot.study.mturk;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import software.amazon.awssdk.services.mturk.model.Assignment;

public interface IAssignmentFilter {

	public boolean accept(Assignment assignment) throws ParserConfigurationException, SAXException, IOException;

	public String getRejectFeedback();
}
