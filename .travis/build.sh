#!/bin/sh
curl -fsLO https://raw.githubusercontent.com/scijava/scijava-scripts/master/travis-build.sh
sh travis-build.sh $encrypted_f8686d8bf72e_key $encrypted_f8686d8bf72e_iv
