#!/bin/bash

BUILD=$1

if [ -z "$BUILD" ]; then
	BUILD=pkg
fi

#
# For test, coverage, clitests, need to spin up docker
#
# docker-compose up -d --build
#
# after the test, it can be shutdown with
# docker-compose rm -s
#

case "$BUILD" in
	doc|docs)
		cd docs_src
		make docs
		;;
	pkg)
		mvn clean package -Dmaven.test.skip=true && cp jaqy-avro/target/jaqy-avro*.jar jaqy-s3/target/jaqy-s3*.jar jaqy-azure/target/jaqy-azure*.jar dist
		;;
	test)
		mvn clean package
		;;
	clitest)
		# Jaqy based tests
		#
		# Need to run after ./make.sh pkg build all the jars
		tests/bin/testall.sh
		;;
	coverage)
		mvn clean clover:setup package && tests/bin/testall.sh -c && mvn clover:aggregate clover:clover && rm -rf coverage && mv target/site/clover coverage
		;;
esac
