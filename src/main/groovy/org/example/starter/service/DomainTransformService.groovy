package org.example.starter.service

import com.netgrif.application.engine.petrinet.domain.dataset.FileListField
import org.springframework.stereotype.Service

@Service
class DomainTransformService {

    public static void transformFiles(FileListField filesToImport) {
//        TODO
        def filePath = filesToImport.value.namesPaths.path[0].toString()
        def fileContent = new File(filePath).text

    }
}
