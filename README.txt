(1) Unzip and place the software somewhere. Let's say you have it in 
INSTALL_DIR/autospatialgrids/lakshmanok-asgbook-xyz

(2) Do a web search and install the following software on your computer:
  (2.1) Java 1.6.0_18 or higher
  (2.2) Eclipse 3.5 or higher     [Optional]
  (2.3) Apache Ant 1.6 or higher  [Optional]

From Eclipse:
  (1) Launch Eclipse.  Set its workspace to be $INSTALL_DIR/autospatialgrids
  (2) In Eclipse, File | New | Project | Java Project from Ant build file
        Browse to and select $INSTALL_DIR/autospatialgrids/lakshmanok-asgbook-xyz/build.xml
  (3) Now you can right-click on any file with a "main" method and run it
      as a Java application

From Ant (on command-line):
  (1) Compile the software by typing:
             ant build
  (2) Run any class with a "main" method as follows (shown for the KalmanFilter as an example):
       java -cp build:lib/Jama-1.0.2.jar edu.ou.asgbook.motion.KalmanFilter
