package domain;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class Attribute {
    private String name;
    private String type;
    private boolean isEnumeration;
    private List<String> options;
    private String parentObjectId;
    private String enumerationId;
}
