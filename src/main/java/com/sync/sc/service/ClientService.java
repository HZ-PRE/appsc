package com.sync.sc.service;

import com.sync.sc.entity.Config;
import com.sync.sc.util.BaseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.logging.Logger;

/**
 * @author ld
 * @since 2024-06-13
 */
@Service
public class ClientService {
    private static final Logger log = Logger.getLogger(ClientService.class.getName());
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Transactional
    public void updateConfigData(Config config) {
        config.setUpdatedAt(BaseUtil.formatDate(new Date(),null));
        int count = jdbcTemplate.queryForObject("SELECT count(val) FROM config WHERE type = ?  LIMIT 1", new Object[]{config.getType()}, Integer.class);
        if(count > 0){
            String updateNameSql = "UPDATE config SET val = ?,updated_at = ? WHERE type = ?";
            jdbcTemplate.update(updateNameSql, config.getVal(),config.getUpdatedAt(),config.getType());
        }else {
            String updateNameSql = "INSERT INTO config(type,val,created_at,updated_at) VALUES (?,?,?,?)";
            jdbcTemplate.update(updateNameSql, config.getType(), config.getVal(),config.getUpdatedAt(),config.getUpdatedAt());
        }
    }
    public Map<String,Object> getConfigByTypes(String types) {
        Map<String,Object> ret = new HashMap<>();
        List<String> ids = Arrays.asList(types.split(","));
        String inSql = String.join(",", Collections.nCopies(ids.size(), "?"));
        String sql = "SELECT type,val FROM config WHERE type in (" + inSql + ")";
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql, ids.toArray());
        for(Map<String, Object> map : list){
            ret.put((String) map.get("type"),map.get("val"));
        }
        return ret;
    }
    public Map<String,Map<String, String>>  getConfigByOss() {
        Map<String,Map<String, String>> ret = new HashMap<>();
        String sql = "SELECT type,val FROM config WHERE type LIKE '%Oss%' AND val is not NULL AND val!='' AND type not LIKE '%RemoteDir'";
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
        String v="";
        String t="";
        for(Map<String, Object> map : list){
            t=(String) map.get("type");
            if(t.endsWith("BucketName")){
                v= "BucketName";
            }else if(t.contains("Endpoint")){
                v = "Endpoint";
            }else if(t.contains("AccessKeyId")){
                v = "AccessKeyId";
            }else if(t.contains("AccessKeySecret")){
                v = "AccessKeySecret";
            }else if(t.contains("PrivateKeyPath")){
                v = "PrivateKeyPath";
            }else {
                continue;
            }
            t=t.replace(v,"");
            if(!ret.containsKey(t)){
                ret.put(t,new HashMap<>());
            }
            ret.get(t).put(v,(String) map.get("val"));
        }
        return ret;
    }
}
