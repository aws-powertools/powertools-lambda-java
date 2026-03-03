FROM squidfunk/mkdocs-material@sha256:8f41b6089700e1c32212c3857936f14e88a3306a35be4ffd1826420e2f3e4197

COPY requirements.txt /tmp/
RUN pip install --require-hashes -r /tmp/requirements.txt
