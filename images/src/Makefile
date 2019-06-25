# ======================================================================
# Makefile - builds PDF files and images of benchmark results
# Copyright (C) 2019 John Neffenger
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

# Commands
LATEXMK = latexmk
MUDRAW = mudraw
PDF2SVG = pdf2svg
EXIFTOOL = exiftool
OPTIPNG = optipng
SED = sed

# HTML Tidy for Linux version 5.7.27
# https://github.com/htacg/tidy-html5
TIDY = $(HOME)/opt/tidy-html5/bin/tidy

# GitHub text widths: 668 px for issues, 882 px for README
dpi = 96

# Minimal XMP metadata for the CC BY-SA 4.0 license
meta_xmp += -XMP-cc:AttributionName='John Neffenger'
meta_xmp += -XMP-cc:License='https://creativecommons.org/licenses/by-sa/4.0/'
meta_xmp += -XMP-dc:Title='Converting AWT Images to JavaFX Images'
meta_xmp += -XMP-xmpRights:Marked='True'
meta_xmp += -XMP-xmpRights:WebStatement='https://github.com/jgneff/tofximage'
meta_xmp += -XMP-xmpRights:UsageTerms='This work is licensed under the \
    Creative Commons Attribution-ShareAlike 4.0 International License.'

# Sed scripts to edit the XMP metadata for the SVG file
sed_xmp += -e '/^[[:space:]]*$$/d' -e '/<?xpacket/d'
sed_xmp += -e 's/x:xmpmeta.*>/metadata>/'

# HTML Tidy options to clean up XML (and SVG) files
tidy_xml = --input-xml yes --output-xml yes --wrap 0 --quiet yes

# Lists the XMP metadata
list_xmp = -xmp:all -groupNames1 -veryShort -duplicates

# ======================================================================
# Pattern Rules
# ======================================================================

%.pdf: %.tex
	$(LATEXMK) -pdf $<
	$(EXIFTOOL) -overwrite_original $(meta_xmp) $@

%.png: %.pdf
	$(MUDRAW) -r $(dpi) -o $@ $<
	$(EXIFTOOL) -overwrite_original $(meta_xmp) $@
	$(OPTIPNG) $@

%.xml: %.pdf
	$(EXIFTOOL) -xmp -binary $< | $(SED) $(sed_xmp) > $@

%.svg: %.pdf %.xml
	$(PDF2SVG) $< $@
	$(SED) '/<svg/r $(word 2,$^)' --in-place $@
	$(TIDY) $(tidy_xml) -modify $@

# ======================================================================
# Explicit rules
# ======================================================================

.PHONY: all list clean purge

all: dell.png dell.svg kobo.png kobo.svg

list:
	$(EXIFTOOL) $(list_xmp) *.pdf *.png *.svg

dell.pdf: chart.tex dell.dat

kobo.pdf: chart.tex kobo.dat

clean:
	$(LATEXMK) -c *.tex

purge: clean
	$(LATEXMK) -C *.tex
	rm -f *.png *.xml *.svg
