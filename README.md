Owl Platform Sensor-Aggregator Library for Java
===============================================

Version 1.0.5

Last updated April 12, 2014

Project Website: <https://github.com/OwlPlatform/java-owl-sensor>

Copyright (C) 2012-2014 Robert Moore and the Owl Platform

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Library General Public
License as published by the Free Software Foundation; either
version 2 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Library General Public License for more details.

You should have received a copy of the GNU Library General Public
License along with this library; if not, write to the
Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
Boston, MA  02110-1301, USA.

## About ##
The Sensor-Aggregator library is an implementation of the GRAIL
Sensor-Aggregator network protocol for the Java language.  It provides the
ability for a Java application to act as a sensor for the Owl Platform, with
the primary objective of forwarding a stream of Sample messages to the
aggregator.  It fully implements all required and optional components of the
network protocol.

## Dependencies ##
This library depends on the following libraries:
* [Owl Common Library version 1.0.3] (https://github.com/OwlPlatform/java-owl-common/downloads 
  "Owl Common Library Downloads")
* [Apache Mina Core version 2.0.7](http://mina.apache.org/downloads.html
  "Apache Mina Downloads")
* [SLF4J API version 1.7.2](http://www.slf4j.org/download.html "SLF4J
  Downloads")

Optionally, you can provide a binding for the SLF4J facade.  If you don't want
any logging, you can use the No-Op binding, or see the [SLF4J
website](http://www.slf4j.org "Simple Logging Facade for Java") for more
options. The most common bindings are all available in the same download
archive.

## Compiling ##
This library should be compiled using the Apache Maven project management
tool.  The project is currently compatible with Apache Maven version 3,
which can be downloaded for free at <http://maven.apache.org/>.  To build
the static JAR file output, the following command should be run from the
project root (where the pom.xml file is located):

    mvn clean package -U

If everything compiles correctly, then near the end of the Maven output,
this line should appear:

    [INFO] BUILD SUCCESS

In this case, the JAR file will be located in the ``target'' subdirectory.
If not, please visit the project website listed at the top of this
document for support.

