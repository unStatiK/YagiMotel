![main-d6ea7587b682cdff205af4e3d15ded93](https://github.com/unStatiK/YagiMotel/assets/343531/a01883b4-dbb4-4ec3-9bc4-901413ff6109)

Platform for interconnection with automate game managment for mahjong-portal.

Written in pure Java and Akka library (https://akka.io/)

Architecture diagram
====================

![https://github.com/unStatiK/YagiMotel/assets/343531/a01883b4-dbb4-4ec3-9bc4-901413ff6109](https://github.com/unStatiK/YagiMotel/blob/main/arch.png)

Build and Run instruction
=========================

Linux instruction
-----------------

0. Install JDK 17+ (https://openjdk.org/install/)

1. Build YagiMotel
```
./gradlew clean build
```
2. Create bin folder
```
mkdir bin
```
3. Unzip distribution to bin folder
```
unzip build/distributions/YagiMotel.zip -d bin
```
4. Copy shell script with JVM OPTS
```
cp deploy/prod_export.sh bin/YagiMotel/bin/
```
5. Create config folder
```
mkdir bin/YagiMotel/bin/config
```
6. Copy config example to config folder
```
cp config/example.config.yaml bin/YagiMotel/bin/config/
```
7. Move to YagiMotel bin folder
```
cd bin/YagiMotel/bin/
```
8. Edit ```config/config.yaml```
9. Run YagiMotel bot!
```
. prod_export.sh && ./YagiMotel
```
