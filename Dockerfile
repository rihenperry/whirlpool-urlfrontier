FROM maven:3.6.1-jdk-13 as whirlpool-urlfrontier

ARG WH_URLFRONTIER_ROOT=/home/whirlpool/whirlpool-urlfrontier
WORKDIR $WH_URLFRONTIER_ROOT

RUN useradd --create-home --shell /bin/bash whirlpool \
  && chown -R whirlpool:whirlpool $WH_URLFRONTIER_ROOT


# files necessary to build the project
COPY logs/ logs/

# copy ready-to-go target development under development java docker environment
COPY whirlpool-urlfrontier-1.0-SNAPSHOT-jar-with-dependencies.jar whirlpool-urlfrontier-1.0-SNAPSHOT-jar-with-dependencies.jar

# docker image for dev/prod target
ENTRYPOINT ["java", "-cp", "whirlpool-urlfrontier-1.0-SNAPSHOT-jar-with-dependencies.jar", "crawler.whirlpool.urlfrontier.Main"]
