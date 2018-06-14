package comparator;

import leaks.Leak;
import tool.ITool;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;


/**
 * This class is responsible for providing overviews of the number of reported leaks and the number of overlaps
 * between the tools. It uses the ITool interface and the Leak objects to get the numbers required. It is being used by
 * the AnalysisRunner.
 *
 * @author Timo Spring
 */
public class ToolComparator {

    private ArrayList<ITool> tools;
    private ArrayList<Leak> leaks;
    private int[][] absMatchingMatrix;
    private double[][] relMatchingMatrix;
    private int numberOfLeaks;
    private int size;

    public ToolComparator(ArrayList<Leak> leaks, ArrayList<ITool> tools){
        assert tools.size() > 0;
        this.tools = tools;
        this.leaks = leaks;
        this.numberOfLeaks = leaks.size();
        this.size = tools.size();
        this.absMatchingMatrix = new int[size][size];
        this.relMatchingMatrix = new double[size][size];
    }


    /**
     * Returns a lower triangular matrix with the absolute number of overlaps between the tools. Each column and row of
     * the matrix corresponds to a certain tool. On the diagonal, the total number of reported leaks for the corresponding
     * tool are listed. Furthermore, a relative lower triangular matrix is returned, where the absolute number are put
     * relatively to the total number of reported leaks per tool.
     *
     * @return String representation of two lower triangular matrices. One showing absolute numbers the other one for
     * relative numbers.
     */
    public String getMatrix(){
        getAbsMatchingMatrix();
        getRelMatchingMatrix();
        return this.toString();
    }

    /**
     * Returns the absolute lower triangular matrix showing the total number of overlaps between the tools. On the diagonal
     * the total number of reported leaks for each tool is listed.
     *
     * @return String representation of the lower triangular absolute matching matrix.
     */
    public int[][] getAbsMatchingMatrix(){
        for(Leak leak : leaks){
            int[][] leakMatrix = createAbsMatchingMatrix(leak);
            for(int i = 0; i < size; i++){
                for(int j = 0; j <= i ; j++){
                    absMatchingMatrix[i][j] += leakMatrix[i][j];
                }
            }

        }
        return absMatchingMatrix;
    }

    /**
     * Returns the relative lower triangular matrix showing the relative number of overlaps between the tools. On the diagonal
     * the total number of reported leaks for each tool is listed.
     *
     * @return String representation of the lower triangular relative matching matrix.
     */
    public double[][] getRelMatchingMatrix(){
        for(int i = 0; i < size; i++){
            for(int j = 0; j <= i; j++){
                relMatchingMatrix[i][j] = (double) absMatchingMatrix[i][j] / numberOfLeaks * 100;
            }
        }
        return relMatchingMatrix;
    }

    /**
     * Creates an absolute lower triangular matching matrix to an underlying Leak object. It takes the Leak object and
     * checks which tools reported this leak. It then transfers the data to a matrix which is later merged with other
     * Leak matrices to yield in a summary for all leaks.
     *
     * @param leak - Leak object: used to get a list of tools that reported the same leak.
     * @return Double Array of Integers representing the absolute leak matrix.
     */
    protected int[][] createAbsMatchingMatrix(Leak leak){
        ArrayList<ITool> leakTools = leak.getTools();
        int[][] leakMatrix = new int[size][size];
        for(int i = 0; i < leakTools.size(); i++){
            for(int j = i; j < leakTools.size(); j++){
                int tool1Index = leakTools.get(i).getMatrixIndex();
                int tool2Index = leakTools.get(j).getMatrixIndex();
                if(tool1Index < tool2Index){
                    leakMatrix[tool2Index][tool1Index]++;
                } else {leakMatrix[tool1Index][tool2Index]++;}
            }
        }
        return leakMatrix;
    }

    /**
     * Provides a string representation of both matrices. This representation is used in the text file summary report.
     *
     * @return String representation of both matrices.
     */
    @Override
    public String toString(){
        StringBuilder toolsAxis = new StringBuilder();
        StringBuilder builder = new StringBuilder();
        builder.append("ABSOLUTE MATCHING MATRIX FOR: ").append(numberOfLeaks).append(" LEAKS\n");
        for(int i = 0; i < size; i++) {
            for(int j = 0; j < size; j++){
                builder.append(absMatchingMatrix[i][j]).append("\t");
            }
            for(ITool tool: tools){
                if(tool.getMatrixIndex() == i){
                    builder.append(tool.getToolName()).append("\t");
                    toolsAxis.append(tool.getToolName().subSequence(0, 3)).append("\t");
                }
            }
            builder.append("\n");
        }
        builder.append(toolsAxis.toString());
        StringBuilder toolAxisRel = new StringBuilder();
        builder.append("\n\n").append("RELATIVE MATCHING MATRIX FOR: ").append(numberOfLeaks).append(" LEAKS\n");
        NumberFormat formatter = new DecimalFormat("#0.00");
        for(int i = 0; i < size; i++) {
            for(int j = 0; j < size; j++){
                double val = relMatchingMatrix[i][j];
                builder.append(formatter.format(val)).append("%").append("\t");
            }
            for(ITool tool: tools){
                if(tool.getMatrixIndex() == i){
                    builder.append(tool.getToolName()).append("\t");
                    toolAxisRel.append(tool.getToolName().subSequence(0, 3)).append("\t");
                }
            }
            builder.append("\n");
        }
        builder.append(toolAxisRel.toString());

        return builder.toString();
    }


}
