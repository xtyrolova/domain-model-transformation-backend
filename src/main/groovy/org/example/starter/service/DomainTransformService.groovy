package org.example.starter.service

import com.netgrif.application.engine.petrinet.domain.dataset.FileListField
import com.netgrif.application.engine.workflow.service.interfaces.IWorkflowService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class DomainTransformService {
    private SCHEMA_FILE_PATH = "../resources/schema/schema.xml"

    @Autowired
    IWorkflowService workflowService

    public static void transformFiles(FileListField filesToImport) {
//        TODO
        def filePath = filesToImport.value.namesPaths.path[0].toString()
        def fileContent = new File(filePath).text
        def root = new XmlSlurper().parseText(fileContent)

        def domainModelName = root[0].attributes.name
        def classesArray = root[0].children[1].children
        def attributesArray = root[0].children[2].children
        def relationsArray = root[0].children[3].children

        createDomainCases(classesArray)
    }

    static def createDomainCases(ArrayList classesArray) {
        classesArray.each { row ->
            row.children.each { column ->
                if (column.name == "Name") {
//                    TODO
//                    workflowService.createCase(column.value, column.value)
                }
            }
        }
    }
}
