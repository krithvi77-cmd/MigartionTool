package framework;
import java.util.HashMap;
import java.util.Map;

public class Model {
    public Map<String, Object> data = new HashMap<>();
    public void addField(String key, Object value) { this.data.put(key, value); }
}