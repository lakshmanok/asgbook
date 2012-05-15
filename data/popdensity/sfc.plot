set terminal png medium
#set output "topo.png"
set xrange [2500:2600]
set yrange [1000:1100]
set pm3d
#splot "data.txt" matrix with pm3d

set output "topothresh.png"
set zrange [100000:400000]
splot (100000),"data.txt" matrix with pm3d
