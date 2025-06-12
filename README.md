# Android Volume Renderer

An Android app that can render 3D volumetric data such as the results of an MRI scan.

This project was created by *Jacob Serfaty* as his final project for the UChicago
computer science course CMSC 33710: Scientific Visualization taught by Professor
Gordon Kindleman. The rendering algorithm used in this project is the same as 
the algorithm taught in Project 3 of this course, though it is presented very
differently from the C code used in the course so it is more idiomatic to Kotlin.

The rendering code is located in the folder `/app/src/main/java/com/example/volumerenderer/Math.kt`. 
The UI of the app is rendered using *Jetpack Compose*.

One notable aspect of this code is that all vector/matrix math operations use a
**custom and well-optimized simple vector math "library"** contained in the file `Math.kt`. 
I opted to write my own vector/matrix math library after testing the renderer 
using the [Multik](https://github.com/Kotlin/multik) math library and observing
that the code was *unbearably* slow (rendering a simple example took over 30 minutes
compared to approximately 0.92 seconds in the C code). A performance profiler showed
that the primary bottleneck with the Multik code was unnecessary memory
allocations throughout the algorithm, so I opted to write a pure Kotlin math
library that prioritizes **efficient memory usage**. The same example using this
new library renders in approximately 1.3 seconds with multithreading and 3.3 seconds
without parallelism.
