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
sp_ho = 0.1;
sp_ve = 0.05;
s_he = 0.18;
s_sc = 0.02;

markers = (n/4):(n/4):((3/4)*n);
% first signal
axes('position', [sp_ho, 1 - sp_ve - s_he, 1 - 2*sp_ho, s_he]);
plot(Xmixed(1, :));
set(gca, 'XTick', [], 'YTick', []);
ylabel('Signal 1');
line_markers(markers);

% second signal
axes('position', [sp_ho, 1 - sp_ve - 2*s_he, 1 - 2*sp_ho, s_he]);
plot(Xmixed(2, :));
set(gca, 'XTick', [], 'YTick', []);
ylabel('Signal 2');
line_markers(markers);

% scatter plots of the epochs
vw = [-5 5];
eps = n / 4; % epoch size
cbas = 'black'; % color for basis of s- and n-space
cebas = 'red'; % color for estimated basis of s- and n-space

axes('position', [sp_ho + s_sc, 1 - sp_ve - 3*s_he + s_sc, (1 - 2*sp_ho)/4 - 2*s_sc, s_he - 2*s_sc]);
scatter(Xest_s(1:eps), Xest_ns(1:eps), '.');
box on;
axis equal;
axis square;
set(gca, 'Xlim', vw, 'Ylim', vw, 'XTick', [], 'YTick', []);
xlabel('est 1');
ylabel('est 2');

axes('position', [sp_ho + s_sc + (1 - 2*sp_ho)/4, 1 - sp_ve - 3*s_he + s_sc, (1 - 2*sp_ho)/4 - 2*s_sc, s_he - 2*s_sc]);
scatter(Xest_s((eps + 1):(2*eps)), Xest_ns((eps + 1):(2*eps)), '.');
box on;
axis equal;
axis square;
set(gca, 'Xlim', vw, 'Ylim', vw, 'XTick', [], 'YTick', []);
xlabel('est 1');
ylabel('est 2');

axes('position', [sp_ho + s_sc + 2*(1 - 2*sp_ho)/4, 1 - sp_ve - 3*s_he + s_sc, (1 - 2*sp_ho)/4 - 2*s_sc, s_he - 2*s_sc]);
scatter(Xest_s((2*eps + 1):(3*eps)), Xest_ns((2*eps + 1):(3*eps)), '.');
box on;
axis equal;
axis square;
set(gca, 'Xlim', vw, 'Ylim', vw, 'XTick', [], 'YTick', []);
xlabel('est 1');
ylabel('est 2');

axes('position', [sp_ho + s_sc + 3*(1 - 2*sp_ho)/4, 1 - sp_ve - 3*s_he + s_sc, (1 - 2*sp_ho)/4 - 2*s_sc, s_he - 2*s_sc]);
scatter(Xest_s((3*eps + 1):end), Xest_ns((3*eps + 1):end), '.');
box on;
axis equal;
axis square;
set(gca, 'Xlim', vw, 'Ylim', vw, 'XTick', [], 'YTick', []);
xlabel('est 1');
ylabel('est 2');

% estimated stationary and non-stationary signal
axes('position', [sp_ho, 1 - sp_ve - 4*s_he, 1 - 2*sp_ho, s_he]);
plot(Xest_s);
set(gca, 'XTick', [], 'YTick', []);
ylabel('est s-src');

axes('position', [sp_ho, 1 - sp_ve - 5*s_he, 1 - 2*sp_ho, s_he]);
plot(Xest_ns);
set(gca, 'XTick', [], 'YTick', []);
ylabel('est n-src');

% plot(Xest_s);
% title('Estimated stationary signal');
% subplot(5, 4, 17:20);
% plot(Xest_ns);
% title('Estimated non-stationary signal');

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
