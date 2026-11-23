package com.university.sms.core.io;

import com.university.sms.core.exception.SmsException;
import com.university.sms.core.model.StudentRecord;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * UNIT 3 - Byte streams, filter streams and object serialization (Anjay).
 *
 * <p>Backup file layout (".smsbak"):</p>
 * <pre>
 *  OutputStream
 *   └─ BufferedOutputStream            (buffering)
 *       └─ GZIPOutputStream            (compression)
 *           ├─ DataOutputStream        header: magic int, version short, timestamp long, count int
 *           └─ ObjectOutputStream      body:   List&lt;StudentRecord&gt; (Serializable objects)
 * </pre>
 * Restoring reads the same chain in reverse and verifies the magic number, so a random file
 * is rejected with a clear {@link SmsException}.
 */
public class SerializationBackupService {

    public static final int MAGIC = 0x534D5342;   // "SMSB"
    public static final short FORMAT_VERSION = 1;

    /** Information read back from a backup file. */
    public record BackupContents(short version, Instant createdAt, List<StudentRecord> students) {
    }

    public long backup(List<StudentRecord> students, OutputStream target) throws IOException {
        GZIPOutputStream gzip = new GZIPOutputStream(new BufferedOutputStream(target));
        DataOutputStream data = new DataOutputStream(gzip);
        data.writeInt(MAGIC);
        data.writeShort(FORMAT_VERSION);
        data.writeLong(System.currentTimeMillis());
        data.writeInt(students.size());
        data.flush();

        ObjectOutputStream objects = new ObjectOutputStream(gzip);
        objects.writeObject(new ArrayList<>(students));   // the whole object graph is serialized
        objects.flush();
        gzip.finish();
        gzip.flush();
        return students.size();
    }

    @SuppressWarnings("unchecked")
    public BackupContents restore(InputStream source) throws SmsException {
        try {
            GZIPInputStream gzip = new GZIPInputStream(new BufferedInputStream(source));
            DataInputStream data = new DataInputStream(gzip);
            int magic = data.readInt();
            if (magic != MAGIC) {
                throw new SmsException("SMS-BACKUP-001", "Not a Student Management System backup file");
            }
            short version = data.readShort();
            Instant createdAt = Instant.ofEpochMilli(data.readLong());
            int expected = data.readInt();

            ObjectInputStream objects = new ObjectInputStream(gzip);
            Object read = objects.readObject();
            if (!(read instanceof List<?> list)) {
                throw new SmsException("SMS-BACKUP-002", "Backup body is not a list");
            }
            List<StudentRecord> students = (List<StudentRecord>) list;
            if (students.size() != expected) {
                throw new SmsException("SMS-BACKUP-003", "Corrupted backup: header says " + expected
                        + " records, body has " + students.size());
            }
            return new BackupContents(version, createdAt, students);
        } catch (IOException | ClassNotFoundException e) {      // multi-catch
            throw new SmsException("SMS-BACKUP-004", "Could not read backup: " + e.getMessage(), e);
        }
    }
}
