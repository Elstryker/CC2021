package FSChunk;

public class FileMetaData {
    private final boolean exists;
    private final int size;
    private final String type;

    public FileMetaData(String content) {
        if (content.trim().equals("EXISTS:false")) {
            exists = false;
            size = 0;
            type = "text/html";
        }
        else {
            String[] tokens = content.split(",");
            exists = Boolean.parseBoolean(tokens[0].split(":")[1]);
            size = Integer.parseInt(tokens[1].split(":")[1]);
            type = tokens[2].split(":")[1].trim();
        }
    }

    public boolean fileExists() {
        return exists;
    }

    public int getSize() {
        return size;
    }

    public String getType() {
        return type;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("EXISTS:").append(exists).append(",");
        sb.append("SIZE:").append(size).append(",");
        sb.append("TYPE:").append(type);
        return sb.toString();
    }
}
