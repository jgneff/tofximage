This project compares the performance of various methods for converting an AWT image to a JavaFX image, including the public utility method [`SwingFXUtils.toFXImage`](src/main/java/javafx/embed/swing/SwingFXUtils.java).
Although some of the methods convert the alpha values incorrectly, they are included in the tests because their conversion is correct when the source AWT image contains no transparent pixels.

## Results

The results of running the benchmarks on my systems are published on the [website associated with this repository](https://jgneff.github.io/tofximage/).

## Licenses

This project is licensed under the [GNU General Public License v3.0](LICENSE) except for the following file, which is licensed by Oracle under the [GNU General Public License v2.0](src/main/java/javafx/embed/swing/LICENSE) with the [Classpath Exception](src/main/java/javafx/embed/swing/ADDITIONAL_LICENSE_INFO):

* [SwingFXUtils.java](src/main/java/javafx/embed/swing/SwingFXUtils.java)

The contents of the [website](https://jgneff.github.io/tofximage/) and the file [doll-dancing.gif](src/main/resources/doll-dancing.gif) are licensed under the [Creative Commons Attribution-ShareAlike 4.0 International License](https://creativecommons.org/licenses/by-sa/4.0/).
The website style is based on [Water.css](https://github.com/kognise/water.css).

## Building

This is a Maven project that depends on the [Java Microbenchmark Harness](https://openjdk.java.net/projects/code-tools/jmh/).
You can build and package the application as the file *target/benchmarks.jar* with the commands:

```console
$ export JAVA_HOME=$HOME/opt/jdk-14.0.1
$ mvn package
```

## Running

Run a quick test with a command like the following:

```ShellSession
$ java -Djava.library.path=$HOME/lib/javafx-sdk-15/lib \
    -jar target/benchmarks.jar -f 1 -i 1 -wi 1
```

Run the benchmarks with their default options for a more thorough test:

```ShellSession
$ java -Djava.library.path=$HOME/lib/javafx-sdk-15/lib \
    -jar target/benchmarks.jar
```

The `-h` option prints a description of all benchmark command options:

```ShellSession
$ java -jar target/benchmarks.jar -h
```
