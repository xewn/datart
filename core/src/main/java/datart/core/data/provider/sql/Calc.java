package datart.core.data.provider.sql;

import lombok.Data;

import java.io.Serializable;

@Data
public class Calc implements Serializable {

    private String key;
    private String type;
    private Object config;
}
