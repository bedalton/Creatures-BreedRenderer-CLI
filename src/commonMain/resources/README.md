# Render Breed CLI tool

This is a Command line tool to render a preview of a norn

## Do Not Glob
Glob/Wildcards simply do not work in Windows, and neither do `./` prefixed paths 

**Best not to glob at all**

Avoid globbing the images directory especially with `./Images/*` or even `./images/[a-q][0-7][0-7][a-zA-Z].c16`.  
In C3DS these two patterns can be thousands of files. It is faster to simply use `./Images/`.
Using a less specific glob will slow down the CLI app to a crawl, 
taking several magnitudes longer, max out your CPUs and basically lead to a bad experience.  
If you must Glob, be as specific as possible, i.e.`./images/[a-q][04][0-7]m.c16`. 
Note when globbing a path for some females, it is sometimes required to include 
the male genus as well for breeds where the females use the male ATTs


## Usage

**Type `render-breed.exe --help` for arguments and options**

###Normal Render

Render a typical ron norn  

Command: ```
render-breed.exe C1 ron-norn-m.png--age 3 --gender m --breed n:6 --scale 2 --pose 312222222111110 ./Images ./Body^ Data

```
