# Please note: 

If you are having a "package cannot be resolved" error for either: 
    cache.java
    client.java

Within the directory where it is giving a package problem, you need to compile the code together. 

You can do this with the following command: 

javac -d classes <path to the .java file for package you are trying to import> <Java file you are trying to compile>

Then to run the code: 

java -cp classes <java file you are trying to run> <command line arguments>