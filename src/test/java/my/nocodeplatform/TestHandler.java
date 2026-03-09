package my.nocodeplatform;

public class TestHandler {
    public static void main(String[] args) throws Exception {
        Class<?> clazz = Class.forName("dev.langchain4j.model.StreamingResponseHandler");
        for (java.lang.reflect.Method m : clazz.getMethods()) {
            System.out.println(m.toString());
        }
    }
}
