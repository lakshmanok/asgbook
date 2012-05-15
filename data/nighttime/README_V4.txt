Version 4 DMSP-OLS Nighttime Lights Time Series
 
The files are cloud-free composites made using all the available
archived DMSP-OLS smooth resolution data for calendar years. In cases
where two satellites were collecting data - two composites were produced.
The products are 30 arc second grids, spanning -180 to 180 degrees
longitude and -65 to 75 degrees latitude. A number of constraints are
used to select the highest quality data for entry into the composites:
 
  * Data are from the center half of the 3000 km wide OLS swaths. 
    Lights in the center half have better geolocation, are smaller, 
    and have more consistent radiometry.
 
  * Sunlit data are excluded based on the solar elevation angle.
 
  * Glare is excluded based on solar elevation angle.
 
  * Moonlit data are excluded based on a calculation of lunar 
    illuminance.

  * Observations with clouds are excluded based on clouds 
    identified with the OLS thermal band data and NCEP surface 
    temperature grids.
 
  * Lighting features from the aurora have been excluded in the 
    northern hemisphere on an orbit-by-orbit manner using visual 
    inspection.

Each composite set is named with the satellite and the year (F121995 is
from DMSP satellite number F12 for the year 1995). Three image types are
available as geotiffs for download from the version 4 composites:
 

    F1?YYYY_v4c_cf_cvg.tif: Cloud-free coverages tally the total 
    number of observations that went into each 30 arc second grid cell. This
    image can be used to identify areas with low numbers of observations
    where the quality is reduced. In some years there are areas with zero
    cloud- free observations in certain locations.
 

    F1?YYYY_v4c_avg_vis.tif: Raw avg_vis contains the average of the 
    visible band digital number values with no further filtering. Data
    values range from 0-63. Areas with zero cloud-free observations are
    represented by the value 255.
 

    F1?YYYY_v4c_stable_lights.avg_vis.tif: The cleaned up avg_vis 
    contains the lights from cities, towns, and other sites with persistent
    lighting, including gas flares. Ephemeral events, such as fires have
    been discarded. Then the background noise was identified and replaced
    with values of zero. Data values range from 1-63. Areas with zero
    cloud-free observations are represented by the value 255.
 

NOTE:  The OLS has no on-board calibration and the gain settings are not
recorded in the data stream.  While the time-series of annual cloud-free
composites were produced using the same algorithms and a stringent data
selection criteria, the digital number (DN) values are not strictly
comparable from one year to the next.  We recommend users perform an
intercalibration prior to direct comparison of the DN values across the
time series.  For discussion on these points contact chris.elvidge@noaa.gov.


Global composities are available for the satellite years below:

    F101992
    F101993
    F101994
    F121994
    F121995
    F121996
    F121997
    F121998
    F121999
    F141997
    F141998
    F141999
    F142000
    F142001
    F142002
    F142003
    F152000
    F152001
    F152002
    F152003
    F152004
    F152005
    F152006
    F152007
    F152008
    F162004
    F162005
    F162006
    F162007
    F162008
    F162009
    F182010
 
 --------------------------------------------------------------------
 
 Each tar ball contains the raw average visible band, cleaned up 
 average visible band, cloud free coverage data, and a readme file. 
 The data files have been compressed with gzip.

 For information on tar and gzip see our link located at:
 http://www.ngdc.noaa.gov/dmsp/tar_zip.html
 
 --------------------------------------------------------------------
 
 Whenever using or distributing DMSP data or derived images, use the
 following credit:
 
 Image and data processing by NOAA's National Geophysical Data Center.
 DMSP data collected by US Air Force Weather Agency.
 
 --------------------------------------------------------------------
 
 ngdc.dmsp@noaa.gov

 National Geophysical Data Center
 E/GC 325 Broadway
 Boulder, Colorado USA 80305-3328
 
 Fax: 303-497-6513

 --------------------------------------------------------------------

