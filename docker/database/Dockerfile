FROM postgres:9.6
USER postgres

COPY init-db.sh /docker-entrypoint-initdb.d

HEALTHCHECK --interval=10s --timeout=10s --retries=10 CMD psql -c 'select 1' -d ccd_data -U ${DATA_STORE_DB_USERNAME}

EXPOSE 5432
