#!/bin/sh
.PHONY: build dev down ssh publish
build:
	docker image rm -f sokomishalov/skraper:latest && docker build --no-cache -t sokomishalov/skraper:latest --progress=plain .
	docker-compose -f docker-compose.yml up  --remove-orphans