FROM squidfunk/mkdocs-material@sha256:23b69789b1dd836c53ea25b32f62ef8e1a23366037acd07c90959a219fd1f285

COPY requirements.txt /tmp/
RUN pip install --require-hashes -r /tmp/requirements.txt
