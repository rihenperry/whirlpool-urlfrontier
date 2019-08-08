run:
	docker-compose -f docker-compose.build.yml run --rm run

compile:
	docker-compose -f docker-compose.build.yml run --rm compile

package:
	docker-compose -f docker-compose.build.yml run --rm package

package-run:
	docker-compose -f docker-compose.build.yml run --rm package-run

cli:
	docker-compose -f docker-compose.build.yml run --rm cli

build:
	docker build --no-cache -t whirlpool-urlfrontier:latest .

prod-build:
	docker build --no-cache -t whirlpool-urlfrontier-prod:latest .

up:
	docker-compose --build -d

dev-prod-down:
	docker-compose down

prod-up:
	docker-compose -f prod-docker-compose.yml up --build -d

logs:
	docker-compose logs -f

prod-logs:
	docker-compose -f prod-docker-compose.yml logs -f

push:
	docker push rihbyne/whirlpool-urlfrontier:latest

push-prod:
	docker push rihbyne/whirlpool-urlfrontier-prod:latest

tag-prod:
	docker tag whirlpool-urlfrontier-prod:latest rihbyne/whirlpool-urlfrontier-prod:latest
