# COMP 4321 Phase 1 README
This readme serves as documentation, which includes instructions on how to build the project and the database schema of the indexer.

## Project Setup
The project uses gradle and java 11. Gradle takes care of bundling all the dependecies, including rocksdb and jsoup.

__IMPORTANT__: Gradle and Java 11 needs to be installed for the project to work. The default Java version in the VM is Java 8. Without the correct Java version and gradle, the project will not be able to build successfully. As such, please install JDK 11 and gradle to run the project.

### Installation of Gradle

Install Gradle 7.4

    wget https://services.gradle.org/distributions/gradle-7.4-bin.zip 

Next we unzip to opt/gradle,

    unzip -d /opt/gradle gradle-7.4-bin.zip

After unzipping, we have to set the environment variables, by editing /etc/profile.d/gradle.sh and inserting this into the file

    export GRADLE_HOME=/opt/gradle/gradle-7.4
    export PATH=${GRADLE_HOME}/bin:${PATH}

Verify installation of gradle by

    gradle -v

After successfully installing gradle, the project can be run by

    ./gradlew run

## Database Schema
Attached is a diagram for the database schema. The schema is represented loosely in UML form.
![](https://i.imgur.com/m2rWFOp.png)

Implementation wise, there are 5 tables, each corresponding to rocksdb instance. The tables are Word_Table, Page_Table, ForwardFrequency_Table, InvertedFrequency_table, and PageInfo_Table.

### Word_Table
Mapping Table between Words and their ids

### Page_Table
Mapping Table between Pages and their ids

### ForwardFrequency_Table
Contains postinglists for each Word, indexed by WordID, obtained from Word_Table

### InvertedFrequency_Table
Contains postinglists for each Word, indexed by WordID, obtained from Word_Table.

### PageInfo_Table
Contains meta information of the individual pages. This includes page title, URL, last modified date, page size and child links.

## Output
The output of the scraped webpages are put in spider_result.txt, which is structured in a similar fashion to the format proposed in the course webpage:

![](https://i.imgur.com/y79QODV.png)
