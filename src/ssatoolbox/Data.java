package ssatoolbox;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeMap;
import org.jblas.DoubleMatrix;

/**
 * This class stores the data and custom epoch definitions and computes epoch-wise
 * means and covariance matrices, either according to a custom definition or based on a
 * specified number of equally sized epochs.
 * 
 * @author Jan Saputra Mueller, saputra@cs.tu-berlin.de
 */
public class Data {
    public static final int DATA_ORIGIN_CSV_TIMESERIES = 1;
    public static final int DATA_ORIGIN_MATLAB = 2;

    public static final int DATAFORMAT_TIME_X_CHANNELS = 1;
    public static final int DATAFORMAT_CHANNELS_X_TIME = 2;

    protected boolean useCustomEpochDefinition;

    protected File timeseriesFile = null;
    protected File epochDefinitionFile = null;

    protected int[] epochDefinition = null;
    protected int customEpochs = 0;
    // data for SSA algorithm
    protected DoubleMatrix X = null;
    protected DoubleMatrix S[]; // covariance matrices
    protected DoubleMatrix mu[]; // means
    protected DoubleMatrix W; // whitening matrix
    protected DoubleMatrix Sall; // covariance matrix over all epochs
    protected DoubleMatrix muall; // mean over all epochs

    protected int numberOfEqualSizeEpochs = -1;

    protected int inputDataformat = -1;
    protected int outputDataformat = -1;
   
    private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    public int getInputDataformat() {
        return inputDataformat;
    }

    public int getOutputDataformat() {
        return outputDataformat;
    }

    /**
     * Returns the file from which the timeseries was loaded.
     *
     * @return timeseries file
     */
    public File getTimeseriesFile() {
        return timeseriesFile;
    }

    /**
     * Returns the file from which the epoch definition was loaded.
     *
     * @return epoch definition file
     */
    public File getEpochDefinitionFile() {
        return epochDefinitionFile;
    }

    /**
     * Returns the number of samples.
     *
     * @return number of samples
     */
    public int getTotalNumberOfSamples() {
        if(X != null) return X.getColumns();
        else return 0;
    }

    /**
     * Returns the dimension of the dataset.
     *
     * @return dimension of dataset
     */
    public int getNumberOfDimensions()
    {
        if(X != null) return X.getRows();
        else return 0;
    }

    /**
     * Returns whether a custom epoch definition will be used.
     *
     * @return true if a custom epoch definition will be used, otherwise false
     */
    public boolean useCustomEpochDefinition() {
        return useCustomEpochDefinition;
    }

    /**
     * Returns the number of equal size epochs.
     *
     * @return number of equal size epochs
     */
    public int getNumberOfEqualSizeEpochs() {
        return numberOfEqualSizeEpochs;
    }

    /**
     * Returns the number of epochs.
     *
     * @return number of epcohs
     */
    public int getNumberOfEpochs() {
        if(useCustomEpochDefinition())
            return customEpochs;
        else
            return getNumberOfEqualSizeEpochs();
    }

    /**
     * Set the number of equal size epochs.
     *
     * @param numberOfEpochs number of equal size epochs
     */
    public void setNumberOfEqualSizeEpochs(int numberOfEpochs) {
        if(numberOfEpochs < 1 /*&& numberOfEpochs != -1*/) throw new IllegalArgumentException("Number of epochs must be positive");

        if(numberOfEpochs != this.numberOfEqualSizeEpochs) {
            if(numberOfEpochs > getTotalNumberOfSamples())
            {
                throw new IllegalArgumentException("Number of epochs must be smaller than the number of samples available");
            }

            if((getTotalNumberOfSamples() / numberOfEpochs) < getNumberOfDimensions())
            {
                throw new IllegalArgumentException("Number of samples per epoch must be at least the dimension of the dataset");
            }
            
            int oldval = this.numberOfEqualSizeEpochs;
            this.numberOfEqualSizeEpochs = numberOfEpochs;
            propertyChangeSupport.firePropertyChange("numberOfEqualSizeEpochs", oldval, this.numberOfEqualSizeEpochs);
        }
    }

    /**
     * Returns whether a custom epoch definition is available.
     *
     * @return true if a custom epoch definition is available, otherwise false
     */
    public boolean hasCustomEpochDefinition() {
        return (getEpochDefinitionFile() != null);
    }

    /**
     * Sets whether to use a custom epoch definition.
     *
     * @param useCustomEpochDefinition set this to true if you want to use a custom epoch definition.
     */
    public void setUseCustomEpochDefinition(boolean useCustomEpochDefinition) {
        if(!hasCustomEpochDefinition() && useCustomEpochDefinition == true) {
            throw new RuntimeException("No custom epoch definition loaded");
        }

        if(useCustomEpochDefinition != this.useCustomEpochDefinition) {
            boolean oldval = this.useCustomEpochDefinition;
            this.useCustomEpochDefinition = useCustomEpochDefinition;
            propertyChangeSupport.firePropertyChange("useCustomEpochDefinition", oldval, this.useCustomEpochDefinition);
        }
    }

    public void setInputDataformat(int newformat) {
        if(newformat != inputDataformat) {
            int oldformat = inputDataformat;
            inputDataformat = newformat;
            propertyChangeSupport.firePropertyChange("inputDataformat", oldformat, newformat);
        }
    }

    public void setOutputDataformat(int newformat) {
        if(newformat != outputDataformat) {
            int oldformat = outputDataformat;
            outputDataformat = newformat;
            propertyChangeSupport.firePropertyChange("outputDataformat", oldformat, newformat);
        }
    }


    /**
     * Sets a custom epoch definition.
     *
     * @param epDef epoch definition containing one epoch belonging per index
     * @param epochs number of epochs in the epoch definition (calculated by the caller)
     * @param minEpochSize smallest number of samples in one epoch (calculated by the caller)
     * @param file file from which the epoch definition was loaded
     */
    public void setCustomEpochDefinition(int[] epDef, int epochs, int minEpochSize, File file)
    {
        if(epochs > getTotalNumberOfSamples())
        {
            throw new IllegalArgumentException("Number of epochs must be smaller than the number of samples available");
        }
        if(minEpochSize < getNumberOfDimensions())
        {
            throw new IllegalArgumentException("Number of samples per epoch must be at least the dimension of the dataset");
        }

        boolean oldHasCustomEpochDef = hasCustomEpochDefinition();
        File oldEpochDefinitionFile = epochDefinitionFile;
        epochDefinition = epDef;
        customEpochs = epochs;
        epochDefinitionFile = file;
        propertyChangeSupport.firePropertyChange("hasCustomEpochDefinition", oldHasCustomEpochDef, true);
        propertyChangeSupport.firePropertyChange("epochDefinitionFile", oldEpochDefinitionFile, file);
    }

    /**
     * Sets a timeseries.
     *
     * @param X matrix with samples in the columns
     * @param file file from which the timeseries was loaded
     */
    public void setTimeSeries(DoubleMatrix X, File file)
    {
        int oldDim = getNumberOfDimensions();
        int oldSamples = getTotalNumberOfSamples();
        File oldFile = getTimeseriesFile();
        timeseriesFile = file;       
        this.X = X;

        // delete epoch definition
        setUseCustomEpochDefinition(false);
        boolean oldHasCustomEpochDef = hasCustomEpochDefinition();
        epochDefinition = null;
        epochDefinitionFile = null;

        propertyChangeSupport.firePropertyChange("numberOfDimensions", oldDim, getNumberOfDimensions());
        propertyChangeSupport.firePropertyChange("totalNumberOfSamples", oldSamples, getTotalNumberOfSamples());
        propertyChangeSupport.firePropertyChange("timeseriesFile", oldFile, file);
        propertyChangeSupport.firePropertyChange("hasCustomEpochDefinition", oldHasCustomEpochDef, false);
    }

    /**
     * Add PropertyChangeListener.
     *
     * @param listener
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    /**
     * Remove PropertyChangeListener.
     *
     * @param listener
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    /**
     * Calculates the covariance matrices and means for each epoch.
     */
    public void epochize()
    {
        if(useCustomEpochDefinition())
        {
            epochizeCustom(epochDefinition);
        }
        else
        {
            epochizeEqually(getNumberOfEqualSizeEpochs());
        }
    }

    /**
     * Calculates the covariance matrices and means for each epoch.
     *
     * @param epochs number of epochs
     */
    private void epochizeEqually(int epochs)
    {
        if(X != null)
        {
            DoubleMatrix S[] = new DoubleMatrix[epochs];
            DoubleMatrix mu[] = new DoubleMatrix[epochs];
            int epochSize = X.getColumns() / epochs;

            for(int i = 0; i < epochs; i++)
            {
                DoubleMatrix epoch = X.getRange(0, X.getRows(), i*epochSize, (i+1)*epochSize);
                mu[i] = MathFunctions.mean(epoch);
                S[i] = MathFunctions.cov(epoch, mu[i]);
            }

            initializeSSA(S, mu);
        }
    }

    private void epochizeCustom(int epDef[])
    {
        TreeMap<Integer, LinkedList<Integer>> map = new TreeMap<Integer, LinkedList<Integer>>();
        for(int i = 0; i < epDef.length; i++)
        {
            LinkedList<Integer> l = map.get(epDef[i]);
            if(l == null)
            {
                l = new LinkedList<Integer>();
                l.add(i);
                map.put(epDef[i], l);
            }
            else
            {
                l.add(i);
            }
        }

        mu = new DoubleMatrix[map.size()];
        S = new DoubleMatrix[map.size()];

        Iterator<LinkedList<Integer>> it = map.values().iterator();
        int i = 0;
        while(it.hasNext())
        {
            LinkedList<Integer> ep = it.next();
            DoubleMatrix samples = X.getColumns(toIntArray(ep));
            mu[i] = MathFunctions.mean(samples);
            S[i] = MathFunctions.cov(samples, mu[i]);
            i++;
        }

        initializeSSA(S, mu);
    }

    private int[] toIntArray(LinkedList<Integer> l)
    {
        int a[] = new int[l.size()];
        Iterator<Integer> it = l.iterator();
        for(int i = 0; i < a.length; i++)
        {
            a[i] = it.next();
        }

        return a;
    }

    /**
     * Initialization for SSA.
     *
     * @param S array of covariance matrices over all epochs
     * @param mu array of means over all epochs
     */
    private void initializeSSA(DoubleMatrix S[], DoubleMatrix mu[])
    {
        // calculate covariance matrix and mean over all epochs
        Sall = DoubleMatrix.zeros(S[0].getRows(), S[0].getColumns());
        muall = DoubleMatrix.zeros(mu[0].getRows());
        for(int i = 0; i < S.length; i++)
        {
            Sall.addi(S[i]);
            muall.addi(mu[i]);
        }
        Sall.divi(S.length);
        muall.divi(mu.length);
        // calculate whitening matrix
        W = MathFunctions.whitening(Sall);

        this.S = S;
        this.mu = mu;
    }
}
