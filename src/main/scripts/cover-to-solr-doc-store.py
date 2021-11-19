#!/usr/bin/env python3

import argparse
import json
import os
import re
import sys

import argcomplete
import psycopg2
import requests as requests


def parse_args():
    parser = argparse.ArgumentParser(description='Bruges til at sende alle poster i cover databasen til solr-doc-store')
    parser.add_argument('database', help='Postgres connection string (samme format som til java)')
    parser.add_argument('solrdocstore', help='Url til solr-doc-store')
    argcomplete.autocomplete(parser)
    return parser.parse_args()


def get_covers(conn_string):
    user_pass, server_port_database = conn_string.split('@')
    user, password = user_pass.split(':')
    server, port_database = server_port_database.split(':')
    port, database = port_database.split('/')

    config = {
        'user': user,
        'password': password,
        'host': server,
        'port': port,
        'database': database
    }

    conn = psycopg2.connect(
        "dbname=%(database)s user=%(user)s password=%(password)s host=%(host)s" % config)

    query = 'SELECT pid, coverexists FROM cover'

    cur = conn.cursor()
    cur.execute(query)
    rows = cur.fetchall()

    covers = []
    for row in rows:
        pid = row[0]
        cover_exists = row[1]

        match = re.match(r'(\d{6})-.*:(.*)', pid)
        agency_id = match.group(1)
        bibliographic_record_id = match.group(2)

        covers.append(
            {'bibliographic_record_id': bibliographic_record_id,
             'agency_id': agency_id,
             'cover_exists': cover_exists}
        )
    print('Fandt %s poster i covers' % len(covers))

    return covers


def send_covers(solr_doc_store_url, covers):
    headers = {'content-type': 'application/json'}
    body = {'has': True}
    progress_increment = len(covers) // 100
    progress = 0
    print('Sending covers to solr-doc-store')
    for cover in covers:
        progress += 1
        url = solr_doc_store_url + '/api/resource/hasDDFCoverUrl/%(agency_id)s:%(bibliographic_record_id)s' % cover

        if cover['cover_exists']:
            response = requests.put(url, data=json.dumps(body), headers=headers)
        else:
            response = requests.delete(url)

        if response.status_code != 200:
            print('Kald til solr-doc-store med url %s fik status %s med besked %s',
                  url, response.status_code, response.text)
            raise (Exception('Fejl ved kald til solr-doc-store'))
        if progress % progress_increment == 0:
            print('.', end='', flush=True)
    print('') # new line after the dots
    print('Done')

if __name__ == "__main__":
    try:
        args = parse_args()
        conn_string = args.database
        solr_doc_store_url = args.solrdocstore

        covers = get_covers(conn_string)
        send_covers(solr_doc_store_url, covers)
    except Exception as e:
        print("Uventet fejl: {}"
              .format(e), file=sys.stderr)
        sys.exit(1)
    sys.exit(os.EX_OK)
