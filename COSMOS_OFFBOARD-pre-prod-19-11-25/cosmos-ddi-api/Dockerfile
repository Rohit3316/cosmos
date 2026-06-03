FROM amazoncorretto:21

ENV HAWKBIT_HOME=/opt/hawkbit

EXPOSE 8080

COPY cosmos-ddi-api/target/cosmos-ddi-api.jar $HAWKBIT_HOME/

VOLUME "$HAWKBIT_HOME/data"

WORKDIR $HAWKBIT_HOME

ENTRYPOINT ["java","-jar","cosmos-ddi-api.jar", "--debug -Xms1024m -Xmx1024m -XX:MaxMetaspaceSize=250m -XX:MetaspaceSize=250m -Xss300K -XX:+UseG1GC -XX:+UseStringDeduplication -XX:+UseCompressedOops -XX:+HeapDumpOnOutOfMemoryError"]


