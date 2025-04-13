package io.cdap.wrangler.api.parser;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.cdap.wrangler.api.annotations.PublicEvolving;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@PublicEvolving
public class ByteSize implements Token {
    private final long bytes;
    private static final Pattern BYTE_PATTERN = Pattern.compile("^([+-]?\\d+(?:\\.\\d+)?)([a-zA-Z]+)$");

    public ByteSize(String value) {
        Matcher matcher = BYTE_PATTERN.matcher(value.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid byte size format: " + value);
        }

        double numericValue = Double.parseDouble(matcher.group(1));
        String unit = matcher.group(2).toUpperCase();

        switch (unit) {
            case "B":
                bytes = (long) numericValue;
                break;
            case "KB":
                bytes = (long) (numericValue * 1024L);
                break;
            case "MB":
                bytes = (long) (numericValue * 1024L * 1024L);
                break;
            case "GB":
                bytes = (long) (numericValue * 1024L * 1024L * 1024L);
                break;
            case "TB":
                bytes = (long) (numericValue * 1024L * 1024L * 1024L * 1024L);
                break;
            default:
                throw new IllegalArgumentException("Unsupported byte unit: " + unit);
        }
    }

    @Override
    public Long value() {
        return bytes;
    }

    @Override
    public TokenType type() {
        return TokenType.BYTE_SIZE;
    }

    @Override
    public JsonElement toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("type", TokenType.BYTE_SIZE.name());
        object.addProperty("value", bytes);
        return object;
    }
}
