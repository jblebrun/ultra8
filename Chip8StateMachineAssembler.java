package com.emerjbl.ultra8;

import java.util.HashMap;
import java.util.List;

public class Chip8StateMachineAssembler {

    public static final String[] registers = new String[]{"V0", "V1", "V2", "V3", "V4", "V5", "V6", "V7", "V8", "V9", "VA", "VB", "VC", "VD", "VE", "VF"};
    public static final String[] specialRegisters = new String[]{"I", "[I]", "LF", "HF", "ST", "DT", "B", "F"};

    public static final int PARSE_SYMBOL = 1 << 0;
    public static final int PARSE_DIRECTIVE = 1 << 1;
    public static final int PARSE_OPCODE = 1 << 2;
    public static final int PARSE_REGISTER = 1 << 3;
    public static final int PARSE_IO_REGISTER = 1 << 4;
    public static final int PARSE_EXPR = 1 << 5;
    public static final int PARSE_COMMENT = 1 << 6;


    public static final String assembleStream() {

    } 
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
