#!/bin/bash

sudo ./target/universal/metasvc-2.0/bin/metasvc -Dconfig.resource=prod/application.conf -Dlogger.resource=prod/logback.xml
