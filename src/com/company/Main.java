package com.company;

import org.apache.log4j.Logger;

import javax.xml.parsers.ParserConfigurationException;

public class Main {
    private static Logger log = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) throws ParserConfigurationException {

        if (args.length < 2) {

            System.err.println("The path for the input or output file is not declared!");
            log.error("The path for the input or output file is not declared!");

            return;

        }
        try {
            GeneratorXMLfromJSON generator = new GeneratorXMLfromJSON();

            generator.processDocument(args[0], args[1]);
            System.out.println("File saved!");

        }
        catch (GeneratorExceptions e) {
            System.err.println("Program failed with error: " + e.getMessage());
            log.error("Program failed with error: ", e);
        }
    }
}
