function [est_Ps, est_Pn, est_As, est_An, loss, iterations, ssa_results] = ssa(X, d, reps, equal_epochs, use_mean, use_covariance)
%SSA Stationary Subspace Analysis
%usage 
%  [est_Ps, est_Pn, est_As, est_An, loss, iterations, ssa_results] 
%   = ssa(X, d, {reps: 5}, {equal_epochs: 10}, {use_mean: true}, {use_covariance: true})
%
%input
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

% set dynamic java path to needed libraries
basedir = fileparts(mfilename('fullpath'));
javaclasspath({[basedir filesep 'lib' filesep 'jblas-1.0.1.jar'], [basedir filesep 'ssa.jar']});

% set default parameters
if ~exist('reps', 'var') reps = 5; end
if ~exist('equal_epochs', 'var') equal_epochs = 10; end
if ~exist('use_mean', 'var') use_mean = true; end
if ~exist('use_covariance', 'var') use_covariance = true; end
    
% instantiate classes
ssadata = ssatoolbox.Data;
ssaparam = ssatoolbox.SSAParameters;
ssaopt = ssatoolbox.SSA;
cl = ssatoolbox.ConsoleLogger;
ssaopt.setLogger(cl);

% detect whether to use equal or custom epochization
if iscell(X)
    % create custom epochization
    fprintf('Custom epochization found...\n');
    Xwoeps = [X{:}];
    Xdm = org.jblas.DoubleMatrix(Xwoeps);
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
    fprintf('No custom epochization found. Using equal sized epochs.\n');
    Xdm = org.jblas.DoubleMatrix(X);
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
ssaresult = ssaopt.optimize(ssaparam, ssadata);

% return results
est_Ps = ssaresult.Ps.toArray2;
est_Pn = ssaresult.Pn.toArray2;
est_As = ssaresult.Bs.toArray2;
est_An = ssaresult.Bn.toArray2;
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
