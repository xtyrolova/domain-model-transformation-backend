package org.example.starter.service;

import groovy.xml.MarkupBuilder
import groovy.util.slurpersupport.GPathResult

import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.text.Normalizer

import java.nio.file.Files
import java.nio.file.Paths

class XmlGenerator {
    private static OUTPUT_DIR = "src/main/resources/petriNets/"
    private static N_OF_COLS = 4
    private static TEMPLATE = 'material'
    private static APPEARANCE = 'outline'
    private static LAYOUT = 'grid'

    static void createXml(String filePath) {
        String fileContent = new File(filePath).getText('UTF-8')
        GPathResult root = new XmlSlurper().parseText(fileContent)
        String domainModelName = root.@name.text()
        LinkedHashMap domainModels = new LinkedHashMap()

        root.'**'.findAll { it.name() == 'Table' && it.@name.text() == 't_object' }.each { table ->
            table.Row.each { row ->
                String objectId = row.Column.find { it.@name.text() == 'Object_ID' }?.@value.text()
                String objectName = row.Column.find { it.@name.text() == 'Name' }?.@value.text()
                String objectType = row.Column.find { it.@name.text() == 'Object_Type' }?.@value.text()

                if (objectId && objectName && objectType == 'Class') {
                    domainModels[objectId] = objectName
                }
            }
        }

        if (domainModels.isEmpty()) {
            println "Input XML files has no domain classes."
            return
        }

        domainModels.each { objectId, modelName ->
            Map attributes = new LinkedHashMap()
            Map enumerations = new LinkedHashMap()

            root.'**'.findAll { it.name() == 'Table' && it.@name.text() == 't_attribute' }.each { table ->
                table.Row.each { row ->
                    if (row.Column.any { col -> col.@name.text() == 'Object_ID' && col.@value.text() == objectId }) {
                        String name = row.Column.find { col -> col.@name.text() == 'Name' }?.@value.text()
                        String type = row.Column.find { col -> col.@name.text() == 'Type' }?.@value.text()
                        boolean isEnumeration = type == 'enumeration'

                        if (name && type) {
                            if (isEnumeration) {
                                enumerations[name] = type
                            } else {
                                attributes[name] = convertType(type)
                            }
                        }
                    }
                }
            }

            Integer counter = 0
            StringWriter writer = new StringWriter()
            new MarkupBuilder(writer).document('xmlns:xsi': "http://www.w3.org/2001/XMLSchema-instance", 'xsi:noNamespaceSchemaLocation': "https://petriflow.com/petriflow.schema.xsd") {
                id modelName.toLowerCase()
                initials modelName.take(3).toUpperCase()
                title modelName
                icon 'device_hub'
                defaultRole true
                anonymousRole true
                transitionRole false

                attributes.each { name, type ->
                    data(type: type) {
                        id convertToId(name)
                        title name
                    }
                }
                transition {
                    id 'generic_transition'
                    x '300'
                    y '140'
                    label modelName
                    roleRef {
                        id 'default'
                        logic {
                            perform true
                            assign true
                            delegate false
                            cancel false
                        }
                    }
                    dataGroup {
                        id 'generic_transition_0'
                        cols N_OF_COLS
                        layout LAYOUT
                        attributes.each { name, type ->
                            dataRef {
                                id convertToId(name)
                                logic {
                                    behaviour 'editable'
                                }
                                layout {
                                    x '0'
                                    y {counter++}
                                    rows '1'
                                    cols N_OF_COLS
                                    template TEMPLATE
                                    appearance APPEARANCE
                                }
                            }
                        }
                    }
                }
            }

            String fileName = convertToId(modelName.toString())
            saveXmlToFile(writer.toString(), new File(OUTPUT_DIR, "${domainModelName.toLowerCase()}/${fileName}.xml").absolutePath)
        }
    }

    static void saveXmlToFile(String xmlContent, String outputFilePath) {
        try {
            Path path = Paths.get(outputFilePath)
            Files.createDirectories(path.parent)
            Files.write(path, xmlContent.bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
            println "XML document created successfully at: $outputFilePath"
        } catch (Exception e) {
            e.printStackTrace()
        }
    }
    static String convertToId(String name) {
        Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase()
                .replaceAll(" ", "_")
    }

    static String convertType(String type) {
        switch (type) {
            case "string":
                return "text"
            case "datetime":
                return "dateTime"
            default:
                return "text"
        }
    }
}