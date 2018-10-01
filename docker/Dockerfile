FROM gcr.io/google-appengine/openjdk:9


COPY libs /app/libs/
COPY resources /app/resources/
COPY classes /app/classes/

EXPOSE 9000
ENTRYPOINT ["java","-Xms512m","-Xdebug","-cp","/app/resources:/app/classes:/app/libs/*","org.cg.GbaasApplication"]
CMD ["echo $FOO"]