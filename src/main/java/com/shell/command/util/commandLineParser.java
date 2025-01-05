package com.shell.command.util;

import com.shell.command.Command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class commandLineParser {

    private static final char BACK_SLASH = '\\';
    private static final char DOUBLE_QUOTE = '\"';
    private static final char SINGLE_QUOTE = '\'';
    private static final char DOLLAR_SIGN = '$';
    private static final char SPACE = ' ';
    private static final char BACK_TICK =  '`';

    private final RedirectionHandler redirectionHandler;

    public commandLineParser() {
        redirectionHandler = new RedirectionHandler();
    }

    public void createCommandObj(String input){
        if(input == null || input.isBlank()){
            return;
        }

        List<String> tokens = getTokens(input);

    }


    private List<String> getTokens(String inputString){

        InputParserObj inputParserObj = new InputParserObj();
        for(int i = 0; i < inputString.length(); i++){
            inputParserObj.character = inputString.charAt(i);
            if(inputParserObj.character == BACK_SLASH) {
                if((i + 1 < inputString.length())) {
                    handleBackSlash(inputParserObj, inputString.charAt(i + 1));
                }
            } else if (inputParserObj.character == DOUBLE_QUOTE) {
                handleDoubleQuote(inputParserObj);
            } else if (inputParserObj.character == SINGLE_QUOTE) {
                handleSingleQuote(inputParserObj);
            } else if(inputParserObj.character == SPACE) {
                handleSpace(inputParserObj);
            } else {
                inputParserObj.appendCharacterToBuffer();
            }
        }
        if (!inputParserObj.stringBuffer.isEmpty()) inputParserObj.addTokenToList();
        return inputParserObj.tokens;
    }

    private void handleSpace(InputParserObj inputParserObj){
        if(!inputParserObj.inSingleQuote && !inputParserObj.inDoubleQuote && !inputParserObj.escapeCharacterFound){
            inputParserObj.addTokenToList();
        } else if(inputParserObj.escapeCharacterFound){
            inputParserObj.appendCharacterToBuffer();
            inputParserObj.escapeCharacterFound = false;
        }
    }

    private void handleSingleQuote(InputParserObj inputParserObj){
        if (inputParserObj.escapeCharacterFound){
            inputParserObj.appendCharacterToBuffer();
            inputParserObj.escapeCharacterFound = false;
        } else if(inputParserObj.inDoubleQuote){
            inputParserObj.appendCharacterToBuffer();
        } else {
            inputParserObj.inSingleQuote = !inputParserObj.inSingleQuote;
        }
    }

    private void handleDoubleQuote(InputParserObj inputParserObj){
        if (inputParserObj.escapeCharacterFound){
            inputParserObj.appendCharacterToBuffer();
            inputParserObj.escapeCharacterFound = false;
        }  else if (inputParserObj.inSingleQuote) {
            inputParserObj.appendCharacterToBuffer();
        } else {
            inputParserObj.inDoubleQuote = !inputParserObj.inDoubleQuote;
        }
    }

    private void handleBackSlash(InputParserObj inputParserObj, char character){
        Set<Character> escapes = Set.of(DOUBLE_QUOTE, DOLLAR_SIGN, BACK_TICK, BACK_SLASH);
        if(escapes.contains(character)){
            if(inputParserObj.inSingleQuote){
                inputParserObj.appendCharacterToBuffer();
                if(character != SINGLE_QUOTE) inputParserObj.escapeCharacterFound = false;
                return;
            }
            inputParserObj.escapeCharacterFound = true;
        } else if(!inputParserObj.inDoubleQuote && !inputParserObj.inSingleQuote && inputParserObj.character == SPACE){
            inputParserObj.escapeCharacterFound = true;
        } else {
            inputParserObj.appendCharacterToBuffer();
            inputParserObj.escapeCharacterFound = false;
        }
    }

    class InputParserObj {
        private boolean inSingleQuote;
        private boolean inDoubleQuote;
        private boolean escapeCharacterFound;
        private char character;
        private StringBuilder stringBuffer = new StringBuilder();

        List<String> tokens = new ArrayList<>();

        public void appendCharacterToBuffer(){
            if(stringBuffer != null){
                stringBuffer.append(character);
            }
        }

        public void addTokenToList(){
            if(!stringBuffer.toString().isBlank()){
                tokens.add(stringBuffer.toString());
            }
            stringBuffer.setLength(0);
        }
    }
}
