#!/bin/sh

R --no-save < treemodel.r > treemodel.txt

convert -rotate 90 treemodel1.ps /tmp/junk1.png
convert -rotate 90 treemodel2.ps /tmp/junk2.png

montage -border 2 -geometry +2+2 -tile 2x2 -resize 841x595\! treemodel1_cv.png /tmp/junk1.png /tmp/junk2.png treemodel.png
rm *.ps
rm treemodel?_cv.png
