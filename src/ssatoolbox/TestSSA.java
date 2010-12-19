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

import junit.framework.*;
import ssatoolbox.*;

public class TestSSA extends TestCase
{
    /**
     * Tests whether exception is thrown if we are using both means and covariances
     * and do not have enough epochs to guarantee determinacy of our solution.
     */
    public void testOptimizeMeanCov()
    {
        SSA ssa = new SSA();
        SSAParameters par = new SSAParameters();
        Data data = new Data();
        
        par.setNumberOfStationarySources(2);
        par.setUseMean(true);
        par.setUseCovariance(true);
        
        data.setTimeSeries(SSAMatrix.zeros(6, 100), null);
        data.setNumberOfEqualSizeEpochs(3); // we would need at least 5 > (6 - 2)/2 + 2 epochs
        data.epochize();
        
        boolean exceptionThrown = false;
        try
        {
            ssa.optimize(par, data);
        }
        catch(Exception e)
        {
            exceptionThrown = true;
        }
        
        assertTrue(exceptionThrown);
    }
    
    /**
     * Tests whether exception is thrown if we are using only means
     * and do not have enough epochs to guarantee determinacy of our solution.
     */
    public void testOptimizeMean()
    {
        SSA ssa = new SSA();
        SSAParameters par = new SSAParameters();
        Data data = new Data();
        
        par.setNumberOfStationarySources(2);
        par.setUseMean(true);
        par.setUseCovariance(false);
        
        data.setTimeSeries(SSAMatrix.zeros(4, 100), null);
        data.setNumberOfEqualSizeEpochs(2); // we would need at least 3 > 4 - 2 epochs
        data.epochize();
        
        boolean exceptionThrown = false;
        try
        {
            ssa.optimize(par, data);
        }
        catch(Exception e)
        {
            exceptionThrown = true;
        }
        
        assertTrue(exceptionThrown);
    }
    
    /**
     * Tests whether exception is thrown if we are using only means
     * and do not have enough epochs to guarantee determinacy of our solution.
     */
    public void testOptimizeCovariance()
    {
        SSA ssa = new SSA();
        SSAParameters par = new SSAParameters();
        Data data = new Data();
        
        par.setNumberOfStationarySources(2);
        par.setUseMean(false);
        par.setUseCovariance(true);
        
        data.setTimeSeries(SSAMatrix.zeros(4, 100), null);
        data.setNumberOfEqualSizeEpochs(2); // we would need at least 4 > 4 - 2 + 1 epochs
        
        boolean exceptionThrown = false;
        try
        {
            ssa.optimize(par, data);
        }
        catch(Exception e)
        {
            exceptionThrown = true;
        }
        
        assertTrue(exceptionThrown);
    }
    
    /**
     * A very simple test for the SSA algorithm.
     */
    public void testOptimize()
    {
        SSA ssa = new SSA();
        SSAParameters par = new SSAParameters();
        Data data = new Data();
        
        par.setNumberOfStationarySources(1);
        par.setUseMean(true);
        par.setUseCovariance(true);
        
        // generate random data set with fixed seed
        java.util.Random rand = new java.util.Random(23);
        int SAMPLES = 200; // has to be even
        SSAMatrix X = SSAMatrix.zeros(2, SAMPLES);
        for(int j = 0; j < SAMPLES; j++)
        {
            X.set(0, j, rand.nextGaussian());
            if(j < (SAMPLES/2))
            {
                X.set(1, j, rand.nextGaussian());
            }
            else
            {
                X.set(1, j, 10.0*rand.nextGaussian());
            }
        }
        
        data.setTimeSeries(X, null);
        data.setNumberOfEqualSizeEpochs(4);
        data.epochize();

        Results res = ssa.optimize(par, data);
        
        assertEquals(0.980, res.Ps.get(0, 0), 0.001);
        assertEquals(0.036, res.Ps.get(0, 1), 0.001);
    }
}
