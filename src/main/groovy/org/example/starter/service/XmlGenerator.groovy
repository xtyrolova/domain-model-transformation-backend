package org.example.starter.service;

import groovy.xml.MarkupBuilder

import java.nio.file.StandardOpenOption
import java.text.Normalizer

import java.nio.file.Files
import java.nio.file.Paths

class XmlGenerator {
    private static OUTPUT_DIR = "src/main/resources/petriNets/transformed/"

    static void createXml(String filePath) {
        def fileContent = new File(filePath).getText('UTF-8')
        def root = new XmlSlurper().parseText(fileContent)

        def domainModelName = root.@name.text()

        def tables = root.'**'.findAll { it.name() == 'Table' && it.@name.text() == 't_attribute' }
        def attributesData = [:]
        tables.each { table ->
            table.Row.each { row ->
                def nameColumn = row.Column.find { it.@name.text() == 'Name' }
                def typeColumn = row.Column.find { it.@name.text() == 'Type' }

                def nameValue = nameColumn ? nameColumn.@value.text() : null
                def typeValue = typeColumn ? typeColumn.@value.text() : null

                if (nameValue && typeValue) {
                    attributesData[nameValue] = typeValue
                }
            }
        }

        // Now, generate the new XML document based on the extracted data
        def writer = new StringWriter()
        def builder = new MarkupBuilder(writer)
        builder.document('xmlns:xsi': "http://www.w3.org/2001/XMLSchema-instance", 'xsi:noNamespaceSchemaLocation': "../../du_schema.xsd") {
            id domainModelName.toLowerCase()
            initials domainModelName.take(3).toUpperCase()
            title domainModelName
            icon 'device_hub'
            defaultRole true
            anonymousRole true
            transitionRole false

            attributesData.each { attribute ->
                data(type: attribute.value) {
                    id Normalizer.normalize(attribute.key, Normalizer.Form.NFD)
                            .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                            .toLowerCase()
                            .replaceAll(" ", "_")
                    title attribute.key
                }
            }
        }
        writer.close()

        println writer.toString()
        saveXmlToFile(writer.toString(), new File(OUTPUT_DIR, "${domainModelName.toLowerCase()}.xml").absolutePath)
    }

    static void saveXmlToFile(String xmlContent, String outputFilePath) {
        try {
            def path = Paths.get(outputFilePath)
            Files.createDirectories(path.parent) // Ensure parent directories are created
            Files.write(path, xmlContent.bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
            println "XML document created successfully at: $outputFilePath"
        } catch (Exception e) {
            e.printStackTrace()
        }
    }
}