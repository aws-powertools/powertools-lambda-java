build-docs:
	@$(MAKE) build-docs-website

build-docs-website:
	git config --global --add safe.directory "/docs"
	mkdir -p dist
	docker build -t squidfunk/mkdocs-material ./docs/
	docker run --rm -t -v ${PWD}:/docs squidfunk/mkdocs-material build
	cp -R site/* dist/

docs-local-docker:
	docker build -t squidfunk/mkdocs-material ./docs/
	docker run --rm -it -p 8000:8000 -v ${PWD}:/docs squidfunk/mkdocs-material

test:
	mvn test

build:
	mvn -B package --file pom.xml -P dev