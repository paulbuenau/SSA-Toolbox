function [est_Ps, est_Pn, est_As, est_An, loss, iterations, ssa_results] = ssa(X, d, reps, equal_epochs, use_mean, use_covariance)
%SSA Stationary Subspace Analysis
%usage 
%  [est_Ps, est_Pn, est_As, est_An, loss, iterations, ssa_results] 
%   = ssa(X, d, {reps: 5}, {equal_epochs: 10}, {use_mean: true}, {use_covariance: true})
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
%  equal_epochs   Optional: Number of equally sized epochs. Default: 10
%  use_mean       Optional: Set this to false to ignore changes in the mean
%                 (for example if your dataset ensures you that no changes
%                 in the mean occur). Default: true
%  use_covariance Optional: Set this to false to ignore changes in the
%                 covariance matrices. Default: true
%
%output
%  est_Ps         Projection matrix to stationary subspace (d x D)
%  est_Pn         Projection matrix to non-stationary subspace ((D-d) x D)
%  est_As         Basis of stationary subspace (D x d)
%  est_An         Basis of non-stationary subspace (D x (D-d))
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
if ~exist('equal_epochs', 'var') equal_epochs = 10; end
if ~exist('use_mean', 'var') use_mean = true; end
if ~exist('use_covariance', 'var') use_covariance = true; end

% detect whether to use equal or custom epochization
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
    ssadata.setUseCustomEpochDefinition(true);
else
    % epochize equally
    fprintf('No custom epochization found. Using equally sized epochs.\n');
    Xdm = ssatoolbox.SSAMatrix(X);
    ssadata.setTimeSeries(Xdm, []);
    ssadata.setNumberOfEqualSizeEpochs(equal_epochs);
end

ssadata.epochize;

% set SSA parameters
ssaparam.setNumberOfStationarySources(d);
ssaparam.setNumberOfRestarts(reps);
ssaparam.setUseMean(use_mean);
ssaparam.setUseCovariance(use_covariance);

% run optimization
fprintf('Starting optimization...\n\n');
try
ssaresult = ssaopt.optimize(ssaparam, ssadata);
catch % me
    %fprintf(me.getReport);
    disp(lasterror);
    return;
end

% return results
est_Ps = ssaresult.Ps.getArray;
est_Pn = ssaresult.Pn.getArray;
est_As = ssaresult.Bs.getArray;
est_An = ssaresult.Bn.getArray;
loss = ssaresult.loss;
iterations = ssaresult.iterations;

% ssa_results structure as described in the manual
ssa_results = struct;
ssa_results.est_Ps = est_Ps;
ssa_results.est_Pn = est_Pn;
ssa_results.est_As = est_As;
ssa_results.est_An = est_An;
if iscell(X)
    ssa_results.est_s_src = est_Ps * Xwoeps;
    ssa_results.est_n_src = est_Pn * Xwoeps;
else
    ssa_results.est_s_src = est_Ps * X;
    ssa_results.est_n_src = est_Pn * X;
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
