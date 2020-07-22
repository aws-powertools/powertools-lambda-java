
dev-docs:
	cd docs && npm install

build-docs:
	@$(MAKE) build-docs-website

build-docs-website: dev-docs
	mkdir -p dist
	cd docs && npm run build
	cp -R docs/public/* dist/

docs-local:
	cd docs && npm start

test:
	mvn test

build:
	mvn -B package --file pom.xml -P dev