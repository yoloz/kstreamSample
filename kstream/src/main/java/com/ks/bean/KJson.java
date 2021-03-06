package com.ks.bean;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import java.io.EOFException;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GSON is always converting a long number to scientific notation format.
 * https://github.com/google/gson/issues/968
 */
public class KJson {

    private static final Gson kJson = new Gson();

    public static <T> T readValue(String value, Type typeOfT) throws IOException {
        try {
            return kJson.fromJson(value, typeOfT);
        } catch (JsonParseException e) {
            throw new IOException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> readValue(String value) throws IOException {
        return (Map<String, Object>) customParse(value);
    }

    public static String writeValueAsString(Map value) throws IOException {
        try {
            return kJson.toJson(value, new TypeToken<HashMap<String, Object>>() {
            }.getType());
        } catch (JsonParseException e) {
            throw new IOException(e);
        }
    }

    public static String writeValueAsString(List value) throws IOException {
        try {
            return kJson.toJson(value, new TypeToken<ArrayList<Object>>() {
            }.getType());
        } catch (JsonParseException e) {
            throw new IOException(e);
        }
    }

    private static Object customParse(final String json) throws IOException {
        if (json == null) {
            return null;
        }
        try (final JsonReader jr = new JsonReader(new StringReader(json))) {
            jr.setLenient(true);
            boolean empty = true;
            Object o = null;
            try {
                jr.peek();
                empty = false;
                o = read(jr);
            } catch (EOFException e) {
                if (!empty) {
                    throw new JsonSyntaxException(e);
                }
            }
            if (o != null && jr.peek() != JsonToken.END_DOCUMENT) {
                throw new JsonIOException("JSON document was not fully consumed.");
            }
            return o;
        }
    }

    private static Object read(JsonReader in) throws IOException {
        JsonToken token = in.peek();
        switch (token) {
            case BEGIN_ARRAY:
                List<Object> list = new ArrayList<>();
                in.beginArray();
                while (in.hasNext()) {
                    list.add(read(in));
                }
                in.endArray();
                return list;

            case BEGIN_OBJECT:
                Map<String, Object> map = new HashMap<>();
                in.beginObject();
                while (in.hasNext()) {
                    map.put(in.nextName(), read(in));
                }
                in.endObject();
                return map;

            case STRING:
                return in.nextString();

            case NUMBER:
                final String s = in.nextString();
                try {
                    return Integer.parseInt(s);
                } catch (NumberFormatException e) {
                    // ignore
                }
                try {
                    return Long.parseLong(s);
                } catch (NumberFormatException e) {
                    // ignore
                }
                return Double.parseDouble(s);
//                return in.nextDouble();

            case BOOLEAN:
                return in.nextBoolean();

            case NULL:
                in.nextNull();
                return null;

            default:
                throw new IllegalStateException();
        }
    }
}
