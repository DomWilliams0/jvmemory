#!/bin/sh

echo Generating proto js...
./gen_proto.sh


echo Running preprocessor...
(cd ../preprocessor && ./gradlew run)

# TODO start python server, which:
# serves files from src
# implements old EventsLoader
#	GET /threads -> counts threads
#	GET /threads/X -> gets thread X
#	GET /definitions -> defs
