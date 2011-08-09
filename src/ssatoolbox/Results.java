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

/**
 * This class stores the results of the SSA algorithm.
 *
 * @author Jan Saputra Mueller, saputra@cs.tu-berlin.de
 */
public class Results {
    /** Projection matrix to estimated stationary subspace */
    public SSAMatrix Ps;
    
    /** Projection matrix to estimated non-stationary subspace */
    public SSAMatrix Pn;

    /** Matrix whose columns span the estimated stationary subspace */
    public SSAMatrix Bs;

    /** Matrix whose columns span the estimated non-stationary subspace */
    public SSAMatrix Bn;

    /** Loss of the found solution */
    public double loss;

    /** Loss of the found solution for the s-source optimization */
    public double loss_s = 0;

    /** Loff of the found solution for the n-source optimization */
    public double loss_n = 0;

    /** Number of iterations needed */
    public int iterations;

    /** Number of iterations needed for the s-source optimization */
    public int iterations_s;

    /** Number of iterations needed for the n-source optimization */
    public int iterations_n;

    /** True, if the SSA algorithm converged */
    public boolean converged;

    // used parameters
    /** Number of stationary sources which has been used */
    public int d;

    /** Number of repetitions which has been used */
    public int reps;

    /** Indicates, whether changes in the mean were considered */
    public boolean useMean;

    /** Indicates, whether changes in the covariance were considered */
    public boolean useCovariance;

    /** Number of equally sized epochs, or zero if a custom epochization has been used */
    public int equalEpochs;

    /** File, from which the time series has been loaded (if avavilable) */
    public String inputFile;

    /** File, from which the custom epochization has been loaded (if available) */
    public String epochFile;

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
     * @param useMean use mean
     * @param useCovariance use covariance matrix
     * @param equalEpochs use equally sized epochs
     * @param inputFile input file used
     * @param epochFile file with custom epochization
     */
    public Results( SSAMatrix Ps,
                    SSAMatrix Pn,
                    SSAMatrix Bs,
                    SSAMatrix Bn,
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

