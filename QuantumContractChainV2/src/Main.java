import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

class Block {
    public int index;
    public String owner;
    public String data;
    public String timestamp;
    public String hash;
    public String previousHash;
    public String state;
    public String postQuantumSignature;
    public String quantumKey;
    public String entangledBlockHash;

    public Block(int index, String owner, String data, String previousHash, String entangledBlockHash) {
        this.index = index;
        this.owner = owner;
        this.data = data;
        this.timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        this.previousHash = previousHash != null ? previousHash : "null";
        this.state = "pending";
        this.quantumKey = generateQuantumKey();
        this.entangledBlockHash = entangledBlockHash != null ? entangledBlockHash : "null";
        this.postQuantumSignature = generatePostQuantumSignature();
        this.hash = generateQuantumHash();
    }

    private String generateQuantumHash() {
        String raw = index + owner + data + timestamp + previousHash + quantumKey + postQuantumSignature + entangledBlockHash;
        String base = Integer.toHexString(raw.hashCode());
        return applyQuantumNoise(base);
    }

    private String applyQuantumNoise(String hash) {
        Random rand = new Random();
        if (rand.nextInt(10) == 0) {
            String noise = Integer.toHexString(rand.nextInt(256));
            if (hash.length() > 4) {
                return hash.substring(0, hash.length() - 4) + noise;
            } else {
                return hash + noise;
            }
        }
        return hash;
    }

    private String generateQuantumKey() {
        Random rand = new Random();
        return Integer.toHexString(rand.nextInt(Integer.MAX_VALUE));
    }

    private String generatePostQuantumSignature() {
        Random rand = new Random();
        return Integer.toHexString(rand.nextInt(Integer.MAX_VALUE));
    }

    public void collapseState(List<String> validatorVotes) {
        long activeVotes = validatorVotes.stream().filter(v -> v.equals("active")).count();
        long settledVotes = validatorVotes.stream().filter(v -> v.equals("settled")).count();
        this.state = activeVotes > settledVotes ? "active" : "settled";
    }
}

public class Main {
    private static List<Block> blockchain = new ArrayList<>();
    private static final Gson gson = new Gson();
    private static final String FILE_PATH = "blockchain.json";
    private static final String PASSWORD = "quantum123";

    public static void main(String[] args) throws IOException {
        loadBlockchain();

        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);

        // --- Home Page ---
        server.createContext("/", (exchange -> {
            long pendingCount = blockchain.stream().filter(b -> b.state.equals("pending")).count();
            long activeCount = blockchain.stream().filter(b -> b.state.equals("active")).count();
            long settledCount = blockchain.stream().filter(b -> b.state.equals("settled")).count();

            String statsHtml = "<div style='margin-top:20px;background:rgba(255,255,255,0.05);padding:15px;border-radius:15px;width:70%;margin:auto;color:#00ffff;'>" +
                    "<h3>🧮 Network Stats</h3>" +
                    "<p>Total Blocks: " + blockchain.size() + "</p>" +
                    "<p>Pending: " + pendingCount + " | Active: " + activeCount + " | Settled: " + settledCount + "</p>" +
                    "</div>";

            String html = """
                <html>
                <head>
                    <meta charset='UTF-8'>
                    <title>Quantum Contract Chain V2</title>
                    <style>
                        body {
                            font-family: 'Poppins', sans-serif;
                            background: linear-gradient(135deg, #0f2027, #203a43, #2c5364);
                            color: #fff;
                            text-align: center;
                            margin: 0;
                            padding: 0;
                        }
                        h1 {
                            font-size: 2.5rem;
                            margin-top: 40px;
                            color: #00e6e6;
                            text-shadow: 0 0 10px #00e6e6;
                        }
                        form {
                            margin-top: 40px;
                            background: rgba(255,255,255,0.1);
                            padding: 30px;
                            border-radius: 15px;
                            width: 400px;
                            margin: 20px auto;
                            box-shadow: 0 4px 20px rgba(0,0,0,0.4);
                        }
                        input[type='text'], input[type='password'] {
                            width: 80%;
                            padding: 10px;
                            border-radius: 10px;
                            border: none;
                            outline: none;
                            font-size: 1rem;
                            margin-bottom: 15px;
                        }
                        .glow-btn {
                            background: transparent;
                            color: #00ffff;
                            border: 2px solid #00ffff;
                            box-shadow: 0 0 15px #00ffff;
                            transition: 0.3s;
                            padding: 10px 20px;
                            border-radius: 10px;
                            cursor: pointer;
                            font-weight: bold;
                        }
                        .glow-btn:hover {
                            background: #00ffff;
                            color: #000;
                            transform: scale(1.05);
                            box-shadow: 0 0 30px #00ffff;
                        }
                        .footer {
                            margin-top: 40px;
                            color: #aaa;
                            font-size: 0.9rem;
                        }
                    </style>
                </head>
                <body>
                    <h1>⚛️ Quantum Contract Chain V2 ⚛️</h1>
                    """ + statsHtml + """
                    <form method='POST' action='/add'>
                        <input type='password' name='password' placeholder='Enter admin password...' required/><br>
                        <input type='text' name='owner' placeholder='Enter contract owner name...' required/><br>
                        <input type='text' name='data' placeholder='Enter contract details...' required/><br>
                        <button type='submit' class='glow-btn'>Add Contract</button>
                    </form>

                    <form method='POST' action='/chain'>
                        <input type='password' name='password' placeholder='Enter admin password...' required/><br>
                        <button type='submit' class='glow-btn'>View Blockchain</button>
                    </form>

                    <div class='footer'> Quantum Contract Chain V2 | Post-Quantum Secured ⚡</div>
                </body>
                </html>
            """;
            sendResponse(exchange, html);
        }));

        // --- Add Block ---
        server.createContext("/add", (exchange -> {
            if ("POST".equals(exchange.getRequestMethod())) {
                String formData = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                Map<String, String> params = parseFormData(formData);
                String password = params.get("password");
                String owner = params.get("owner");
                String data = params.get("data");

                if (!PASSWORD.equals(password)) {
                    sendResponse(exchange, "<script>alert('❌ Invalid Password'); window.location='/'</script>");
                    return;
                }

                addBlock(owner, data);
                saveBlockchain();
                sendResponse(exchange, "<script>alert('✅ Block Added Successfully!'); window.location='/'</script>");
            }
        }));

        // --- View Blockchain ---
        server.createContext("/chain", (exchange -> sendResponse(exchange, buildBlockchainPage())));

        // --- Collapse, Delete, Update (same as before) ---
        server.createContext("/collapse", exchange -> handleCollapse(exchange));
        server.createContext("/delete", exchange -> handleDelete(exchange));
        server.createContext("/update", exchange -> handleUpdate(exchange));

        server.start();
        System.out.println("🚀 Server running on http://localhost:8000");
    }

    private static String buildBlockchainPage() {
        StringBuilder html = new StringBuilder("<html><head><meta charset='UTF-8'><title>Blockchain</title></head><body style='background:linear-gradient(135deg,#000428,#004e92);color:white;text-align:center;font-family:Poppins;'>");
        html.append("<h1 style='color:#00ffff;'>🔗 Blockchain Explorer</h1>");
        for (Block b : blockchain) {
            html.append("<div style='background:rgba(255,255,255,0.1);border:1px solid #00ffff;padding:20px;margin:20px auto;width:70%;border-radius:15px;'>")
                    .append("<h2 style='color:#00ffff;'>Block #").append(b.index).append("</h2>")
                    .append("<p><b>👤 Owner:</b> ").append(b.owner).append("</p>")
                    .append("<p><b>📜 Contract:</b> ").append(b.data).append("</p>")
                    .append("<p><b>🕒 Timestamp:</b> ").append(b.timestamp).append("</p>")
                    .append("<p><b>🔑 Hash:</b> ").append(b.hash).append("</p>")
                    .append("<p><b>🔗 Previous Hash:</b> ").append(b.previousHash).append("</p>")
                    .append("<p><b>⚡ State:</b> ").append(b.state.toUpperCase()).append("</p>")
                    .append("<form method='POST' action='/collapse' style='display:inline-block;margin-right:10px;'><input type='hidden' name='index' value='").append(b.index).append("'><button class='glow-btn'>Collapse ⚛️</button></form>")
                    .append("<form method='POST' action='/delete' style='display:inline-block;margin-right:10px;'><input type='hidden' name='index' value='").append(b.index).append("'><button class='glow-btn' style='border-color:red;color:red;box-shadow:0 0 15px red;'>Delete 🗑️</button></form>")
                    .append("<form method='POST' action='/update' style='display:inline-block;'><input type='hidden' name='index' value='").append(b.index).append("'><input type='text' name='owner' placeholder='New Owner' required><input type='text' name='data' placeholder='New Data' required><button class='glow-btn' style='border-color:#ffaa00;color:#ffaa00;box-shadow:0 0 15px #ffaa00;'>Update ✏️</button></form>")
                    .append("</div>");
        }
        html.append("<a href='/'><button class='glow-btn' style='margin-top:20px;'>⬅ Back</button></a></body></html>");
        return html.toString();
    }

    private static void handleCollapse(HttpExchange exchange) throws IOException {
        if ("POST".equals(exchange.getRequestMethod())) {
            String formData = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> params = parseFormData(formData);
            try {
                int index = Integer.parseInt(params.getOrDefault("index", "-1"));
                if (index >= 0 && index < blockchain.size()) {
                    List<String> votes = new ArrayList<>();
                    Random rand = new Random();
                    for (int i = 0; i < 5; i++) votes.add(rand.nextBoolean() ? "active" : "settled");
                    blockchain.get(index).collapseState(votes);
                    saveBlockchain();
                }
            } catch (Exception ignored) {}
            exchange.getResponseHeaders().set("Location", "/chain");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        }
    }

    private static void handleDelete(HttpExchange exchange) throws IOException {
        if ("POST".equals(exchange.getRequestMethod())) {
            String formData = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> params = parseFormData(formData);
            try {
                int index = Integer.parseInt(params.get("index"));
                if (index >= 0 && index < blockchain.size()) {
                    blockchain.remove(index);
                    for (int i = 0; i < blockchain.size(); i++) blockchain.get(i).index = i;
                    saveBlockchain();
                }
            } catch (Exception ignored) {}
            exchange.getResponseHeaders().set("Location", "/chain");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        }
    }

    private static void handleUpdate(HttpExchange exchange) throws IOException {
        if ("POST".equals(exchange.getRequestMethod())) {
            String formData = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> params = parseFormData(formData);
            try {
                int index = Integer.parseInt(params.get("index"));
                String newOwner = params.get("owner");
                String newData = params.get("data");
                if (index >= 0 && index < blockchain.size()) {
                    Block b = blockchain.get(index);
                    b.owner = newOwner;
                    b.data = newData;
                    b.timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                    b.hash = Integer.toHexString((b.index + b.owner + b.data).hashCode());
                    saveBlockchain();
                }
            } catch (Exception ignored) {}
            exchange.getResponseHeaders().set("Location", "/chain");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        }
    }

    private static void addBlock(String owner, String data) {
        String prev = blockchain.isEmpty() ? null : blockchain.get(blockchain.size() - 1).hash;
        String entangle = blockchain.size() > 1 ? blockchain.get(blockchain.size() - 2).hash : null;
        blockchain.add(new Block(blockchain.size(), owner, data, prev, entangle));
    }

    private static void saveBlockchain() {
        try (FileWriter writer = new FileWriter(FILE_PATH)) {
            gson.toJson(blockchain, writer);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private static void loadBlockchain() {
        File file = new File(FILE_PATH);
        if (file.exists()) {
            try (Reader reader = new FileReader(file)) {
                Block[] blocks = gson.fromJson(reader, Block[].class);
                blockchain = new ArrayList<>(Arrays.asList(blocks));
            } catch (IOException e) { e.printStackTrace(); }
        } else {
            blockchain.add(new Block(0, "System", "Genesis Block", null, null));
            saveBlockchain();
        }
    }

    private static Map<String, String> parseFormData(String formData) {
        Map<String, String> params = new HashMap<>();
        for (String pair : formData.split("&")) {
            String[] kv = pair.split("=");
            if (kv.length == 2)
                params.put(kv[0], URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
        }
        return params;
    }

    private static void sendResponse(HttpExchange exchange, String response) throws IOException {
        exchange.sendResponseHeaders(200, response.getBytes().length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(response.getBytes()); }
    }
}
