# README
To start, clone this repo:
```
git clone https://github.com/freyalaluna/StravaInterview.git
```
<br/>

A .jar file has been provided with the code for this project. To run, enter 
```
java -jar ./IndexParser.jar
```
in the terminal alongside the necessary flags, which are described below. 

<br/>

This program is made in Java 23. Attempting to run this with older versions of the Java Runtime may result in errors.
## Arguments
### --debug
`--debug` will cause the program to read from a static JSON file found in `/src/main/resources/input.json`. All other flags will be ignored if this is provided.
```
java -jar ./IndexParser.jar --debug
```

### --endpoint
`--endpoint` supplies the program with an endpoint to submit API calls to. The endpoints assumed for this program are in the form `https://<ENDPOINT>/_cat/indices/*<YEAR>*<MONTH>*<DAY>`.
```
java -jar ./IndexParser.jar --endpoint <ENDPOINT_NAME>
```

### --days
`--days` supplies the program with the number of days to submit API calls to, starting with the current day and working backwards. 
Days are assumed to be 0-indexed (i.e. 4/1/2025 would become \*2025\*4*0 for the corresponding API call). `--days` must be supplied with an integer value.
```
java -jar ./IndexParser.jar --endpoint <ENDPOINT_NAME> --days <DAYS>
```
