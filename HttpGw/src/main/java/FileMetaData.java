public class FileMetaData {
    private final boolean exists;
    private final long size;
    private final String type;

    public FileMetaData(String content) {
        String[] tokens = content.split(",");
        exists = Boolean.parseBoolean(tokens[0].split(":")[1]);
        size = Long.parseLong(tokens[1].split(":")[1]);
        type = tokens[2].split(":")[1].trim();
    }

    public boolean fileExists() {
        return exists;
    }

    public long getSize() {
        return size;
    }

    public String getType() {
        return type;
    }
}
