package ssatoolbox;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * This class stores the parameters for the SSA algorithm. Its attributes
 * are accessed and set by the view (in response to user input) and by the
 * controller, if certain parameter combinations are invalid.
 *
 * @author Jan Saputra Mueller, saputra@cs.tu-berlin.de
 */
public class SSAParameters {

    protected int numberOfStationarySources = -1;

    /**
     * Returns the number of stationary sources.
     *
     * @return number of stationary sources
     */
    public int getNumberOfStationarySources() {
        return numberOfStationarySources;
    }

    /**
     * Set the number of stationary sources.
     *
     * @param numberOfStationarySources number of stationary sources
     */
    public void setNumberOfStationarySources(int numberOfStationarySources) {
        if(numberOfStationarySources != -1 && numberOfStationarySources < 1)
                throw new IllegalArgumentException("Number of epochs must be positive");

        if(numberOfStationarySources != this.numberOfStationarySources) {
            int oldval = this.numberOfStationarySources;
            this.numberOfStationarySources = numberOfStationarySources;
            propertyChangeSupport.firePropertyChange("numberOfStationarySources", oldval, this.numberOfStationarySources);
        }
    }

    private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

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

    protected int numberOfRestarts = 5;
    /**
     * Get the value of numberOfRestarts
     *
     * @return the value of numberOfRestarts
     */
    public int getNumberOfRestarts() {
        return numberOfRestarts;
    }

    /**
     * Sets the number of restarts
     *
     * @param numberOfRestarts number of restarts
     */
    public void setNumberOfRestarts(int numberOfRestarts) {
        if(numberOfRestarts < 1) throw new IllegalArgumentException("Number of restarts must be positive");

        if(numberOfRestarts != this.numberOfRestarts) {
            int oldval = this.numberOfRestarts;
            this.numberOfRestarts = numberOfRestarts;
            propertyChangeSupport.firePropertyChange("numberofRestarts", oldval, numberOfRestarts);
        }
    }
    //protected boolean ignoreChangeInMeans = false;
    protected boolean useMean = true;
    protected boolean useCovariance = true;

    /**
     * Returns whether the means should be used.
     * 
     * @return true if means should be used
     */
    public boolean isUseMean()
    {
        return useMean;
    }

    /**
     * Sets whether to use the means.
     *
     * @param useMean set this to true if the means should be used
     */
    public void setUseMean(boolean useMean) {
        if(useMean != this.useMean) {
            boolean oldval = this.useMean;
            this.useMean = useMean;
            propertyChangeSupport.firePropertyChange("useMean", oldval, useMean);
        }
    }

    /**
     * Returns whether the covariance matrices should be used.
     *
     * @return true if covariance matrices should be used
     */
    public boolean isUseCovariance()
    {
        return useCovariance;
    }

    /**
     * Sets whether to use the coavriance matrices.
     *
     * @param useCovariance set this to true if the covariance matrices should be used
     */
    public void setUseCovariance(boolean useCovariance)
    {
        if(useCovariance != this.useCovariance)
        {
            boolean oldval = this.useCovariance;
            this.useCovariance = useCovariance;
            propertyChangeSupport.firePropertyChange("useCovariance", oldval, useCovariance);
        }
    }

    protected boolean NSA = false;
    public boolean isNSA()
    {
        return NSA;
    }

    public void setNSA(boolean NSA)
    {
        if(NSA != this.NSA)
        {
            boolean oldval = this.NSA;
            this.NSA = NSA;
            propertyChangeSupport.firePropertyChange("NSA", oldval, NSA);
        }
    }
}
