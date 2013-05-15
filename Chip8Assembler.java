package com.emerjbl.ultra8;

import java.util.HashMap;
import java.util.List;

public class Chip8Assembler {

    public enum Token {ADD, AND};

    public static final String[] op_tokens = {};
    public static final String[] directive_tokens = {};
    public final String[] symbol_tokens = {};
    private final HashMap<String, Integer> symTable = new HashMap<String, Integer>();
    private final HashMap<String, List<Integer>> forwardRefs = new HashMap<String, List<Integer>>();

    private static final int SYMBOL_PASS = 0;
    private static final int ASSEMBLE_PASS = 1;

    public static final String[] registers = new String[]{"V0", "V1", "V2", "V3", "V4", "V5", "V6", "V7", "V8", "V9", "VA", "VB", "VC", "VD", "VE", "VF"};
    public static final String[] specialRegisters = new String[]{"I", "[I]", "LF", "HF", "ST", "DT", "B", "F"};
    //Pass 1: assemble and generate symbols
    //Pass 2: fill forward refs

    public static final int parseSpecialRegister(String operand) {
        if(operand.charAt(0) == 'V') {
            char val = Character.toUpperCase(operand.charAt(1));
            if(val >= '0' && val <= '9') {
                return val - '0';
            }
            if(val >= 'A' && val <= 'F') {
                return val - 'A';
            }
            return -1;
        }
    }

    private int tryParseLine(String line, int pass) {
        String rest = line;
        String symbol = null;
        String expr = null;
        String[] fieldSplit;
        String opcode = null;
        String[] operands = null;

        //Throw away comment 
        if(line.contains(";")) {
            fieldSplit = line.split(";", 2);
            rest = fieldSplit[0].trim();
        }

        //Parse out equates done with =
        if(line.contains("=")) {
            fieldSplit = line.split("=", 2);
            symbol = fieldSplit[0].trim();
            expr = fieldSplit[1].trim();
        }

        //Parse out equates done with EQU
        if(line.contains("EQU")) {
            fieldSplit = rest.split("EQU", 2); 
            symbol = fieldSplit[0].trim();
            expr = fieldSplit[1].trim();
        }

        //Parse out a leading symbol. 
        if(line.contains(":")) {
            fieldSplit = line.split(":", 2);
            symbol = fieldSplit[0].trim();
            rest = fieldSplit[1].trim();
        }

        //Get the op
        fieldSplit = line.split(" ", 2);
        opcode = fieldSplit[0].trim();
        rest = fieldSplit[1].trim();

        //Get the operands
        operands = rest.split(",");



        switch(Token.valueOf(opcode)) {
            case ADD:
                if(operands.length != 2) {
                    throw new IllegalStateException("ADD requires two operands");
                }

                break;
            case AND:
                break;
            case CALL:
                break;
            case CLS:
                break;
            case "DRW":
                break;
            case "EXIT":
                break;
            case "HIGH":
                break;
            case "JP":
                break;
            case "LD":
                break;
            case "LOW":
                break;
            case "OR":
                break;
            case "RET":
                break;
            case "RND":
                break;
            case "SCD":
                break;
            case "SCL":
                break;
            case "SCR":
                break;
            case "SE":
                break;
            case "SHL":
                break;
            case "SHR":
                break;
            case "SKP":
                break;
            case "SKNP":
                break;
            case "SNE": 
                break;
            case "SUB":
                break;
            case "SUBN":
                break;
            case "SYS":
                break;
            case "XOR":
                break;

            case "ALIGN":
                break;
            case "DEFINE":
                break;
        }



    }
}
