package org.example.starter.service

import domain.*
import groovy.xml.MarkupBuilder
import groovy.util.slurpersupport.GPathResult
import org.example.starter.helper.DomainTransformHelper
import org.springframework.stereotype.Service

import java.nio.file.Path
import java.nio.file.StandardOpenOption

import java.nio.file.Files
import java.nio.file.Paths

@Service
class XmlGenerator {
    private static final OUTPUT_DIR = "src/main/resources/petriNets/"
    private static final N_OF_COLS = 4
    private static final TEMPLATE = 'material'
    private static final APPEARANCE = 'outline'
    private static final LAYOUT = 'grid'

    private List<Domain> domainList
    private List<Connector> connectorList
    private List<Attribute> attributeList
    private List<Attribute> enumerationList

    void createXml(String filePath) {
        String fileContent = new File(filePath).getText('UTF-8')
        GPathResult root = new XmlSlurper().parseText(fileContent)
        String domainModelName = root.@name.text()
        this.domainList = new ArrayList<>()
        this.connectorList = new ArrayList<>()
        this.enumerationList = new ArrayList<>()

        root.'**'.findAll { it.name() == 'Table' && it.@name.text() == 't_object' }.each { table ->
            table.Row.each { row ->
                String objectId = row.Column.find { it.@name.text() == 'Object_ID' }?.@value.text()
                String objectName = row.Column.find { it.@name.text() == 'Name' }?.@value.text()
                String objectType = row.Column.find { it.@name.text() == 'Object_Type' }?.@value.text()

                if (objectId && objectName && objectType == 'Class') {
                    domainList.add(new Domain(objectName, objectId, new ArrayList<>(), new ArrayList<>()));
                }
                if (objectId && objectName && objectType == 'Enumeration') {
                    objectType = DomainTransformHelper.convertType(objectType)
                    enumerationList.add(new Attribute(objectName, objectType, true, new ArrayList<>(), null, objectId))
                }
            }
        }

        if (domainList.isEmpty()) {
            println "Input XML file has no domain classes."
            return
        }

        root.'**'.findAll { it.name() == 'Table' && it.@name.text() == 't_connector' }.each { table ->
            table.Row.each { row ->
                def startObjectId = row.Column.find { it.@name.text() == 'Start_Object_ID' }?.@value.text()
                def endObjectId = row.Column.find { it.@name.text() == 'End_Object_ID' }?.@value.text()

                if ((domainList.stream().anyMatch(d -> d.getObjectId().equals(startObjectId)) &&
                        domainList.stream().anyMatch(d -> d.getObjectId().equals(endObjectId))) ||
                        (enumerationList.stream().anyMatch(d -> d.getEnumerationId().equals(startObjectId)))) {
                    String connectorName = row.Column.findAll { it.@name.text() == 'Name' }?.@value.text()
                    String connectorType = row.Column.findAll { it.@name.text() == 'Connector_Type' }?.@value.text()
                    String sourceCard = row.Column.findAll { it.@name.text() == 'SourceCard' }?.@value.text()
                    String destCard = row.Column.findAll { it.@name.text() == 'DestCard' }?.@value.text()
                    boolean sourceMultiplicity = sourceCard.contains("*");
                    boolean destinationMultiplicity = destCard.contains("*");
                    connectorList.add(new Connector(connectorName, startObjectId, endObjectId, connectorType, sourceCard, destCard, sourceMultiplicity, destinationMultiplicity))
                }
            }
        }

        domainList.each {domain ->
            connectorList.each {connector ->
                if (connector.startObjectId == domain.objectId || connector.endObjectId == domain.objectId && !domain.connectors.contains(connector)){
                    List<Connector> domainConnectors = domain.getConnectors();
                    domainConnectors.add(connector)
                    domain.setConnectors(domainConnectors)
                }
            }
            String objectId = domain.getObjectId()
            String modelName = domain.getName()
            this.attributeList = new ArrayList<>()

            root.'**'.findAll { it.name() == 'Table' && it.@name.text() == 't_attribute' }.each { table ->
                table.Row.each { row ->
                    if (row.Column.any { col -> col.@name.text() == 'Object_ID' && col.@value.text() == objectId}) {
                        String name = row.Column.find { col -> col.@name.text() == 'Name' }?.@value.text()
                        String type = row.Column.find { col -> col.@name.text() == 'Type' }?.@value.text()

                        if (name && type) {
                            type = DomainTransformHelper.convertType(type)
                            attributeList.add(new Attribute(name, type, false, null, objectId, null))
                        }
                    }
                    if (domain.getConnectors().stream().anyMatch {c -> c.getStartObjectId().equals(row.Column.findAll { it.@name.text() == 'Object_ID' }?.@value.text())}) {
                        String name = row.Column.find { col -> col.@name.text() == 'Name' }?.@value.text()
                        String stereotype = row.Column.find { col -> col.@name.text() == 'Stereotype' }?.@value.text()
                        boolean isEnumeration = stereotype == 'enum'

                        if (isEnumeration) {
                            String enumParentObjectId = row.Column.find { col -> col.@name.text() == 'Object_ID' }?.@value.text()
                            Attribute enumParentAttribute = enumerationList.stream()
                                    .filter(d -> d.getEnumerationId().equals(enumParentObjectId))
                                    .findFirst().value;
                            if (name ==~ /.*[A-Za-z0-9].*/) {
                                enumParentAttribute.getOptions().add(name)
                                if (!attributeList.contains(enumParentAttribute)) {
                                    attributeList.add(enumParentAttribute)
                                }
                            }
                        }
                    }
                }
            }
            domain.setAttributes(attributeList)
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

                domain.getAttributes().each { attribute ->
                    if (attribute.getType() == "enumeration_map") {
                        data(type: attribute.getType()) {
                            id "${DomainTransformHelper.convertToId(attribute.getName())}"
                            title attribute.getName()
                            options {
                                attribute.getOptions().each { o ->
                                    option(key: "${DomainTransformHelper.convertToId(o)}", o)
                                }
                            }
                        }
                    } else {
                        data(type: attribute.getType()) {
                            id "${DomainTransformHelper.convertToId(attribute.getName())}"
                            title attribute.getName()
                        }
                    }
                }

                connectorList.each { connector ->
                    if (connector.getStartObjectId() == objectId && connector.getType() != 'Aggregation') {
                        data(type: 'caseRef') {
                            id "${DomainTransformHelper.resolveRelationName(connector.destinationMultiplicity, "caseRef", domainList, connector.endObjectId)}"
                            title ""
                            allowedNets {
                                allowedNet "${DomainTransformHelper.resolveDomainName(connector.getEndObjectId(), domainList)}"
                            }
                        }
                        data(type: 'taskRef') {
                            id "${DomainTransformHelper.resolveRelationName(connector.destinationMultiplicity, "taskRef", domainList, connector.endObjectId)}"
                            title ""
                        }
                    } else if (connector.getEndObjectId() == objectId && connector.getType() != 'Aggregation') {
                        data(type: 'caseRef') {
                            id "${DomainTransformHelper.resolveRelationName(connector.sourceMultiplicity, "caseRef", domainList, connector.startObjectId)}"
                            title ""
                            allowedNets {
                                allowedNet "${DomainTransformHelper.resolveDomainName(connector.getStartObjectId(), domainList)}"
                            }
                        }
                        data(type: 'taskRef') {
                            id "${DomainTransformHelper.resolveRelationName(connector.sourceMultiplicity, "taskRef", domainList, connector.startObjectId)}"
                            title ""
                        }
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
                        attributeList.each { attribute ->
                            dataRef {
                                id DomainTransformHelper.convertToId(attribute.getName())
                                logic {
                                    behavior 'editable'
                                }
                                layout {
                                    x '0'
                                    y "${counter}"
                                    rows '1'
                                    cols N_OF_COLS
                                    template TEMPLATE
                                    appearance APPEARANCE
                                }
                            }
                            counter++
                        }
                    }
                }
            }

            String fileName = DomainTransformHelper.convertToId(modelName.toString())
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

}