FROM mysql:latest

COPY include/create_developer.sql /scripts/
COPY include/docker-entrypoint.sh /scripts/

EXPOSE 3306 33060

ENTRYPOINT ["/scripts/docker-entrypoint.sh"]

CMD ["mysqld"]