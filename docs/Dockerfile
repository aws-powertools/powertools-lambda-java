FROM squidfunk/mkdocs-material@sha256:1c30983ef0681684d07a90f4f34a2ff8adf7aaa0bfc1c0d518873bb5e7912f37

COPY requirements.txt /tmp/
RUN pip install --require-hashes -r /tmp/requirements.txt
