package com.sync.sc.common;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class StartupRunner implements CommandLineRunner {

    private final DBUpdateExecutor executor;

    public StartupRunner(DBUpdateExecutor executor) {
        this.executor = executor;
    }

    @Override
    public void run(String... args) throws Exception {

        if(!executor.isTableExist("config")){
            executor.executeDBUpdate(1, "db/DB_install.xml");
        }
        executor.executeDBUpdate(0,"db/DB_update.xml");
    }
}

