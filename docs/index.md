---
layout: default
title: Benchmarks
description: Benchmark results comparing methods for converting an AWT image to a JavaFX image.
---

# Benchmarks

This is the website of the [AWT to JavaFX Image Conversion Benchmarks](https://github.com/jgneff/tofximage) repository.

This site documents the results of the benchmark tests comparing various methods for converting an AWT image to a JavaFX image, including the public utility method [`SwingFXUtils.toFXImage`](https://github.com/jgneff/tofximage/blob/master/src/main/java/javafx/embed/swing/SwingFXUtils.java).
Although some of the methods convert the alpha values incorrectly, they are included in the tests because their conversion is correct when the source AWT image contains no transparent pixels.

## 2020-06

The results of my first round of tests in June 2020 are published on the [2020-06](2020-06/) page.

## 2020-09

The results of my second round of tests in September 2020 are published on the [2020-09](2020-09/) page.
