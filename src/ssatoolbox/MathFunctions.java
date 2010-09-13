package ssatoolbox;

import org.jblas.*;

/**
 * Mathematical functions used in the SSA implementation
 *
 * @author Jan Saputra Mueller, saputra@cs.tu-berlin.de
 */
public class MathFunctions
{
    /**
     * Calculates the mean of the column vectors of a matrix
     * @param M matrix
     * @return mean of the column vectors
     */
    public static DoubleMatrix mean(DoubleMatrix M)
    {
        int n = M.getColumns();

        return M.mmul(DoubleMatrix.ones(n, 1)).divi((double)n);
    }

    /**
     * Calculates the covariance matrix of the column vectors of a matrix
     * @param M matrix
     * @param mean mean of the column vectors
     * @return covariance matrix of the column vectors
     */
    public static DoubleMatrix cov(DoubleMatrix M, DoubleMatrix mean)
    {
        int n = M.getColumns();
        DoubleMatrix Mc = M.sub(mean.mmul(DoubleMatrix.ones(1, n))); // matrix with centered data

        return Mc.mmul(Mc.transpose()).divi((double)(n-1));
    }

    /**
     * Calculates the covariance matrix of the column vectors of a matrix
     * @param M matrix
     * @return covariance matrix of the column vectors
     */
    public static DoubleMatrix cov(DoubleMatrix M)
    {
        return cov(M, mean(M));
    }

    /**
     * Calculates the determinante of a symmetric, positive definite matrix
     * using the Cholesky-factorization.
     *
     * @param A symmetric, positive definite matrix
     * @return determinante of A
     */
    public static double det(DoubleMatrix A)
    {
        double d = Decompose.cholesky(A).diag().prod();
        return d*d;
    }

    /**
     * Calculates the inverse of a symmetric, positive definite matrix
     * using the Cholesky-factorization.
     * @param A symmetric, positive definite matrix
     * @return inverse of A
     */
    public static DoubleMatrix inv(DoubleMatrix A)
    {
        DoubleMatrix U = Decompose.cholesky(A);
        DoubleMatrix UInv = Solve.solve(U, DoubleMatrix.eye(A.getRows()));
        return UInv.mmuli(UInv.transpose());
    }

    /*public static DoubleMatrix expm(DoubleMatrix A)
    {
        // constants for pade approximation
        final double c0 = 1.0;
        final double c1 = 0.5;
        final double c2 = 0.11764705882352941;
        final double c3 = 0.017156862745098037;
        final double c4 = 0.001715686274509804;
        final double c5 = 1.2254901960784314E-4;
        final double c6 = 6.2845651080945196E-6;
        final double c7 = 2.2444875386051856E-7;
        final double c8 = 5.101108042284513E-9;
        final double c9 = 5.6678978247605695E-11;

        int j = Math.max(0, 1 + (int)Math.floor(Math.log(A.normmax())/Math.log(2)));
        DoubleMatrix As = A.div(Math.pow(2, j)); // scaled version of A
        int n = A.getRows();

        // calculate D and N using special Horner techniques
        DoubleMatrix As_2 = As.mmul(As);
        DoubleMatrix As_4 = As_2.mmul(As_2);
        // U = c0*I + c2*A^2 + (c4*I + c6*A^2 + c8*A^4)*A^4
        DoubleMatrix U = DoubleMatrix.eye(n).muli(c0).addi(As_2.mul(c2)).addi(
                    DoubleMatrix.eye(n).muli(c4).addi(As_2.mul(c6)).addi(As_4.mul(c8)).mmuli(As_4));
        // V = c1*I + c3*A^2 + (c5*I + c7*A^2 + c9*A^4)*A^4
        DoubleMatrix V = DoubleMatrix.eye(n).muli(c1).addi(As_2.mul(c3)).addi(
                    DoubleMatrix.eye(n).muli(c5).addi(As_2.mul(c7)).addi(As_4.mul(c9)).mmuli(As_4));
        DoubleMatrix AV = As.mmuli(V);
        DoubleMatrix N = U.add(AV);
        DoubleMatrix D = U.subi(AV);

        // solve DF = N for F
        DoubleMatrix F = Solve.solve(D, N);

        // now square j times
        for(int k = 0; k < j; k++)
        {
            F.mmuli(F);
        }

        return F;
    }*/

    /**
     * Calculate matrix exponential of a square matrix.
     *
     * A scaled Pade approximation algorithm is used.
     * The algorithm has been directly translated from Golub & Van Loan "Matrix Computations",
     * algorithm 11.3.1. Special Horner techniques from 11.2 are also used to minimize the number
     * of matrix multiplications.
     *
     * @param A square matrix
     * @return matrix exponential of A
     */
    /*public static DoubleMatrix expm(DoubleMatrix A)
    {
        // constants for pade approximation
        final double c0 = 1.0;
        final double c1 = 0.5;
        final double c2 = 0.12;
        final double c3 = 0.01833333333333333;
        final double c4 = 0.0019927536231884053;
        final double c5 = 1.630434782608695E-4;
        final double c6 = 1.0351966873706E-5;
        final double c7 = 5.175983436853E-7;
        final double c8 = 2.0431513566525E-8;
        final double c9 = 6.306022705717593E-10;
        final double c10 = 1.4837700484041396E-11;
        final double c11 = 2.5291534915979653E-13;
        final double c12 = 2.8101705462199615E-15;
        final double c13 = 1.5440497506703084E-17;

        int j = Math.max(0, 1 + (int)Math.floor(Math.log(A.normmax())/Math.log(2)));
        DoubleMatrix As = A.div(Math.pow(2, j)); // scaled version of A
        int n = A.getRows();

        // calculate D and N using special Horner techniques
        DoubleMatrix As_2 = As.mmul(As);
        DoubleMatrix As_4 = As_2.mmul(As_2);
        DoubleMatrix As_6 = As_4.mmul(As_2);
        // U = c0*I + c2*A^2 + c4*A^4 + (c6*I + c8*A^2 + c10*A^4 + c12*A^6)*A^6
        DoubleMatrix U = DoubleMatrix.eye(n).muli(c0).addi(As_2.mul(c2)).addi(As_4.mul(c4)).addi(
            DoubleMatrix.eye(n).muli(c6).addi(As_2.mul(c8)).addi(As_4.mul(c10)).addi(As_6.mul(c12)).mmuli(As_6));
        // V = c1*I + c3*A^2 + c5*A^4 + (c7*I + c9*A^2 + c11*A^4 + c13*A^6)*A^6
        DoubleMatrix V = DoubleMatrix.eye(n).muli(c1).addi(As_2.mul(c3)).addi(As_4.mul(c5)).addi(
            DoubleMatrix.eye(n).muli(c7).addi(As_2.mul(c9)).addi(As_4.mul(c11)).addi(As_6.mul(c13)).mmuli(As_6));

        DoubleMatrix AV = As.mmuli(V);
        DoubleMatrix N = U.add(AV);
        DoubleMatrix D = U.subi(AV);

        // solve DF = N for F
        DoubleMatrix F = Solve.solve(D, N);

        // now square j times
        for(int k = 0; k < j; k++)
        {
            F.mmuli(F);
        }

        return F;
    }*/

    /**
     * Calculates the whitening matrix C^(-1/2) given a covariance matrix C.
     *
     * @return whitening matrix
     */
    public static DoubleMatrix whitening(DoubleMatrix C)
    {
        DoubleMatrix V[] = Eigen.symmetricEigenvectors(C);
        for(int i = 0; i < C.getRows(); i++)
        {
            V[1].put(i, i, 1 / Math.sqrt(V[1].get(i, i)));
        }

        return V[0].mmuli(V[1].mmuli(V[0].transpose()));
    }


    /**
     * Generates a random rotation matrix.
     *
     * @param size size of rotation matrix
     * @return random rotation matrix
     */
    public static DoubleMatrix randRot(int size)
    {
        DoubleMatrix M = DoubleMatrix.rand(size, size).subi(0.5);

        M.subi(M.transpose());

        return MatrixFunctions.expm(M);
    }
}
