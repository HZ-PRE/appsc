package com.sync.sc.util;

import org.lionsoul.ip2region.xdb.Searcher;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class Ip2regionUtil {
    private static final Logger log = Logger.getLogger(Ip2regionUtil.class.getName());
    private static volatile Searcher searcher;

    private static Searcher getSearcher() {
        Searcher current = searcher;
        if (current != null) {
            return current;
        }
        synchronized (Ip2regionUtil.class) {
            if (searcher == null) {
                searcher = loadSearcher();
            }
            return searcher;
        }
    }

    private static Searcher loadSearcher() {
        Path userDb = Paths.get(System.getProperty("user.home"), "appsc", "data", "ip2region.xdb");
        Path appDb = Paths.get(System.getProperty("user.dir"), "data", "ip2region.xdb");
        try {
            Path dbPath = resolveDbPath(userDb, appDb);
            if (dbPath == null) {
                log.warning("ip2region.xdb not found");
                return null;
            }

            String dbFile = dbPath.toAbsolutePath().toString();
            try {
                // Load only vector index into heap; data blocks are read from file on demand.
                // This avoids OOM when ip2region.xdb is large and keeps lookup fast.
                byte[] vectorIndex = Searcher.loadVectorIndexFromFile(dbFile);
                return Searcher.newWithVectorIndex(dbFile, vectorIndex);
            } catch (IOException vectorError) {
                log.warning("failed to load ip2region vector index, fallback to file-only mode: " + vectorError.getMessage());
                return Searcher.newWithFileOnly(dbFile);
            }
        } catch (Exception e) {
            log.warning("failed to load ip2region.xdb: " + e.getMessage());
            return null;
        }
    }

    private static Path resolveDbPath(Path userDb, Path appDb) throws IOException {
        if (Files.exists(userDb)) {
            return userDb;
        }
        if (Files.exists(appDb)) {
            return appDb;
        }

        try (InputStream in = Ip2regionUtil.class.getClassLoader().getResourceAsStream("data/ip2region.xdb")) {
            if (in == null) {
                return null;
            }
            Files.createDirectories(userDb.getParent());
            Files.copy(in, userDb, StandardCopyOption.REPLACE_EXISTING);
            return userDb;
        }
    }

    public static Map<String, String> search(String ip) {
        Map<String, String> results = new HashMap<>();
        try {
            Searcher current = getSearcher();
            if (current == null) {
                return results;
            }
            String region = current.search(ip);
            if (!StringUtils.isEmpty(region)) {
                String[] ipInfoArr = region.split("\\|");
                if (ipInfoArr.length >= 5) {
                    results.put("country", "0".equals(ipInfoArr[0]) ? "" : ipInfoArr[0]);
                    results.put("continent", "0".equals(ipInfoArr[1]) ? "" : ipInfoArr[1]);
                    results.put("region", "0".equals(ipInfoArr[2]) ? "" : ipInfoArr[2]);
                    results.put("city", "0".equals(ipInfoArr[3]) ? "" : ipInfoArr[3]);
                    results.put("isp", "0".equals(ipInfoArr[4]) ? "" : ipInfoArr[4]);
                }
            }
        } catch (Exception e) {
            log.warning(e.getMessage());
        }
        return results;
    }
}
