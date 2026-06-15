package exception;

public class ResourceNotFoundException extends AppException {
    public ResourceNotFoundException(String resource, Object id) {
        super(404, resource + " không tìm thấy với id: " + id);
    }
}
