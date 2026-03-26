FROM squidfunk/mkdocs-material@sha256:868ad4d39fb5865b72d00173ade00f4eae2b38dde7ff790a011cc44ce4a8ff8e

COPY requirements.txt /tmp/
RUN pip install --require-hashes -r /tmp/requirements.txt
