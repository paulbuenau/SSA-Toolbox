function ssa_example

%%%%%%%%%%%%%%%%%%%%
% generate data set
%%%%%%%%%%%%%%%%%%%%
n = 500; % number of samples
n_changepoints = 5; % number of changepoints

% random change points
changepoints = sort(floor(n * rand(n_changepoints, 1)));

% variances
sigmas = [2; 0.2; 2.5; 0.1; 2.3; 0.1];

% generate non-stationary signal
Xns = zeros(1, n);
for i=1:(n_changepoints + 1)
    if i == 1
        Xns(1:changepoints(1)) = sigmas(1) * randn(1, changepoints(1));
    elseif i == (n_changepoints + 1)
        Xns((changepoints(end) + 1):end) = sigmas(i) * randn(1, n - changepoints(end));
    else
        Xns((changepoints(i-1) + 1):changepoints(i)) = sigmas(i) * randn(1, changepoints(i) - changepoints(i-1));
    end
end

% generate stationary signal
Xs = randn(1, n);

X = [Xs; Xns];

% random mixing matrix
sc = 3;
A = randn(2, 2) .* [1, sc; 1, sc];

% mix signals
Xmixed = A * X;

%%%%%%%%%%%%%%%%%%%%
% run SSA
%%%%%%%%%%%%%%%%%%%%
% 1 stationary source, 5 repetitions, 4 equally-sized epochs
[est_Ps, est_Pn, est_As, est_An, loss, iterations, ssa_results] = ssa(Xmixed, 1, 5, 4);

% project to stationary and non-stationary subspace
Xest_s = est_Ps * Xmixed; 
Xest_ns = est_Pn * Xmixed;
%Xest_s = Xs;
%Xest_ns = Xns;

%%%%%%%%%%%%%%%%%%%%
% plot signals
%%%%%%%%%%%%%%%%%%%%
figure;

markers = (n/4):(n/4):((3/4)*n);
% first signal
subplot(5, 4, 1:4);
plot(Xmixed(1, :));
title('Input signal 1');
line_markers(markers);

% second signal
subplot(5, 4, 5:8);
plot(Xmixed(2, :));
title('Input signal 2');
line_markers(markers);

% scatter plots of the epochs
eps = n / 4; % epoch size
subplot(5, 4, 9);
scatter(Xmixed(1, 1:eps), Xmixed(2, 1:eps));
title('scatter epoch 1');
subplot(5, 4, 10);
scatter(Xmixed(1, (eps + 1):(2*eps)), Xmixed(2, (eps + 1):(2*eps)));
title('scatter epoch 2');
subplot(5, 4, 11);
scatter(Xmixed(1, (2*eps + 1):(3*eps)), Xmixed(2, (2*eps + 1):(3*eps)));
title('scatter epoch 3');
subplot(5, 4, 12);
scatter(Xmixed(1, (3*eps + 1):end), Xmixed(2, (3*eps + 1):end));
title('scatter epoch 4');

% estimated stationary and non-stationary signal
subplot(5, 4, 13:16);
plot(Xest_s);
title('Estimated stationary signal');
subplot(5, 4, 17:20);
plot(Xest_ns);
title('Estimated non-stationary signal');

function line_markers(x)
lim = get(gca,'YLim');
h = arrayfun(@(x) line([x x], lim, 'r'), x);
% set color?
