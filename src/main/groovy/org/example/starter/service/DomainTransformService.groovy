package org.example.starter.service

import com.netgrif.application.engine.auth.service.interfaces.IUserService
import com.netgrif.application.engine.petrinet.domain.dataset.FileListField
import com.netgrif.application.engine.petrinet.domain.dataset.FileListFieldValue
import com.netgrif.application.engine.petrinet.service.interfaces.IPetriNetService
import com.netgrif.application.engine.workflow.service.interfaces.IWorkflowService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import com.netgrif.application.engine.petrinet.domain.VersionType

@Service
class DomainTransformService {

    @Autowired
    IWorkflowService workflowService

    @Autowired
    IUserService userService

    @Autowired
    IPetriNetService petriNetService

    @Autowired
    XmlGenerator xmlGenerator

    /**
     * Method reads the input file path, call xmlGenerator method and measure execution time of the transformation.
     * @param filesToImport
     * @return transformed files for user to download
     */
    FileListFieldValue transformFiles(FileListField filesToImport) {
        String filePath = filesToImport.value.namesPaths.path[0].toString()

        long startTime = System.currentTimeMillis()

        FileListFieldValue files = xmlGenerator.createXml(filePath)

        long endTime = System.currentTimeMillis()

        long duration = endTime - startTime
        println "Execution time of createXml: ${duration} ms"

        files.namesPaths.each {
            String importedFilePath = it.path.toString()

            InputStream fileStream = new FileInputStream(importedFilePath)
            petriNetService.importPetriNet(fileStream, VersionType.MAJOR, userService.loggedUserFromContext)
        }

        return files
    }
}
