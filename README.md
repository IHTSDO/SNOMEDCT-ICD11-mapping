## This repository is no longer maintained or used and has therefore been archived and made read-only. SNOMED International does not recommend using the code in this repository. ##

# SNOMEDCT-ICD11-mapping
This repository hosts coding approaches to generating parts of a map from SNOMED CT to ICD-11

# ICD 11 Map Generation

ICD 11 Mapping prototype generates SNOMED CT to ICD 11 mappings.

SNOMED CT version used is 20190731 International version. 

# File resources
The file resouces used in the prototype are large files and are zipped, download the file from https://cloudstor.aarnet.edu.au/plus/s/2RR41vMGk7OIQto

Please unzip the file to local folder and refer loation <your-resource-file> from the run command below

# Run 

```sh
$ mvn -q clean compile exec:java -Dexec.mainClass="au.csiro.aehrc.icdmapping.ICDMappingPrototype" -Dexec.args="<your-resource-file> <your-input-file> <output-file>"
```

your-resource-file is the folder of unzipped resouece files 

your-input-file is SNOMED CONCEPT ID list in txt file format

output-file is the output mapping file in .xlsx format
