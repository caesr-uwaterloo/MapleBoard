TARGET = rltotiff

TIFF_LIBDIR = ./libtiff
TIFF_INCDIR = ./libtiff
OBJS = rltotiff.o tiff_rgba_io.o
LDFLAGS = -L$(TIFF_LIBDIR) -lgcc_s -ltiff
CFLAGS = -I$(TIFF_INCDIR)
HOSTCC ?= gcc

x = *

ifdef version
  ifeq "$(version)" "IN_PARSEC"
    PREFIX=${PARSECDIR}/ext/splash2/apps/${TARGET}/inst/${PARSECPLAT}
    MACROS = ../../../../null_macros/c.m4.null.pthread
  endif
endif

$(TARGET): $(OBJS)
	${HOSTCC} $(OBJS) $(CFLAGS) -o $(TARGET) $(LDFLAGS)


M4 = -m4 -Uindex -Ulen
RM = -rm -f

.SUFFIXES:
.SUFFIXES:      .o .c

.c.o:
	${HOSTCC} -c $(CFLAGS) $*.c

