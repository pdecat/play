package play.data.binding.types;

import play.data.Upload;
import play.data.binding.TypeBinder;
import play.mvc.Http.Request;

import java.lang.annotation.Annotation;
import java.util.List;

/**
 * Bind byte[] form multipart/form-data request.
 */
public class ByteArrayBinder implements TypeBinder<byte[]> {

    @SuppressWarnings("unchecked")
    public byte[] bind(String name, Annotation[] annotations, String value, Class actualClass) {
        List<Upload> uploads = (List<Upload>) Request.current().args.get("__UPLOADS");
        for(Upload upload : uploads) {
            if(upload.getFieldName().equals(value)) {
                return upload.asBytes();
            }
        }
        return null;
    }
}
