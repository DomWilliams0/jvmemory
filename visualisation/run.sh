#!/bin/sh

echo Generating proto js...
./gen_proto.sh


#echo Running preprocessor...
#(cd ../preprocessor && ./gradlew run)
# use `-Pcached` when running vis server to preprocess separately

echo Starting visualisation server...
(cd ../visualisation-server && ./gradlew run)
