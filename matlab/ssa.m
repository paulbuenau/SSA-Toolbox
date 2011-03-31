function [Ps, Pn, As, An, loss, iterations, ssa_results] = ssa(X, d, reps, equal_epochs, use_mean, use_covariance, matrix_library)
%SSA Stationary Subspace Analysis
%usage 
%  [Ps, Pn, As, An, loss, iterations, ssa_results] 
%   = ssa(X, d, {reps: 5}, {equal_epochs: 0}, {use_mean: true},
%         {use_covariance: true}, {matrix_library: 'colt'})
%
%input
%  <no input>     Show version information
%  X              Data in one of two possible formats:
%                  * D x n matrix with data in the columns
%                  * cell array where each X{i} is a D x n_i matrix
%                    and n_i the number of samples in epoch i
%  d              Dimensionality of stationary subspace
%  reps           Optional: Number of restarts (the one with the lowest
%                 objective function value is returned). Default: 5
%  equal_epochs   Optional: Number of equally sized epochs. equal_epochs=0 means, that
%                 the number of epochs is chosen by a heuristic. Default: 0 (chose by heuristic)
%  use_mean       Optional: Set this to false to ignore changes in the mean
%                 (for example if your dataset ensures you that no changes
%                 in the mean occur). Default: true
%  use_covariance Optional: Set this to false to ignore changes in the
%                 covariance matrices. Default: true
%  matrix_library matrix library to use. Has to be 'colt' or 'jblas'.
%                 Default: 'colt'
%
%output
%  Ps         Projection matrix to stationary subspace (d x D)
%  Pn         Projection matrix to non-stationary subspace ((D-d) x D)
%  As         Basis of stationary subspace (D x d)
%  An         Basis of non-stationary subspace (D x (D-d))
%  loss           Objective function value
%  iterations     Number of iterations until convergence
%  ssa_results    SSA results structure as described in the manual
%
%author
%  Jan Saputra Mueller, saputra@cs.tu-berlin.de
%
%license
%  This software is distributed under the BSD license. See COPYING for
%  details.

% Copyright (c) 2010, Jan Saputra Müller, Paul von Bünau, Frank C. Meinecke,
% Franz J. Kiraly and Klaus-Robert Müller.
% All rights reserved.
% 
% Redistribution and use in source and binary forms, with or without modification,
% are permitted provided that the following conditions are met:
% 
% * Redistributions of source code must retain the above copyright notice, this
% list of conditions and the following disclaimer.
% 
% * Redistributions in binary form must reproduce the above copyright notice, this
% list of conditions and the following disclaimer in the documentation and/or other
%  materials provided with the distribution.
% 
% * Neither the name of the Berlin Institute of Technology (Technische Universität
% Berlin) nor the names of its contributors may be used to endorse or promote
% products derived from this software without specific prior written permission.
% 
% THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
%  EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
% OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
% SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
% INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
% PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
% INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
% STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
% OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

% set dynamic java path to needed libraries
basedir = fileparts(mfilename('fullpath'));
javaclasspath({[basedir filesep 'ssa.jar']});

% instantiate classes
ssadata = ssatoolbox.Data;
ssaparam = ssatoolbox.SSAParameters;
ssaopt = ssatoolbox.SSA;
cl = ssatoolbox.ConsoleLogger;
ssaopt.setLogger(cl);
ssadata.setLogger(cl);

% no parameter?
if ~exist('X', 'var')
    version = ssadata.getClass.getPackage.getImplementationVersion;
    fprintf(['SSA Toolbox version ' char(version) '\n']);
    fprintf('This software is distributed under the BSD license. See COPYING for details.\n');
    return;
end

% set default parameters
if ~exist('reps', 'var') reps = 5; end
if ~exist('equal_epochs', 'var') equal_epochs = 0; end
if ~exist('use_mean', 'var') use_mean = true; end
if ~exist('use_covariance', 'var') use_covariance = true; end
if ~exist('matrix_library', 'var') matrix_library = 'colt'; end
    
if strcmp(matrix_library, 'colt')
    fprintf('Using Colt library...\n');
    ssatoolbox.SSAMatrix.setGlobalLib(ssatoolbox.SSAMatrix.COLT);
elseif strcmp(matrix_library, 'jblas')
    fprintf('Using jBlas library...\n');
    fprintf('If you get problems with jBlas, try the Colt library.\n');
    ssatoolbox.SSAMatrix.setGlobalLib(ssatoolbox.SSAMatrix.JBLAS);
else
    fprintf('Error: Unknown matrix library %s.\n', matrix_library);
    return;
end

% set SSA parameters
ssaparam.setNumberOfStationarySources(d);
ssaparam.setNumberOfRestarts(reps);
ssaparam.setUseMean(use_mean);
ssaparam.setUseCovariance(use_covariance);

% detect whether to use equal or custom epochization
try
 if iscell(X)
    % create custom epochization
    fprintf('Custom epochization found...\n');
    Xwoeps = [X{:}];
    Xdm = ssatoolbox.SSAMatrix(Xwoeps);
    ssadata.setTimeSeries(Xdm, []);
    epdef = zeros(1, size(X, 2));
    epochs = length(X);
    min_ep_size = Inf;
    last_pos = 1;
    for i=1:epochs
        ep_size = length(X{i});
        epdef(last_pos:(last_pos+ep_size-1)) = i*ones(1, ep_size);
        last_pos = last_pos + ep_size;
        if min_ep_size > ep_size
            min_ep_size = ep_size;
        end
    end
    fakefile = java.io.File('');
    ssadata.setCustomEpochDefinition(epdef, epochs, min_ep_size, fakefile);
    ssadata.setEpochType(ssadata.EPOCHS_CUSTOM);
 else
    % epochize equally
    fprintf('No custom epochization found. Using equally sized epochs.\n');
    Xdm = ssatoolbox.SSAMatrix(X);
    ssadata.setTimeSeries(Xdm, []);
    if equal_epochs == 0
        % use heuristic
        ssadata.setEpochType(ssadata.EPOCHS_EQUALLY_HEURISTIC);
    else
        ssadata.setNumberOfEqualSizeEpochs(equal_epochs);
        ssadata.setEpochType(ssadata.EPOCHS_EQUALLY);
    end
 end
catch
 e = lasterror;
 if ~isempty(strfind(e.message, 'OutOfMemoryError'))
     printJavaHeapSpaceError;
 else
     disp(e.message);
 end
 return;
end

% ssadata.epochize; (epochize() is now automatically called from
% optimize())

% run optimization
fprintf('Starting optimization...\n\n');
try
ssaresult = ssaopt.optimize(ssaparam, ssadata);
catch
 e = lasterror;
 if ~isempty(strfind(e.message, 'OutOfMemoryError'))
     printJavaHeapSpaceError;
 else
     disp(e.message);
 end
 return;
end

% return results
Ps = ssaresult.Ps.getArray;
Pn = ssaresult.Pn.getArray;
As = ssaresult.Bs.getArray;
An = ssaresult.Bn.getArray;
loss = ssaresult.loss;
iterations = ssaresult.iterations;

% ssa_results structure as described in the manual
ssa_results = struct;
ssa_results.Ps = Ps;
ssa_results.Pn = Pn;
ssa_results.As = As;
ssa_results.An = An;
if iscell(X)
    ssa_results.s_src = Ps * Xwoeps;
    ssa_results.n_src = Pn * Xwoeps;
else
    ssa_results.s_src = Ps * X;
    ssa_results.n_src = Pn * X;
end
parameters = struct;
parameters.input_file = '';
parameters.epoch_file = '';
parameters.no_s_src = d;
parameters.no_restarts = reps;
parameters.use_mean = use_mean;
parameters.use_covariance = use_covariance;
if iscell(X)
    parameters.eq_epochs = 0;
else
    parameters.eq_epochs = equal_epochs;
end
ssa_results.parameters = parameters;
ssa_results.description = ['SSA results (' datestr(now) ')'];

function printJavaHeapSpaceError
    fprintf('ERROR: Not enough Java heap space.\n');
    fprintf('To increase the Java heap space in Matlab, have a look at this website:\n\n');
    fprintf('http://www.mathworks.com/support/solutions/en/data/1-18I2C/\n\n');
    fprintf('In case you are using Matlab 2010a or later,\nthis can be easily done using Matlab''s preferences dialog.\n');
