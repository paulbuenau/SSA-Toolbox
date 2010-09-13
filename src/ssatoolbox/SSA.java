package ssatoolbox;

import org.jblas.*;

/**
 * Implementation of the "Stationary Subspace Analysis" (SSA) algorithm
 *
 * @author Jan Saputra Mueller, saputra@cs.tu-berlin.de
 */
public class SSA
{
    // buffer for rotated covariance matrices and means
    private DoubleMatrix Snew[];
    private DoubleMatrix munew[];

    // constants for line-search
    private static final double LSALPHA = 0.5*(0.01+0.3);
    private static final double LSBETA = 0.4;
    private static final double RDEC_THRESHOLD = 1e-8;

    // handle to logger
    private Logger logger = null;
    
    private volatile boolean stopped = false;

    /**
     * Solves the SSA optimization problem using backtracking linesearch once.
     *
     * @param par class containing the SSA parameters
     * @param data class containing the data
     * @return Results object
     */
    public Results optimizeOnce(SSAParameters par, Data data)
    {
        DoubleMatrix S[] = new DoubleMatrix[data.S.length];
        DoubleMatrix mu[] = new DoubleMatrix[data.mu.length];

        int n = data.S[0].getRows();
        int d = par.isNSA() ? (n - par.getNumberOfStationarySources()) : par.getNumberOfStationarySources();

        // start with whitening + random rotation
        DoubleMatrix B = MathFunctions.randRot(n).mmuli(data.W);

        // apply initialization matrix to covariance matrices and means
        for(int i = 0; i < data.S.length; i++)
        {
            S[i] = B.mmul(data.S[i]).mmuli(B.transpose());
            mu[i] = B.mmul(data.mu[i].sub(data.muall));
        }

        // Optimization loop
        DoubleMatrix grad, gradOld = null;
        DoubleMatrix alpha, alphaOld = null;
        double loss = 0, lossNew = 0;
        boolean converged = false;
        int i;
        for(i = 0; i < Integer.MAX_VALUE; i++)
        {
            // get current objective function value and gradient
            DoubleMatrix ret[] = objectiveFunction( d,
                                                    S, mu, null, true,
                                                    par.isUseMean());
            loss = ret[0].get(0);
            grad = ret[1];

            // do NSA instead of SSA?
            if(par.isNSA())
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
            DoubleMatrix search = alpha.div(Math.sqrt(alpha.mul(alpha).sum() * 2));
            //DoubleMatrix search = alpha.div(alpha.norm2());

            // backtracking line search
            double t = 1;
            for(int j = 0; j < 10; j++, t *= LSBETA)
            {
                DoubleMatrix M = search.mul(t);
                ret = objectiveFunction(d,
                                        S, mu, M, false,
                                        par.isUseMean());
                lossNew = ret[0].get(0);
                if(par.isNSA())
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
        DoubleMatrix Ps = B.getRange(0, d, 0, n);
        // projection matrix for non-stationary subspace
        DoubleMatrix Pn = B.getRange(d, n, 0, n);
        // mixing matrix is the inverse of B
        DoubleMatrix Mix = Solve.solve(B, DoubleMatrix.eye(n));
        // basis for stationary subspace
        DoubleMatrix Bs = Mix.getRange(0, n, 0, d);
        // basis for non-stationary subspace
        DoubleMatrix Bn = Mix.getRange(0, n, d, n);

        // do NSA instead of SSA?
        if(par.isNSA())
        {
            // exchange stationary <-> non-stationary
            DoubleMatrix buf = Ps;
            Ps = Pn;
            Pn = buf;
            buf = Bs;
            Bs = Bn;
            Bn = buf;
        }

        //return new DoubleMatrix[]{Ps, Pn, Mix, new DoubleMatrix(new double[]{matLoss}), new DoubleMatrix(new double[]{converged})};
        return new Results(Ps, Pn, Bs, Bn, Math.min(loss, lossNew), converged, i,
                          par.getNumberOfStationarySources(),
                          par.getNumberOfRestarts(),
                          par.isUseMean(),
                          par.isUseCovariance(),
                          data.useCustomEpochDefinition() ? 0 : data.getNumberOfEqualSizeEpochs(),
                          data.getTimeseriesFile() == null ? "" : data.getTimeseriesFile().toString(),
                          data.getEpochDefinitionFile() == null ? "" : data.getEpochDefinitionFile().toString());
    }

    /**
     * Solves the SSA optimization problem using backtracking linesearch
     * the number of times specified in the variable var. The best result is returned.
     *
     * @param par class containing the SSA parameters
     * @param data class containing the data
     * @return Results object
     */
    public Results optimize(SSAParameters par, Data data)
    {
        if(par.isUseCovariance())
        {
            // optimization by gradient decent
            Results opt = new Results(null, null, null, null, Double.POSITIVE_INFINITY, false, 0, 0, 0, false, false, 0, null, null);

            stopped = false;
            for(int i = 0; i < par.getNumberOfRestarts(); i++)
            {
                Results buf = optimizeOnce(par, data);
                if(buf.loss < opt.loss)
                {
                    opt = buf;
                }
                if(logger != null)
                {
                    // show progress
                    logger.appendToLog("Repetition " + (i+1) + ": iterations=" + buf.iterations + ", min. objective function value=" + buf.loss);
                }
                if(stopped)
                {
                    break;
                }
            }

            return opt;
        }
        else
        {
            // use only mean; SSA as an eigenvalue problem
            logger.appendToLog("Only mean should be used; Solving SSA as an eigenvalue problem.");

            DoubleMatrix mu[] = new DoubleMatrix[data.mu.length];

            int n = data.S[0].getRows();
            int d = par.getNumberOfStationarySources();

            // prepare matrix H on which we want to solve the eigenvalue problem
            DoubleMatrix H = DoubleMatrix.zeros(n, n);
            for(int i = 0; i < data.S.length; i++)
            {
                // whiten means
                mu[i] = data.W.mmul(data.mu[i].sub(data.muall));
                // add mu'*mu to H
                H.addi(mu[i].mmul(mu[i].transpose()));
            }

            // solve eigenvalue problem
            DoubleMatrix E[] = Eigen.symmetricEigenvectors(H);
            // the eigenvectors are in the columnsand are our projection directions; we need them
            // in the rows in our projection matrices
            DoubleMatrix B = E[0].transpose();
            // loss is the sum of the eigenvalues for the stationary subspace
            double loss = E[1].diag().getRowRange(0, d, 0).sum();

            // projection matrix for stationary subspace
            DoubleMatrix Ps = B.getRange(0, d, 0, n);
            // projection matrix for non-stationary subspace
            DoubleMatrix Pn = B.getRange(d, n, 0, n);
            // mixing matrix is the inverse of B
            DoubleMatrix Mix = Solve.solve(B, DoubleMatrix.eye(n));
            // basis for stationary subspace
            DoubleMatrix Bs = Mix.getRange(0, n, 0, d);
            // basis for non-stationary subspace
            DoubleMatrix Bn = Mix.getRange(0, n, d, n);

            return new Results(Ps, Pn, Bs, Bn, loss, true, 1,
                              par.getNumberOfStationarySources(),
                              1,
                              par.isUseMean(),
                              par.isUseCovariance(),
                              data.useCustomEpochDefinition() ? 0 : data.getNumberOfEqualSizeEpochs(),
                              data.getTimeseriesFile() == null ? "" : data.getTimeseriesFile().toString(),
                              data.getEpochDefinitionFile() == null ? "" : data.getEpochDefinitionFile().toString());
        }
    }

    /**
     * Computes the objective function (and optionally the gradient)
     *
     * @param d number of stationary sources/non-stationary sources (depends on whether SSA or NSA is done)
     * @param S array with covariance matrices over all epochs
     * @param mu array with means over all epochs
     * @param M antisymmetric matrix such that the current rotation matrix is exp(M) (if M == null a zero matrix is assumed)
     * @param calcGradient set this to true if the gradient should also be calculated
     * @param useMean if false, the objective function without the mean is used
     *
     * @return array of matrices: 1x1 matrix with the loss at exp(M) at index 0 and optionally
     *         the gradient at exp(M) w.r.t. M at index 1 (only if calcGradient was set to true) and exp(M) at index 2
     */
    public DoubleMatrix[] objectiveFunction(    int d,
                                                DoubleMatrix S[],
                                                DoubleMatrix mu[],
                                                DoubleMatrix M,
                                                boolean calcGradient,
                                                boolean useMean)
    {
        double loss = 0.0;
        DoubleMatrix gradient = null;

        // rotated covariance matrices and means
        Snew = new DoubleMatrix[S.length];
        munew = new DoubleMatrix[mu.length];

        int n = S[0].getRows();

        // only initialize variable gradient if it is needed later
        if(calcGradient)
        {
            gradient = DoubleMatrix.zeros(d, n);
        }

        // calculate rotation matrix
        DoubleMatrix Rcomplete;
        if(M == null)
        {
            Rcomplete = DoubleMatrix.eye(n);
        }
        else
        {
            Rcomplete = MatrixFunctions.expm(M);
        }
        //DoubleMatrix R = Rcomplete.getRange(0, d, 0, n);
        DoubleMatrix Rtcomplete = Rcomplete.transpose();

        for(int i = 0; i < S.length; i++)
        {
            // rotate covariance matrix and mean vector in epoch i
            DoubleMatrix RScomplete = Rcomplete.mmul(S[i]); // R multiplied only from left side (needed for gradient)
            DoubleMatrix RSRtcomplete = RScomplete.mmul(Rtcomplete); // rotated covariance matrix
            DoubleMatrix Rmucomplete = Rcomplete.mmul(mu[i]); // rotated mean

            // truncate to the stationary subspace
            DoubleMatrix RS = RScomplete.getRange(0, d, 0, n);
            DoubleMatrix RSRt = RSRtcomplete.getRange(0, d, 0, d);
            DoubleMatrix Rmu = Rmucomplete.getRange(0, d, 0, 1);

            Snew[i] = RSRtcomplete;
            munew[i] = Rmucomplete;

            loss += -Math.log(MathFunctions.det(RSRt));
            if(useMean)
            {
                loss +=  Rmu.mul(Rmu).sum();
            }

            // calculate gradient if needed
            if(calcGradient)
            {
                gradient.subi(MathFunctions.inv(RSRt).mmul(RS));
                if(useMean)
                {
                    gradient.addi(Rmu.mmul(mu[i].transpose()));
                }
            }
        }

        if(!calcGradient)
        {
            // only return loss
            return new DoubleMatrix[]{new DoubleMatrix(new double[]{loss}), null, Rcomplete};
        }
        else
        {
            // concatenate (n-d) x n zero matrix to gradient (gradient is square afterwards)
            gradient = DoubleMatrix.concatVertically(gradient.muli(2.0), DoubleMatrix.zeros(n - d, n));
            // calculate the gradient at exp(M) w.r.t. M
            gradient = gradient.mmul(Rcomplete.transpose()).subi(Rcomplete.mmul(gradient.transpose()));
            // return loss and gradient
            return new DoubleMatrix[]{new DoubleMatrix(new double[]{loss}), gradient, Rcomplete};
        }
    }

    public void stop()
    {
        this.stopped = true;
    }

    public void setLogger(Logger logger)
    {
        this.logger = logger;
    }
}
