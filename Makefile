
dev-docs:
	cd docs && yarn install

build-docs:
	@$(MAKE) build-docs-website

build-docs-website: dev-docs
	mkdir -p dist
	cd docs && yarn build
	cp -R docs/public/* dist/

docs-local:
	cd docs && yarn start

test:
	mvn test

build:
	mvn -B package --file pom.xml -P dev