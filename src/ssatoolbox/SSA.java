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
 * Implementation of the "Stationary Subspace Analysis" (SSA) algorithm.
 *
 * @author Jan Saputra Mueller, saputra@cs.tu-berlin.de
 */
public class SSA
{
    // buffer for rotated covariance matrices and means
    private SSAMatrix Snew[];
    private SSAMatrix munew[];

    // constants for line-search
    private static final double LSALPHA = 0.5*(0.01+0.3);
    private static final double LSBETA = 0.4;
    private static final double RDEC_THRESHOLD = 1e-8;

    // handle to logger
    private Logger logger = null;
    
    private volatile boolean stopped = false;

    /**
     * Creates a new instance of the class SSA.
     */
    public SSA() { }

    /**
     * Checks whether the parameters for SSA are valid, and throws an exception if they are not.
     *
     * @param par class containing the SSA parameters
     * @param data class containing the data
     */
    public void checkParameters(SSAParameters par, Data data)
    {
        if(par.isUseCovariance())
        {
            // if covariances are used, the number of samples per epoch has to be at least the dimension of the dataset
            if((data.getTotalNumberOfSamples() / data.getNumberOfEpochs()) < data.getNumberOfDimensions())
            {
                throw new IllegalArgumentException("Number of samples per epoch must be at least the dimension of the dataset");
            }
        }

        if(par.isUseMean() && par.isUseCovariance())
        {
            // mean + covariance
            if(data.getNumberOfEpochs() <=  (data.getNumberOfDimensions() - par.getNumberOfStationarySources())/2 + 2)
            {
                if(!par.isIgnoreDeterminacy())
                {
                    throw new IllegalArgumentException("Too few epochs specified; there may be spurious stationary directions.\nYou need to have at least "
                                                 +  ((data.getNumberOfDimensions() - par.getNumberOfStationarySources())/2 + 3) 
                                                 + " distinct epochs to guarantee determinacy of the solution.");
                }
            }
        }
        else if( (par.isUseMean() && !par.isUseCovariance()) || (!par.isUseMean() && par.isUseCovariance()) )
        {
            // mean only or covariance only
            if(data.getNumberOfEpochs() <= (data.getNumberOfDimensions() - par.getNumberOfStationarySources()) + 1)
            {
                if(!par.isIgnoreDeterminacy())
                {
                    throw new IllegalArgumentException("Too few epochs specified; there may be spurious stationary directions.\nYou need to have at least "
                                                 +  ((data.getNumberOfDimensions() - par.getNumberOfStationarySources()) + 2)
                                                 + " distinct epochs to guarantee determinacy of the solution.");
                }
            }
        }
        else
        {
            throw new IllegalArgumentException("At least one of the options 'use mean' or 'use covariance matrix' has to be selected.");
        }
    }

    /**
     * Solves the SSA optimization problem using backtracking linesearch once.
     *
     * @param par class containing the SSA parameters
     * @param data class containing the data
     * @param optNSources optimize the n-sources instead of the s-sources
     * @param init initialization rotation matrix for the gradient descent *in whitening coordinates*! (Should be normally null!)
     * @return Results object
     */
    public Results optimizeOnce(SSAParameters par, Data data, boolean optNSources, SSAMatrix init)
    {
        SSAMatrix S[] = new SSAMatrix[data.S.length];
        SSAMatrix mu[] = null;
        if(par.isUseMean())
	        mu = new SSAMatrix[data.mu.length];

        int n = data.getNumberOfDimensions();
        int d = optNSources ? (n - par.getNumberOfStationarySources()) : par.getNumberOfStationarySources();

        SSAMatrix B;
        if(init == null)
        {
            // start with whitening + random rotation
            B = MathFunctions.randRot(n).mmuli(data.W);
        }
        else
        {
            // start with given initialization matrix
            B = init;
        }

        // apply initialization matrix to covariance matrices and means
        for(int i = 0; i < data.S.length; i++)
        {
            S[i] = B.mmul(data.S[i]).mmuli(B.transpose());
            if(par.isUseMean())
	            mu[i] = B.mmul(data.mu[i].sub(data.muall));
        }

        int k; // degrees of freedom of chi^2 distribution
        if(par.isUseMean() && par.isUseCovariance())
        {
            // use both means and covariances
            k = (S.length*d*(d+3)) / 2;
        }
        else if(par.isUseCovariance())
        {
            // use only covariances
            k = (S.length*d*(d+1)) / 2;
        }
        else
        {
            // use only means
            k = S.length*d;
        }
        // Optimization loop
        SSAMatrix grad, gradOld = null;
        SSAMatrix alpha, alphaOld = null;
        double loss = 0, lossNew = 0;
        boolean converged = false;
        int i;
        for(i = 0; i < Integer.MAX_VALUE; i++)
        {
            // get current objective function value and gradient
            SSAMatrix ret[] = objectiveFunction(    n, d,
                                                    S, mu, data.epochSizes, null, true,
                                                    par.isUseMean());
            loss = normalizeObjectiveFunction(ret[0].get(0, 0), k);
            //loss = ret[0].get(0, 0);
            grad = normalizeGradient(ret[1], ret[0].get(0, 0));

            // optimize n-sources?
            if(optNSources)
            {
                // simply change sign of loss and gradient
                loss = -loss;
                grad.muli(-1);
            }

            // conjugate gradient
            if(i == 0)
            {
                alpha = grad.mul(-1);
            }
            else
            {
                double gamma = grad.mul(grad.sub(gradOld)).sum()/gradOld.mul(gradOld).sum();
                alpha = grad.mul(-1).addi(alphaOld.mul(gamma));
            }
            gradOld = grad;
            alphaOld = alpha;

            // normalize search direction
            SSAMatrix search = alpha.div(Math.sqrt(alpha.mul(alpha).sum() * 2));
            //SSAMatrix search = alpha.div(alpha.norm2());

            // backtracking line search
            double t = 1;
            for(int j = 0; j < 10; j++, t *= LSBETA)
            {
                SSAMatrix M = search.mul(t);
                ret = objectiveFunction(n, d,
                                        S, mu, data.epochSizes, M, false,
                                        par.isUseMean());
                lossNew = normalizeObjectiveFunction(ret[0].get(0, 0), k);
                //lossNew = ret[0].get(0, 0);
                if(optNSources)
                {
                    lossNew = -lossNew;
                }

                // function decrease sufficient?
                if(lossNew <= (loss + LSALPHA*t*(0.5*grad.mul(search).sum())))
                {
                    break;
                }
            }

            // stop if line search failed
            if(lossNew >= loss)
            {
                converged = true;
                break;
            }

            // stop if relative function decrease is below threshold
            double relDecrease = Math.abs((loss - lossNew)/loss);
            if(relDecrease < RDEC_THRESHOLD)
            {
                converged = true;
                break;
            }

            // rotated S and mu
            S = Snew;
            mu = munew;

            // update demixing matrix
            B = ret[2].mmul(B);
        }

        // projection matrix for stationary subspace
        SSAMatrix Ps = B.getRange(0, d, 0, n);
        // projection matrix for non-stationary subspace
        SSAMatrix Pn = B.getRange(d, n, 0, n);
        // mixing matrix is the inverse of B
        SSAMatrix Mix = SSAMatrix.solve(B, SSAMatrix.eye(n));
        // basis for stationary subspace
        SSAMatrix Bs = Mix.getRange(0, n, 0, d);
        // basis for non-stationary subspace
        SSAMatrix Bn = Mix.getRange(0, n, d, n);

        // do optimization of n-sources instead of s-sources?
        if(optNSources)
        {
            // exchange stationary <-> non-stationary
            SSAMatrix buf = Ps;
            Ps = Pn;
            Pn = buf;
            buf = Bs;
            Bs = Bn;
            Bn = buf;
        }

        //return new SSAMatrix[]{Ps, Pn, Mix, new SSAMatrix(new double[]{matLoss}), new SSAMatrix(new double[]{converged})};

        return new Results(Ps, Pn, Bs, Bn, Math.min(loss, lossNew), converged, i,
                          par.getNumberOfStationarySources(),
                          par.getNumberOfRestarts(),
                          par.isUseMean(),
                          par.isUseCovariance(),
                          data.getEpochType() == Data.EPOCHS_CUSTOM ? 0 : data.getNumberOfEqualSizeEpochs(),
                          data.getTimeseriesFile() == null ? "" : data.getTimeseriesFile().toString(),
                          data.getEpochDefinitionFile() == null ? "" : data.getEpochDefinitionFile().toString());
    }

    /**
     * Solves the SSA optimization problem using backtracking linesearch.
     * The best result is returned.
     *
     * @param par class containing the SSA parameters
     * @param data class containing the data
     * @return Results object
     */
    public Results optimize(SSAParameters par, Data data)
    {
        appendToLog(""); // empty line

        if(data.getEpochType() == Data.EPOCHS_EQUALLY_HEURISTIC)
        {
            data.setNumberOfEpochsByHeuristic(par.getNumberOfStationarySources(), par.isUseMean(), par.isUseCovariance());
        }

        checkParameters(par, data);
		
		if(data.getEpochType() != data.EPOCHS_SPECIFIED_MOMENTS)
		{
           appendToLog("Calculating covariance matrices and means...");
		}
        data.epochize(par.isUseCovariance());

        appendToLog("Running SSA...");

        if(par.isUseCovariance())
        {
            // optimization by gradient descent

            stopped = false;

            // optimization of the s-sources
            Results optSSrc = new Results(null, null, null, null, Double.POSITIVE_INFINITY, false, 0, 0, 0, false, false, 0, null, null);
            appendToLog("Optimizing the stationarity of the s-sources...");
            for(int i = 0; i < par.getNumberOfRestarts(); i++)
            {
                Results buf = optimizeOnce(par, data, false, null);
                if(buf.loss < optSSrc.loss)
                {
                    optSSrc = buf;
                }
                if(logger != null)
                {
                    // show progress
                    appendToLog("Repetition " + (i+1) + ": iterations=" + buf.iterations + ", min. objective function value=" + buf.loss);
                }
                if(stopped)
                {
                    return optSSrc;
                }
            }

            // optimization of the n-sources
            Results optNSrc = new Results(null, null, null, null, Double.POSITIVE_INFINITY, false, 0, 0, 0, false, false, 0, null, null);
            appendToLog("Optimizing the non-stationarity of the n-sources...");
            for(int i = 0; i < par.getNumberOfRestarts(); i++)
            {
                Results buf;
                if(i == 0)
                {
                    buf = optimizeOnce(par, data, true, SSAMatrix.concatVertically(optSSrc.Pn, optSSrc.Ps));
                }
                else
                {
                    buf = optimizeOnce(par, data, true, null);
                }

                if(buf.loss < optNSrc.loss)
                {
                    optNSrc = buf;
                }
                if(logger != null)
                {
                    // show progress
                    appendToLog("Repetition " + (i+1) + ": iterations=" + buf.iterations + ", max. objective function value=" + -buf.loss);
                }
                if(stopped)
                {
                    break;
                }
            }

            // now put results of both optimizations together
            int n = data.getNumberOfDimensions();
            SSAMatrix Mix = SSAMatrix.solve(SSAMatrix.concatVertically(optSSrc.Ps, optNSrc.Pn), SSAMatrix.eye(n));
            // basis for stationary subspace
            SSAMatrix Bs = Mix.getRange(0, n, 0, optSSrc.d);
            // basis for non-stationary subspace
            SSAMatrix Bn = Mix.getRange(0, n, optSSrc.d, n);
            Results opt = new Results(optSSrc.Ps,
                                      optNSrc.Pn,
                                      Bs,
                                      Bn,
                                      0,
                                      optSSrc.converged,
                                      0,
                                      optSSrc.d,
                                      optSSrc.reps,
                                      optSSrc.useMean,
                                      optSSrc.useCovariance,
                                      optSSrc.equalEpochs,
                                      optSSrc.inputFile,
                                      optSSrc.epochFile);
             opt.loss_s = optSSrc.loss;
             opt.loss_n = -optNSrc.loss;
             opt.iterations_s = optSSrc.iterations;
             opt.iterations_n = optNSrc.iterations;
             return opt;
        }
        else if(par.isUseMean()) {
            // use only mean; SSA as an eigenvalue problem
            appendToLog("Only mean should be used; Solving SSA as an eigenvalue problem.");

            SSAMatrix mu[] = new SSAMatrix[data.mu.length];

            int n = data.getNumberOfDimensions();
            int d = par.getNumberOfStationarySources();

            // prepare matrix H on which we want to solve the eigenvalue problem
            SSAMatrix H = SSAMatrix.zeros(n, n);
            for(int i = 0; i < data.mu.length; i++)
            {
                // whiten means; assume identity covariance matrix
                //mu[i] = data.W.mmul(data.mu[i].sub(data.muall));
                mu[i] = data.mu[i].sub(data.muall);
                // add mu'*mu to H
                H.addi(mu[i].mmul(mu[i].transpose()).muli(data.epochSizes[i]));
            }

            // solve eigenvalue problem
            SSAMatrix E[] = H.symmetricEigenvectors();
            // the eigenvectors are in the columns and are our projection directions; we need them
            // in the rows in our projection matrices
            SSAMatrix B = E[0].transpose();
            // loss_s is the sum of the eigenvalues for the stationary subspace
            SSAMatrix diag = E[1].diag();
            double loss_s = diag.getRange(0, d, 0, 1).sum();
            // loss_n is the sum of the eigenvalues for the non-stationary subspace
            double loss_n = diag.getRange(d, n, 0, 1).sum();
            // Now normalize losses
            int k_s = data.mu.length * d; // degrees of freedom of chi^2 distribution
            int k_n = data.mu.length * (n - d);
            loss_s = normalizeObjectiveFunction(loss_s, k_s);
            loss_n = normalizeObjectiveFunction(loss_n, k_n);

            // projection matrix for stationary subspace
            SSAMatrix Ps = B.getRange(0, d, 0, n);
            // projection matrix for non-stationary subspace
            SSAMatrix Pn = B.getRange(d, n, 0, n);
            // mixing matrix is the inverse of B
            SSAMatrix Mix = SSAMatrix.solve(B, SSAMatrix.eye(n));
            // basis for stationary subspace
            SSAMatrix Bs = Mix.getRange(0, n, 0, d);
            // basis for non-stationary subspace
            SSAMatrix Bn = Mix.getRange(0, n, d, n);

            appendToLog("Solved.");
            appendToLog("Objective function value for the s-sources=" + loss_s);
            appendToLog("Objective function value for the n-sources=" + loss_n);

            Results results =  new Results(Ps, Pn, Bs, Bn, 0, true, 1,
                              par.getNumberOfStationarySources(),
                              1,
                              par.isUseMean(),
                              par.isUseCovariance(),
                              data.getEpochType() == Data.EPOCHS_CUSTOM ? 0 : data.getNumberOfEqualSizeEpochs(),
                              data.getTimeseriesFile() == null ? "" : data.getTimeseriesFile().toString(),
                              data.getEpochDefinitionFile() == null ? "" : data.getEpochDefinitionFile().toString());
            results.loss_s = loss_s;
            results.loss_n = loss_n;
            
            return results;
        } 
        else 
        {
            throw new RuntimeException("ERROR: Both mean and covariance matrix deactivated, please choose at least one.");
        }
    }

    /**
     * Computes the objective function (and optionally the gradient)
     *
     * @param n number of dimensions
     * @param d number of stationary sources
     * @param S array with covariance matrices over all epochs
     * @param mu array with means over all epochs
     * @param M antisymmetric matrix such that the current rotation matrix is exp(M) (if M == null a zero matrix is assumed)
     * @param calcGradient set this to true if the gradient should also be calculated
     * @param useMean if false, the objective function without the mean is used
     *
     * @return array of matrices: 1x1 matrix with the loss at exp(M) at index 0 and optionally
     *         the gradient at exp(M) w.r.t. M at index 1 (only if calcGradient was set to true) and exp(M) at index 2
     */
    public SSAMatrix[] objectiveFunction(       int n,
                                                int d,
                                                SSAMatrix S[],
                                                SSAMatrix mu[],
                                                int epochSizes[],
                                                SSAMatrix M,
                                                boolean calcGradient,
                                                boolean useMean)
    {
        double loss = 0.0;
        SSAMatrix gradient = null;

        // rotated covariance matrices and means
        Snew = new SSAMatrix[S.length];
        if(useMean)
	        munew = new SSAMatrix[mu.length];

        //int n = data.getNumberOfDimensions();

        // only initialize variable gradient if it is needed later
        if(calcGradient)
        {
            gradient = SSAMatrix.zeros(d, n);
        }

        // calculate rotation matrix
        SSAMatrix Rcomplete;
        if(M == null)
        {
            Rcomplete = SSAMatrix.eye(n);
        }
        else
        {
            Rcomplete = M.expm();
        }
        //SSAMatrix R = Rcomplete.getRange(0, d, 0, n);
        SSAMatrix Rtcomplete = Rcomplete.transpose();

        for(int i = 0; i < S.length; i++)
        {
            // rotate covariance matrix and mean vector in epoch i
            SSAMatrix RScomplete = Rcomplete.mmul(S[i]); // R multiplied only from left side (needed for gradient)
            SSAMatrix RSRtcomplete = RScomplete.mmul(Rtcomplete); // rotated covariance matrix
            SSAMatrix Rmucomplete = null;
            if(useMean)
            	Rmucomplete = Rcomplete.mmul(mu[i]); // rotated mean

            // truncate to the stationary subspace
            SSAMatrix RS = RScomplete.getRange(0, d, 0, n);
            SSAMatrix RSRt = RSRtcomplete.getRange(0, d, 0, d);
            SSAMatrix Rmu = null;
            if(useMean)
            	Rmu = Rmucomplete.getRange(0, d, 0, 1);

            Snew[i] = RSRtcomplete;
            if(useMean)
	            munew[i] = Rmucomplete;

            double add = -Math.log(MathFunctions.det(RSRt));
            //loss += -Math.log(MathFunctions.det(RSRt));
            if(useMean)
            {
                //loss += Rmu.mul(Rmu).sum();
                add += Rmu.mul(Rmu).sum();
            }
            loss += ((double)epochSizes[i]) * add;

            // calculate gradient if needed
            if(calcGradient)
            {
                //gradient.subi(MathFunctions.inv(RSRt).mmul(RS));
                gradient.subi(MathFunctions.inv(RSRt).mmul(RS).muli((double)epochSizes[i]));
                if(useMean)
                {
                    gradient.addi(Rmu.mmul(mu[i].transpose()).muli((double)epochSizes[i]));
                }
            }
        }

        if(!calcGradient)
        {
            // only return loss
            return new SSAMatrix[]{new SSAMatrix(new double[][]{{loss}}), null, Rcomplete};
        }
        else
        {
            // concatenate (n-d) x n zero matrix to gradient (gradient is square afterwards)
            gradient = SSAMatrix.concatVertically(gradient.muli(2.0), SSAMatrix.zeros(n - d, n));
            // calculate the gradient at exp(M) w.r.t. M
            gradient = gradient.mmul(Rcomplete.transpose()).subi(Rcomplete.mmul(gradient.transpose()));
            // return loss and gradient
            return new SSAMatrix[]{new SSAMatrix(new double[][]{{loss}}), gradient, Rcomplete};
        }
    }

    /**
     * Normalizes the objective function value.
     *
     * @param loss objective function value as calculated by objectiveFunction()
     * @param k degrees of freedom of chi^2 distribution
     */
    public double normalizeObjectiveFunction(double loss, int k)
    {
        return Math.sqrt(2.0 * loss) - Math.sqrt(2.0*((double)k) - 1.0);
    }

    /**
     * Normalizes the gradient.
     *
     * @param grad gradient as calculated by objectiveFunction()
     * @param loss objective function value as calculated by objectiveFunction()
     */
    public SSAMatrix normalizeGradient(SSAMatrix grad, double loss)
    {
        return grad.div(Math.sqrt(2.0*loss));
    }

    /**
     * Stops the optimization.
     */
    public void stop()
    {
        this.stopped = true;
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
}

