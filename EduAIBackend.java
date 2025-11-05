import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import org.json.JSONArray;
import org.json.JSONObject;

public class EduAIBackend {

    private static final int PORT = 8080;
    private static final String USER_FILE = "users.json";
    private static final String DOC_FILE = "documents.json";

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        System.out.println("✅ EduAI Backend running at http://localhost:" + PORT);

        server.createContext("/api/register", new RegisterHandler());
        server.createContext("/api/login", new LoginHandler());
        server.createContext("/api/upload", new DocumentUploadHandler());
        server.createContext("/api/getDocuments", new GetDocumentsHandler());
        server.createContext("/", new DefaultHandler());

        server.setExecutor(null);
        server.start();
    }

    private static String readBody(InputStream is) throws IOException {
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }

    private static JSONArray readJSONFile(String fileName) {
        try {
            String content = Files.readString(Path.of(fileName));
            return new JSONArray(content);
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    private static void writeJSONFile(String fileName, JSONArray data) {
        try (FileWriter writer = new FileWriter(fileName)) {
            writer.write(data.toString(4));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

   private static void sendResponse(HttpExchange exchange, int code, String response) throws IOException {
    Headers headers = exchange.getResponseHeaders();
    headers.add("Content-Type", "application/json");

    // ✅ Add CORS headers
    headers.add("Access-Control-Allow-Origin", "*");
    headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
    headers.add("Access-Control-Allow-Headers", "Content-Type");

    // ✅ Handle preflight OPTIONS request (for browsers)
    if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
        exchange.sendResponseHeaders(204, -1); // No content
        return;
    }

    // Send actual response
    exchange.sendResponseHeaders(code, response.getBytes().length);
    try (OutputStream os = exchange.getResponseBody()) {
        os.write(response.getBytes());
    }
}


    static class RegisterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            JSONObject body = new JSONObject(readBody(exchange.getRequestBody()));
            String email = body.optString("email");
            String password = body.optString("password");
            String firstName = body.optString("firstName");
            String lastName = body.optString("lastName");

            JSONArray users = readJSONFile(USER_FILE);
            for (Object obj : users) {
                JSONObject user = (JSONObject) obj;
                if (user.getString("email").equalsIgnoreCase(email)) {
                    sendResponse(exchange, 400, "{\"error\":\"User already exists\"}");
                    return;
                }
            }

            JSONObject newUser = new JSONObject();
            newUser.put("email", email);
            newUser.put("password", password);
            newUser.put("firstName", firstName);
            newUser.put("lastName", lastName);
            newUser.put("role", body.optString("role", "Official"));
            newUser.put("department", body.optString("department", "Education"));

            users.put(newUser);
            writeJSONFile(USER_FILE, users);

            sendResponse(exchange, 200, "{\"message\":\"User registered successfully\"}");
        }
    }

    static class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            JSONObject body = new JSONObject(readBody(exchange.getRequestBody()));
            String email = body.optString("email");
            String password = body.optString("password");

            JSONArray users = readJSONFile(USER_FILE);
            for (Object obj : users) {
                JSONObject user = (JSONObject) obj;
                if (user.getString("email").equalsIgnoreCase(email)
                        && user.getString("password").equals(password)) {
                    sendResponse(exchange, 200, "{\"message\":\"Login successful\", \"user\":\"" + email + "\"}");
                    return;
                }
            }

            sendResponse(exchange, 401, "{\"error\":\"Invalid credentials\"}");
        }
    }

    static class DocumentUploadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            JSONObject body = new JSONObject(readBody(exchange.getRequestBody()));
            JSONArray docs = readJSONFile(DOC_FILE);

            JSONObject newDoc = new JSONObject();
            newDoc.put("title", body.optString("title"));
            newDoc.put("type", body.optString("type"));
            newDoc.put("tags", body.optString("tags", ""));
            newDoc.put("date", new Date().toString());
            newDoc.put("status", "Uploaded");

            docs.put(newDoc);
            writeJSONFile(DOC_FILE, docs);

            sendResponse(exchange, 200, "{\"message\":\"Document uploaded successfully\"}");
        }
    }

    static class GetDocumentsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            JSONArray docs = readJSONFile(DOC_FILE);
            sendResponse(exchange, 200, docs.toString());
        }
    }

    static class DefaultHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String msg = "{\"message\": \"EduAI JSON Backend Running\"}";
            sendResponse(exchange, 200, msg);
        }
    }
}
