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

public class SSAMatrix
{
    public static final int JBLAS = 0;
    public static final int COLT = 1;
    
    private static int globalLib = COLT; // global variable indicates which matrix library is used for *all* matrices
    
    private int lib; // saves the library which this matrix internally uses
    
    // matrix variables for the different libraries
    private org.jblas.DoubleMatrix MATRIX_JBLAS; // jBlas
    private cern.colt.matrix.DoubleMatrix2D MATRIX_COLT; // Colt
    
    
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
}
