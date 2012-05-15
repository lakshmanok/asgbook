#!/bin/sh
R --no-save < clustering.r > clustering.txt
montage  -border 2 -geometry +2+2  cluster1.png cluster2.png intracluster.png
rm cluster1.png cluster2.png
