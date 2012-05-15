#!/bin/sh

DATA="montage -geometry +0+0 -tile 2x5 -border 2 "
ALIGNED="montage  -geometry +2+2 -tile 3x5 -border 2 "
for SCALE in 4 3 2 1 0; do
   DATA="$DATA  pxdata0_${SCALE}.png pxdata1_${SCALE}.png"

   RES=`echo "$SCALE - 1" | bc`
   ALIGNED="$ALIGNED pxu_${SCALE}.png pxv_${SCALE}.png pxaligned_${RES}.png"
done
DATA="$DATA  pyramiddata.png"
ALIGNED="$ALIGNED  pyramidresult.png"

echo $DATA
`$DATA`
echo $ALIGNED
`$ALIGNED`
