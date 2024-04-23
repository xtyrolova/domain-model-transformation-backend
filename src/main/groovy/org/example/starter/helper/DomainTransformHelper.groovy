package org.example.starter.helper

import domain.Domain

import java.text.Normalizer

class DomainTransformHelper {

    static String convertType(String type) {
        switch (type) {
            case "string":
                return "text"
            case "datetime":
                return "dateTime"
            case "enumeration":
                return "enumeration_map"
            default:
                return "text"
        }
    }

    static String resolveDomainName(String objectId, domainModels) {
        String domainModelName = domainModels.stream()
                .filter(d -> d.getObjectId().equals(objectId))
                .findFirst().value.name;
        return convertToId(domainModelName)
    }

    static String convertToId(String name) {
        Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase()
                .replaceAll(" ", "_")
    }


    static String resolveRelationName(boolean destinationMultiplicity, String dataType, List<Domain> domainModels, String endObjectId) {
        String endString = resolveRelationType(destinationMultiplicity, dataType)
        String endDomainName = resolveDomainName(endObjectId, domainModels)
        return endDomainName + endString
    }

    static String resolveRelationType(boolean destinationMultiplicity, String dataType) {
        if (dataType == "taskRef" && destinationMultiplicity)
            return "_forms"
        else if (dataType == "taskRef" && !destinationMultiplicity)
            return "_form"
        if (dataType == "caseRef" && destinationMultiplicity)
            return "_ids"
        else if (dataType == "caseRef" && !destinationMultiplicity)
            return "_id"
        else
            return ""
    }
}
