%RWF=/scratch/Gau-12-ethanediol/,32GB
%Nosave
%Chk=12-ethanediol-opt-5-1-2-6-163.chk
%Mem=389242KB
%Nproc=1
#HF/6-31G* Opt=ModRed MaxDisk=32GB

12-ethanediol Rotatable Bond Optimization on node9.bme.utexas.edu

0 1
 C    0.403141    0.647792    0.260116
 C   -0.403141   -0.647792    0.260116
 H    0.072160    2.484222   -0.264829
 H   -0.072160   -2.484222   -0.264829
 O   -0.403141    1.665751   -0.275974
 O    0.403141   -1.665751   -0.275974
 H    0.730658    0.904314    1.264420
 H    1.284821    0.489869   -0.352497
 H   -0.730658   -0.904314    1.264420
 H   -1.284821   -0.489869   -0.352497

5 1 2 6     162.64 F

