package ssatoolbox;

import org.jblas.DoubleMatrix;

/**
 * This class stores the results of the SSA algorithm.
 *
 * @author Jan Saputra Mueller, saputra@cs.tu-berlin.de
 */
public class Results {
    // results
    public DoubleMatrix Ps, Pn;
    public DoubleMatrix Bs, Bn;
    public double loss;
    public int iterations;
    public boolean converged;

    // used parameters
    public int d; // number of stationary sources
    public int reps; // number of repetitions
    public boolean useMean;
    public boolean useCovariance;
    public int equalEpochs; // number of equal epochs; 0 in case of custom epochs
    public String inputFile; // file from which the data was loaded
    public String epochFile; // custom epochization file (if used)

    /**
     * Constructs a new Result object.
     *
     * @param Ps projection matrix of the stationary subspace
     * @param Pn projection matrix of the non-stationary subspace
     * @param Bs basis of the stationary subspace
     * @param Bn basis of the non-stationary subspace
     * @param loss value of the objective function
     * @param converged detects whether the backtracking linesearch converged
     * @param iterations number of iterations needed
     * @param d number of stationary sources
     * @param reps number of repetitions
     * @param ignoreMean ignore mean
     * @param equalEpochs use equally sized epochs
     * @param inputFile input file used
     * @param epochFile file with custom epochization
     */
    public Results( DoubleMatrix Ps,
                    DoubleMatrix Pn,
                    DoubleMatrix Bs,
                    DoubleMatrix Bn,
                    double loss,
                    boolean converged,
                    int iterations,
                    int d,
                    int reps,
                    boolean useMean,
                    boolean useCovariance,
                    int equalEpochs,
                    String inputFile,
                    String epochFile)
    {
        this.Ps = Ps;
        this.Pn = Pn;
        this.Bs = Bs;
        this.Bn = Bn;
        this.loss = loss;
        this.converged = converged;
        this.iterations = iterations;

        this.d = d;
        this.reps = reps;
        this.useMean = useMean;
        this.useCovariance = useCovariance;
        this.equalEpochs = equalEpochs;
        this.inputFile = inputFile;
        this.epochFile = epochFile;
    }
}
