Demonstration-Toolbox
=====================

This Atlas Demonstration Toolbox is meant to provide an interesting sample of the many applications of Atlas. To use the Demonstration Toolbox, you must have a copy of Eclipse with Atlas installed. See http://www.ensoftcorp.com/atlas/.

The Demonstration Toolbox is organized as follows:

resources/ Example Java and Android projects to import and analyze.
src/       Pre-written Atlas analysis scripts


EXAMPLE ANALYSIS SCRIPTS

src/com.ensoftcorp.atlas.java.demo.comprehension.ComprehensionUtils
Atlas scripts which are intended to provide general comprehension of large software. Provides call graphs, type hierarchies, data flow graphs, and more. Intended for demonstration with ConnectBot, a FOSS Android App which is large and difficult to understand.

src/com.ensoftcorp.atlas.java.demo.synchronization.RaceCheck
Atlas scripts which detect unsynchronized read/write access of specified shared objects. Intended for demonstration with Synchronization-Example, a multi-threaded producer/consumer Java application.

src/com.ensoftcorp.atlas.java.demo.apicompliance.APICompliance
Atlas scripts which detect violations of expected API usage patterns. In particular, the provided script checks for violations of the expected pattern of Android's MediaRecorder API for recording audio. Intended for demonstration with Android-Audio-Recording-Example, a simple Android app which records audio. 


ADDITIONAL INFORMATION

The analysis use cases presented by the Demonstration Toolbox are only the tip of the iceberg of what Atlas can do. As an analogy, Atlas provides lumber and tools, and it's up to the user to construct an interesting building. We hope we have provided a taste of what is possible. 

http://www.ensoftcorp.com/atlas/
corporate@ensoftcorp.com

