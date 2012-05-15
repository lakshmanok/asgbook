#!/bin/sh


# get data
#rm *.tar; wget http://www.ngdc.noaa.gov/dmsp/data/web_data/v4composites/F182010.v4.tar
#tar xvf *.tar
#gunzip F182010.v4c_web.stable_lights.tif.gz
#rm *.gz
#rm *.tfw

# covert to grayscale image and scale it to match popdensity resolution
#convert *.tif lights1.pgm
#pamscale -reduce 5 -filter gauss lights1.pgm > lights2.pgm

pamcut -bottom=3192 lights2.pgm | pnmpad -plain -top=239 -black > lights3.pgm

# add ESRI header
echo "ncols         8640" > lights.txt
echo "nrows         3432" >> lights.txt
echo "xllcorner     -180" >> lights.txt
echo "yllcorner     -58" >> lights.txt
echo "cellsize      0.0416666666667" >> lights.txt
echo "NODATA_value  -9999" >> lights.txt

NLINES=`wc lights3.pgm | awk '{print $1}'`
NLINES=`echo "$NLINES - 3" | bc`
echo "Catting the last $NLINES as data ..."
tail -n $NLINES lights3.pgm >> lights.txt
gzip lights.txt

mv lights.txt.gz nighttimelights.txt.gz
