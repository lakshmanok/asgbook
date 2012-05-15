#!/bin/sh
#INFILE=orig/TOArads_g12_soivis_2009_0813_1800.cdf.gz
INFILE=/scratch2/lakshman/visnn/without_sun_angle/wrfout_d01_2009-08-13_19_00_00
OUT=sfcalbedo.txt
NCDUMP=ncdump

#VARNAME=sfcalb_b01
#VARNAME=IMGR_refl_b01
#VARNAME=altitude
VARNAME=ALBEDO

rm -f $OUT $OUT.gz
touch $OUT
echo "        ELLIPSOID   WGS-84" >> $OUT
echo "        PROJECTION  LAMBERT2SP" >> $OUT
for var in :TRUELAT1 :TRUELAT2 :CEN_LAT :CEN_LON :DX :DY; do 
    $NCDUMP -h $INFILE | grep $var | sed 's/[:=";f]//g' | sed 's/DX/DELTA_EW/g' | sed 's/DY/DELTA_NS/g' >> $OUT
done
$NCDUMP -h $INFILE | grep "south_north =" | sed 's/[=;]//g' | sed 's/south_north/NROWS/g' >> $OUT
$NCDUMP -h $INFILE | grep "west_east =" | sed 's/[=;]//g' | sed 's/west_east/NCOLS/g'  >> $OUT

$NCDUMP -l 100000 -v $VARNAME $INFILE | grep -v "[a-Z]" | sed 's/,/ /g' | sed 's/^ //g' >> $OUT
gzip $OUT
