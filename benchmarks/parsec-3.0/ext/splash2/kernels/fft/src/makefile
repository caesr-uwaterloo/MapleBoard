TARGET = fft
OBJS = fft.o
CFLAGS =  ${shell printenv CFLAGS} -O2 -w
LDFLAGS = ${shell printenv LDFLAGS} -lpthread -lm
MACROS = ../../../null_macros/c.m4.null.pthread


x = *

ifdef version
  ifeq "$(version)" "IN_PARSEC"
    PREFIX=${PARSECDIR}/ext/splash2/kernels/${TARGET}/inst/${PARSECPLAT}
    MACROS = ../../../../null_macros/c.m4.null.pthread
  endif
endif

$(TARGET): $(OBJS)
	echo HI
	${CC} $(OBJS) $(CFLAGS) -o $(TARGET) $(LDFLAGS)

install:
	mkdir -p $(PREFIX)/bin
	cp -f $(TARGET) $(PREFIX)/bin/$(TARGET)
	cp -f run.sh $(PREFIX)/bin/run.sh

clean:
	rm -rf *.c *.h *.o $(TARGET)

.SUFFIXES:
.SUFFIXES:	.o .c .C .h .H

.H.h:
	m4 ${MACROS} $*.H > $*.h

.C.c:
	m4 $(MACROS) $*.C > $*.c

.c.o:
	${CC} -c $(CFLAGS) $*.c

.C.o:
	m4 $(MACROS) $*.C > $*.c
	${CC} -c $(CFLAGS) $*.c

