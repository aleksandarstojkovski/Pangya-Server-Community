# PangyaAPI.IFF.GB

| [![Nuget](https://img.shields.io/nuget/v/PangyaAPI.IFF.GB.svg)](https://www.nuget.org/packages/PangyaAPI.IFF.GB/) | [![Nuget](https://img.shields.io/nuget/dt/PangyaAPI.IFF.GB.svg)](https://www.nuget.org/packages/PangyaAPI.IFF.GB/) | [Issues](https://github.com/pangyatools/PangLib/labels/PangyaAPI.IFF.GB) |
| ------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------- |  

Library to handle and parse data from Pangya metadata and item (`.iff`) files.

## Installation

You can download this package on NuGet using the .NET CLI

```
dotnet add package PangyaAPI.IFF.GB
```

## Usage

```cs
using System.IO;
using PangyaAPI.IFF.GB;
using PangyaAPI.IFF.GB.DataModels;

// Create new instance
IFFFile<Caddie> IFF = new IFFFile<Caddie>();

// If you create an empty instance, don't forget to set both Version and Binding properties which are
// required for the IFF files to be interpreted by the game properly!
 
// or load file into a new instance
IFFFile<Caddie> IFF = new IFFFile<Caddie>(File.Open("./Caddie.iff", FileMode.Open));

// You can now access the IFF file entries on IFF.Entries
// IFF.Entries is a List<T> of the type you pass to the IFFFile instance
// here you can add, remove, manipulate entries

// Save IFFFile instance back to file
IFF.Save("./Caddie2.iff");
```
