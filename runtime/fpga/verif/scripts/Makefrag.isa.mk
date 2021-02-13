XLEN ?= 64

# ui
RVUI_INSN = \
auipc 	 fence_i  lhu      sd       slt      sraw     sw             \
add      beq      jal      lui      sh       slti     srl      xor   \
addi     bge      jalr     lw       simple   sltiu    srli     xori  \
addiw    bgeu     lb       lwu      sll      sltu     srliw          \
addw     blt      lbu      or       slli     sra      srlw           \
and      bltu     ld       ori      slliw    srai     sub            \
andi     bne      lh       sb       sllw     sraiw    subw

# ua
RVUA_INSN = \
 amoswap_w amoswap_d \
 amoadd_w  amoadd_d \
 lrsc

# mi
RVMI_INSN = \
mcsr     ma_addr  breakpoint
# the following might require some tweaking about the CSR
# csr    scall    ma_fetch access   sbreak

$(RVUI_INSN): %: compile
	$(SIM_DIR)/$(RT) -m $(MEM_SIZE) $(ELF_ROOT)/rv$(XLEN)ui-p-$*

$(RVMI_INSN): %: compile
	$(SIM_DIR)/$(RT) -m $(MEM_SIZE) $(ELF_ROOT)/rv$(XLEN)mi-p-$*

$(RVUA_INSN): %: compile
	$(SIM_DIR)/$(RT) -m $(MEM_SIZE) $(ELF_ROOT)/rv$(XLEN)ua-p-$*

isa-ui-test: $(RVUI_INSN)
isa-mi-test: $(RVMI_INSN)

isa-test: isa-ui-test isa-mi-test
