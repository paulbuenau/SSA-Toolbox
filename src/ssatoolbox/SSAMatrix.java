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
 * Wrapper class for different matrix libraries. All matrrices in the toolbox are instances of this class. It passes
 * the matrix operations to the currently used matrix library.
 *
 * @author Jan Saputra Mueller, saputra@cs.tu-berlin.de
 */
public class SSAMatrix
{
    public static final int JBLAS = 0;
    public static final int COLT = 1;
    
    private static int globalLib = COLT; // global variable indicates which matrix library is used for *all* matrices
    
    private int lib; // saves the library which this matrix internally uses
    
    // matrix variables for the different libraries
    private org.jblas.DoubleMatrix MATRIX_JBLAS; // jBlas
    private cern.colt.matrix.DoubleMatrix2D MATRIX_COLT; // Colt

    private static java.util.Random random = new java.util.Random();
    
    
    /**
     * SSAMatrix constructor.
     *
     * @param m matrix as a double[][] array
     */
    public SSAMatrix(double[][] m)
    {
        this.lib = globalLib;
        
        setArray(m);
    }
    
    /**
     * Copy constructor.
     * 
     * @param m matrix to copy from
     */
    public SSAMatrix(SSAMatrix m)
    {
        this.lib = m.lib;
        
        switch(lib)
        {
            case JBLAS:
                MATRIX_JBLAS = m.MATRIX_JBLAS.dup();
                break;
            case COLT:
                MATRIX_COLT = m.MATRIX_COLT.copy();
                break;
        }
    }
    
    /**
     * Copy constructor for the jBlas library.
     *
     * @param m jBlas DoubleMatrix matrix to copy from
     */
    public SSAMatrix(org.jblas.DoubleMatrix m)
    {
        this.lib = JBLAS;
        MATRIX_JBLAS = m.dup();
    }
    
    /**
     * Copy constructor for the Colt library.
     *
     * @param m Colt DoubleMatrix2D matrix to copy from
     */
    public SSAMatrix(cern.colt.matrix.DoubleMatrix2D m)
    {
        this.lib = COLT;
        MATRIX_COLT = m.copy();
    }
    
    /**
     * Returns the matrix library which is used for *all* matrices.
     *
     * @return global library
     */
    public static int getGlobalLib()
    {
        return globalLib;
    }
    
    /**
     * Sets the matrix library which should be used for *all* matrices.
     */
    public static void setGlobalLib(int library)
    {
        globalLib = library;
    }
    
    /**
     * Converts the saved matrix to the new global library (if necessary).
     * This method should normally be called by any method in the class SSAMatrix which operates on the internal matrix
     * before doing anything else.
     */
    private void convert()
    {
        if(lib != globalLib)
        {
            // conversion is needed
            setArray(getArray());
            switch(lib)
            {
                case JBLAS:
                    MATRIX_JBLAS = null;
                    break;
                    
                case COLT:
                    MATRIX_COLT = null;
                    break;
            }
            
            lib = globalLib;
        }
    }
    
    /**
     * Returns the matrix as a double[][] array.
     *
     * @return matrix as a double[][] array.
     */
    public double[][] getArray()
    {
        // This method does NOT call convert() !!!!!!!!!!!!!!!!!!!!!!!!!
        // It uses the variable "lib" to find out the matrix library to convert from.
        double[][] m = null;
        switch(lib)
        {
            case JBLAS:
                m = MATRIX_JBLAS.toArray2();
                break;
                
            case COLT:
                m = MATRIX_COLT.toArray();
                break;
        }
        
        return m;
    }
    
    /**
     * Sets the values in the matrix from a double[][] array.
     *
     * @param m matrix as a double[][] array
     */
    public void setArray(double[][] m)
    {
        // This method does NOT call convert() !!!!!!!!!!!!!!!!!!!!!!!!!
        // It uses the variable "globalLib" to find out the matrix library to use.
        switch(globalLib)
        {
            case JBLAS:
                MATRIX_JBLAS = new org.jblas.DoubleMatrix(m);
                break;
                
            case COLT:
                MATRIX_COLT = cern.colt.matrix.DoubleFactory2D.dense.make(m);
                break;
        }
    }
    
    /**
     * Returns the number of rows.
     *
     * @return number of rows
     */
    public int getRows()
    {
        switch(lib)
        {
            case JBLAS:
                return MATRIX_JBLAS.getRows();
                
            case COLT:
                return MATRIX_COLT.rows();
        }
        
        return 0; // should not happen
    }
    
    /**
     * Returns the number of rows.
     *
     * @return number of rows
     */
    public int getColumns()
    {
        switch(lib)
        {
            case JBLAS:
                return MATRIX_JBLAS.getColumns();
                
            case COLT:
                return MATRIX_COLT.columns();
        }
        
        return 0; // should not happen
    }
    
    /**
     * Returns the element at index (i,j).
     * 
     * @param i row index
     * @param j column index
     * @return value at index (i,j)
     */
    public double get(int i, int j)
    {
        switch(lib)
        {
            case JBLAS:
                return MATRIX_JBLAS.get(i, j);
            case COLT:
                return MATRIX_COLT.get(i, j);
        }
        
        return 0; // should not happen
    }
    
    /**
     * Sets an entry in the matrix.
     *
     * @param i row index
     * @param j column index
     * @param value value
     */
    public void set(int i, int j, double value)
    {
        convert();
        
        switch(globalLib)
        {
            case JBLAS:
                MATRIX_JBLAS.put(i, j, value);
                break;
            case COLT:
                MATRIX_COLT.set(i, j, value);
                break;
        }
    }
    
    /**
     * Returns a transposed version of the matrix.
     */
    public SSAMatrix transpose()
    {
        convert();
        
        switch(lib)
        {
            case JBLAS:
                return new SSAMatrix(MATRIX_JBLAS.transpose());
            case COLT:
                return new SSAMatrix(cern.colt.matrix.linalg.Algebra.DEFAULT.transpose(MATRIX_COLT));
        }
        
        return null; // should not happen
    }
    
    /**
     * Returns the diagonal of the matrix (which has to be square).
     *
     * @return diagonal of the matrix
     */
    public SSAMatrix diag()
    {
        convert();
        
        switch(lib)
        {
            case JBLAS:
                return new SSAMatrix(MATRIX_JBLAS.diag());
            case COLT:
                return new SSAMatrix(cern.colt.matrix.DoubleFactory2D.dense.make(cern.colt.matrix.DoubleFactory2D.dense.diagonal(MATRIX_COLT).toArray(), getRows()));
        }
        
        return null; // should not happen
    }
    
    /**
     * Calculates the sum of all elements in the matrix.
     *
     * @return sum
     */
    public double sum()
    {
        switch(lib)
        {
            case JBLAS:
                return MATRIX_JBLAS.sum();
            case COLT:
                return MATRIX_COLT.zSum();
        }
        
        return 0; // should not happen
    }
    
    /**
     * Calculates the product of all elements in the matrix.
     *
     * @return product
     */
    public double prod()
    {
        switch(lib)
        {
            case JBLAS:
                return MATRIX_JBLAS.prod();
            case COLT:
                return MATRIX_COLT.aggregate(cern.jet.math.Functions.mult, cern.jet.math.Functions.identity);
        }
        
        return 0; // should not happen
    }
    
    /**
     * Adds another matrix to this matrix (in place).
     *
     * @param m other matrix
     * @return sum of matrices
     */
    public SSAMatrix addi(SSAMatrix m)
    {
        convert();
        m.convert();
        
        switch(lib)
        {
            case JBLAS:
                MATRIX_JBLAS.addi(m.MATRIX_JBLAS);
                break;
                
            case COLT:
                MATRIX_COLT.assign(m.MATRIX_COLT, cern.jet.math.Functions.plus);
                break;
        }
        
        return this;
    }
    
    /**
     * Adds another matrix to this matrix.
     *
     * @param m other matrix
     * @return sum of matrices
     */
    public SSAMatrix add(SSAMatrix m)
    {
        convert();
        
        SSAMatrix newM = new SSAMatrix(this);
        
        return newM.addi(m);
    }
    
    /**
     * Subtracts another matrix from this matrix (in place).
     *
     * @param m other matrix
     * @return subtraction result
     */
    public SSAMatrix subi(SSAMatrix m)
    {
        convert();
        m.convert();
        
        switch(lib)
        {
            case JBLAS:
                MATRIX_JBLAS.subi(m.MATRIX_JBLAS);
                break;
                
            case COLT:
                MATRIX_COLT.assign(m.MATRIX_COLT, cern.jet.math.Functions.minus);
                break;
        }
        
        return this;
    }
    
    /**
     * Subtracts another matrix from this matrix.
     *
     * @param m other matrix
     * @return subtraction result
     */
    public SSAMatrix sub(SSAMatrix m)
    {
        convert();
        
        SSAMatrix newM = new SSAMatrix(this);
        
        return newM.subi(m);
    }
    
    /**
     * Subtraction by a scalar (in place).
     *
     * @param s scalar
     * @return subtraction result
     */
    public SSAMatrix subi(double s)
    {
        convert();
        
        switch(lib)
        {
            case JBLAS:
                MATRIX_JBLAS.subi(s);
                break;
                
            case COLT:
                MATRIX_COLT.assign(cern.jet.math.Functions.bindArg2(cern.jet.math.Functions.minus, s));
                break;
        }
        
        return this;
    }
    
    /**
     * Element-wise multiplication (in place).
     *
     * @param m other matrix
     * @return sum of matrices
     */
    public SSAMatrix muli(SSAMatrix m)
    {
        convert();
        m.convert();
        
        switch(lib)
        {
            case JBLAS:
                MATRIX_JBLAS.muli(m.MATRIX_JBLAS);
                break;
                
            case COLT:
                MATRIX_COLT.assign(m.MATRIX_COLT, cern.jet.math.Functions.mult);
                break;
        }
        
        return this;
    }
    
    /**
     * Element-wise multiplication.
     *
     * @param m other matrix
     * @return sum of matrices
     */
    public SSAMatrix mul(SSAMatrix m)
    {
        convert();
        
        SSAMatrix newM = new SSAMatrix(this);
        
        return newM.muli(m);
    }
    
    /**
     * Multiplication with a scalar (in place).
     *
     * @param s scalar
     * @return multiplication result
     */
    public SSAMatrix muli(double s)
    {
        convert();
        
        switch(lib)
        {
            case JBLAS:
                MATRIX_JBLAS.muli(s);
                break;
                
            case COLT:
                MATRIX_COLT.assign(cern.jet.math.Functions.bindArg2(cern.jet.math.Functions.mult, s));
                break;
        }
        
        return this;
    }
    
    /**
     * Multiplication with a scalar.
     *
     * @param s scalar
     * @return multiplication result
     */
    public SSAMatrix mul(double s)
    {
        convert();
        
        SSAMatrix m = new SSAMatrix(this);
        
        return m.muli(s);
    }
    
    /**
     * Division by a scalar (in place).
     *
     * @param s scalar
     * @return division result
     */
    public SSAMatrix divi(double s)
    {
        convert();
        
        switch(lib)
        {
            case JBLAS:
                MATRIX_JBLAS.divi(s);
                break;
                
            case COLT:
                MATRIX_COLT.assign(cern.jet.math.Functions.bindArg2(cern.jet.math.Functions.div, s));
                break;
        }
        
        return this;
    }
    
    /**
     * Division by a scalar.
     *
     * @param s scalar
     * @return division result
     */
    public SSAMatrix div(double s)
    {
        convert();
        
        SSAMatrix m = new SSAMatrix(this);
        
        return m.divi(s);
    }
    
    /**
     * Matrix multiplication (in place).
     *
     * @param m other matrix
     * @return multiplication result
     */
    public SSAMatrix mmuli(SSAMatrix m)
    {
        convert();
        m.convert();
        
        switch(lib)
        {
            case JBLAS:
                MATRIX_JBLAS.mmuli(m.MATRIX_JBLAS);
                break;
            case COLT:
                MATRIX_COLT = cern.colt.matrix.linalg.Algebra.DEFAULT.mult(MATRIX_COLT, m.MATRIX_COLT);
                break;
        }
        
        return this;
    }
    
    /**
     * Matrix multiplication.
     *
     * @param m other matrix
     * @return multiplication result
     */
    public SSAMatrix mmul(SSAMatrix m)
    {
        convert();
        m.convert();
        
        switch(lib)
        {
            case JBLAS:
                return new SSAMatrix(MATRIX_JBLAS.mmul(m.MATRIX_JBLAS));
            case COLT:
                return new SSAMatrix(cern.colt.matrix.linalg.Algebra.DEFAULT.mult(MATRIX_COLT, m.MATRIX_COLT));
        }
        
        return null; // should not happen
    }
    
    /**
     * Returns a zero-matrix.
     *
     * @param rows number of rows
     * @param columns number of columns
     * @return zero matrix
     */
    public static SSAMatrix zeros(int rows, int columns)
    {
        switch(globalLib)
        {
            case JBLAS:
                return new SSAMatrix(org.jblas.DoubleMatrix.zeros(rows, columns));
            case COLT:
                return new SSAMatrix(cern.colt.matrix.DoubleFactory2D.dense.make(rows, columns));
        }
        
        return null; // should not happen
    }
    
    /**
     * Returns a matrix with only ones.
     *
     * @param rows number of rows
     * @param columns number of columns
     * @return matrix with only ones
     */
    public static SSAMatrix ones(int rows, int columns)
    {
        switch(globalLib)
        {
            case JBLAS:
                return new SSAMatrix(org.jblas.DoubleMatrix.ones(rows, columns));
            case COLT:
                return new SSAMatrix(cern.colt.matrix.DoubleFactory2D.dense.make(rows, columns, 1.0));
        }
        
        return null; // should not happen
    }
    
    /**
     * Returns the identity matrix.
     *
     * @param rowsAndColumns number of rows and columns
     * @return identity matrix
     */
    public static SSAMatrix eye(int rowsAndColumns)
    {
        switch(globalLib)
        {
            case JBLAS:
                return new SSAMatrix(org.jblas.DoubleMatrix.eye(rowsAndColumns));
            case COLT:
                return new SSAMatrix(cern.colt.matrix.DoubleFactory2D.dense.identity(rowsAndColumns));
        }
        
        return null; // should not happen
    }
    
    /**
     * Computes the cholesky decomposition of the matrix.
     * The matrix has to be square, symmetric and positive definite.
     *
     * @return lower triangular matrix U such that the original matrix is equal to U'*U 
     */
    public SSAMatrix cholesky()
    {
        convert();
        
        switch(globalLib)
        {
            case JBLAS:
                return new SSAMatrix(org.jblas.Decompose.cholesky(MATRIX_JBLAS));
            case COLT:
                return new SSAMatrix(cern.colt.matrix.linalg.Algebra.DEFAULT.transpose((new cern.colt.matrix.linalg.CholeskyDecomposition(MATRIX_COLT)).getL()));
        }
        
        return null; // should not happen
    }
    
    /**
     * Computes the eigenvectors and eigenvalues (ascending) of a symmetric matrix.
     *
     * @return array, with eigenvectors at index 0 and diagonal eigenvalue matrix at index 1.
     */
    public SSAMatrix[] symmetricEigenvectors()
    {
        convert();
        
        SSAMatrix V[] = new SSAMatrix[2];
        
        switch(globalLib)
        {
            case JBLAS:
                org.jblas.DoubleMatrix[] Vjblas = org.jblas.Eigen.symmetricEigenvectors(MATRIX_JBLAS);
                V[0] = new SSAMatrix(Vjblas[0]);
                V[1] = new SSAMatrix(Vjblas[1]);
                break;
            case COLT:
                cern.colt.matrix.linalg.EigenvalueDecomposition Vcolt = new cern.colt.matrix.linalg.EigenvalueDecomposition(MATRIX_COLT);
                V[0] = new SSAMatrix(Vcolt.getV());
                V[1] = new SSAMatrix(Vcolt.getD());
                break;
        }
        
        return V;
    }
    
    /**
     * Calculates the eigenvalues of a symmetric matrix.
     *
     * @return a square matrix with the eigenvalues on its diagonal (ascending).
     */
    public SSAMatrix symmetricEigenvalues()
    {
        convert();

        switch(globalLib)
        {
            case JBLAS:
                return new SSAMatrix(org.jblas.Eigen.symmetricEigenvalues(MATRIX_JBLAS));
            case COLT:
                return new SSAMatrix((new cern.colt.matrix.linalg.EigenvalueDecomposition(MATRIX_COLT)).getD());
        }
        
        return null; // should not happen
    }
    
    /**
     * Solves A*X=B for given matrices A and B.
     *
     * @return matrix X
     */
    public static SSAMatrix solve(SSAMatrix A, SSAMatrix B)
    {
        A.convert();
        B.convert();
        
        switch(globalLib)
        {
            case JBLAS:
                return new SSAMatrix(org.jblas.Solve.solve(A.MATRIX_JBLAS, B.MATRIX_JBLAS));
            case COLT:
                return new SSAMatrix(cern.colt.matrix.linalg.Algebra.DEFAULT.solve(A.MATRIX_COLT, B.MATRIX_COLT));
        }
        
        return null;
    }
    
    /**
     * Generates a random matrix (each entry is drawn uniformly from (0, 1) ).
     *
     * @param rows number of rows
     * @param columns number of columns
     */
    public static SSAMatrix rand(int rows, int columns)
    {
        // removed this code; using java.util.Random now, to make it possible to set the random seed
        /*switch(globalLib)
        {
            case JBLAS:
                return new SSAMatrix(org.jblas.DoubleMatrix.rand(rows, columns));
            case COLT:
                return new SSAMatrix(cern.colt.matrix.DoubleFactory2D.dense.random(rows, columns));
        }*/

        double randMatrix[][] = new double[rows][columns];
        for(int i = 0; i < rows; i++)
        {
            for(int j = 0; j < columns; j++)
            {
                randMatrix[i][j] = random.nextDouble();
            }
        }
        
        return new SSAMatrix(randMatrix); // should not happen
    }

    /**
     * Sets the seed of the random number generator.
     *
     * @param seed seed
     */
    public static void setRandomSeed(long seed)
    {
        random.setSeed(seed);
    }
    
    /**
     * Calculates the maximum norm of the matrix.
     *
     * @return maximum norm of the matrix
     */
    public double normmax()
    {
        convert();
        
        switch(globalLib)
        {
            case JBLAS:
                return MATRIX_JBLAS.normmax();
            case COLT:
                return MATRIX_COLT.aggregate(cern.jet.math.Functions.max, cern.jet.math.Functions.abs);
        }
        
        return 0; // should not happen
    }
    
    /**
     * Returns a range of the matrix.
     *
     * @param ra start row
     * @param rb end row + 1
     * @param ca start column
     * @param cb end column + 1
     * @return range of the matrix
     */
    public SSAMatrix getRange(int ra, int rb, int ca, int cb)
    {
        convert();
        
        switch(globalLib)
        {
            case JBLAS:
                return new SSAMatrix(MATRIX_JBLAS.getRange(ra, rb, ca, cb));
            case COLT:
                return new SSAMatrix(MATRIX_COLT.viewPart(ra, ca, rb - ra, cb - ca));
        }
        
        return null; // should not happen
    }
    
    /**
     * Returns columns of the matrix.
     *
     * @param cindices array of column indices
     * @return matrix with the selected columns
     */
    public SSAMatrix getColumns(int cindices[])
    {
        convert();
        
        switch(globalLib)
        {
            case JBLAS:
                return new SSAMatrix(MATRIX_JBLAS.getColumns(cindices));
            case COLT:
                return new SSAMatrix(MATRIX_COLT.viewSelection(null, cindices));
        }
        
        return null; // should not happen
    }
    
    /**
     * Concatenates two matrices vertically.
     *
     * @param A first matrix
     * @param B second matrix
     * @return concatenated matrix
     */
    public static SSAMatrix concatVertically(SSAMatrix A, SSAMatrix B)
    {
        A.convert();
        B.convert();
        
        switch(globalLib)
        {
            case JBLAS:
                return new SSAMatrix(org.jblas.DoubleMatrix.concatVertically(A.MATRIX_JBLAS, B.MATRIX_JBLAS));
            case COLT:
                return new SSAMatrix(cern.colt.matrix.DoubleFactory2D.dense.appendRows(A.MATRIX_COLT, B.MATRIX_COLT));
        }
        
        return null; // should not happen
    }
    
    /**
     * Concatenates two matrices horizontally.
     *
     * @param A first matrix
     * @param B second matrix
     * @return concatenated matrix
     */
    public static SSAMatrix concatHorizontally(SSAMatrix A, SSAMatrix B)
    {
        A.convert();
        B.convert();
        
        switch(globalLib)
        {
            case JBLAS:
                return new SSAMatrix(org.jblas.DoubleMatrix.concatHorizontally(A.MATRIX_JBLAS, B.MATRIX_JBLAS));
            case COLT:
                return new SSAMatrix(cern.colt.matrix.DoubleFactory2D.dense.appendColumns(A.MATRIX_COLT, B.MATRIX_COLT));
        }
        
        return null; // should not happen
    }
    
    /**
     * Calculates the matrix exponential function value of this matrix.
     *
     * @return matrix exponential function value
     */
    public SSAMatrix expm()
    {
        convert();
        
        switch(globalLib)
        {
            case JBLAS:
                return new SSAMatrix(org.jblas.MatrixFunctions.expm(MATRIX_JBLAS));
            case COLT:
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

		int j = Math.max(0, 1 + (int)Math.floor(Math.log(this.normmax())/Math.log(2)));
		SSAMatrix As = this.div(Math.pow(2, j)); // scaled version of A
		int n = this.getRows();

		// calculate D and N using special Horner techniques
		SSAMatrix As_2 = As.mmul(As);
		SSAMatrix As_4 = As_2.mmul(As_2);
		SSAMatrix As_6 = As_4.mmul(As_2);
		// U = c0*I + c2*A^2 + c4*A^4 + (c6*I + c8*A^2 + c10*A^4 + c12*A^6)*A^6
		SSAMatrix U = SSAMatrix.eye(n).muli(c0).addi(As_2.mul(c2)).addi(As_4.mul(c4)).addi(
		    SSAMatrix.eye(n).muli(c6).addi(As_2.mul(c8)).addi(As_4.mul(c10)).addi(As_6.mul(c12)).mmuli(As_6));
		// V = c1*I + c3*A^2 + c5*A^4 + (c7*I + c9*A^2 + c11*A^4 + c13*A^6)*A^6
		SSAMatrix V = SSAMatrix.eye(n).muli(c1).addi(As_2.mul(c3)).addi(As_4.mul(c5)).addi(
		    SSAMatrix.eye(n).muli(c7).addi(As_2.mul(c9)).addi(As_4.mul(c11)).addi(As_6.mul(c13)).mmuli(As_6));

		SSAMatrix AV = As.mmuli(V);
		SSAMatrix N = U.add(AV);
		SSAMatrix D = U.subi(AV);

		// solve DF = N for F
		SSAMatrix F = solve(D, N);

		// now square j times
		for(int k = 0; k < j; k++)
		{
		    F.mmuli(F);
		}
		return F;
        }
        
        return null; // should not happen
    }
}

