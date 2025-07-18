import com.trackops.server.domain.model.enums.EventType;

public enum EventType {
    ORDER_CREATED("ORDER_CREATED"),
    ORDER_STATUS_UPDATED("ORDER_STATUS_UPDATED"),
    ORDER_DELIVERED("ORDER_DELIVERED"),
    ORDER_CANCELLED("ORDER_CANCELLED");
    
    private final String value;
    
    EventType(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
}