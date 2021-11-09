# Maven Project Importer

This project contains a setup for using M2Eclipse to programmatically convert Maven projects to Eclipse projects.
It is created with the sole purpose of debugging M2E.

## How to use

Follow these steps:

1. Download and install Eclipse for RCP Developers 2021-06. 
2. Import the parent project as an Existing Maven Project. Afterwards, import 'minimal-m2e-maven-plugin' project too.
3. Run `mvn clean verify` in the parent project root. 
4. Create a file *manifest.standalone* containing the list (one per line) of all the plugin names generated in *releng/target/repository/plugins*. 
 In Linux, this can be achieved by running the following in the parent project root folder:
```shell
 ls -1a  releng/target/repository/plugins | egrep "^[a-zA-Z]" > manifest.standalone

``` 
 
5. Copy all the entries in the *releng/target/repository/plugins* and the `manifest.standalone` to *minimal-m2e-maven-plugin/src/main/resources*. 
6. Change to  *minimal-m2e-maven-plugin/* and run `mvn clean install`.  

Now, minimal-m2e maven plugin is ready to be used. Import the plugin into any maven project, e.g., [minimal-project](https://github.com/Splendit/minimal-project):

```xml

<build>
    <plugins>
        <plugin>
                <groupId>at.splendit</groupId>
                <artifactId>minimal-m2e-maven-plugin</artifactId>
                <version>1.0.0-SNAPSHOT</version>
        </plugin>
    </plugins>
</build>
```

Run `mvnDebug minimal-m2e:import` to start debugging. 

* Maven 3.6.0+
* Java 11
