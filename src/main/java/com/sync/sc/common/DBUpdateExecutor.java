package com.sync.sc.common;

import com.sync.sc.entity.Config;
import com.sync.sc.service.ClientService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.sql.*;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@Component
public class DBUpdateExecutor {
    private static final Logger log = Logger.getLogger(DBUpdateExecutor.class.getName());
    @Value("${app.version}")
    private String appVersion;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private ClientService clientService;

    /**
     * 执行 DB_update.xml 文件
     * @param xmlPath 文件路径，例如 "src/main/resources/db/DB_update.xml"
     */
    @Transactional
    public void executeDBUpdate(int type,String xmlPath) {
        try {
            int appV = -1;
            if(isTableExist("config")){
                appV = Integer.parseInt(appVersion.replaceAll("\\.",""));
                String sql = "SELECT val FROM config WHERE type = 'version'  LIMIT 1";
                List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
                if (!rows.isEmpty()) {
                    Map<String, Object> row = rows.get(0);
                    int val = Integer.parseInt(((String) row.get("val")).replaceAll("\\.", ""));
                    if (appV <= val) {
                        return;
                    }
                }
            }
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(xmlPath);
            if (inputStream == null) {
                log.warning(xmlPath+"没有找到");
                return;
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            Document doc = factory.newDocumentBuilder().parse(inputStream);
            inputStream.close();
            NodeList updates = doc.getElementsByTagName("update");

            for (int i = 0; i < updates.getLength(); i++) {
                Element update = (Element) updates.item(i);
                if(0 == type){
                    int tableVersion = Integer.parseInt(update.getAttribute("version"));
                    if(tableVersion < appV){
                        continue;
                    }
                }
                String tableName = update.getAttribute("table");
                String sqlText = update.getTextContent().trim();

                // 检查表是否存在
                if (!isTableExist(tableName) && !sqlText.toLowerCase().startsWith("create ")) {
                    System.out.println("表 " + tableName + " 不存在，跳过更新");
                    continue;
                }

                // 按行拆分 SQL（支持多条 ALTER 语句）
                String[] sqls = sqlText.split(";");
                for (String sql : sqls) {
                    sql = sql.trim();
                    if (sql.isEmpty()) continue;
                    // 处理 ALTER TABLE ADD COLUMN
                    if (sql.toLowerCase().startsWith("alter table") && sql.toLowerCase().contains("add column")) {
                        String columnName = parseColumnName(sql);
                        if (columnName != null && isColumnExist(tableName, columnName)) {
                            System.out.println("字段 " + columnName + " 已存在，跳过");
                            continue;
                        }
                    }

                    // 执行 SQL
                    try {
                        jdbcTemplate.execute(sql);
                        System.out.println("执行成功: " + sql);
                    } catch (Exception e) {
                        System.err.println("执行失败: " + sql);
                        e.printStackTrace();
                    }
                }
            }
            Config config =new Config();
            config.setType("version");
            config.setVal(appVersion);
            clientService.updateConfigData(config);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 判断表是否存在
    public boolean isTableExist(String tableName) throws SQLException {
        try (Connection conn = jdbcTemplate.getDataSource().getConnection();
             ResultSet rs = conn.getMetaData().getTables(null, null, tableName, new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    // 判断字段是否存在
    private boolean isColumnExist(String tableName, String columnName) throws SQLException {
        try (Connection conn = jdbcTemplate.getDataSource().getConnection();
             ResultSet rs = conn.getMetaData().getColumns(null, null, tableName, columnName)) {
            return rs.next();
        }
    }

    // 解析 ALTER TABLE ADD COLUMN 语句的列名
    private String parseColumnName(String sql) {
        sql = sql.trim().replaceAll("\\s+", " "); // 压缩空格
        sql = sql.toLowerCase();
        int idx = sql.indexOf("add column");
        if (idx < 0) return null;
        String colDef = sql.substring(idx + 10).trim();
        String[] parts = colDef.split(" ");
        return parts.length > 0 ? parts[0] : null;
    }
}
