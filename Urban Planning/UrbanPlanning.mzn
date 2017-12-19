include "globals.mzn";
include "jacop.mzn";

% From file
int : n;
int : n_commercial;
int : n_residential;

array[0..n] of int : point_distribution;

% Model
array[0..n-1,0..n-1] of var 0..1: grid; % 0 for commercial, 1 for residential
array[0..n-1] of var -100..100: row_points;
array[0..n-1] of var -100..100: col_points;
var -100..100 : points;

array[0..n-1] of var 0..n_residential: nbr_res;
array[0..n-1] of var 0..n_commercial: nbr_com;

% Constraints

% Set the points
constraint forall(i in 0..n-1)(col_points[i]=point_distribution[count(col(grid,i),1)]);
constraint forall(i in 0..n-1)(row_points[i]=point_distribution[count(row(grid,i),1)]);

% Count the number of residential and commercial
constraint forall(i in 0..n-1)(nbr_res[i]=count(row(grid,i),1) /\ nbr_com[i]=count(row(grid,i),0));

% Restrict the number allowed of each
constraint sum(nbr_res)=n_residential;
constraint sum(nbr_com)=n_commercial;

% Sum the row and column points
constraint points = sum(row_points)+sum(col_points);

% Symmetry breaking constraint

%constraint lex2(grid); % From globals.mzn

% From jacop.mzn Faster than lex2 for some reason.
% For each column and row checks that the next is lexicographically larger.
constraint forall(i in 0..n-2, j in 0..n-1)
  (lex_less_int(col(grid,i),col(grid,i+1)) /\
   lex_less_int(row(grid,i),row(grid,i+1)) /\
   lex_lesseq_int(row(grid,j),col(grid,j))
  );
  
solve maximize points;

output["Points: ",show(points),"\nRow points: ",show(row_points),"\nColumn points: ",show(col_points),"\nGrid: \n"]
++
[
show(col(grid,i))++"\n" | i in 0..n-1
];