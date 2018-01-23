#!/bin/sh

#echo Running preprocessor...
#(cd ../preprocessor && ./gradlew run)
# use `-Pcached` when running vis server to preprocess separately

echo Starting visualisation server...
(cd ../visualisation-server && ./gradlew buildJar)
java -jar ../visualisation-server/build/libs/visualisation-server-0.1.jar
