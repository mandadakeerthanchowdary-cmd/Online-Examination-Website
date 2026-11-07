import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.Executors;

public class OnlineExamServer {
    private static final int PORT = 8090;
    private static final String[] QUESTIONS = {
        "Which language is used to structure the content of a web page?",
        "Which HTML tag is used to create a hyperlink?",
        "Which CSS property changes the text color?",
        "Which keyword declares a block-scoped variable in JavaScript?",
        "Which method converts a JSON string into a JavaScript object?"
    };
    private static final String[][] OPTIONS = {
        {"CSS", "HTML", "Java", "SQL"},
        {"<link>", "<a>", "<href>", "<p>"},
        {"font-style", "background", "color", "text-size"},
        {"var", "define", "let", "print"},
        {"JSON.stringify()", "JSON.parse()", "JSON.object()", "parse.JSON()"}
    };
    // Correct option indexes are kept on the server.
    private static final int[] ANSWERS = {1, 1, 2, 2, 1};

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/api/login", OnlineExamServer::login);
        server.createContext("/api/questions", OnlineExamServer::questions);
        server.createContext("/api/submit", OnlineExamServer::submit);
        server.createContext("/", OnlineExamServer::staticFiles);
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("Online Examination Website running at http://localhost:" + PORT);
    }

    private static void login(HttpExchange ex) throws IOException {
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
            send(ex, 405, "{\"message\":\"POST required\"}");
            return;
        }
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String name = jsonValue(body, "name");
        String studentId = jsonValue(body, "studentId");
        if (name.isBlank() || studentId.isBlank()) {
            send(ex, 400, "{\"message\":\"Please enter your name and student ID.\"}");
            return;
        }
        send(ex, 200, "{\"message\":\"Login successful\",\"name\":\"" + escape(name) +
                "\",\"studentId\":\"" + escape(studentId) + "\"}");
    }

    private static void questions(HttpExchange ex) throws IOException {
        StringBuilder out = new StringBuilder("{\"questions\":[");
        for (int i = 0; i < QUESTIONS.length; i++) {
            if (i > 0) out.append(",");
            out.append("{\"id\":").append(i)
               .append(",\"question\":\"").append(escape(QUESTIONS[i])).append("\",\"options\":[");
            for (int j = 0; j < OPTIONS[i].length; j++) {
                if (j > 0) out.append(",");
                out.append("\"").append(escape(OPTIONS[i][j])).append("\"");
            }
            out.append("]}");
        }
        out.append("]}");
        send(ex, 200, out.toString());
    }

    private static void submit(HttpExchange ex) throws IOException {
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
            send(ex, 405, "{\"message\":\"POST required\"}");
            return;
        }
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String raw = jsonValue(body, "answers");
        // The browser sends answers as a comma-separated string, e.g. "1,1,-1,2,1".
        String[] values = raw.split(",", -1);
        int score = 0;
        int attempted = 0;
        for (int i = 0; i < ANSWERS.length && i < values.length; i++) {
            try {
                int answer = Integer.parseInt(values[i].trim());
                if (answer >= 0) {
                    attempted++;
                    if (answer == ANSWERS[i]) score++;
                }
            } catch (NumberFormatException ignored) { }
        }
        send(ex, 200, "{\"score\":" + score + ",\"total\":" + ANSWERS.length +
                ",\"attempted\":" + attempted + ",\"percentage\":" +
                Math.round(score * 100.0 / ANSWERS.length) + "}");
    }

    private static String jsonValue(String json, String key) {
        String marker = "\"" + key + "\"";
        int start = json.indexOf(marker);
        if (start < 0) return "";
        start = json.indexOf(':', start + marker.length()) + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        if (start >= json.length()) return "";
        if (json.charAt(start) == '"') {
            int end = start + 1;
            while (end < json.length()) {
                if (json.charAt(end) == '"' && json.charAt(end - 1) != '\\') break;
                end++;
            }
            return json.substring(start + 1, Math.min(end, json.length())).replace("\\\"", "\"");
        }
        if (json.charAt(start) == '[') {
            int end = json.indexOf(']', start);
            return end < 0 ? "" : json.substring(start + 1, end).replaceAll("\\s+", "");
        }
        return "";
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\\n", " ").replace("\\r", " ");
    }

    private static void send(HttpExchange ex, int status, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    private static void staticFiles(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        if (path.equals("/")) path = "/index.html";
        Path root = Paths.get("public").toAbsolutePath().normalize();
        Path file = root.resolve(path.substring(1)).normalize();
        if (!file.startsWith(root) || !Files.exists(file) || Files.isDirectory(file)) {
            byte[] notFound = "404 - File not found".getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(404, notFound.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(notFound); }
            return;
        }
        String type = file.toString().endsWith(".css") ? "text/css" :
                file.toString().endsWith(".js") ? "application/javascript" : "text/html";
        byte[] bytes = Files.readAllBytes(file);
        ex.getResponseHeaders().set("Content-Type", type + "; charset=UTF-8");
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }
}
