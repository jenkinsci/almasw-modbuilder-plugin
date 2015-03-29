# almasw-modbuilder-plugin

[![Build Status](https://travis-ci.org/atejeda/almasw-modbuilder-plugin.svg?branch=master)](https://travis-ci.org/atejeda/almasw-modbuilder-plugin) 

An ACS/ALMASW module builder for Jenkins.

More info, check the [wiki](https://github.com/atejeda/almasw-modbuilder-plugin/wiki).

This plugin aims to build ALMASW modules, but can be used to build ACS software modules as well, feel free to use in any ACS based projects.

The plugin support:
   * build an ACS based module effortless
   * Choose the ACS version to use
   * Enable [CCACHE](https://ccache.samba.org/) (within workspace)
   * Makefile parallel jobs
   * No static
   * No IFR check
   * Add other projects/jobs as dependencies located at job id, artifact or workspace level.

Thanks to everyone who helped in the development of the several bash scripts, work who was the base of the development for this plugin.

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
