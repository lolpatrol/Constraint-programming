include "globals.mzn";
include "jacop.mzn";

% From file
int : del_add;
int : del_mul;
int : number_add;
int : number_mul;
int : n;
set of int : last;
set of int : add;
set of int : mul;
array[1..n] of set of int : dependencies;

% Setup
array[1..n] of int : ones = [1|i in 1..n];

int : max_cost = n*max(del_mul, del_add);

% General pattern: one array for all, one for add, one for mul
% because diff2 doesn't seem to work properly otherwise (mixes up the processors?)

array[1..n] of var 0..max_cost : start_time;
array[1..card(add)] of var 0..max_cost : start_time_add;
array[1..card(mul)] of var 0..max_cost : start_time_mul;

array[1..n] of var int : duration;
array[1..card(add)] of int : duration_add = [del_add|i in 1..card(add)];
array[1..card(mul)] of int : duration_mul = [del_mul|i in 1..card(mul)];

array[1..n] of var int : processor;
array[1..card(add)] of var 1..number_add : processor_add;
array[1..card(mul)] of var 1..number_mul : processor_mul;

var 1..number_add : n_add;
var 1..number_mul : n_mul;

var int : completionTime;

% Constraints

% Each node should finish before or at the same time as the next begins
constraint forall(i in 1..n, j in dependencies[i])(start_time[i] + duration[i] <= start_time[j]);

% Schedule the start times for add and mul based on their capacity
constraint diff2(start_time_add, processor_add, duration_add, [1|i in 1..card(add)]);
constraint diff2(start_time_mul, processor_mul, duration_mul, [1|i in 1..card(mul)]);

% Collect the start times of add and mul into one
constraint forall(i in 1..card(add))(start_time[add[i]]=start_time_add[i]);
constraint forall(i in 1..card(mul))(start_time[mul[i]]=start_time_mul[i]);

% Collect the durations of add and mul into one
constraint forall(i in 1..card(add))(duration[add[i]]=duration_add[i]);
constraint forall(i in 1..card(mul))(duration[mul[i]]=duration_mul[i]);

% % Collect the processing availability of add and mul into one
constraint forall(i in 1..card(add))(processor[add[i]]=processor_add[i]);
constraint forall(i in 1..card(mul))(processor[mul[i]]=processor_mul[i]);

% Collect the run time(s)
array[1..card(last)] of var int : last_ones;

constraint forall(i in 1..card(last)) (last_ones[i] = start_time[last[i]] + duration[last[i]]);

% Pick the longest run time.
constraint completionTime = max(last_ones);

% Search for solution to start times and processor availability
% and minimize completion time
solve :: seq_search([
    int_search(start_time, first_fail, indomain_min, complete),
    int_search(processor, first_fail, indomain_min, complete)
  ])
minimize completionTime;

output["Completion time: ",show(completionTime),"\n","Cycles: ",show(sum(duration)),"\n","Start times add: ",show(start_time_add),"\n","Start times mul: ",show(start_time_mul),"\n","Processor add: ",show(processor_add),"\n","Processor mul: ",show(processor_mul)];