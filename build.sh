#!/bin/sh
sbt -Denv=complete $@
sbt -Denv=fi-small $@
sbt -Denv=non-fi $@
sbt -Denv=small $@
sbt -Denv=fi $@
