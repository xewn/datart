FROM amazoncorretto:8-alpine-jre

LABEL org.opencontainers.image.authors="running-elephant/datart contributors"

RUN addgroup -S datart \
    && adduser -S -G datart datart \
    && mkdir -p /datart/files /datart/logs \
    && chown -R datart:datart /datart

COPY --chown=datart:datart ./bin/ /datart/bin/
COPY --chown=datart:datart ./config/ /datart/config/
COPY --chown=datart:datart ./lib/ /datart/lib/
COPY --chown=datart:datart ./static/ /datart/static/

ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-Xms512m -Xmx2g"

EXPOSE 8080
WORKDIR /datart
USER datart

ENTRYPOINT ["sh", "-c", "exec java -server $JAVA_OPTS -Dspring.profiles.active=config -Dfile.encoding=UTF-8 -cp 'lib/*' datart.DatartServerApplication"]
