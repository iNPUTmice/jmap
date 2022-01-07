package rs.ltt.jmap.client.io;

import com.google.common.base.Preconditions;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;

public class ByteStreams {

    private ByteStreams() {}

    public static long copy(final InputStream from, final OutputStream to) throws IOException {
        Preconditions.checkNotNull(from);
        Preconditions.checkNotNull(to);
        final byte[] buffer = new byte[8096];
        long total = 0;
        while (true) {
            if (Thread.interrupted()) {
                throw new InterruptedIOException();
            }
            final int read = from.read(buffer);
            if (read == -1) {
                break;
            }
            to.write(buffer, 0, read);
            total += read;
        }
        return total;
    }
}
