# almasw-modbuilder-plugin

[![Build Status](https://travis-ci.org/atejeda/almasw-modbuilder-plugin.svg?branch=master)](https://travis-ci.org/atejeda/almasw-modbuilder-plugin) 

An ACS/ALMASW module builder for Jenkins.

This plugin aims to build ALMASW modules, but can be used to build ACS software modules as well, feel free to use in ACS based projects as well.

JDKs supported:
   * oraclejdk8
   * oraclejdk7
   * openjdk7

## ALMASW

Basically ALMASW is a set of software modules to control the [ALMA](http://en.wikipedia.org/wiki/Atacama_Large_Millimeter_Array) telescope instruments and manage the data produced by the telescope. These modules are built on top of ACS, a LGPL software framework/infrastructure which provides common CORBA-based services such as logging, error and alarm management, configuration database and lifecycle management in a container-model fashion. 

* [Github ACS-community](https://github.com/ACS-Community/ACS)
* [ESO ACS](http://www.eso.org/projects/alma/develop/acs/)

## Disclaimer

Even though that the name is almasw, this is a personal project developed during weekends. Support and bug fixing might be slow due free time schedule.

## License

All the code in this repository is licensed under [GPLv3](https://www.gnu.org/copyleft/gpl.html).
