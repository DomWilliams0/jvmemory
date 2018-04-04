#!/bin/sh

# using this script standalone will use $PWD, but using the Makefile will substitute
# this for the true install dir
INSTALL_DIR=$PWD
DIR=$INSTALL_DIR

echo Opening browser
xdg-open $DIR/visualisation/vis.html
