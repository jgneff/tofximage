# ======================================================================
# Makefile - builds bar charts of the benchmark results
# Copyright (C) 2019-2020 John Neffenger
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
# ======================================================================
SHELL = /bin/bash

# Directories of log files and public website
logdir = ../logs
webdir = ../docs

# Commands
LATEXMK  = latexmk
EXIFTOOL = exiftool
MUDRAW   = mutool draw
OPTIPNG  = optipng
INKSCAPE = inkscape
CLEANER  = $(HOME)/opt/svgcleaner-0.9.5/svgcleaner
RSYNC    = rsync

# Command options
LATEXMK_OPTS = -lualatex
MUDRAW_OPTS  = -r 96 -c rgb
OPTIPNG_OPTS = -quiet
CLEANER_OPTS = --indent=0 --remove-declarations=no
RSYNC_OPTS   = --archive --verbose --checksum --ignore-missing-args

# ExifTool options to list the Creative Commons license metadata
exif_xmp := -XMP-cc:all -XMP-dc:all -XMP-xmpRights:all \
    -groupNames1 -veryShort -duplicates

# Sed scripts to create PGFPLOTS data from the result text files
sed_txt := -e "s/^Benchmark[ ]\{11\}/Benchmark/"
sed_txt += -e "s/^Benchmarks\.//"
sed_txt += -e "s/Score[ ]*Error/Score x Error/"

# Sed script to add font stack to the SVG files
sed_svg := -e "s/Arimo/Arimo,Liberation Sans,Arial,sans-serif/g"

# Sed scripts to create Markdown pipe tables from the result text files
header    := :---------|-----:|----:|------:|---|------:|-----:
sed_table := -e "/^Benchmark /a $(header)"
sed_table += -e "s/Score[ ]\+Error/Score ± Error/"
sed_table += -e "s/[ ]\+/ | /g"
sed_table += -e "s/^Benchmarks\.//"

# Sed scripts to edit the XMP metadata for the SVG files
sed_xmp := -e "s/x:xmpmeta.*>/metadata>/"

# List of source base names
texfiles := $(notdir $(basename $(wildcard src/*.tex)))

# List of target file names
pdffiles := $(addsuffix .pdf,$(texfiles))
pngfiles := $(addsuffix .png,$(texfiles))
svgfiles := $(addsuffix .svg,$(texfiles))
markdown := $(addsuffix .md,$(texfiles))

# Lists of targets
imgfiles := $(addprefix out/,$(pdffiles) $(pngfiles) $(svgfiles))
allfiles := $(imgfiles) $(addprefix out/,$(markdown))

# $(call publish,files,subdir)
# Publishes files from the output directory to the website subdirectory
define publish
    $(RSYNC) $(RSYNC_OPTS) $(foreach f,$1,out/$f) $(webdir)/$2/
endef

# ======================================================================
# Pattern Rules
# ======================================================================

tmp/%.dat: $(logdir)/%.txt | tmp
	sed $(sed_txt) $< > $@

tmp/%.pdf: src/%.tex tmp/%.dat chart.tex
	$(LATEXMK) $(LATEXMK_OPTS) -output-directory=$(@D) $<

out/%.pdf: tmp/%.pdf metadata.xmp | out
	$(EXIFTOOL) -tagsFromFile $(word 2,$^) -out - $< > $@

tmp/%.png: out/%.pdf
	$(MUDRAW) $(MUDRAW_OPTS) -o $@ $<

out/%.png: tmp/%.png metadata.xmp
	$(EXIFTOOL) -tagsFromFile $(word 2,$^) -out - $< > $@
	$(OPTIPNG) $(OPTIPNG_OPTS) $@

tmp/%.svg: out/%.pdf
	$(INKSCAPE) --export-plain-svg=$@ $<

tmp/%-clean.svg: tmp/%.svg
	$(CLEANER) $(CLEANER_OPTS) $< $@

out/%.svg: tmp/%-clean.svg tmp/metadata.xml
	sed -e "/<svg/r $(word 2,$^)" $(sed_svg) $< > $@

out/%.md: $(logdir)/%.txt | out
	sed $(sed_table) $< > $@

# ======================================================================
# Explicit rules
# ======================================================================

.PHONY: all list pub clean

all: $(allfiles)

tmp out:
	mkdir -p $@

tmp/metadata.xml: metadata.xmp
	sed $(sed_xmp) $< > $@

list: $(imgfiles)
	$(EXIFTOOL) $(exif_xmp) $^

# Add PDF files after enabling reproducible builds (pdfTeX 1.40.17 or later).
pub:
	$(call publish,*.md,_includes)
	$(call publish,*2020-06-??.png *2020-06-??.svg,2020-06)

clean:
	rm -f tmp/* out/*
