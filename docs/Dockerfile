FROM squidfunk/mkdocs-material@sha256:c3739993c3e7c92cfe649ba03cbc04f4fa9b1497886abd16267eb413cda8aecf

COPY requirements.txt /tmp/
RUN pip install --require-hashes -r /tmp/requirements.txt
