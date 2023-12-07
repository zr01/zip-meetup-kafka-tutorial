local-up:
	docker-compose -f .docker/dev-compose.yaml up -d

local-down:
	docker-compose -f .docker/dev-compose.yaml down --remove-orphans

clear-db:
	rm -rf .docker/.db