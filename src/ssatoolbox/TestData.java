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

public class TestData extends TestCase
{
    /**
     * Tests whether exception is thrown if we have more epochs than data points.
     */
    public void testSetNumberOfEqualSizeEpochs()
    {
        Data data = new Data();
        data.setTimeSeries(SSAMatrix.zeros(2, 10), null);
        boolean exceptionThrown = false;
        try
        {
            data.setNumberOfEqualSizeEpochs(11);
        }
        catch(Exception e)
        {
            exceptionThrown = true;
        }

        assertTrue(exceptionThrown);
    }

    /**
     * Tests whether exception is thrown if we have more epochs than data points.
     */
    public void testSetCustomEpochDefinition()
    {
        Data data = new Data();
        data.setTimeSeries(SSAMatrix.zeros(2, 4), null);
        boolean exceptionThrown = false;
        try
        {
            data.setCustomEpochDefinition(new int[]{1, 2, 3, 4, 5}, 5, 1, null);
        }
        catch(Exception e)
        {
            exceptionThrown = true;
        }

        assertTrue(exceptionThrown);
    }
}
