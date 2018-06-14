package parser;


import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Responsible for parsing flowdroid tool results files and returning leaks that are indicated inside this results file (.txt)
 * Defines the regex required to find the desired components inside the results file such as leak description, class toolName,
 * method toolName etc. Furthermore, it overrides the method to find the method toolName inside the leak description.
 *
 * It extends the Parsers class that implements the IParser Interface.
 *
 * @author Timo Spring
 */
public class FlowDroidParser extends Parsers {
    protected Pattern flowdroidPattern;
    private Matcher flowdroidMatcher;

    /**
     * Defines the regex required to parse the flowdroid results files and prepares the pattern for the matching.
     * @param file - flowdroid tool results file (.xml) describing the found leaks
     */
    public FlowDroidParser(File file){
        super(file);
        setLeakTag("Found a flow");
        setCutAwayTag("Maximum memory");

        setClassNameRegex("(?<=\\(in \\<).*(?=\\: )");
        setFullMethodRegex("(?<=\\(in \\<).*(?=\\>\\))");
        setMethodNameRegex("(?<= ).*");
        setMethodReturnRegex(".*(?= )");
        setSinkMethodRegex("(?<=: ).*(?=>)");

        pattern = Pattern.compile(getLeakTag());

    }

    /**
     * Finds the name of the parent method, where the call to the data sink occurs. It takes the description of the
     * vulnerability, shortens it to only include the class and method name, then splits it between these two. The second
     * part contains the method name and method return, which needs to be split again.
     *
     * @param finding - String description of the leak.
     * @return String Array: containing first the method return type and then the method name as Strings.
     */
    @Override
    protected String[] findMethodName(String finding) {
        String shortedFinding = findLastOccurrence(finding, getFullMethodRegex());
        String[] classAndMethod = shortedFinding.split(" ");
        String[] method = {classAndMethod[2], classAndMethod[1]};
        return method;
    }


    /**
     * Finds the class name within the leak description from FlowDroid.
     *
     * @param finding - String description of the leak.
     * @return String name of the class where the data leak occurs.
     */
    @Override
    protected String findClassName(String finding) {
        String className = findLastOccurrence(finding, getClassNameRegex());
        return className;

    }

    /**
     * Shortens the leak description by containing only the class and method name (and method return).
     * Is used to find the method name afterwards.
     *
     * @param finding - String: description of the leak
     * @param regex - String: used to shorten the leak Description.
     * @return shortened description of the leak containing the class and method name.
     */
    protected String findLastOccurrence (String finding, String regex){
        String fullMethod = "";
        flowdroidPattern = Pattern.compile(regex);
        flowdroidMatcher = flowdroidPattern.matcher(finding);

        while(flowdroidMatcher.find()){
            fullMethod = flowdroidMatcher.group();
        }
        return fullMethod;
    }

}
