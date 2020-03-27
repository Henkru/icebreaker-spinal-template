TOPLEVEL = TopLevel

BUILDFOLDER = build
$(shell mkdir -p $(BUILDFOLDER))
RTLFOLDER = rtl

PROJ = $(BUILDFOLDER)/$(TOPLEVEL)
PCF = $(RTLFOLDER)/icebreaker.pcf

FREQFILE = $(RTLFOLDER)/freq.txt
FREQ = $(shell cat $(FREQFILE))
PLL = $(RTLFOLDER)/pll.v

RTLFILES = $(RTLFOLDER)/$(TOPLEVEL).v $(PLL)

all: $(PROJ).rpt $(PROJ).bin

$(PLL): $(FREQFILE)
ifeq ("$(FREQ)", "12")
	echo '' > $(PLL)
else
	icepll -m -o $(FREQ) |sed 's/SB_PLL40_CORE/SB_PLL40_PAD/' | sed 's/REFERENCECLK/PACKAGEPIN/' >  $(PLL)
endif

$(PROJ).json: $(RTLFILES)
	yosys -ql $(PROJ).yslog -p 'synth_ice40 -top $(TOPLEVEL) -json $@' $^

$(PROJ).asc: $(PROJ).json $(PCF)
	nextpnr-ice40 -ql $(PROJ).nplog --up5k --package sg48 --freq $(FREQ) --asc $@ --pcf $(PCF) --json $<

$(PROJ).bin: $(PROJ).asc
	icepack $< $@

$(PROJ).rpt: $(PROJ).asc
	icetime -d up5k -c $(FREQ) -mtr $@ $<

prog: $(PROJ).bin
	iceprog $<

sudo-prog: $(PROJ).bin
	@echo 'Executing prog as root!!!'
	sudo iceprog $<

clean:
	rm -f $(PROJ).yslog $(PROJ).nplog $(PROJ).json $(PROJ).asc $(PROJ).rpt $(PROJ).bin
	rm -f $(PROJ)_tb $(PROJ)_tb.vcd $(PROJ)_syn.v $(PROJ)_syntb $(PROJ)_syntb.vcd
	rm -f $(PLL)

FORCE:

.SECONDARY:
.PHONY: all prog clean
