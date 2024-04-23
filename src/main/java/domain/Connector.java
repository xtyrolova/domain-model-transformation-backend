package domain;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Connector {
    private String name;
    private String startObjectId;
    private String endObjectId;
    private String type;
    private String sourceCardinality;
    private String destinationCardinality;
    private boolean sourceMultiplicity = false;
    private boolean destinationMultiplicity = false;
}
