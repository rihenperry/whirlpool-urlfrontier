### whirlpool-urlfrontier

steps to build and push the service after pulling the repository.

`
docker build --no-cache -t whirlpool-urlfrontier:latest --target whirlpool-urlfrontier .
`

`
docker tag whirlpool-urlfrontier:latest rihbyne/whirlpool-urlfrontier:latest
`

`
docker push rihbyne/whirlpool-urlfrontier:latest
`

`
docker-compose -f docker-compose.yml build --no-cache whirlpool-urlfrontier
`

start the container with build flag in detach mode (will build all the images before starting)

`
docker-compose -f docker-compose.yml up --build -d whirlpool-urlfrontier
`

stop the container by removing non-running containers 
`
docker-compose -f docker-compose.yml down --remove-orphans
`

Start with no dependencies
`docker-compose run --no-deps SERVICE COMMAND [ARGS...]`

test connection to postgres
`psql -h <host> -U <user> --dbname=<db>`
