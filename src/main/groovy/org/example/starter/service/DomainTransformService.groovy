package org.example.starter.service

import com.netgrif.application.engine.petrinet.domain.dataset.FileListField
import com.netgrif.application.engine.workflow.service.interfaces.IWorkflowService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class DomainTransformService {

    @Autowired
    IWorkflowService workflowService

    public static void transformFiles(FileListField filesToImport) {
        def filePath = filesToImport.value.namesPaths.path[0].toString()
        XmlGenerator.createXml(filePath)
    }
}
