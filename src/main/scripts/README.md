# Introduction
This folder contains helper scripts for set-ddf-cover-service

## Setup
First create venv:
```
python3 -m venv venv 
```
and then source activate
``` 
source venv/bin/activate
```
and install required packages
```
pip install -r requirements.txt
```

## Scripts
### cover-to-solr-doc-store
This script is used for sending all covers to solr-doc-store. 
If the coverexist attribute is false then the flag is deleted in solr-doc-store.

#### Usage
With the terminal in the src/main/scripts folder run:
```
source venv/bin/activate
./cover-to-solr-doc-store.py <database> <url to solr-doc-store>
```
The <database> is a connection string of the same format which Java and the deployment uses, so you can copy it from the deployment project

Example:
```
source venv/bin/activate
./cover-to-solr-doc-store.py user:password@localhost:5432/user_db http://solr-doc-store-service.fbstest.svc.cloud.dbc.dk
```
