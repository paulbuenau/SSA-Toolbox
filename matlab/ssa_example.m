function ssa_example

% fixed random seed
rand('seed', 23);
randn('seed', 34);

%%%%%%%%%%%%%%%%%%%%
% generate data set
%%%%%%%%%%%%%%%%%%%%
n = 500; % number of samples
n_changepoints = 5; % number of changepoints

% random change points
changepoints = sort(floor(n * rand(n_changepoints, 1)));

% generate stationary and non-stationary signal
X = zeros(2, n);
for i=1:(n_changepoints + 1)
    if i == 1
        X(:, 1:changepoints(1)) = mvnrnd([0, 3*randn], randcov, changepoints(1))';
    elseif i == (n_changepoints + 1)
        X(:, (changepoints(end) + 1):end) = mvnrnd([0, 3*randn], randcov, n - changepoints(end))';
    else
        X(:, (changepoints(i-1) + 1):changepoints(i)) = mvnrnd([0, 3*randn], randcov, changepoints(i) - changepoints(i-1))';
    end
end

% random mixing matrix
% random angles in [(1/8)*pi, (3/8)*pi]
alpha = (pi/4)*rand + (1/8)*pi;
beta = (pi/4)*rand + (1/8)*pi;
A = [cos(alpha) sin(alpha); cos(beta) sin(beta)] .* sign(randn(2,2));

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
%Xest_s = X(1, :);
%Xest_ns = X(2, :);

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
vw = [-10 10];
eps = n / 4; % epoch size
cbas = 'black'; % color for basis of s- and n-space
cebas = 'red'; % color for estimated basis of s- and n-space

subplot(5, 4, 9);
scatter(Xmixed(1, 1:eps), Xmixed(2, 1:eps), '.');
drawaxis(A(:,1), A(:,2), cbas, 10); % basis of s- and n-space
drawaxis(est_As, est_An, cebas, 10); % basis of estimated s- and n-space
set(gca, 'Xlim', vw, 'Ylim', vw, 'XTick', [], 'YTick', []);
title('scatter epoch 1');

subplot(5, 4, 10);
scatter(Xmixed(1, (eps + 1):(2*eps)), Xmixed(2, (eps + 1):(2*eps)), '.');
drawaxis(A(:,1), A(:,2), cbas, 10); % basis of s- and n-space
drawaxis(est_As, est_An, cebas, 10); % basis of estimated s- and n-space
set(gca, 'Xlim', vw, 'Ylim', vw, 'XTick', [], 'YTick', []);
title('scatter epoch 2');

subplot(5, 4, 11);
scatter(Xmixed(1, (2*eps + 1):(3*eps)), Xmixed(2, (2*eps + 1):(3*eps)), '.');
drawaxis(A(:,1), A(:,2), cbas, 10); % basis of s- and n-space
drawaxis(est_As, est_An, cebas, 10); % basis of estimated s- and n-space
set(gca, 'Xlim', vw, 'Ylim', vw, 'XTick', [], 'YTick', []);
title('scatter epoch 3');

subplot(5, 4, 12);
scatter(Xmixed(1, (3*eps + 1):end), Xmixed(2, (3*eps + 1):end), '.');
drawaxis(A(:,1), A(:,2), cbas, 10); % basis of s- and n-space
drawaxis(est_As, est_An, cebas, 10); % basis of estimated s- and n-space
set(gca, 'Xlim', vw, 'Ylim', vw, 'XTick', [], 'YTick', []);
title('scatter epoch 4');

% estimated stationary and non-stationary signal
subplot(5, 4, 13:16);
plot(Xest_s);
title('Estimated stationary signal');
subplot(5, 4, 17:20);
plot(Xest_ns);
title('Estimated non-stationary signal');

% random covariance matrix with a fixed stationary part
function C = randcov
sig = 1;
sc = 1;
m = sqrt(sig*sig/2);
A = [m m; sc*randn(1, 2)];
C = A*A';

function drawaxis(x, y, c, s)
h1 = line([0, s*x(1)], [0, s*x(2)]);
h2 = line([0, s*y(1)], [0, s*y(2)]);
set(h1, 'Color', c);
set(h2, 'Color', c);

function line_markers(x)
lim = get(gca,'YLim');
h = arrayfun(@(x) line([x x], lim), x);
set(h, 'Color', 'red');
