FROM squidfunk/mkdocs-material@sha256:0bfdba448e93984191246f7a28abeacc79f789e7e9cf0c639a48fe4365e880a7

COPY requirements.txt /tmp/
RUN pip install --require-hashes -r /tmp/requirements.txt
