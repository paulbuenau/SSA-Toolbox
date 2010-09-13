package ssatoolbox;

import com.jmatio.io.*;
import com.jmatio.types.*;
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
import org.jblas.DoubleMatrix;

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
    private SSA ssa = new SSA();
    private SSAParameters ssa_parameters = new SSAParameters();
    private Results results = null;
    private Data data = new Data();
    private GUI gui = null;
    private Logger logger = null;
    protected ToolboxConfig toolboxConfig = new ToolboxConfig();
    //private boolean stop_ssa = false;

    public static final String SSA_VERSION = "0.1";

    /**
     * Constructor for class Main.
     */
    public Main() {
        data.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent arg0) {                
                if(arg0.getPropertyName().equals("numberOfEqualSizeEpochs")) {
                    if(data.getNumberOfEqualSizeEpochs() != -1) {
                        if(data.getNumberOfEqualSizeEpochs() < 2) {
                            logger.appendToLog("ERROR: Number of epochs < 2");
                            data.setNumberOfEqualSizeEpochs( ((Integer)arg0.getOldValue()).intValue() );
                        } else {
                            checkDeterminacy();
                        }
                    }
                    return;
                } 
            }
        });

        ssa_parameters.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent arg0) {
                if(arg0.getPropertyName().equals("numberOfStationarySources")) {
                    if(ssa_parameters.getNumberOfStationarySources() != -1) {
                        if(ssa_parameters.getNumberOfStationarySources() >= data.getNumberOfDimensions()) {
                            logger.appendToLog("ERROR: Number of stationary sources >= number of input dimensions");
                            ssa_parameters.setNumberOfStationarySources(((Integer)arg0.getOldValue()).intValue());
                        } else {
                            checkDeterminacy();
                        }
                    }
                    return;
                }

                if(arg0.getPropertyName().equals("ignoreChangeInMeans")) {                    
                    checkDeterminacy();
                    return;
                }
            }
        });

        
         UIManager.LookAndFeelInfo lif [] =  UIManager.getInstalledLookAndFeels();

/*         GUI_1 gui = new GUI_1();

        for(int i = 0; i < lif.length; i++) {
            if(lif[i].getClassName().equals(UIManager.getSystemLookAndFeelClassName())) {
                try {
                    UIManager.setLookAndFeel(lif[i].getClassName());
                    SwingUtilities.updateComponentTreeUI(gui);                    
                } catch(Exception Ex) {}
            }
        }


        gui.setVisible(true);*/

        gui = new GUI(this, ssa_parameters, data);
        logger = gui;
        gui.setGUIState(GUI.STATE_NO_DATA);
        gui.showGUI();

        System.out.println(gui.getPreferredSize());   

//        gui.setSystemLookAndFeel();
        logger.appendToLog("*** Welcome to the SSA Toolbox (version " + SSA_VERSION + ") ***");
        logger.appendToLog("Now you might want to load some data (see menu File).");
    }

    /**
     * Checks whether determinacy conditions are violated.
     */
    private void checkDeterminacy() {
        if(data.getNumberOfEpochs() >= 2 && ssa_parameters.getNumberOfStationarySources() >= 1) {
            if(!ssa_parameters.isUseMean()) {
                if(data.getNumberOfEpochs() <= (data.getNumberOfDimensions() - ssa_parameters.getNumberOfStationarySources()) + 1) {
                    //TODO more details.
                    logger.appendToLog("WARNING: determinacy condition violated");
                }
            } else {
                if(data.getNumberOfEpochs() <=  (data.getNumberOfDimensions() - ssa_parameters.getNumberOfStationarySources())/2 + 2) {
                    logger.appendToLog("WARNING: determinacy condition violated");
                    //TODO more details.
                }
            }
        }
    }

    /**
     * Main function.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        new Main();
    }

    /**
     * Starts the SSA algorithm in a seperate thread.
     */
    public void runSSA() {
        if(ssa_parameters.getNumberOfStationarySources() == -1) {
            logger.appendToLog("ERROR: Number of stationary sources not specified");
            return;
        }
            
        if(data.getNumberOfEpochs() == -1) {
            logger.appendToLog("ERROR: Epochs not specified");
            return;
        }

        (new Thread() {
            @Override
            public void run() {
                logger.appendToLog("Calculating covariance matrices and means...");
                data.epochize();

                logger.appendToLog("Running SSA...");
                gui.setGUIState(GUI.STATE_SSA_RUNNING);

                //stop_ssa = false;

                ssa.setLogger(gui);
                results = ssa.optimize(ssa_parameters, data);

                logger.appendToLog("SSA terminated.");
                logger.appendToLog("Best result:");
                logger.appendToLog("  Ps: " + results.Ps);
                logger.appendToLog("  Pn: " + results.Pn);
                logger.appendToLog("  As: " + results.Bs);
                logger.appendToLog("  An: " + results.Bn);
                logger.appendToLog("  Loss: " + results.loss);
                logger.appendToLog("  Converged: " + results.converged);
                gui.setGUIState(GUI.STATE_RESULT_AVAILABLE);
            }
        }).start();
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

    public void loadTimeseries(File f)
    {
        String filename = f.getPath().toLowerCase();
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
            logger.appendToLog("Error: Unknown file extension.");
        }
    }

    /**
     * Loads a timeseries from a CSV-file.
     * The file has to contain one sample per row.
     *
     * @param f CSV-file
     */
    public void loadTimeseriesCSV(File f) {
        logger.appendToLog("Loading data ...");

        // try to open csv-file
        BufferedReader br = null;
        try
        {
            br = new BufferedReader(new FileReader(f));
        }
        catch(IOException e)
        {
            logger.appendToLog("Error opening file: " + e);
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
            logger.appendToLog("Error reading file: " + e);
        }
        catch(NumberFormatException e)
        {
            logger.appendToLog("Error converting string to number: " + e);
        }

        double dataArray[][] = new double[lineList.size()][lineList.getFirst().length];
        for(int i = 0; i < dataArray.length; i++)
        {
            dataArray[i] = lineList.get(i);
        }

        if(dataArray[0].length > dataArray.length)
        {
            // more columns than rows => samples are in the columns
            DoubleMatrix timeSeries = new DoubleMatrix(dataArray);
            data.setTimeSeries(timeSeries, f);
            data.setInputDataformat(Data.DATAFORMAT_CHANNELS_X_TIME);
            data.setOutputDataformat(Data.DATAFORMAT_CHANNELS_X_TIME);
        }
        else
        {
            // more rows than columns => samples are in the rows
            DoubleMatrix timeSeries = new DoubleMatrix(dataArray).transpose();
            data.setTimeSeries(timeSeries, f);
            data.setInputDataformat(Data.DATAFORMAT_TIME_X_CHANNELS);
            data.setOutputDataformat(Data.DATAFORMAT_TIME_X_CHANNELS);
        }

        logger.appendToLog("Loaded data from file " + f.getPath() + ":");
        logger.appendToLog("  dimensions=" + data.getNumberOfDimensions() + ",total number of samples=" + data.getTotalNumberOfSamples());
        gui.setGUIState(GUI.STATE_PARAMETRIZATION);
    }

    /**
     * Loads an epoch definition from a CSV-file.
     * For each loaded sample this file has to contain one row with the epoch
     * number to which the sample belongs to.
     *
     * @param f CSV epoch definition file
     */
    public void loadEpochDefinitionCSV(File f) {
        logger.appendToLog("Loading epoch definition file ...");

        // try to open csv-file
        BufferedReader br = null;
        try
        {
            br = new BufferedReader(new FileReader(f));
        }
        catch(IOException e)
        {
            logger.appendToLog("Error opening file: " + e);
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
                    logger.appendToLog("Error: File contains more epoch belongings than available samples");
                    return;
                }

                epDef[i] = (int)Double.valueOf(tLine).doubleValue();
                i++;
            }
            br.close();
            if(i < data.getTotalNumberOfSamples())
            {
                logger.appendToLog("Error: File contains less epoch belongings than available samples");
                return;
            }
        }
        catch(IOException e)
        {
            logger.appendToLog("Error reading file: " + e);
        }
        catch(NumberFormatException e)
        {
            logger.appendToLog("Error converting string to number: " + e);
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
            logger.appendToLog(e.getMessage());
            return;
        }
        
        logger.appendToLog("Loaded epoch definition from file " + f.getPath() + ":");
        logger.appendToLog("  number of epochs=" + count.keySet().size()
                         + ",min. no. samples in epoch=" + minEpochSize
                         + ",max. no. samples in epoch=" + maxEpochSize);
    }

    /**
     * Loads data (and an epoch definition if available) from a MAT-file
     *
     * @param f MAT-file
     */
    public void loadDataMatlab(File f) {
        logger.appendToLog("Loading data from Matlab file ...");

        MatFileReader mfr;
        try
        {
            mfr = new MatFileReader(f);
        }
        catch (IOException e)
        {
            logger.appendToLog("Error opening file: " + e);
            return;
        }

        Map<String, MLArray> map = mfr.getContent();
        MLArray Xmat = map.get("X");
        if(Xmat == null)
        {
            logger.appendToLog("Error: No timeseries found in file (i.e. no variable X)");
            return;
        }

        switch(Xmat.getType())
        {
            case MLArray.mxDOUBLE_CLASS:
                DoubleMatrix dm = new DoubleMatrix(((MLDouble)Xmat).getArray());
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
                logger.appendToLog("Loaded data from file " + f.getPath() + ":");
                logger.appendToLog("  dimensions=" + data.getNumberOfDimensions() + ",total number of samples=" + data.getTotalNumberOfSamples());
                break;
            case MLArray.mxCELL_CLASS:
                MLCell mlc = (MLCell)Xmat;
                DoubleMatrix timeSeries = null;
                int dim = 0;
                LinkedList<Integer> epList = new LinkedList<Integer>();
                int minEpochSize = Integer.MAX_VALUE;
                int maxEpochSize = 0;
                boolean dataInCol = true;
                for(int i = 0; i < mlc.getSize(); i++)
                {
                    if(mlc.get(i).getType() != MLArray.mxDOUBLE_CLASS)
                    {
                        logger.appendToLog("Error: Cell array X has to contain only real-valued matrices");
                        return;
                    }
                    if(i == 0)
                    {
                        timeSeries = new DoubleMatrix(((MLDouble)mlc.get(0)).getArray());
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
                        DoubleMatrix nextX = new DoubleMatrix(((MLDouble)mlc.get(i)).getArray());
                        if(!dataInCol)
                        {
                            nextX = nextX.transpose();
                        }

                        if(nextX.getRows() != dim)
                        {
                            logger.appendToLog("Error: All samples in X must have the same dimension");
                            return;
                        }
                        timeSeries = DoubleMatrix.concatHorizontally(timeSeries, nextX);
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
                    logger.appendToLog(e.getMessage());
                    logger.appendToLog("Loaded data only from file " + f.getPath() + ":");
                    logger.appendToLog("  dimensions=" + data.getNumberOfDimensions() + ",total number of samples="
                                + data.getTotalNumberOfSamples());
                    return;
                }
                logger.appendToLog("Loaded data and epoch definition from file " + f.getPath() + ":");
                logger.appendToLog("  dimensions=" + data.getNumberOfDimensions() + ",total number of samples="
                                + data.getTotalNumberOfSamples() + ",number of epochs=" + mlc.getSize()
                                + ",min. no. samples in epoch=" + minEpochSize
                                + ",max. no. samples in epoch=" + maxEpochSize);
                break;
            default:
                logger.appendToLog("Error: Variable X is not a real-valued matrix or a cell array");
                return;
        }

        gui.setGUIState(GUI.STATE_PARAMETRIZATION);
    }

    /**
     * Saves a matrix to a CSV file.
     *
     * @param M matrix to save
     * @param f file to save to
     */
    private void saveCSV(DoubleMatrix M, File f)
    {
        logger.appendToLog("Saving...");

        PrintWriter pw;
        try
        {
            pw = new PrintWriter(new BufferedWriter(new FileWriter(f)));
        }
        catch(IOException e)
        {
            logger.appendToLog("Error opening file: " + e);
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

        logger.appendToLog("Saving successful.");
    }

    /**
     * Saves the stationary sources in a CSV-file.
     * 
     * @param f file to save to
     */
    public void saveStationarySourcesCSV(File f) {
        logger.appendToLog("Extracting stationary signal...");
        DoubleMatrix ss;
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
        logger.appendToLog("Extracting non-stationary signal...");
        DoubleMatrix nss;
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
     * Saves all results to a MAT-file.
     *
     * @param f file to save to
     */
    public void saveResultMatlab(File f) {
        logger.appendToLog("Saving results...");
        LinkedList<MLArray> list = new LinkedList<MLArray>();

        MLStructure mls = new MLStructure("ssa_results", new int[]{1,1});
        mls.setField("est_Ps", new MLDouble("est_Ps", results.Ps.toArray2()));
        mls.setField("est_Pn", new MLDouble("est_Pn", results.Pn.toArray2()));
        mls.setField("est_As", new MLDouble("est_As", results.Bs.toArray2()));
        mls.setField("est_An", new MLDouble("est_An", results.Bn.toArray2()));
        DoubleMatrix ss = results.Ps.mmul(data.X);
        DoubleMatrix nss = results.Pn.mmul(data.X);
        if(data.getOutputDataformat() == Data.DATAFORMAT_CHANNELS_X_TIME)
        {
            mls.setField("est_s_src", new MLDouble("est_s_src", ss.toArray2()));
            mls.setField("est_n_src", new MLDouble("est_n_src", nss.toArray2()));
        }
        else
        {
            mls.setField("est_s_src", new MLDouble("est_s_src", ss.transpose().toArray2()));
            mls.setField("est_n_src", new MLDouble("est_n_src", nss.transpose().toArray2()));
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
            logger.appendToLog("Error saving results: " + e);
            return;
        }

        logger.appendToLog("Results successfully saved.");
    }    
}
