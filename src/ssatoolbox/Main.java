/*

Copyright (c) 2010, Jan Saputra Müller, Paul von Bünau, Frank C. Meinecke,
Franz J. Kiraly and Klaus-Robert Müller.
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice, this
list of conditions and the following disclaimer in the documentation and/or other
 materials provided with the distribution.

* Neither the name of the Berlin Institute of Technology (Technische Universität
Berlin) nor the names of its contributors may be used to endorse or promote
products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 */

package ssatoolbox;

import com.jmatio.io.*;
import com.jmatio.types.*;
import gnu.getopt.*;
import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * This is the main class which implements the business-logic. It reacts to events
 * from the GUI (class GUI) via the ControllerInterface and to changes in the data
 * (class Data) and the SSA parameters (class SSAParameters). 
 * 
 * In particular, Main is responsible for verifying the validity of SSA parameter 
 * settings, invoking the SSA algorithm and driving the data import/export 
 * process. Moreover, it controls the state of the GUI via the ViewInterface.
 *
 * @author Jan Saputra Mueller, saputra@cs.tu-berlin.de
 */
public class Main {
    public SSA ssa = new SSA();
    public SSAParameters parameters = new SSAParameters();
    public Results results = null;
    public Data data = new Data();
    private GUI gui = null;
    private Logger logger = null;

    /** Saves the toolbox configuration */
    protected ToolboxConfig toolboxConfig = new ToolboxConfig();

    /** Saves the current SSA version, which is loaded from the package descriptor */
    public String SSA_VERSION = null;

    public static final int TOOLBOX_MODE_OTHER = 0;
    public static final int TOOLBOX_MODE_STANDALONE = 1;
    public static final int TOOLBOX_MODE_MATLAB = 2;

    public int toolboxMode = TOOLBOX_MODE_OTHER;

    /**
     * Constructor for class Main.
     * 
     * @param startGUI set this to true to start the GUI of the standalone version of the toolbox
     * @param logger logger to use (is ignored in case startGUI == true)
     */
    public Main(boolean startGUI, Logger logger) {
        Package p = this.getClass().getPackage();
        SSA_VERSION = p.getImplementationVersion();

	data.addPropertyChangeListener(new PropertyChangeListener() {
	    public void propertyChange(PropertyChangeEvent arg0) {                
		if(arg0.getPropertyName().equals("numberOfEqualSizeEpochs")) {
		    if(data.getNumberOfEqualSizeEpochs() != -1) {
			if(data.getNumberOfEqualSizeEpochs() < 2) {
			    appendToLog("ERROR: Number of epochs must be greater than 2");
			    data.setNumberOfEqualSizeEpochs( ((Integer)arg0.getOldValue()).intValue() );
			}
		    }
		    return;
		} 
	    }
	});
    
	parameters.addPropertyChangeListener(new PropertyChangeListener() {
	    public void propertyChange(PropertyChangeEvent arg0) {
		if(arg0.getPropertyName().equals("numberOfStationarySources")) {
		    if(parameters.getNumberOfStationarySources() != -1) {
			if(parameters.getNumberOfStationarySources() >= data.getNumberOfDimensions()) {
			    appendToLog("ERROR: Number of stationary sources >= number of input dimensions");
			    parameters.setNumberOfStationarySources(((Integer)arg0.getOldValue()).intValue());
			}
		    }
		    return;
		}
	    }
	});

	parameters.addPropertyChangeListener(new PropertyChangeListener() {
	    public void propertyChange(PropertyChangeEvent arg0) {
		if(arg0.getPropertyName().equals("useCovariance") ||
			arg0.getPropertyName().equals("useMean") ) {
		    if(parameters.getNumberOfStationarySources() != -1) {
			if(parameters.getNumberOfStationarySources() >= data.getNumberOfDimensions()) {
			    appendToLog("ERROR: Number of stationary sources >= number of input dimensions");
			    parameters.setNumberOfStationarySources(((Integer)arg0.getOldValue()).intValue());
			}
		    }
		    return;
		}
	    }
	});
            
        if(startGUI)
        {
            toolboxMode = TOOLBOX_MODE_STANDALONE;
            UIManager.LookAndFeelInfo lif [] =  UIManager.getInstalledLookAndFeels();

            gui = new GUI(this, parameters, data);
            setLogger(gui);
            gui.setGUIState(GUI.STATE_NO_DATA);
            gui.showGUI();
        }
        else
        {
            setLogger(logger);
        }

        appendToLog("*** Welcome to the SSA Toolbox (version " + SSA_VERSION + ") ***");
        if(hasGUI())
        {
            appendToLog("Now you might want to load some data (see menu File).");
        }
    }
    
    /**
     * Returns true, if we the GUI of the standalone version is active.
     */
    public boolean hasGUI()
    {
        return (gui != null);
    }

    /**
     * Main function.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        if(args.length == 0)
        {
            new Main(true, null);
        }
        else
        {
            ConsoleLogger cl = new ConsoleLogger();
            Main ssaMain = new Main(false, cl);
            ssaMain.toolboxMode = TOOLBOX_MODE_STANDALONE;

            String inputFile = null;
            int d = -1;
            int reps = 5;
            String epochFile = null;
            int equalEpochs = -1;
            boolean useMean = true;
            boolean useCovariance = true;
            long randomSeed = 0;
            boolean useJBlas = false;
            String outputFile = null;

            // Parse the command line using java-getopt.
            // i: input data (time series)
            // d: number of stationary sources
            // r: number of restarts
            // e: custom epochization
            // n: number of epochs
            // m: use mean
            // c: use covariance
            // s: random seed
            // j: use jBlas
            // o: output file
            Getopt g = new Getopt("ssa.jar", args, "i:d:r:e:n:m:c:s:j:o:");
            int c;
            String arg;
            while((c = g.getopt()) != -1)
            {
                arg = g.getOptarg();

                switch(c)
                {
                    case 'i':
                        inputFile = arg;
                        break;
                    case 'd':
                        try
                        {
                            d = Integer.parseInt(arg);
                        }
                        catch(NumberFormatException e)
                        {
                            ssaMain.appendToLog("Argument of option -d has to be a number.");
                            return;
                        }
                        break;
                    case 'r':
                        try
                        {
                            reps = Integer.parseInt(arg);
                        }
                        catch(NumberFormatException e)
                        {
                            ssaMain.appendToLog("Argument of option -r has to be a number.");
                            return;
                        }
                        break;
                    case 'e':
                        epochFile = arg;
                        break;
                    case 'n':
                        try
                        {
                            equalEpochs = Integer.parseInt(arg);
                        }
                        catch(NumberFormatException e)
                        {
                            ssaMain.appendToLog("Argument of option -r has to be a number.");
                            return;
                        }
                        break;
                    case 'm':
                        if(arg.equals("0")) useMean = false;
                        else if(arg.equals("1")) useMean = true;
                        else
                        {
                            ssaMain.appendToLog("Argument of option -m has to be 0 or 1.");
                            return;
                        }
                        break;
                    case 'c':
                        if(arg.equals("0")) useCovariance = false;
                        else if(arg.equals("1")) useCovariance = true;
                        else
                        {
                            ssaMain.appendToLog("Argument of option -c has to be 0 or 1.");
                            return;
                        }
                        break;
                    case 's':
                        try
                        {
                            randomSeed = Long.parseLong(arg);
                        }
                        catch(NumberFormatException e)
                        {
                            ssaMain.appendToLog("Argument of option -s has to be a number.");
                            return;
                        }
                        break;
                    case 'j':
                        if(arg.equals("0")) useJBlas = false;
                        else if(arg.equals("1")) useJBlas = true;
                        else
                        {
                            ssaMain.appendToLog("Argument of option -j has to be 0 or 1.");
                            return;
                        }
                        break;
                    case 'o':
                        outputFile = arg;
                        break;
                }
            }

            if(useJBlas)
            {
                SSAMatrix.setGlobalLib(SSAMatrix.JBLAS);
                ssaMain.appendToLog("Using jBlas as library instead of Colt.");
            }

            if(inputFile != null)
            {
                ssaMain.loadTimeseries(new java.io.File(inputFile));
            }
            else
            {
                ssaMain.appendToLog("An input file has to be passed using the option -i.");
                return;
            }

            if(outputFile == null)
            {
                ssaMain.appendToLog("An output file has to be passed using the option -o.");
                return;
            }

            if(d > -1)
            {
                ssaMain.parameters.setNumberOfStationarySources(d);
            }
            else
            {
                ssaMain.appendToLog("You have to specify the number of stationary sources using the option -d");
                return;
            }

            if(reps > 0)
            {
                ssaMain.parameters.setNumberOfRestarts(reps);
            }
            else
            {
                ssaMain.appendToLog("The number of restarts specified by the option -r has to be a positive number.");
                return;
            }

            ssaMain.parameters.setUseMean(useMean);
            ssaMain.parameters.setUseCovariance(useCovariance);

            if(equalEpochs > -1)
            {
                ssaMain.data.setNumberOfEqualSizeEpochs(equalEpochs);
                ssaMain.data.setEpochType(Data.EPOCHS_EQUALLY);
            }
            else if(epochFile != null)
            {
                ssaMain.loadEpochDefinitionCSV(new java.io.File(epochFile));
                ssaMain.data.setEpochType(Data.EPOCHS_CUSTOM);
            }
            else if(!ssaMain.data.hasCustomEpochDefinition())
            {
                ssaMain.data.setEpochType(Data.EPOCHS_EQUALLY_HEURISTIC);
            }

            if(randomSeed > 0)
            {
                SSAMatrix.setRandomSeed(randomSeed);
                ssaMain.appendToLog("Random seed set to " + randomSeed + ".");
            }

            boolean ret = ssaMain.runSSA(false);
            if(!ret)
            {
                // running was not successful
                return;
            }

            if(outputFile.toLowerCase().endsWith(".mat"))
            {
                // output to *.mat file
                ssaMain.saveResultMatlab(new java.io.File(outputFile));
            }
            else
            {
                // output to multiple *.csv files, using outputFile as a prefix
                if(outputFile.endsWith(File.separator)) outputFile = outputFile.substring(0, outputFile.length() - 1);
                ssaMain.saveAllToCSV(new java.io.File(outputFile));
            }
        }
    }

    /**
     * Starts the SSA algorithm.
     *
     * @param sepThread set this to true to run SSA in a seperate thread.
     */
    public boolean runSSA(boolean sepThread) {
        if(parameters.getNumberOfStationarySources() == -1) {
            appendToLog("ERROR: Number of stationary sources not specified");
            return false;
        }
            
        if(data.getEpochType() == Data.EPOCHS_EQUALLY && data.getNumberOfEpochs() == -1) {
            appendToLog("ERROR: Epochs not specified");
            return false;
        }

        ssa.setLogger(logger);
        data.setLogger(logger);

        if(hasGUI())
        {
            gui.setGUIState(GUI.STATE_SSA_RUNNING);
        }
                    
        if(sepThread)
        {
            (new Thread() {
                @Override
                public void run() {
                    try {
                        results = ssa.optimize(parameters, data);
                        if(hasGUI())
                        {
                            gui.setGUIState(GUI.STATE_RESULT_AVAILABLE);
                        }
                    }
                    catch(RuntimeException ex) {
                        appendToLog(ex.getMessage());
                        if(hasGUI())
                        {
                            gui.setGUIState(GUI.STATE_PARAMETRIZATION);
                        }
                    }
                    catch(java.lang.OutOfMemoryError e)
                    {
                        printJavaHeapSpaceError();
                    }
                }
            }).start();
        }
        else
        {
            try {
                results = ssa.optimize(parameters, data);
            }
            catch(RuntimeException ex) {
                appendToLog(ex.getMessage());
            }
            catch(java.lang.OutOfMemoryError e)
            {
                printJavaHeapSpaceError();
                return false;
            }
        }

        return true;
    }

    /**
     * Stops the SSA algorithm.
     */
    public void stopSSA() {
        ssa.stop();
    }

    /**
     * Exits the program.
     */
    public void exit() {
        System.exit(0);
    }

    /**
     * Loads a time series from a file.
     *
     * @param f file (in *.csv or *.mat format)
     */
    public void loadTimeseries(File f)
    {
        String filename = f.getPath().toLowerCase();

        try {
            if(filename.endsWith(".mat"))
            {
                loadDataMatlab(f);
            }
            else if(filename.endsWith(".csv"))
            {
                loadTimeseriesCSV(f);
            }
            else
            {
                appendToLog("Error: Unknown file extension.");
            }
        }
        catch(java.lang.OutOfMemoryError e)
        {
            printJavaHeapSpaceError();
        }
    }

    /**
     * Loads a timeseries from a CSV-file.
     * The file has to contain one sample per row.
     *
     * @param f CSV-file
     */
    public void loadTimeseriesCSV(File f) {
        appendToLog("Loading data ...");

        // try to open csv-file
        BufferedReader br = null;
        try
        {
            br = new BufferedReader(new FileReader(f));
        }
        catch(IOException e)
        {
            appendToLog("Error opening file: " + e);
            return;
        }

        // parse csv-file
        String line;
        LinkedList<double[]> lineList = new LinkedList<double[]>();
        try
        {

            while((line = br.readLine()) != null)
            {
                String tLine = line.trim();
                if(tLine.equals("") || tLine.charAt(0) == '#')
                {
                    // skip empty line or comment
                    continue;
                }
                StringTokenizer st = new StringTokenizer(tLine, " \t,;");
                int tokens = st.countTokens();
                double doubleLine[] = new double[tokens];
                for(int i = 0; i < tokens; i++)
                {
                    doubleLine[i] = Double.valueOf(st.nextToken());
                }
                lineList.add(doubleLine);
            }
            br.close();
        }
        catch(IOException e)
        {
            appendToLog("Error reading file: " + e);
        }
        catch(NumberFormatException e)
        {
            appendToLog("Error converting string to number: " + e);
        }

        double dataArray[][] = new double[lineList.size()][lineList.getFirst().length];
        for(int i = 0; i < dataArray.length; i++)
        {
            dataArray[i] = lineList.get(i);
        }

        if(dataArray[0].length > dataArray.length)
        {
            // more columns than rows => samples are in the columns
            SSAMatrix timeSeries = new SSAMatrix(dataArray);
            data.setTimeSeries(timeSeries, f);
            data.setInputDataformat(Data.DATAFORMAT_CHANNELS_X_TIME);
            data.setOutputDataformat(Data.DATAFORMAT_CHANNELS_X_TIME);
        }
        else
        {
            // more rows than columns => samples are in the rows
            SSAMatrix timeSeries = new SSAMatrix(dataArray).transpose();
            data.setTimeSeries(timeSeries, f);
            data.setInputDataformat(Data.DATAFORMAT_TIME_X_CHANNELS);
            data.setOutputDataformat(Data.DATAFORMAT_TIME_X_CHANNELS);
        }

        appendToLog("Loaded data from file " + f.getPath() + ":");
        appendToLog("  dimensions=" + data.getNumberOfDimensions() + ",total number of samples=" + data.getTotalNumberOfSamples());
        if(hasGUI())
        {
            gui.setGUIState(GUI.STATE_PARAMETRIZATION);
        }
    }

    /**
     * Loads an epoch definition from a CSV-file.
     * For each loaded sample this file has to contain one row with the epoch
     * number to which the sample belongs to.
     *
     * @param f CSV epoch definition file
     */
    public void loadEpochDefinitionCSV(File f) {
        appendToLog("Loading epoch definition file ...");

        // try to open csv-file
        BufferedReader br = null;
        try
        {
            br = new BufferedReader(new FileReader(f));
        }
        catch(IOException e)
        {
            appendToLog("Error opening file: " + e);
            return;
        }

        // parse csv-file
        int epDef[] = new int[data.getTotalNumberOfSamples()];
        String line;
        try
        {
            int i = 0;
            while((line = br.readLine()) != null)
            {
                String tLine = line.trim();
                if(tLine.equals("") || tLine.charAt(0) == '#')
                {
                    // skip empty line or comment
                    continue;
                }
                if(i == data.getTotalNumberOfSamples())
                {
                    appendToLog("Error: File contains more epoch belongings than available samples");
                    return;
                }

                epDef[i] = (int)Double.valueOf(tLine).doubleValue();
                i++;
            }
            br.close();
            if(i < data.getTotalNumberOfSamples())
            {
                appendToLog("Error: File contains less epoch belongings than available samples");
                return;
            }
        }
        catch(IOException e)
        {
            appendToLog("Error reading file: " + e);
        }
        catch(NumberFormatException e)
        {
            appendToLog("Error converting string to number: " + e);
        }
        catch(java.lang.OutOfMemoryError e)
        {
            printJavaHeapSpaceError();
        }

        // count epochs
        TreeMap<Integer, Integer> count = new TreeMap<Integer, Integer>();
        for(int i = 0; i < epDef.length; i++)
        {
            Integer samples;
            if((samples = count.get(epDef[i])) == null)
            {
                samples = 0;
            }
            samples++;
            count.put(epDef[i], samples);
        }

        int minEpochSize = Integer.MAX_VALUE;
        int maxEpochSize = 0;
        Iterator<Integer> it = count.values().iterator();
        while(it.hasNext())
        {
            int next = it.next();
            if(next < minEpochSize) minEpochSize = next;
            if(next > maxEpochSize) maxEpochSize = next;
        }
        try
        {
            data.setCustomEpochDefinition(epDef, count.keySet().size(), minEpochSize, f);
        }
        catch(IllegalArgumentException e)
        {
            appendToLog(e.getMessage());
            return;
        }
        catch(java.lang.OutOfMemoryError e)
        {
            printJavaHeapSpaceError();
        }
        
        appendToLog("Loaded epoch definition from file " + f.getPath() + ":");
        appendToLog("  number of epochs=" + count.keySet().size()
                         + ",min. no. samples in epoch=" + minEpochSize
                         + ",max. no. samples in epoch=" + maxEpochSize);
    }

    /**
     * Loads data (and an epoch definition if available) from a MAT-file.
     *
     * @param f MAT-file
     */
    public void loadDataMatlab(File f) {
        appendToLog("Loading data from Matlab file ...");

        MatFileReader mfr;
        try
        {
            mfr = new MatFileReader(f);
        }
        catch (IOException e)
        {
            appendToLog("Error opening file: " + e);
            return;
        }

        Map<String, MLArray> map = mfr.getContent();
        MLArray Xmat = map.get("X");
        if(Xmat == null)
        {
            appendToLog("Error: No timeseries found in file (i.e. no variable X)");
            return;
        }

        switch(Xmat.getType())
        {
            case MLArray.mxDOUBLE_CLASS:
                SSAMatrix dm = new SSAMatrix(((MLDouble)Xmat).getArray());
                if(dm.getColumns() > dm.getRows())
                {
                    data.setTimeSeries(dm, f);
                    data.setInputDataformat(Data.DATAFORMAT_CHANNELS_X_TIME);
                    data.setOutputDataformat(Data.DATAFORMAT_CHANNELS_X_TIME);
                }
                else
                {
                    data.setTimeSeries(dm.transpose(), f);
                    data.setInputDataformat(Data.DATAFORMAT_TIME_X_CHANNELS);
                    data.setOutputDataformat(Data.DATAFORMAT_TIME_X_CHANNELS);
                }
                appendToLog("Loaded data from file " + f.getPath() + ":");
                appendToLog("  dimensions=" + data.getNumberOfDimensions() + ",total number of samples=" + data.getTotalNumberOfSamples());
                break;
            case MLArray.mxCELL_CLASS:
                MLCell mlc = (MLCell)Xmat;
                SSAMatrix timeSeries = null;
                int dim = 0;
                LinkedList<Integer> epList = new LinkedList<Integer>();
                int minEpochSize = Integer.MAX_VALUE;
                int maxEpochSize = 0;
                boolean dataInCol = true;
                for(int i = 0; i < mlc.getSize(); i++)
                {
                    if(mlc.get(i).getType() != MLArray.mxDOUBLE_CLASS)
                    {
                        appendToLog("Error: Cell array X has to contain only real-valued matrices");
                        return;
                    }
                    if(i == 0)
                    {
                        timeSeries = new SSAMatrix(((MLDouble)mlc.get(0)).getArray());
                        if(timeSeries.getColumns() > timeSeries.getRows())
                        {
                            data.setInputDataformat(Data.DATAFORMAT_CHANNELS_X_TIME);
                            data.setOutputDataformat(Data.DATAFORMAT_CHANNELS_X_TIME);
                            dataInCol = true;
                        }
                        else
                        {
                            timeSeries = timeSeries.transpose();
                            data.setInputDataformat(Data.DATAFORMAT_TIME_X_CHANNELS);
                            data.setOutputDataformat(Data.DATAFORMAT_TIME_X_CHANNELS);
                            dataInCol = false;
                        }
                        dim = timeSeries.getRows();
                        for(int j = 0; j < timeSeries.getColumns(); j++)
                        {
                            epList.add(i);
                        }
                        if(timeSeries.getColumns() < minEpochSize) minEpochSize = timeSeries.getColumns();
                        if(timeSeries.getColumns() > maxEpochSize) maxEpochSize = timeSeries.getColumns();
                    }
                    else
                    {
                        SSAMatrix nextX = new SSAMatrix(((MLDouble)mlc.get(i)).getArray());
                        if(!dataInCol)
                        {
                            nextX = nextX.transpose();
                        }

                        if(nextX.getRows() != dim)
                        {
                            appendToLog("Error: All samples in X must have the same dimension");
                            return;
                        }
                        timeSeries = SSAMatrix.concatHorizontally(timeSeries, nextX);
                        for(int j = 0; j < nextX.getColumns(); j++)
                        {
                            epList.add(i);
                        }
                        if(nextX.getColumns() < minEpochSize) minEpochSize = nextX.getColumns();
                        if(nextX.getColumns() > maxEpochSize) maxEpochSize = nextX.getColumns();
                    }
                }
                data.setTimeSeries(timeSeries, f);
                int epDef[] = new int[epList.size()];
                Iterator<Integer> it = epList.iterator();
                for(int j = 0; j < epDef.length; j++)
                {
                    epDef[j] = it.next();
                }
                try
                {
                    data.setCustomEpochDefinition(epDef, mlc.getSize(), minEpochSize, f);
                }
                catch(IllegalArgumentException e)
                {
                    appendToLog(e.getMessage());
                    appendToLog("Loaded data only from file " + f.getPath() + ":");
                    appendToLog("  dimensions=" + data.getNumberOfDimensions() + ",total number of samples="
                                + data.getTotalNumberOfSamples());
                    return;
                }
                appendToLog("Loaded data and epoch definition from file " + f.getPath() + ":");
                appendToLog("  dimensions=" + data.getNumberOfDimensions() + ",total number of samples="
                                + data.getTotalNumberOfSamples() + ",number of epochs=" + mlc.getSize()
                                + ",min. no. samples in epoch=" + minEpochSize
                                + ",max. no. samples in epoch=" + maxEpochSize);
                break;
            default:
                appendToLog("Error: Variable X is not a real-valued matrix or a cell array");
                return;
        }
        
        if(hasGUI())
        {
            gui.setGUIState(GUI.STATE_PARAMETRIZATION);
        }
    }

    /**
     * Saves a matrix to a CSV file.
     *
     * @param M matrix to save
     * @param f file to save to
     */
    private void saveCSV(SSAMatrix M, File f)
    {
        appendToLog("Saving...");

        PrintWriter pw;
        try
        {
            pw = new PrintWriter(new BufferedWriter(new FileWriter(f)));
        }
        catch(IOException e)
        {
            appendToLog("Error opening file: " + e);
            return;
        }

        for(int i = 0; i < M.getRows(); i++)
        {
            for(int j = 0; j < M.getColumns(); j++)
            {
                pw.print(M.get(i, j));
                if((j + 1) < M.getColumns())
                {
                    pw.print(",");
                }
            }
            pw.println();
        }

        pw.close();

        appendToLog("Saving successful.");
    }

    /**
     * Saves the stationary sources in a CSV-file.
     * 
     * @param f file to save to
     */
    public void saveStationarySourcesCSV(File f) {
        appendToLog("Extracting stationary signal...");
        SSAMatrix ss;
        if(data.getOutputDataformat() == Data.DATAFORMAT_CHANNELS_X_TIME)
        {
            ss = results.Ps.mmul(data.X);
        }
        else
        {
            ss = results.Ps.mmul(data.X).transpose();
        }
        saveCSV(ss, f);
    }

    /**
     * Saves the non-stationary sources in a CSV-file.
     *
     * @param f file to save to
     */
    public void saveNonstationarySourcesCSV(File f) {
        appendToLog("Extracting non-stationary signal...");
        SSAMatrix nss;
        if(data.getOutputDataformat() == Data.DATAFORMAT_CHANNELS_X_TIME)
        {
            nss = results.Pn.mmul(data.X);
        }
        else
        {
            nss = results.Pn.mmul(data.X).transpose();
        }
        saveCSV(nss, f);
    }

    /**
     * Saves a basis of the stationary subspace to a CSV-file.
     *
     * @param f file to save to
     */
    public void saveStationaryBasisCSV(File f) {
        saveCSV(results.Bs, f);
    }

   /**
     * Saves a basis of the non-stationary subspace to a CSV-file.
     *
     * @param f file to save to
     */
    public void saveNonstationaryBasisCSV(File f) {
        saveCSV(results.Bn, f);
    }

   /**
     * Saves the projection matrix for the stationary subspace.
     *
     * @param f file to save to
     */
    public void saveStationaryProjectionCSV(File f) {
        saveCSV(results.Ps, f);
    }

   /**
     * Saves the projection matrix for the non-stationary subspace.
     *
     * @param f file to save to
     */
    public void saveNonstationaryProjectionCSV(File f) {
        saveCSV(results.Pn, f);
    }

    /**
     * Save all results to CSV-files.
     * This method creates multiple CSV-files containing the results of SSA.
     * The filenames are stationary_sources.csv, nonstationary_sources.csv,
     * stationary_basis.csv, nonstationary_basis.csv, stationary_projection.csv,
     * nonstationary_projection.csv.
     *
     * @param f path to directory where the files should be saved
     */
    public void saveAllToCSV(File f) {
        final String absPath = f.getAbsolutePath() + File.separator;
        saveStationarySourcesCSV(new File(absPath + "stationary_sources.csv"));
        saveNonstationarySourcesCSV(new File(absPath + "nonstationary_sources.csv"));

        saveStationaryBasisCSV(new File(absPath + "stationary_basis.csv"));
        saveNonstationaryBasisCSV(new File(absPath + "nonstationary_basis.csv"));
            
        saveStationaryProjectionCSV(new File(absPath + "stationary_projection.csv"));
        saveNonstationaryProjectionCSV(new File(absPath + "nonstationary_projection.csv"));
    }

   /**
     * Saves all results to a MAT-file.
     *
     * @param f file to save to
     */
    public void saveResultMatlab(File f) {
        appendToLog("Saving results...");
        LinkedList<MLArray> list = new LinkedList<MLArray>();

        MLStructure mls = new MLStructure("ssa_results", new int[]{1,1});
        mls.setField("Ps", new MLDouble("Ps", results.Ps.getArray()));
        mls.setField("Pn", new MLDouble("Pn", results.Pn.getArray()));
        mls.setField("As", new MLDouble("As", results.Bs.getArray()));
        mls.setField("An", new MLDouble("An", results.Bn.getArray()));
        SSAMatrix ss = results.Ps.mmul(data.X);
        SSAMatrix nss = results.Pn.mmul(data.X);
        if(data.getOutputDataformat() == Data.DATAFORMAT_CHANNELS_X_TIME)
        {
            mls.setField("s_src", new MLDouble("s_src", ss.getArray()));
            mls.setField("n_src", new MLDouble("n_src", nss.getArray()));
        }
        else
        {
            mls.setField("s_src", new MLDouble("s_src", ss.transpose().getArray()));
            mls.setField("n_src", new MLDouble("n_src", nss.transpose().getArray()));
        }
        
        // parameter structure
        // TODO: more parameters (no equal epochs etc.)?
        MLStructure mlsparam = new MLStructure("ssa_results", new int[]{1,1});
        mlsparam.setField("input_file", new MLChar("input_file", results.inputFile));
        mlsparam.setField("epoch_file", new MLChar("epoch_file", results.epochFile));
        mlsparam.setField("no_s_src", new MLDouble("no_s_src", new double[][]{{results.d}}));
        mlsparam.setField("no_restarts", new MLDouble("no_restarts", new double[][]{{results.reps}}));
        mlsparam.setField("use_mean", new MLDouble("use_mean", new double[][]{{results.useMean ? 1 : 0}}));
        mlsparam.setField("use_covariance", new MLDouble("use_covariance", new double[][]{{results.useCovariance ? 1 : 0}}));
        mlsparam.setField("eq_epochs", new MLDouble("eq_epochs", new double[][]{{results.equalEpochs}}));

        mls.setField("parameters", mlsparam);
        
        mls.setField("loss_s", new MLDouble("loss_s", new double[][]{{results.loss_s}}));
        mls.setField("loss_n", new MLDouble("loss_n", new double[][]{{results.loss_n}}));
        mls.setField("iterations_s", new MLDouble("iterations_s", new double[][]{{results.iterations_s}}));
        mls.setField("iterations_n", new MLDouble("iterations_n", new double[][]{{results.iterations_n}}));

        mls.setField("description", new MLChar("description", "SSA results (" + new Date() + ")"));
        
        list.add(mls);

        MatFileWriter mfw;
        try
        {
            // check for *.mat extension
            String filename = f.getPath();
            if(!filename.toLowerCase().endsWith(".mat"))
            {
                f = new File(filename + ".mat");
            }
            mfw = new MatFileWriter(f, list);
        } 
        catch(IOException e)
        {
            appendToLog("Error saving results: " + e);
            return;
        }

        appendToLog("Results successfully saved.");
    }
    
    /**
     * Appends a message to the log.
     *
     * @param s message to append
     */
    public void appendToLog(String s)
    {
        if(logger != null)
        {
            logger.appendToLog(s);
        }
    }
    
    /**
     * Sets which logger to use.
     *
     * @param logger logger
     */
    public void setLogger(Logger logger)
    {
        this.logger = logger;
    }

    /**
     * Appends an error message to the logger, which explains how increase the Java heap space.
     */
    public void printJavaHeapSpaceError()
    {
        if(toolboxMode == TOOLBOX_MODE_STANDALONE)
        {
            appendToLog("");
            appendToLog("ERROR: Not enough Java heap space.");
            appendToLog("You can increase the Java heap space by running the SSA toolbox from the command line like this:");
            appendToLog("");
            appendToLog("  java -Xmx512M -jar ssa.jar");
            appendToLog("");
            appendToLog("This would result in a Java heap space of 512M. Of course you can replace \"512M\" with your desired size.");
        }
        else if(toolboxMode == TOOLBOX_MODE_MATLAB)
        {
            appendToLog("");
            appendToLog("ERROR: Not enough Java heap space.");
            appendToLog("To increase the Java heap space in Matlab, have a look at this website:");
            appendToLog("");
            appendToLog("http://www.mathworks.com/support/solutions/en/data/1-18I2C/");
            appendToLog("");
            appendToLog("In case you are using Matlab 2010a or later,");
            appendToLog("this can be easily done using Matlab''s preferences dialog.");
        }
    }
}

