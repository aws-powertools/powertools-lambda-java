FROM squidfunk/mkdocs-material@sha256:fd13d78a4b1da637405c0336c9f0a4e0594b59271df106bd9cd64cae56cd1f38

COPY requirements.txt /tmp/
RUN pip install --require-hashes -r /tmp/requirements.txt
