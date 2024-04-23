package domain;

import lombok.Data;

import java.util.List;

@Data
public class Domain {
    private String name;
    private String objectId;
    private List<Connector> connectors;
    private List<Attribute> attributes;

    public Domain(String name, String objectId) {
        this.name = name;
        this.objectId = objectId;
    }
}
