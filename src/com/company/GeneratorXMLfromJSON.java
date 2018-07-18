package com.company;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.Map;


public class GeneratorXMLfromJSON {
    private static Attr attr;
    private final static String firstAttribute = "name";
    private final static String secondAttribute = "source";
    private final static String thirdAttribute = "type";
    private final static String arraysAttribute = "repeat";
    private final static String nameOfElement = "fieldDesc";
    private final static String nameOfRootElement = "messageDesc";
    private final static String nameXmlElement = "xml";
    private final static String defaultFileName = "dictionary_";


    public void processDocument(String pathInputFile,String pathOutputFile) throws ParserConfigurationException, GeneratorExceptions {

        Document doc = this.createDocument();
        Element rootElement = this.createRootElement(doc);
        String path = "";
        Path filePath = checkFilePath(pathInputFile);
        ContainerNode tree = this.getTreeOfNodes(filePath);

        if(tree==null)   throw new GeneratorExceptions(String.format("File %s is empty!",path));
        if(tree.isArray()){
            this.processArrayNode(doc,rootElement,(ArrayNode) tree,path);
        }
        else{
            this.processObjNode(doc,rootElement,(ObjectNode) tree,path);
        }

        this.saveDocument(doc,pathOutputFile,pathInputFile);

    }

    private Path checkFilePath(String path) throws GeneratorExceptions {

        Path filePath = Paths.get(path);
        String fileName = filePath.getFileName().toString();
        if(Files.exists(filePath) && fileName.contains(".")) return filePath;
        else throw new GeneratorExceptions(String.format("File %s not found!",path));

    }

    private void processObjNode(Document doc, Element beginField, ObjectNode node, String path) {

        Iterator<Map.Entry<String, JsonNode>> c = node.fields();
        while(c.hasNext()) {

            Map.Entry<String, JsonNode> entry = c.next();
            String key = entry.getKey();
            JsonNode curNode = entry.getValue();
            Element field = doc.createElement(nameOfElement);
            beginField.appendChild(field);

            if (curNode.isArray()) {
                setAttrForArray(doc,field,key,path);

                if(findArrayType((ArrayNode) curNode).name().toLowerCase().equals(FieldType.ARRAY.name().toLowerCase())) {
                    processArrayNode(doc,field,(ArrayNode) curNode,testPath(key,path));
                }
                if(findArrayType((ArrayNode) curNode).name().toLowerCase().equals(FieldType.OBJECT.name().toLowerCase())) {
                    JsonNode obj = curNode.get(0);
                    processObjNode(doc,field,(ObjectNode) obj,testPath(key,path));
                }

            }
            if (curNode.isObject()) {

                attr = doc.createAttribute(secondAttribute);
                attr.setValue(key);
                field.setAttributeNode(attr);

                if(curNode.isArray()) processArrayNode(doc,field,(ArrayNode) curNode,testPath(key,path));
                else processObjNode(doc,field,(ObjectNode) curNode, testPath(key,path));

            }
            else setAttrType(doc,field,key,path,curNode);

        }

    }

    private void processArrayNode(Document doc,Element beginField,ArrayNode node,String path){

        JsonNode childNode = node.get(0);
        Element field = doc.createElement(nameOfElement);
        beginField.appendChild(field);
        setAttrForObjectArray(doc,field);

        if (childNode.isArray()) processArrayNode(doc,field,(ArrayNode) childNode,path);
        else processObjNode(doc,field,(ObjectNode) childNode,path);

    }

    private String firstUpperCase(String key){

        key = key.substring(0,1).toUpperCase()+key.substring(1);
        return key;

    }

    private String testPath(String key,String path){

        String newPath;
        if(path.isEmpty()) newPath=key;
        else newPath=path+firstUpperCase(key);
        return newPath;

    }

    private FieldType findArrayType(ArrayNode array){

        JsonNode node = array.get(0);

        if(node!=null){
            if(node.isNumber()) return FieldType.NUMBER;
            if(node.isBoolean()) return FieldType.BOOLEAN;
            if(node.isArray()) return FieldType.ARRAY;
            if(node.isObject()) return FieldType.OBJECT;
        }

        return FieldType.TEXT;

    }

    private void setAttrForArray(Document doc,Element field,String key,String path){

        setCommonsAttributes(doc,field,key,path);

        setRepeatAttribute(doc,field);

    }

    private void setAttrType(Document doc,Element field,String key,String path,JsonNode curNode){

        setCommonsAttributes(doc,field,key,path);

        attr = doc.createAttribute(thirdAttribute);

        if(curNode.isArray()) curNode=curNode.get(0);
        if(curNode.isTextual())
            attr.setValue(FieldType.TEXT.name().toLowerCase());
        else{   if(curNode.isNumber()) attr.setValue(FieldType.NUMBER.name().toLowerCase());
                else attr.setValue(FieldType.BOOLEAN.name().toLowerCase());
        }

        field.setAttributeNode(attr);

    }

    private void setAttrForObjectArray(Document doc,Element field){

        attr = doc.createAttribute(firstAttribute);
        attr.setValue("");
        field.setAttributeNode(attr);

        attr = doc.createAttribute(secondAttribute);
        attr.setValue("");
        field.setAttributeNode(attr);
        setRepeatAttribute(doc,field);

    }

    private void setCommonsAttributes(Document doc,Element field,String key,String path){

        attr = doc.createAttribute(firstAttribute);
        attr.setValue(testPath(key,path));
        field.setAttributeNode(attr);

        attr = doc.createAttribute(secondAttribute);
        attr.setValue(key);
        field.setAttributeNode(attr);

    }

    private void setRepeatAttribute(Document doc,Element field){

        attr = doc.createAttribute(arraysAttribute);
        attr.setValue("true");
        field.setAttributeNode(attr);
    }


    private void saveDocument(Document doc,String outputFilePath,String inputFilePath) throws GeneratorExceptions {
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            DOMSource source = new DOMSource(doc);
            Path outputPath = Paths.get(outputFilePath);
            StreamResult result;

            Path inputPath = Paths.get(inputFilePath);

            if (Files.exists(outputPath)){
                outputPath=changePath(outputPath,inputPath);
                result = new StreamResult(outputPath.toFile());
            }
            else {
                if(outputPath.getParent()!=null) result = new StreamResult(outputPath.toFile());
                else {
                    outputPath =inputPath.resolveSibling(outputPath.toString()) ;
                    result = new StreamResult(outputPath.toFile());
                }

            }

            transformer.transform(source, result);

        }catch (TransformerConfigurationException e) {
            throw new GeneratorExceptions("Error of Transformer configuration",e);
        }catch (TransformerException e) {
            throw new GeneratorExceptions("Error of save result document with Transformer",e);
        }
    }

    private Path changePath(Path outputPath,Path inputPath ) {

        SimpleDateFormat timeStampFormat = new SimpleDateFormat("yyyy_MM_dd_hh_mm_ss_SSS");
        String timestamp = timeStampFormat.format(System.currentTimeMillis());
        String fileName = outputPath.getFileName().toString();
        String newFileName;

        if(fileName.contains(".")){
                newFileName=fileName.substring(0,fileName.lastIndexOf('.'))+timestamp+fileName.substring(fileName.lastIndexOf('.'));
                outputPath = outputPath.resolveSibling(newFileName);
        }
        else {
            newFileName = defaultFileName+timestamp+".xml";
            outputPath = inputPath.resolveSibling(newFileName);
        }

        return outputPath;
    }

    private Document createDocument() throws ParserConfigurationException, GeneratorExceptions {

        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder;

        try {
            docBuilder = docFactory.newDocumentBuilder();
        }catch (ParserConfigurationException e) {
            throw new GeneratorExceptions("Error of Parser Configuration",e);
        }
        if (docBuilder != null) {
            return docBuilder.newDocument();
        }
        else {
            docBuilder = docFactory.newDocumentBuilder();
            return docBuilder.newDocument();
        }

    }

    private Element createRootElement(Document doc){

        Element xmlElement = doc.createElement(nameXmlElement);
        doc.appendChild(xmlElement);
        Element rootElement = doc.createElement(nameOfRootElement);
        xmlElement.appendChild(rootElement);

        attr = doc.createAttribute(firstAttribute);
        attr.setValue("");
        rootElement.setAttributeNode(attr);
        attr = doc.createAttribute(thirdAttribute);
        attr.setValue("");
        rootElement.setAttributeNode(attr);

        return rootElement;
    }

    private  ContainerNode getTreeOfNodes(Path path) throws GeneratorExceptions {

        ObjectMapper mapper = new ObjectMapper();
        ContainerNode tree;

        try {
            tree = (ContainerNode) mapper.readTree(path.toFile());
        }catch(JsonParseException ex){
            throw new GeneratorExceptions(String.format("File %s is not JSON file!",path),ex);
        }catch (IOException e) {
            throw new GeneratorExceptions(String.format("Error read file %s",path),e); //ошибка при чтении файла
        }

        return tree;
    }
}
