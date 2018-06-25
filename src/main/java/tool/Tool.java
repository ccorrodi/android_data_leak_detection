package tool;

import leaks.Leak;
import parser.IParser;
import shell.control.ShellExecutor;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Responsible for the characteristics and generic methods of the different tools. All tools should use the same home
 * directory, apk path and android sdk. Furthermore, they should know in what directory their root is and what command
 * is used to trigger their analysis, which is dependent on the tools (therefore not static).
 * It also provides methods to run the analysis, which is pretty similar for all tools by simply passing the directory and
 * the command to run to the ShellExecutor. Other methods such as cleaning tool output or setting the command are more
 * tool specific and are therefore defined abstract.
 *
 * The Tool class implements the ITool Interface and is abstract. It will be extended by the different types of tools e.g.
 * FlowDroidTool
 *
 * @author Timo Spring
 * @see ITool
 */
public abstract class Tool implements ITool {
    protected static ShellExecutor shell;
    protected IParser parser;

    protected static String apkPath;
    protected static String androidSdk;
	protected final String appName;
	protected int matrixIndex;
	protected int exitCode;

	protected long duration;
	static long totalDuration;
    protected String command;
    protected static String homeDir;
    protected String toolDir;
    protected String toolName;
    protected String resultsFileName;
    protected ArrayList<Leak> foundLeaks;


    public Tool(String apkPath){
        String workingDir = System.getProperty("user.dir");
        this.homeDir = workingDir + "/..";
        this.apkPath = homeDir + apkPath;
        this.appName = apkPath.substring(apkPath.lastIndexOf('/') + 1, apkPath.indexOf(".apk"));
        this.androidSdk = workingDir + "/../tools/commonConfiguration/android-23.jar";
        this.shell = new ShellExecutor();
    }

    public Tool(String apkPath, String androidSdk){
        String workingDir = System.getProperty("user.dir");
        this.homeDir = workingDir + "/..";
        this.apkPath = apkPath;
        this.appName = apkPath.substring(apkPath.lastIndexOf('/') + 1, apkPath.indexOf(".apk"));
        this.androidSdk = androidSdk;
        this.shell = new ShellExecutor();

    }

    /**
     * Sets the command used to run the analysis
     */
    protected abstract void setCommand();

    /**
     * Cleans all folders containing results after they have been transferred and summarized in our report. This makes
     * sure that there is no chaos when analysing several apps in a row.
     */
    public abstract void cleanToolOutput();

    /**
     * Gets the results file containing all findings from the analysis.
     * @return File - results file of the tool
     */
    public abstract File getResultsFile();

    /**
     * Initializes the parser for the given results file.
     * @param file - File : results file containing all findings from the analysis.
     * @return new Parser Object
     */
    protected abstract IParser setParser(File file);

    /**
     * Gets the matrix index of a tool. The index is required to write to the collect column in the csv summary reports
     * or for creating the matching matrices.
     * @return - Integer between 0 and 4 (size of tools list).
     */
    public int getMatrixIndex(){
        return this.matrixIndex;
    }

    /**
     * Collect all leaks from the tool as a List of Leak objects.
     * @return ArrayList of Leak objects representing the tools findings.
     */
    @Override
    public ArrayList<Leak> getLeaks() {
        File file = getResultsFile();
        IParser parser = setParser(file);
        this.foundLeaks = parser.getLeaks(this);
        return foundLeaks;
    }


    /**
     * Runs the analysis for the given tool e.g. of type FlowDroidTool. It does so by calling the ShellExecutor and
     * passing it the directory where the command should be executed in and the command as a string. The ShellExecutor
     * will then take care of the execution and console output.
     */
    @Override
    public boolean runAnalysis()
    {
        long startTime = System.currentTimeMillis();
        exitCode = shell.runCommand(toolDir, command);
        long endTime = System.currentTimeMillis();

        if(exitCode == 0) {
            duration = endTime - startTime;
            totalDuration += duration;
            return true;
        }
        return false;
    }

    /**
     * Changes the used home directory. By default it points to the root of the droid-security-thesis workspace (from
     * gitHub) i.e. the parent folder of the benchmarking project. That's because this directory contains the "tools"
     * folder containing all required JARs and work files to run the individual analysis. Furthermore, it contains the
     * "apk_sample" folder, which contains the test apks to be run the analysis with. The home directory is currently
     * specified by taking the working directory (benchmarking folder) and then navigating to its parent folder.
     *
     * However, you have the possibility to change the home directory if required. This is mainly used for testing.
     * @param homeDir - String path to the home directory i.e. the folder containing all workspace data such as the
     *                "tools" folder or the "apk_sample" folder. Currently set to "droid-Security-Thesis" folder.
     *                Must be a valid path to directory.
     */
    @Override
    public void setHomeDir(String homeDir){
        this.homeDir = homeDir;
    }

    /**
     * Gets the command to a given Tool Object. Since the String commands used to trigger the analysis to run are
     * highly diverse depending on the tools, we provide a mean to check these commands.
     *
     * @return command as a String, used to trigger analysis to run in shell.
     */
    @Override
    public String getCommand(){
        return command;
    }


    /**
     * Gets the path to the directory in which the tool runs as a String.
     * @return String path to the tool's root directory.
     */
    @Override
    public String getToolDir() {
        return toolDir;
    }

    /**
     * Gets the toolName of the tool
     * @return String toolName of the tool
     */
    @Override
    public String getToolName() {
        return toolName;
    }

    /**
     * Get the duration of the analysis as a String.
     * @return String representation of the analysis duration
     */
    @Override
    public String getDuration(){
        String runningTime = format(duration);
        return runningTime;
    }

    /**
     * Formats the duration in the format hour: minute: second: miliseconds
     * and returns string representation of it.
     * @param duration - Long: duration of the tool analysis in miliseconds.
     * @return String representation of total duration in format hh:mm:ss:msms
     */
    private static String format(long duration) {
        long millis = duration;
        long second = TimeUnit.MILLISECONDS.toSeconds(millis);
        long minute = TimeUnit.MILLISECONDS.toMinutes(millis);
        long hour = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.SECONDS.toMillis(second);
        return String.format("%02d:%02d:%02d:%d", hour, minute, second, millis);
    }


	/**
	 * Get the filename to redirect stdout.
	 * It should be unique for each apk, so we can run stuff in parallel.
	 * For example, use apkFileName + "-icctaResults.txt" for iccta.
	 * @return
	 */
	protected String getResultsFileName(){
	    assert appName != null && toolName != null;
	    return appName + "-" + toolName + "Results.txt";
    }

    /**
     * Get the total duration (sum) of all analysis tools
     * @return String representation of duration
     */
    @Override
    public String getTotalDuration(){
	    String runningTime = format(totalDuration);
	    return runningTime;
    }

    /**
     * Gets the name of the application currently under investigation.
     * @return String: name of the application.
     */
    @Override
    public String getAppName(){
	    return appName;
    }

    /**
     * Gets the exit code of the tool. Important for deciding whether the analysis timed out or not.
     * @return boolean - exit code: 0 if analysis completed, 1 if time out occurred.
     */
    @Override
    public int getExitCode(){
	    return exitCode;
    }

}
