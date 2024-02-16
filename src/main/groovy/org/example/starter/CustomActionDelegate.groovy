package org.example.starter

import com.netgrif.application.engine.petrinet.domain.dataset.logic.action.ActionDelegate
import org.example.starter.service.DomainTransformService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class CustomActionDelegate extends ActionDelegate {

    @Autowired
    private DomainTransformService domainTransformService
}