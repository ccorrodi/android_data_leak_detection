package main;

import comparator.LeakComparator;
import comparator.ToolComparator;
import leaks.Leak;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import tool.*;
import writer.Writer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Responsible for running all the selected analysis tools on a given apk file. The
 * analysis tools are run sequentially on the apk each producing it's own results.
 *
 * The runner uses the Tools Interface, which all required methods to trigger the analysis for each
 * type of tool. To run the analysis tools, a script needs to be triggered in the console. For that purpose,
 * the Tools classes are calling the Shell and giving it the required command to trigger the tool as a String.
 *
 * ATTENTION: The results management is so far not implemented yet. This is why the stack trace is shown in
 * the console during the analyses since some results are only shown in the console.
 *
 * @author Timo Spring
 */
public class AnalysisRunner {
    private String appName;
    private ArrayList<Leak> allLeaks = new ArrayList<>();
    private ArrayList<Leak> groupedLeaks;
    private String matchingMatrix;
    private static ArrayList<ITool> tools;
    private static StringBuilder timedOut = new StringBuilder();
    private static final String APKFOLDERPATH = System.getProperty("user.dir") + "/../apksToTest/";


    /**
     * Runs the whole Analysis on a given application. It gets the apk to a provided folder path, initialized the tools
     * with the path to the apk, runs the tools on the application, groups and summarizes the Leak objects, and writes
     * the results to a CSV and text file.
     * @param args - path to the APK that you want to check in the analysis.
     */
    public static void main(String[] args){
    	String apkPath = args[0].substring(2);
        AnalysisRunner runner = new AnalysisRunner();
        String appName = apkPath.substring(apkPath.lastIndexOf("../")+2);
        runner.initializeTools(appName);
        runner.runAnalysis(appName);
        runner.summarizeLeaks();
        runner.createMatchingMatrix();
        runner.createSummaryReport();
        runner.createCsvResultsFile();
        runner.cleanTestResults();
        runner.printProgress(8, "");

       /*//LOCAL RUNNING CONFIG
        AnalysisRunner runner = new AnalysisRunner();
        File apkFolder = new File(APKFOLDERPATH);
        File[] apksToTest = apkFolder.listFiles((APKFOLDERPATH, apkName) -> apkName.endsWith(".apk"));
        System.out.printf("Total APKs to test: %s\n", apksToTest.length);

        for(File apks: apksToTest){
            String apkPath = apks.getPath();
            String appName = apkPath.substring(apkPath.lastIndexOf("../")+2);
            runner.initializeTools(appName);
            runner.runAnalysis(appName);
            runner.summarizeLeaks();
            //runner.createMatchingMatrix();
            runner.createSummaryReport();
            runner.createCsvResultsFile();
            runner.cleanTestResults();
            runner.printProgress(8, "");
        }*/


    }

    /**
     * Adds all tools to run to the ArrayList of Tools and passes the path to the apk to the tools.
     *
     * @param apkPath - String: Path to apk that should be analysed.
     */
    private void initializeTools(String apkPath){
        tools = new ArrayList<>();
        tools.add(new FlowDroidTool(apkPath));
        tools.add(new CovertTool(apkPath));
        tools.add(new IcctaTool(apkPath));
        tools.add(new Ic3Tool(apkPath));
        //tools.add(new HorndroidTool(apkPath));

    }

    /**
     * Runs the analysis for all tools. It does so by iterating through the list of tools and telling them to run themselves.
     * After a tool has run, we collect the Leaks that the tool reported.
     *
     * @param apkPath - String: path to the apk to be analysed. Used to get the name of the application.
     */
    private void runAnalysis(String apkPath) {
        appName = getAppName(apkPath);

        for(ITool tool: tools){
            printProgress(0, tool.getToolName());
            boolean exitCode = tool.runAnalysis();

            if(!exitCode){
                timedOut.append(appName).append(";").append(tool.getToolName()).append("\n");
            }else{
                printProgress(1, tool.getToolName());
                backupResultsFiles(tool, appName);
                collectLeaks(tool);
            }
        }

        printProgress(3, "");
    }

    /**
     * Summarizes the Leaks by using the LeakComparator object to compare all Leaks within the list of reported Leaks.
     * Results in a list of summarized and grouped Leak objects.
     */
    private void summarizeLeaks(){
        assert allLeaks != null && groupedLeaks.size() == 0;
        LeakComparator leakComparator = new LeakComparator();
        groupedLeaks = leakComparator.groupLeaks(allLeaks);
        printProgress(4, "");

    }

    /**
     * Gets the matching matrix which is a lower triangular matrix with the absolute and relative number of overlaps
     * in the findings between the tools. The matrix is provided by the ToolComparator object.
     */
    private void createMatchingMatrix(){
        ToolComparator comparator = new ToolComparator(groupedLeaks, tools);
        matchingMatrix = comparator.getMatrix();
    }

    /**
     * Collects all reported vulnerabilities from a tool and adds them to a global list of all vulnerabilities.
     *
     * @param tool - ITool interface implemented by the Tool class. Tool to collect the vulnerabilities from.
     */
    private void collectLeaks(ITool tool) {
        ArrayList<Leak> leaks = tool.getLeaks();
        printProgress(2, tool.getToolName());
        for(Leak leak : leaks){
            allLeaks.add(leak);
        }

    }

    /**
     * Creates a summary report of the final list of all leaks to a application. It does so by calling the Writer object
     * with all required information, such as the list of leaks, application name, matching matrices and status on which
     * tools timed out during the analysis.
     */
    private void createSummaryReport(){
        printProgress(5, "");
        Writer writer = new Writer(tools);
        writer.writeToTxtFile(groupedLeaks, appName, matchingMatrix, timedOut.toString());
    }

    /**
     * Creates a CSV summary of all findings, with each leak occupying a row in the file. Again this is done by calling
     * the Writer object and passing the list of leaks and the application name.
     */
    private void createCsvResultsFile(){
        printProgress(6, "");
        Writer writer = new Writer(tools);
        writer.writeToCsvFile(groupedLeaks, appName);
    }

    /**
     * Copies the results file from the tool directory to the "results" folder.
     * @param tool - ITool : selected tool from which to gather the analysis results.
     */
    private void backupResultsFiles(ITool tool, String appName){
        String workingDir = System.getProperty("user.dir");
        String resultsDir = workingDir + "/../results/rawResults/" + appName + "/";
        String toolName = tool.getToolName() + "Results";
        File resultsFile = tool.getResultsFile();
        String suffix = FilenameUtils.getExtension(resultsFile.getName());
        File targetDir = new File(resultsDir + "/" + appName + "-" + toolName + "." + suffix);

        try {
            FileUtils.copyFile(tool.getResultsFile(), targetDir);
        } catch (IOException e){
            System.out.println("Error: Results couldn't be transferred. " + e.getStackTrace());
        }
    }

    /**
     * Cleans the test outputs of the tools i.e. empties results folders, deletes created txt reports etc.
     * so that the analysis could be run on the same apk again without creating duplicates.
     * Uses the Tools Interface and its implementation of the clean methods.
     */
    private void cleanTestResults() {
        printProgress(7, "");
        for(ITool tool: tools){
            tool.cleanToolOutput();
        }
        tools.clear();
        allLeaks.clear();
        groupedLeaks.clear();
    }

    /**
     * Gets the name of the application out of the folder path.
     * @param apkPath - String path to the application
     * @return String - name of the application.
     */
    private String getAppName(String apkPath){
        return apkPath.substring(apkPath.lastIndexOf('/') + 1, apkPath.indexOf(".apk"));
    }

    /**
     * Prints the current analysis status to the console
     * @param i - Integer: status within the analysis.
     * @param toolName - name of the tool currently in progress of the analysis.
     */
    private void printProgress(int i, String toolName){
        switch (i){
            case 0: System.out.println(appName + ".apk: Running " + toolName + " analysis..."); break;
            case 1: System.out.println(appName + ".apk: Completed " + toolName + " analysis."); break;
            case 2: System.out.println(appName + ".apk: Collecting Leaks from " + toolName + "\n"); break;
            case 3: System.out.println(appName + ".apk: Full analysis with all tools completed. Raw results saved here: ~/droid-Security-thesis/results/rawResults/" + appName +"\n");break;
            case 4: System.out.println(appName + ".apk: Grouping and comparing Leaks..."); break;
            case 5: System.out.println(appName + ".apk: Creating summary report...");break;
            case 6: System.out.println(appName + ".apk: Adding Leaks to global CSV File...");break;
            case 7: System.out.println(appName + ".apk: Cleaning test output in tools folders...");break;
            case 8: System.out.println(appName + ".apk: Full analysis and evaluation completed successfully! Results can be found here: \n"
            + "Summary Text file: \t~/droid-Security-thesis/results/summary_" + appName + ".txt\n"
            + "Global CSV file: \t~/droid-Security-thesis/results/csv/summary_" + appName + ".csv\n\n"
            + "*************************************************************************\n\n");break;

        }
    }


}
