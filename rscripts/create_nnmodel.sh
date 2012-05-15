#!/bin/sh

rm *.pdf
R --no-save < nnmodel.r > nnmodel.txt

convert Rplots.pdf /tmp/junk1.png
convert Rplots1.pdf /tmp/junk2.png

montage -border 2 -geometry +2+2 /tmp/junk1.png /tmp/junk2.png nnmodel.png
rm *.pdf
