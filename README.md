# pdf-to-json #

A tool to convert PDF documents to JSON.

## Impetus ##

Even when they attempt to preserve layout, existing PDF-to-text converters tend to lose structure, especially when dealing with overlapping text. This tool attempts to address that problem by capturing individual fields as elements in a line. The original layout can be recreated with the original coordinates that accompany each line and element.

## Output ##

The top level of the output JSON is an array of objects representing lines. Each line includes top and bottom coordinates (floats) and an array of elements. Each element is an object that represents a text fields and contains left and right coordinates (floats) and a value (string).

## Building ##

The project is designed to be built and managed using [SBT](http://www.scala-sbt.org/) or [activator](https://www.typesafe.com/community/core-tools/activator-and-sbt).

The project can be compiled with

    sbt compile
    
An executable jar can be created with

    sbt assembly

## Running ##

The tool takes a single argument, the path to the PDF file. The program can be run in a number of ways, including interactively from within SBT or using the executable jar. To run from the command line using SBT, you can use something like

    sbt "run '/path/to/file.pdf'"