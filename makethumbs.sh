#!/usr/bin/env bash

fd -g '*.png' arts/* -x nconvert -ratio -resize 0 240 -o {.}_thumb.png {}
