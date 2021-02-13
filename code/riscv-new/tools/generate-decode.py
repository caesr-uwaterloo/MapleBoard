"""
Generate Decode stage out of the riscv-opcodes repo.
The opcode can be found at:
    https://github.com/riscv/riscv-opcodes/blob/master/opcodes

USE PYTHON3 TO RUN THIS SCRIPT

Normally, this is only executed once
"""
import sys
INPUT = './opcodes'


class Instruction(object):
    R_TYPE = 0
    I_TYPE = 1
    S_TYPE = 2
    B_TYPE = 3
    U_TYPE = 4
    J_TYPE = 5

    def __init__(self, insn_type):
        self.insn_type = insn_type

    def generate_bitpat_scala(self):
        raise RuntimeError('generate_bitpat in Instruction class should not be called')


class InstructionFactory(object):
    args_insn_type = { 
            Instruction.B_TYPE: ('bimm12hi', 'bimm12lo'),
            Instruction.J_TYPE: ('jimm20'),
            Instruction.S_TYPE: ('imm12hi', 'imm12lo'),
            Instruction.I_TYPE: ('imm12', 'shamt', 'shamtw'),  # shift instructions has overlap with imm12 field
            Instruction.U_TYPE: ('imm20'),
            Instruction.R_TYPE: ()  # or default type
            }
    @staticmethod
    def _check_type(instruction_description_array):
        for k, v in InstructionFactory.args_insn_type.items():
            for arg in v:
                if arg in instruction_description_array:
                    return k
        return Instruction.R_TYPE

    @staticmethod
    def create_instruction(instruction_array):
        insn_type = InstructionFactory._check_type(instruction_array)


def parse_opcodes():
    with open(INPUT, 'r') as f:
        lines = f.readlines()
    lines = map(lambda x: x.strip(), lines)
    lines = filter(lambda x: len(x) > 0 and x[0] != '#', lines)
    lines = list(lines)
    insn_desc_arr = list(filter(None, lines[0].split(' ')))
    x = InstructionFactory.create_instruction(insn_desc_arr)

if __name__ == '__main__':
    print('Generating Decode from opcode list')
    parse_opcodes()

