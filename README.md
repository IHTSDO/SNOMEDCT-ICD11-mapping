# SNOMEDCT-ICD11-mapping
This repository hosts coding approaches to generating parts of a map from SNOMED CT to ICD-11

# ICD 11 Map Generation

ICD 11 Mapping prototype generates SNOMED CT to ICD 11 mappings.

SNOMED CT version used is 20190731 International version. 


# Run 

```sh
$ mvn -q clean compile exec:java -Dexec.mainClass="au.csiro.aehrc.icdmapping.ICDMappingPrototype" -Dexec.args="<your-input-file> <output-file>"
```
your-input-file is SNOMED CONCEPT ID list in txt file format
output-file is the output mapping file in .xlsx format