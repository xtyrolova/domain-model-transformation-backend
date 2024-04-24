package domain;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class Domain {
    private String name;
    private String objectId;
    private List<Connector> connectors;
    private List<Attribute> attributes;
}
