# set-ddf-cover-service

---

## Description
Web service used for setting hasCover attribute in openSearch for records which DBC doesn't have a cover for.

## Developer instructions
You need Java 11 SDK and maven.

To build code run
```bash
mvn clean verify
```
or
```bash 
scripts/build
```

To run the docker:
First, the following environment variables must be set:
```bash
SET_DDF_COVER_DB
SOLR_DOC_STORE_URL
OAUTH2_CLIENT_ID
OAUTH2_CLIENT_SECRET
```
If you are not using real OAUTH2_CLIENT attributes you probably need to set ```OAUTH2_INTROSPECTION_URL``` as well.

Then run ```scripts/build docker``` to build to docker image and then ```scripts/start-database``` to start the database.

In another terminal run ```scripts/start-server``` to start the set-ddf-cover-service.

## Client instructions
OpenAPI spec can be found [here](https://raw.githubusercontent.com/DBCDK/set-ddf-cover-service/main/docs/openapi.yaml)

There is only one endpoint which can be called like this:
```bash
curl -X POST --data '{"pid":"<pid>", "coverExists": <true|false>}'  -H 'content-type: application/json' -H 'Authorization: Bearer <token>' https://ddfhascover-stg.dbc.dk/api/v1/events
```

For example:
```bash
curl -X POST --data '{"pid":"870970-basis:12345678", "coverExists": true}'  -H 'content-type: application/json' -H 'Authorization: Bearer 12345678901234567890' https://ddfhascover-stg.dbc.dk/api/v1/events
```

A bearer token must be provided.

### Getting a token

One way of generating a token is to run
```bash
curl --user "<client_id>:<client_secret>" -X POST https://auth.dbc.dk/oauth/token -d "grant_type=password&username=@&password=@"
```
