rm -rf latex html
doxygen
cd latex
make
cd ..
cp latex/refman.pdf .
rm -rf latex error.txt
